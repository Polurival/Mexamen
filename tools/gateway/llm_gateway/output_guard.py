"""Output Guard: проверка ответа модели до отдачи пользователю.

Ловит:
  * SECRET        — «сгенерированные» моделью ключи/токены (заглушки вроде sk-xxxx и …EXAMPLE пропускаются);
  * SYSTEM_PROMPT — попытки вывести system prompt (длинное дословное совпадение + типовые фразы);
  * SUSPICIOUS_URL — IP-литералы, сокращатели ссылок, userinfo, punycode, javascript:/data:, «выгрузка» в query;
  * DANGEROUS_COMMAND — `curl | sh`, `rm -rf /`, reverse shell, fork-бомба и т. п. (текст и tool_use).

Режимы: block (по умолчанию) — подмена ответа предупреждением; redact — маскирует секреты/URL,
а при командах и утечке промпта всё равно блокирует; log — только журнал, ответ отдаётся как есть.
"""

from __future__ import annotations

import copy
import hashlib
import ipaddress
import re
from dataclasses import dataclass, field
from typing import Optional
from urllib.parse import urlparse

from .anthropic_wire import text_message
from .detectors import SECRET_KINDS, looks_like_placeholder, scan_text
from .input_guard import Slot, _collect_content

MODE_BLOCK = "block"
MODE_REDACT = "redact"
MODE_LOG = "log"
VALID_MODES = (MODE_BLOCK, MODE_REDACT, MODE_LOG)

SHINGLE_WORDS = 12

URL_RE = re.compile(r"""(?i)(?:https?|ftp)://[^\s<>"'`\]\)]+|(?:javascript|data):[^\s<>"'`\]\)]+""")
SHORTENERS = frozenset({"bit.ly", "tinyurl.com", "t.co", "goo.gl", "is.gd", "ow.ly", "rb.gy", "cutt.ly", "shorturl.at", "tiny.cc"})
SUSPICIOUS_TLDS = frozenset({"tk", "ml", "ga", "cf", "gq", "zip", "mov"})

COMMAND_PATTERNS: tuple[tuple[str, "re.Pattern[str]"], ...] = (
    ("pipe_to_shell", re.compile(r"(?i)\b(?:curl|wget)\b[^\n|;]*\|\s*(?:sudo\s+)?(?:ba|z|da|k)?sh\b")),
    ("pipe_to_interpreter", re.compile(r"(?i)\b(?:curl|wget)\b[^\n|;]*\|\s*(?:sudo\s+)?(?:python3?|perl|ruby|node)\b")),
    (
        "rm_rf_root",
        re.compile(
            r"\brm\s+(?:-\S+\s+)*-\S*[rR]\S*\s+(?:-\S+\s+)*(?:--no-preserve-root\s+)?"
            r"(?:/\*?|~/?\*?|\$HOME/?|\$\{HOME\}/?)(?=[\s;&|\"')]|$)"
        ),
    ),
    ("fork_bomb", re.compile(r":\(\)\s*\{\s*:\s*\|\s*:\s*&\s*\}\s*;\s*:")),
    ("reverse_shell", re.compile(r"(?i)/dev/tcp/|\bnc\b[^\n]*\s-e\s|\bbash\s+-i\b[^\n]*>&")),
    ("disk_wipe", re.compile(r"(?i)\bmkfs(?:\.\w+)?\b|\bdd\s+if=\S+\s+of=/dev/(?:sd|nvme|hd|vd)\w*|>\s*/dev/sd[a-z]")),
    ("chmod_777_root", re.compile(r"\bchmod\s+-R\s+0?777\s+/(?:\s|$)")),
    ("decode_to_shell", re.compile(r"(?i)\bbase64\s+(?:-d|--decode)\b[^\n|]*\|\s*(?:sudo\s+)?(?:ba|z)?sh\b")),
    ("eval_remote", re.compile(r"(?i)\beval\s*\$?\(?\s*[`$(]*\s*(?:curl|wget)\b")),
    ("powershell_encoded", re.compile(r"(?i)\bpowershell(?:\.exe)?\b[^\n]*\s-enc(?:odedcommand)?\s")),
)

