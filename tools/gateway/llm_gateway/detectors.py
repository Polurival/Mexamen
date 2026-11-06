"""Детекторы секретов и PII: общий движок для Input Guard и Output Guard.

Три «взгляда» на текст:
  plain  — обычный regex-поиск;
  concat — текст с «склеенными» строковыми литералами ("sk-" + "proj-abc" -> sk-proj-abc);
  base64 — блоки, которые после декодирования содержат секрет (до 2 уровней вложенности).

Сами секреты наружу не уходят: в Finding хранится текст (нужен для маскирования),
но в логи попадает только `to_log()` — тип, длина, отпечаток sha256 и безопасное превью.
"""

from __future__ import annotations

import base64
import binascii
import fnmatch
import hashlib
import math
import re
from dataclasses import dataclass, field, replace
from functools import lru_cache
from typing import Callable, Iterable, Optional

# Типы, для которых безопасно показывать начало значения (префикс ключа не секрет).
PREVIEW_KINDS = frozenset({"API_KEY", "GITHUB_TOKEN", "AWS_ACCESS_KEY", "JWT"})

# Секреты (а не PII): именно их ищет Output Guard в ответах модели.
SECRET_KINDS = frozenset(
    {"API_KEY", "GITHUB_TOKEN", "AWS_ACCESS_KEY", "AWS_SECRET_KEY", "PRIVATE_KEY", "JWT", "SECRET"}
)


@dataclass(frozen=True)
class Finding:
    kind: str
    start: int
    end: int
    via: str = "plain"  # plain | concat | base64
    text: str = field(default="", repr=False, compare=False)

    @property
    def length(self) -> int:
        return self.end - self.start

    def fingerprint(self) -> str:
        return hashlib.sha256(self.text.encode("utf-8", "replace")).hexdigest()[:10]

    def preview(self) -> str:
        if self.kind in PREVIEW_KINDS and len(self.text) >= 12:
            return self.text[:4] + "…[" + str(len(self.text)) + " chars]"
        return "***[" + str(len(self.text)) + " chars]"

    def to_log(self) -> dict:
        return {
            "kind": self.kind,
            "via": self.via,
            "length": self.length,
            "fingerprint": self.fingerprint(),
            "preview": self.preview(),
        }


@dataclass(frozen=True)
class Detector:
    kind: str
    pattern: "re.Pattern[str]"
    group: int = 0
    validator: Optional[Callable[[str], bool]] = None


# --------------------------------------------------------------------------- валидаторы


def luhn_ok(digits: str) -> bool:
    total = 0
    for i, ch in enumerate(reversed(digits)):
        d = int(ch)
        if i % 2 == 1:
            d *= 2
            if d > 9:
                d -= 9
        total += d
    return total % 10 == 0


def _issuer_ok(d: str) -> bool:
    n = len(d)
    p2, p3, p4 = int(d[:2]), int(d[:3]), int(d[:4])
    if d[0] == "4":
        return n in (13, 16, 19)
    if 51 <= p2 <= 55 or 2221 <= p4 <= 2720:
        return n == 16
    if p2 in (34, 37):
        return n == 15
    if p4 == 6011 or p2 == 65 or 644 <= p3 <= 649:
        return n in (16, 19)
    if p2 in (36, 38, 39) or 300 <= p3 <= 305:
        return n == 14
    if p2 == 35 or p2 == 62:
        return 16 <= n <= 19
    return False


def _card_ok(text: str) -> bool:
    digits = re.sub(r"\D", "", text)
    if not 13 <= len(digits) <= 19 or len(set(digits)) == 1:
        return False
    return _issuer_ok(digits) and luhn_ok(digits)


def _phone_ok(text: str) -> bool:
    return 10 <= len(re.sub(r"\D", "", text)) <= 15


def _secret_value_ok(value: str) -> bool:
    """Значение присваивания `API_KEY=...` похоже на секрет, а не на выражение кода."""
    if len(value) < 12 or any(c in value for c in "(){}$<>"):
        return False
    return any(c.isdigit() for c in value) and any(c.isalpha() for c in value)


def shannon_entropy(s: str) -> float:
    if not s:
        return 0.0
    counts = {c: s.count(c) for c in set(s)}
    return -sum(n / len(s) * math.log2(n / len(s)) for n in counts.values())


# --------------------------------------------------------------------------- реестр

