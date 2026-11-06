"""Input Guard: проверка входящего запроса (Anthropic Messages API) до отправки в LLM.

Режимы:
  block  — найден секрет → запрос НЕ уходит в LLM, клиент получает 400 с предупреждением;
  redact — секрет заменяется на [REDACTED_<KIND>], запрос уходит дальше.

Сканируются: `system`, текстовые блоки сообщений, `tool_result`, строки внутри `tool_use.input`.
Не трогаются (нельзя менять/не текст): thinking/redacted_thinking (подпись), image, document.
"""

from __future__ import annotations

import copy
import json
from dataclasses import dataclass, field
from typing import Any

from .detectors import Finding, redaction_label, scan_text

MODE_BLOCK = "block"
MODE_REDACT = "redact"
VALID_MODES = (MODE_BLOCK, MODE_REDACT)

_SKIP_BLOCK_TYPES = frozenset({"thinking", "redacted_thinking", "image", "document"})


@dataclass
class Slot:
    """Одна строка запроса, которую можно прочитать и заменить: container[key]."""

    container: Any
    key: Any
    path: str
    in_messages: bool
    role: str = ""

    @property
    def text(self) -> str:
        return self.container[self.key]

    def set(self, value: str) -> None:
        self.container[self.key] = value


@dataclass
class LocatedFinding:
    finding: Finding
    path: str

    def to_log(self) -> dict:
        return {**self.finding.to_log(), "location": self.path}


@dataclass
class InputResult:
    mode: str
    findings: list[LocatedFinding] = field(default_factory=list)
    body: dict = field(default_factory=dict)  # санитизированная копия (секретов в ней нет)

    @property
    def blocked(self) -> bool:
        return self.mode == MODE_BLOCK and bool(self.findings)

    @property
    def redacted(self) -> bool:
        return self.mode == MODE_REDACT and bool(self.findings)

    def kinds(self) -> list[str]:
        return sorted({lf.finding.kind for lf in self.findings})


def _walk_strings(node: Any, path: str, in_messages: bool, out: list[Slot]) -> None:
    if isinstance(node, dict):
        for k, v in node.items():
            if isinstance(v, str):
                out.append(Slot(node, k, f"{path}.{k}", in_messages))
            else:
                _walk_strings(v, f"{path}.{k}", in_messages, out)
    elif isinstance(node, list):
        for i, v in enumerate(node):
            if isinstance(v, str):
                out.append(Slot(node, i, f"{path}[{i}]", in_messages))
            else:
                _walk_strings(v, f"{path}[{i}]", in_messages, out)


def _collect_content(content: Any, container: Any, key: Any, path: str, in_messages: bool, out: list[Slot]) -> None:
    if isinstance(content, str):
        out.append(Slot(container, key, path, in_messages))
        return
    if not isinstance(content, list):
        return
    for i, block in enumerate(content):
        bpath = f"{path}[{i}]"
        if isinstance(block, str):
            out.append(Slot(content, i, bpath, in_messages))
            continue
        if not isinstance(block, dict):
            continue
        btype = block.get("type")
        if btype in _SKIP_BLOCK_TYPES:
            continue
        if btype == "tool_use":
            _walk_strings(block.get("input"), f"{bpath}.input", in_messages, out)
        elif btype == "tool_result":
            _collect_content(block.get("content"), block, "content", f"{bpath}.content", in_messages, out)
        elif isinstance(block.get("text"), str):
            out.append(Slot(block, "text", f"{bpath}.text", in_messages))


def collect_slots(body: dict) -> list[Slot]:
    slots: list[Slot] = []
    if "system" in body:
        _collect_content(body["system"], body, "system", "system", False, slots)
    messages = body.get("messages")
    if isinstance(messages, list):
        for i, msg in enumerate(messages):
            if isinstance(msg, dict) and "content" in msg:
                before = len(slots)
                _collect_content(msg["content"], msg, "content", f"messages[{i}].content", True, slots)
                for slot in slots[before:]:
                    slot.role = str(msg.get("role", ""))
    return slots


class InputGuard:
    def __init__(self, disabled: frozenset[str] = frozenset(), email_allow: tuple[str, ...] = ()) -> None:
        self.disabled = disabled
        self.email_allow = email_allow

    def inspect(self, body: dict, mode: str = MODE_REDACT) -> InputResult:
        if mode not in VALID_MODES:
            mode = MODE_BLOCK  # неизвестный режим → безопасный вариант
        clean = copy.deepcopy(body)
        slots = collect_slots(clean)
        result = InputResult(mode=mode, body=clean)

        # slot index -> список (start, end, replacement)
        edits: dict[int, list[tuple[int, int, str]]] = {}

        for idx, slot in enumerate(slots):
            for f in scan_text(slot.text, disabled=self.disabled, email_allow=self.email_allow):
                result.findings.append(LocatedFinding(f, slot.path))
                edits.setdefault(idx, []).append((f.start, f.end, redaction_label(f.kind)))

        self._scan_split_across_messages(slots, result, edits)

        for idx, items in edits.items():
            text = slots[idx].text
            for s, e, repl in sorted(items, key=lambda t: t[0], reverse=True):
                text = text[:s] + repl + text[e:]
            slots[idx].set(text)
        return result

    def _scan_split_across_messages(
        self, slots: list[Slot], result: InputResult, edits: dict[int, list[tuple[int, int, str]]]
    ) -> None:
        """Секрет, разрезанный между репликами пользователя («sk-» | «proj-abc»), в т. ч. через ответ ассистента."""
        msg_slots = [(i, s) for i, s in enumerate(slots) if s.in_messages and s.role == "user" and s.text]
        if len(msg_slots) < 2:
            return
        offsets: list[tuple[int, int, int]] = []  # (slot idx, start, end) в склеенном тексте
        parts: list[str] = []
        pos = 0
        for i, s in msg_slots:
            t = s.text
            offsets.append((i, pos, pos + len(t)))
            parts.append(t)
            pos += len(t)
        joined = "".join(parts)
        for f in scan_text(joined, disabled=self.disabled, cache=False, email_allow=self.email_allow):
            touched = [(i, s0, e0) for i, s0, e0 in offsets if s0 < f.end and f.start < e0]
            if len(touched) < 2:
                continue  # внутри одной строки — уже поймано обычным проходом
            for n, (i, s0, e0) in enumerate(touched):
                local_s = max(f.start, s0) - s0
                local_e = min(f.end, e0) - s0
                edits.setdefault(i, []).append((local_s, local_e, redaction_label(f.kind) if n == 0 else ""))
            first_idx = touched[0][0]
            result.findings.append(
                LocatedFinding(Finding(f.kind, f.start, f.end, "split", f.text), slots[first_idx].path)
            )


def blocked_error_body(result: InputResult, request_id: str) -> dict:
    """Тело ответа 400 в формате ошибок Anthropic API."""
    kinds = ", ".join(result.kinds())
    return {
        "type": "error",
        "error": {
            "type": "invalid_request_error",
            "message": (
                f"[LLM Gateway] Запрос заблокирован Input Guard: во входящем промпте найдены чувствительные данные "
                f"({kinds}). В LLM ничего не отправлено. Удалите секреты или включите режим redact. "
                f"request_id={request_id}"
            ),
        },
    }


def dumps(body: dict) -> bytes:
    return json.dumps(body, ensure_ascii=False, separators=(",", ":")).encode("utf-8")