LEAK_PHRASES: tuple["re.Pattern[str]", ...] = (
    re.compile(
        r"(?i)\b(?:here\s+is|here's|below\s+is|this\s+is)\s+(?:my|the)\s+(?:full\s+|complete\s+|entire\s+|exact\s+|original\s+)?"
        r"(?:system\s+prompt|system\s+message|initial\s+instructions|hidden\s+instructions)\s*[:\n]"
    ),
    re.compile(r"(?i)\bmy\s+(?:system\s+prompt|initial\s+instructions)\s+(?:is|are|says|reads)\b"),
    re.compile(r"(?i)вот\s+(?:мой|весь|полный)\s+(?:системный\s+промпт|системная\s+инструкция)"),
)


@dataclass
class OutputFinding:
    category: str  # SECRET | SYSTEM_PROMPT | SUSPICIOUS_URL | DANGEROUS_COMMAND
    detail: str
    location: str
    sample: str = field(default="", repr=False)
    length: int = 0
    via: str = "plain"
    span: Optional[tuple[int, int]] = None  # для маскирования (None — только блок)
    slot_index: int = -1

    @property
    def redactable(self) -> bool:
        return self.span is not None

    def to_log(self) -> dict:
        return {
            "category": self.category,
            "detail": self.detail,
            "via": self.via,
            "location": self.location,
            "length": self.length,
            "fingerprint": hashlib.sha256(self.sample.encode("utf-8", "replace")).hexdigest()[:10],
        }


@dataclass
class OutputResult:
    action: str  # pass | redacted | blocked | logged
    findings: list[OutputFinding]
    message: dict  # то, что уйдёт клиенту (оригинал, замаскированная копия или предупреждение)

    def categories(self) -> list[str]:
        return sorted({f.category for f in self.findings})


def _words(text: str) -> list[str]:
    return re.findall(r"\w+", text.lower())


def system_text_of(body: dict) -> str:
    system = body.get("system")
    if isinstance(system, str):
        return system
    if isinstance(system, list):
        return "\n".join(b.get("text", "") for b in system if isinstance(b, dict))
    return ""


def url_reason(url: str) -> Optional[str]:
    low = url.lower()
    if low.startswith(("javascript:", "data:")):
        return "dangerous_scheme"
    try:
        parsed = urlparse(url)
        host = (parsed.hostname or "").lower()
    except ValueError:
        return "malformed_url"
    if "@" in parsed.netloc:
        return "userinfo_in_url"
    try:
        ip = ipaddress.ip_address(host)
    except ValueError:
        ip = None
    if ip is not None:
        return None if (ip.is_private or ip.is_loopback or ip.is_link_local) else "public_ip_literal"
    if host in SHORTENERS:
        return "url_shortener"
    if "xn--" in host:
        return "punycode_host"
    if host.rsplit(".", 1)[-1] in SUSPICIOUS_TLDS:
        return "suspicious_tld"
    if len(parsed.query) >= 60 and re.search(r"[A-Za-z0-9+/=_\-]{40,}", parsed.query):
        return "long_query_payload"
    return None


