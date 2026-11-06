"""Security review: схема ответа, разбор, решение (вернуть на генерацию / предупредить / коммит), формат фидбека."""

from __future__ import annotations

import json
import re
from dataclasses import dataclass, field
from typing import Optional

SEVERITIES = ("critical", "high", "medium", "low")
BLOCKING = frozenset({"critical", "high"})

REVIEW_SCHEMA = {
    "type": "object",
    "properties": {
        "summary": {"type": "string"},
        "findings": {
            "type": "array",
            "items": {
                "type": "object",
                "properties": {
                    "severity": {"type": "string", "enum": list(SEVERITIES)},
                    "category": {"type": "string"},
                    "file": {"type": "string"},
                    "line": {"type": "integer"},
                    "title": {"type": "string"},
                    "explanation": {"type": "string"},
                    "fix": {"type": "string"},
                },
                "required": ["severity", "category", "file", "line", "title", "explanation", "fix"],
            },
        },
    },
    "required": ["summary", "findings"],
}


@dataclass
class ReviewFinding:
    severity: str
    category: str
    file: str
    line: int
    title: str
    explanation: str = ""
    fix: str = ""
    source: str = "llm"  # llm | gateway

    @property
    def blocking(self) -> bool:
        return self.severity in BLOCKING

    def to_dict(self) -> dict:
        return {
            "severity": self.severity,
            "category": self.category,
            "file": self.file,
            "line": self.line,
            "title": self.title,
            "explanation": self.explanation,
            "fix": self.fix,
            "source": self.source,
        }


@dataclass
class ReviewResult:
    summary: str
    findings: list[ReviewFinding] = field(default_factory=list)


class ReviewParseError(ValueError):
    pass


def _normalize_severity(value: object) -> str:
    sev = str(value).strip().lower()
    if sev in SEVERITIES:
        return sev
    if sev in ("blocker", "severe", "crit"):
        return "critical"
    if sev in ("moderate", "med"):
        return "medium"
    if sev in ("info", "minor", "informational"):
        return "low"
    # Неизвестная серьёзность трактуется как High: безопаснее вернуть на доработку, чем пропустить.
    return "high"


def _extract_json(text: str) -> dict:
    text = text.strip()
    fenced = re.search(r"```(?:json)?\s*(\{[\s\S]*\})\s*```", text)
    if fenced:
        text = fenced.group(1)
    elif not text.startswith("{"):
        start, end = text.find("{"), text.rfind("}")
        if start == -1 or end <= start:
            raise ReviewParseError("в ответе ревьюера нет JSON")
        text = text[start : end + 1]
    try:
        data = json.loads(text)
    except json.JSONDecodeError as exc:
        raise ReviewParseError(f"ревьюер вернул невалидный JSON: {exc}") from exc
    if not isinstance(data, dict):
        raise ReviewParseError("ожидался JSON-объект")
    return data


def parse_review(payload: object) -> ReviewResult:
    """payload — structured_output (dict) или текст ответа. Невалидное → ReviewParseError (fail-closed)."""
    data = payload if isinstance(payload, dict) else _extract_json(str(payload))
    raw = data.get("findings")
    if not isinstance(raw, list):
        raise ReviewParseError("в ответе нет списка findings")
    findings: list[ReviewFinding] = []
    for item in raw:
        if not isinstance(item, dict):
            raise ReviewParseError("элемент findings не объект")
        try:
            line = int(item.get("line", 0))
        except (TypeError, ValueError):
            line = 0
        findings.append(
            ReviewFinding(
                severity=_normalize_severity(item.get("severity")),
                category=str(item.get("category", "")),
                file=str(item.get("file", "")),
                line=line,
                title=str(item.get("title", "")),
                explanation=str(item.get("explanation", "")),
                fix=str(item.get("fix", "")),
            )
        )
    findings.sort(key=lambda f: SEVERITIES.index(f.severity))
    return ReviewResult(str(data.get("summary", "")), findings)


@dataclass
class Decision:
    action: str  # fix | commit_with_warnings | commit_clean
    blocking: list[ReviewFinding]
    warnings: list[ReviewFinding]


def decide(findings: list[ReviewFinding]) -> Decision:
    """Critical/High → вернуть на генерацию; только Medium/Low → предупреждение и коммит; пусто → коммит."""
    blocking = [f for f in findings if f.blocking]
    warnings = [f for f in findings if not f.blocking]
    if blocking:
        return Decision("fix", blocking, warnings)
    if warnings:
        return Decision("commit_with_warnings", [], warnings)
    return Decision("commit_clean", [], [])


def security_feedback(blocking: list[ReviewFinding]) -> str:
    """Фидбек генератору: «исправь: SQL injection в строке 42»."""
    lines = ["Security review не пропустил изменения. Исправь:"]
    for n, f in enumerate(blocking, 1):
        where = f"{f.file}:{f.line}" if f.line else f.file
        lines.append(f"{n}. [{f.severity.upper()}] {f.title} в {where} — {f.fix or f.explanation}")
    lines.append(
        "Исправь только перечисленное, не ломая тесты и правила CLAUDE.md; после правок убедись, что Gradle-проверки зелёные."
    )
    return "\n".join(lines)


def number_lines(text: str) -> str:
    return "\n".join(f"{i:>4}| {line}" for i, line in enumerate(text.split("\n"), 1))


CODE_SUFFIXES = (".kt", ".kts", ".xml", ".gradle", ".properties", ".json", ".yml", ".yaml")


def build_review_input(diff: str, files: dict[str, str], per_file_cap: int = 40_000, total_cap: int = 160_000) -> str:
    """Материал для ревьюера: diff + пронумерованные полные файлы (чтобы ссылки на строки были точными)."""
    parts = ["## DIFF (git diff --cached)\n```diff\n" + diff + "\n```\n", "## FILES (numbered, current content)\n"]
    used = sum(len(p) for p in parts)
    for path, content in files.items():
        body = number_lines(content[:per_file_cap])
        chunk = f"\n### {path}\n```\n{body}\n```\n"
        if used + len(chunk) > total_cap:
            parts.append(f"\n### {path}\n(пропущен: превышен лимит размера материала)\n")
            continue
        parts.append(chunk)
        used += len(chunk)
    return "".join(parts)


def line_of(text: str, offset: int) -> int:
    return text.count("\n", 0, offset) + 1


def gateway_block_findings(
    files: dict[str, str], kinds: Optional[list[str]] = None, scan=None
) -> list[ReviewFinding]:
    """Gateway заблокировал ревью-запрос из-за секрета в коде → ищем файл:строку своими детекторами.

    Пусто = в изменённых файлах ничего не нашлось: блокировка вызвана не кодом (например, контекстом клиента) —
    это сбой конфигурации, а не повод гонять генератор по кругу (цикл вернёт REVIEW_ERROR).
    """
    if scan is None:
        return []
    found: list[ReviewFinding] = []
    for path, content in files.items():
        for f in scan(content):
            if kinds and f.kind not in kinds and f.kind in ("EMAIL", "PHONE"):
                continue
            found.append(
                ReviewFinding(
                    severity="critical",
                    category="hardcoded-secret",
                    file=path,
                    line=line_of(content, f.start),
                    title=f"Секрет/PII ({f.kind}) в коде — запрос на ревью заблокирован LLM Gateway",
                    explanation="Input Guard шлюза нашёл чувствительные данные в изменённых файлах и не пропустил их в LLM.",
                    fix="Удали значение из исходников; для ключей — внешняя конфигурация/Keystore, не литерал в коде.",
                    source="gateway",
                )
            )
    return found