DETECTORS: tuple[Detector, ...] = (
    Detector(
        "PRIVATE_KEY",
        re.compile(
            r"-----BEGIN (?:[A-Z]+ )*PRIVATE KEY-----[\s\S]*?(?:-----END (?:[A-Z]+ )*PRIVATE KEY-----|\Z)"
        ),
    ),
    Detector(
        "API_KEY",
        re.compile(
            r"(?<![A-Za-z0-9])(?:"
            r"sk-[A-Za-z0-9_\-]{6,}"  # OpenAI / Anthropic (sk-proj-…, sk-ant-…)
            r"|gsk_[A-Za-z0-9]{20,}"  # Groq
            r"|AIza[0-9A-Za-z_\-]{35}"  # Google
            r"|xox[abprs]-[A-Za-z0-9\-]{10,}"  # Slack
            r"|(?:sk|rk|pk)_(?:live|test)_[A-Za-z0-9]{16,}"  # Stripe
            r")"
        ),
    ),
    Detector(
        "GITHUB_TOKEN",
        re.compile(r"(?<![A-Za-z0-9])(?:gh[pousr]_[A-Za-z0-9]{20,}|github_pat_[A-Za-z0-9_]{22,})"),
    ),
    Detector(
        "AWS_ACCESS_KEY",
        re.compile(r"(?<![A-Z0-9])(?:AKIA|ASIA|AGPA|AIDA|AROA)[0-9A-Z]{16}(?![A-Z0-9])"),
    ),
    Detector(
        "AWS_SECRET_KEY",
        re.compile(
            r"""(?i)aws_?secret_?(?:access_?)?key["']?\s*[:=]\s*["']?([A-Za-z0-9/+=]{40})(?![A-Za-z0-9/+=])"""
        ),
        group=1,
    ),
    Detector(
        "JWT",
        re.compile(r"(?<![A-Za-z0-9_\-])eyJ[A-Za-z0-9_\-]{8,}\.eyJ[A-Za-z0-9_\-]{8,}\.[A-Za-z0-9_\-]{8,}"),
    ),
    Detector(
        "SECRET",
        re.compile(
            r"""(?i)\b[A-Z0-9_]*(?:API_?KEY|SECRET|TOKEN|PASSWORD|PASSWD|PWD)[A-Z0-9_]*\s*[:=]\s*["']?([^\s"',;]{8,})"""
        ),
        group=1,
        validator=_secret_value_ok,
    ),
    Detector(
        "EMAIL",
        re.compile(
            r"(?<![A-Za-z0-9._%+\-])[A-Za-z0-9._%+\-]+@[A-Za-z0-9\-]+(?:\.[A-Za-z0-9\-]+)*\.[A-Za-z]{2,}"
        ),
    ),
    Detector(
        "CARD",
        # Нулевая ширина + группа: проверяем окно с каждого начала числа (не «съедаем» соседей).
        re.compile(r"(?<!\d)(?=((?:\d[ -]?){12,18}\d)(?!\d))"),
        group=1,
        validator=_card_ok,
    ),
    Detector("PHONE", re.compile(r"(?<![\w+])\+\d[\d\s().\-]{8,17}\d(?!\d)"), validator=_phone_ok),
    Detector(
        "PHONE",
        re.compile(r"(?<![\w.])(?:\(\d{3}\)\s?|\d{3}[\s.\-])\d{3}[\s.\-]\d{4}(?!\d)"),
        validator=_phone_ok,
    ),
    Detector("PHONE", re.compile(r"(?<![\w.])\d{2}[\s\-]\d{4}[\s\-]\d{4}(?!\d)"), validator=_phone_ok),
    Detector(
        "PHONE",
        re.compile(r"(?<![\w.])8[\s\-]\(?\d{3}\)?[\s\-]\d{3}[\s\-]?\d{2}[\s\-]?\d{2}(?!\d)"),
        validator=_phone_ok,
    ),
)

_CONCAT_RE = re.compile(r"""["'`]\s*(?:\+\s*)?["'`]""")
_B64_BLOB_RE = re.compile(r"(?<![A-Za-z0-9+/=_\-])[A-Za-z0-9+/_\-]{16,}={0,2}(?![A-Za-z0-9+/_\-])")


# --------------------------------------------------------------------------- движок


def email_allowed(email: str, allow: tuple[str, ...]) -> bool:
    """Адрес подпадает под allowlist (fnmatch без учёта регистра: `noreply@*`, `*@corp.io`, точный адрес)."""
    low = email.lower()
    return any(fnmatch.fnmatchcase(low, pattern.lower()) for pattern in allow)


def _scan_plain(text: str, disabled: frozenset[str], email_allow: tuple[str, ...] = ()) -> list[Finding]:
    found: list[Finding] = []
    for det in DETECTORS:
        if det.kind in disabled:
            continue
        for m in det.pattern.finditer(text):
            s, e = m.span(det.group)
            if s < 0 or e <= s:
                continue
            value = text[s:e]
            if det.validator is not None and not det.validator(value):
                continue
            if det.kind == "EMAIL" and email_allowed(value, email_allow):
                continue
            found.append(Finding(det.kind, s, e, "plain", value))
    return found