class OutputGuard:
    def __init__(
        self,
        disabled: frozenset[str] = frozenset(),
        canary: str = "",
        shingle_words: int = SHINGLE_WORDS,
    ) -> None:
        self.disabled = disabled
        self.canary = canary
        self.shingle_words = shingle_words

    # ------------------------------------------------------------------ публичный API

    def inspect(self, message: dict, system_text: str = "", mode: str = MODE_BLOCK) -> OutputResult:
        if mode not in VALID_MODES:
            mode = MODE_BLOCK
        msg = copy.deepcopy(message)
        slots: list[Slot] = []
        _collect_content(msg.get("content", []), msg, "content", "content", False, slots)
        # tool_use.input попадает в slots через _collect_content; thinking пропускается.

        shingles = self._shingles(system_text)
        findings: list[OutputFinding] = []
        for idx, slot in enumerate(slots):
            is_text = slot.path.endswith(".text") or slot.path == "content"
            findings.extend(self._scan_slot(idx, slot, is_text, shingles))

        if not findings:
            return OutputResult("pass", [], message)
        if mode == MODE_LOG:
            return OutputResult("logged", findings, message)
        if mode == MODE_REDACT and all(f.redactable for f in findings):
            self._apply_redaction(slots, findings)
            return OutputResult("redacted", findings, msg)
        return OutputResult("blocked", findings, text_message(message, self._warning(findings)))

    # ------------------------------------------------------------------ детекция

    def _scan_slot(self, idx: int, slot: Slot, is_text: bool, shingles: set[str]) -> list[OutputFinding]:
        text = slot.text
        out: list[OutputFinding] = []

        for f in scan_text(text, disabled=self.disabled):
            if f.kind in SECRET_KINDS and not looks_like_placeholder(f.text):
                out.append(
                    OutputFinding("SECRET", f.kind, slot.path, f.text, f.length, f.via, (f.start, f.end), idx)
                )

        for m in URL_RE.finditer(text):
            url = m.group(0).rstrip(".,;:!?")
            reason = url_reason(url)
            if reason:
                out.append(
                    OutputFinding("SUSPICIOUS_URL", reason, slot.path, url, len(url), "plain", (m.start(), m.start() + len(url)), idx)
                )

        for name, pat in COMMAND_PATTERNS:
            m = pat.search(text)
            if m:
                out.append(OutputFinding("DANGEROUS_COMMAND", name, slot.path, m.group(0), len(m.group(0))))

        if is_text:
            out.extend(self._system_prompt_leak(slot, text, shingles))
        return out

    def _system_prompt_leak(self, slot: Slot, text: str, shingles: set[str]) -> list[OutputFinding]:
        found: list[OutputFinding] = []
        for pat in LEAK_PHRASES:
            m = pat.search(text)
            if m:
                found.append(OutputFinding("SYSTEM_PROMPT", "leak_phrase", slot.path, m.group(0), len(m.group(0))))
                break
        if self.canary and self.canary in text:
            found.append(OutputFinding("SYSTEM_PROMPT", "canary_echoed", slot.path, self.canary, len(self.canary)))
        if shingles:
            words = _words(text)
            n = self.shingle_words
            for i in range(max(0, len(words) - n + 1)):
                chunk = " ".join(words[i : i + n])
                if chunk in shingles:
                    found.append(OutputFinding("SYSTEM_PROMPT", "verbatim_overlap", slot.path, chunk, len(chunk)))
                    break
        return found

    def _shingles(self, system_text: str) -> set[str]:
        words = _words(system_text)
        n = self.shingle_words
        if len(words) < n:
            return set()
        return {" ".join(words[i : i + n]) for i in range(len(words) - n + 1)}

    # ------------------------------------------------------------------ реакция

    @staticmethod
    def _apply_redaction(slots: list[Slot], findings: list[OutputFinding]) -> None:
        by_slot: dict[int, list[OutputFinding]] = {}
        for f in findings:
            by_slot.setdefault(f.slot_index, []).append(f)
        for idx, items in by_slot.items():
            text = slots[idx].text
            for f in sorted(items, key=lambda x: x.span[0], reverse=True):  # type: ignore[index]
                s, e = f.span  # type: ignore[misc]
                label = f"[REDACTED_{f.detail}]" if f.category == "SECRET" else "[REDACTED_URL]"
                text = text[:s] + label + text[e:]
            slots[idx].set(text)

    @staticmethod
    def _warning(findings: list[OutputFinding]) -> str:
        what = ", ".join(sorted({f"{f.category}:{f.detail}" for f in findings}))
        return (
            "⚠️ [LLM Gateway] Ответ модели заблокирован Output Guard и не доставлен. "
            f"Причина: {what}. Подробности — в security_events.jsonl."
        )
