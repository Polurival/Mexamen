"""Доказательная база: утечки (канарейки и реальные секреты), сводка по Gateway, статические проверки кода."""

from __future__ import annotations

import json
import os
import re
import sys
from collections import Counter
from typing import Iterable

_GATEWAY_DIR = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.abspath(__file__)))), "gateway")
if _GATEWAY_DIR not in sys.path:
    sys.path.insert(0, _GATEWAY_DIR)

from llm_gateway.detectors import scan_text  # noqa: E402  (переиспользуем детекторы шлюза из Дня 13)


DEFAULT_EMAIL_ALLOW = ("noreply@*", "no-reply@*")


def scan_secrets(text: str, email_allow: tuple = DEFAULT_EMAIL_ALLOW) -> list:
    return scan_text(text, cache=False, email_allow=email_allow)


# ------------------------------------------------------------------ утечки


def real_secret_values(repo_root: str) -> dict:
    """Реальные секреты проекта (local.properties, сервис-аккаунт) — ТОЛЬКО для поиска утечек, наружу не печатаются."""
    values: dict[str, str] = {}
    props = os.path.join(repo_root, "local.properties")
    if os.path.exists(props):
        with open(props, encoding="utf-8") as fh:
            for raw in fh:
                line = raw.strip().lstrip("#").strip()
                key, sep, val = line.partition("=")
                if sep and re.search(r"KEY|TOKEN|SECRET|PASSWORD", key, re.I) and len(val.strip()) >= 8:
                    values[f"local.properties:{key.strip()}"] = val.strip()
    for name in os.listdir(os.path.join(repo_root, "composeApp")) if os.path.isdir(os.path.join(repo_root, "composeApp")) else []:
        if name.endswith(".json") and name.startswith("mexamen-"):
            try:
                with open(os.path.join(repo_root, "composeApp", name), encoding="utf-8") as fh:
                    data = json.load(fh)
            except (OSError, ValueError):
                continue
            body = re.sub(r"-----[A-Z ]+-----|\\n|\s", "", str(data.get("private_key", "")))
            if len(body) >= 60:
                values["service-account:private_key"] = body[:60]
            if data.get("private_key_id"):
                values["service-account:private_key_id"] = str(data["private_key_id"])
    return values


def find_leaks(files: Iterable[str], secrets: dict) -> list:
    """[(файл, имя_секрета, число_вхождений)]. Сами значения не возвращаются."""
    hits = []
    for path in files:
        try:
            with open(path, encoding="utf-8", errors="ignore") as fh:
                text = fh.read()
        except OSError:
            continue
        for name, value in secrets.items():
            count = text.count(value)
            if count:
                hits.append((path, name, count))
    return hits


# ------------------------------------------------------------------ Gateway


def read_jsonl(path: str) -> list:
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8") as fh:
        return [json.loads(line) for line in fh if line.strip()]


def summarize_gateway(audit_path: str, events_path: str) -> dict:
    audit = [r for r in read_jsonl(audit_path) if r.get("path") == "/v1/messages"]
    events = read_jsonl(events_path)
    in_guard = Counter(r.get("input_guard") for r in audit)
    out_guard = Counter(r.get("output_guard") for r in audit if r.get("output_guard"))
    by_kind: Counter = Counter()
    unique: dict = {}
    for e in events:
        label = e.get("kind") or f"{e.get('category')}:{e.get('detail')}"
        by_kind[(e["direction"], e["action"], label)] += 1
        # История диалога пересылается в каждом запросе → одно и то же значение ловится снова и снова; уникальность — по отпечатку
        unique.setdefault((e["direction"], e["action"], label), set()).add(e.get("fingerprint"))
    return {
        "llm_requests": len(audit),
        "forwarded_clean": in_guard.get("pass", 0),
        "forwarded_redacted": in_guard.get("redacted", 0),
        "blocked_input": in_guard.get("blocked", 0),
        "output_pass": out_guard.get("pass", 0),
        "output_flagged": sum(v for k, v in out_guard.items() if k != "pass"),
        "events": [
            {"direction": d, "action": a, "type": t, "count": c, "unique_values": len(unique[(d, a, t)])}
            for (d, a, t), c in sorted(by_kind.items())
        ],
        "tokens_in": sum((r.get("usage") or {}).get("input_tokens", 0) for r in audit),
        "cost_usd": round(sum(((r.get("cost") or {}).get("usd") or 0) for r in audit), 4),
    }


# ------------------------------------------------------------------ статические проверки итогового кода

_STATIC_RULES = (
    ("cleartext-http", re.compile(r"""["']http://(?!localhost|127\.0\.0\.1|10\.0\.2\.2)[^"'\s]+""")),
    ("log-call", re.compile(r"\b(?:println|Log\.[dviwe]|Logger\.\w+)\s*\(")),
    ("ktor-verbose-logging", re.compile(r"LogLevel\.(?:ALL|BODY|HEADERS)")),
    ("plain-local-storage", re.compile(r"localStorage\.setItem|sessionStorage\.setItem")),
    ("plain-datastore-or-prefs", re.compile(r"stringPreferencesKey|getSharedPreferences|SharedPreferences")),
    ("cleartext-manifest", re.compile(r"usesCleartextTraffic\s*=\s*\"true\"|cleartextTrafficPermitted\s*=\s*\"true\"")),
    ("trust-all-tls", re.compile(r"TrustManager|HostnameVerifier|trustAll", re.I)),
    ("sensitive-header-in-code", re.compile(r"(?i)authorization|x-api-key|bearer\s")),
)


def static_checks(diff_text: str) -> list:
    """Независимый от LLM baseline: правила по ДОБАВЛЕННЫМ строкам diff. [(правило, файл, строка_diff)]"""
    hits = []
    current = ""
    for raw in diff_text.split("\n"):
        if raw.startswith("+++ b/"):
            current = raw[6:]
        elif raw.startswith("+") and not raw.startswith("+++"):
            line = raw[1:]
            for name, rx in _STATIC_RULES:
                if rx.search(line):
                    hits.append((name, current, line.strip()[:120]))
            for f in scan_secrets(line):
                hits.append((f"secret:{f.kind}", current, f"[{f.kind}, {f.length} chars]"))
    return hits