def _deconcat(text: str) -> Optional[tuple[str, list[int]]]:
    """Убирает «"  +  "» между литералами. Возвращает (текст, карта нового индекса -> исходный)."""
    matches = list(_CONCAT_RE.finditer(text))
    if not matches:
        return None
    out: list[str] = []
    idx_map: list[int] = []
    pos = 0
    for m in matches:
        out.append(text[pos : m.start()])
        idx_map.extend(range(pos, m.start()))
        pos = m.end()
    out.append(text[pos:])
    idx_map.extend(range(pos, len(text)))
    return "".join(out), idx_map


def _decode_b64(blob: str) -> Optional[str]:
    s = blob.replace("-", "+").replace("_", "/").rstrip("=")
    if len(s) % 4 == 1:
        return None
    try:
        raw = base64.b64decode(s + "=" * (-len(s) % 4), validate=True)
        decoded = raw.decode("utf-8")
    except (binascii.Error, UnicodeDecodeError, ValueError):
        return None
    printable = sum(1 for c in decoded if c.isprintable() or c in "\n\r\t")
    return decoded if decoded and printable / len(decoded) >= 0.9 else None


def _covered(f: Finding, others: Iterable[Finding]) -> bool:
    return any(o.start <= f.start and f.end <= o.end for o in others)


def _scan(text: str, depth: int, disabled: frozenset[str], email_allow: tuple[str, ...]) -> list[Finding]:
    findings = _scan_plain(text, disabled, email_allow)

    norm = _deconcat(text)
    if norm is not None:
        norm_text, idx_map = norm
        for f in _scan_plain(norm_text, disabled, email_allow):
            start, end = idx_map[f.start], idx_map[f.end - 1] + 1
            cand = Finding(f.kind, start, end, "concat", f.text)
            if not _covered(cand, findings):
                findings.append(cand)

    if depth > 0:
        for m in _B64_BLOB_RE.finditer(text):
            decoded = _decode_b64(m.group(0))
            if decoded is None:
                continue
            inner = _scan(decoded, depth - 1, disabled, email_allow)
            if inner:
                f = Finding(inner[0].kind, m.start(), m.end(), "base64", m.group(0))
                if not _covered(f, findings):
                    findings.append(f)
    return [replace(f, text=text[f.start : f.end]) for f in merge(findings)]


@lru_cache(maxsize=4096)
def _scan_cached(text: str, depth: int, disabled: frozenset[str], email_allow: tuple[str, ...]) -> tuple[Finding, ...]:
    return tuple(_scan(text, depth, disabled, email_allow))


def scan_text(
    text: str,
    depth: int = 2,
    disabled: frozenset[str] = frozenset(),
    cache: bool = True,
    email_allow: tuple[str, ...] = (),
) -> list[Finding]:
    """Находит секреты/PII в тексте. cache=False — для больших одноразовых строк."""
    if not text:
        return []
    if cache:
        return list(_scan_cached(text, depth, disabled, email_allow))
    return _scan(text, depth, disabled, email_allow)


def merge(findings: list[Finding]) -> list[Finding]:
    """Склеивает пересекающиеся находки; тип берётся у самой длинной."""
    ordered = sorted(findings, key=lambda f: (f.start, -f.length))
    merged: list[Finding] = []
    for f in ordered:
        if merged and f.start < merged[-1].end:
            last = merged[-1]
            if f.end > last.end:
                # Тип остаётся у первой (более длинной/ранней) находки.
                merged[-1] = Finding(last.kind, last.start, f.end, last.via)
            continue
        merged.append(f)
    return merged


def redaction_label(kind: str) -> str:
    return f"[REDACTED_{kind}]"


def redact(text: str, findings: list[Finding]) -> str:
    """Заменяет найденные диапазоны на [REDACTED_<KIND>]."""
    out = text
    for f in sorted(findings, key=lambda x: x.start, reverse=True):
        out = out[: f.start] + redaction_label(f.kind) + out[f.end :]
    return out


def looks_like_placeholder(value: str) -> bool:
    """Заглушка из документации/примера, а не «настоящий» ключ (для Output Guard)."""
    low = value.lower()
    markers = (
        "example", "xxx", "your", "placeholder", "redacted", "dummy", "fake", "sample",
        "<", "...", "***", "0000000", "abc123", "123456",
    )
    if any(mk in low for mk in markers):
        return True
    return shannon_entropy(re.sub(r"[-_]", "", value)) < 3.2
