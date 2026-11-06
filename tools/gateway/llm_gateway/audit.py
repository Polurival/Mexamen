"""Аудит-логи (JSONL).

audit.jsonl           — каждый запрос: метаданные, САНИТИЗИРОВАННЫЙ запрос и доставленный ответ, токены, стоимость.
security_events.jsonl — по строке на каждую перехваченную находку (тип, где, отпечаток, превью). Сырых секретов нет.

Заголовки авторизации (x-api-key, authorization) не логируются никогда.
"""

from __future__ import annotations

import json
import os
import threading
from datetime import datetime, timezone
from typing import Optional


def now_iso() -> str:
    return datetime.now(timezone.utc).isoformat(timespec="milliseconds")


class AuditLog:
    def __init__(self, directory: str, max_chars: int = 20000) -> None:
        self.directory = directory
        self.max_chars = max_chars
        self._lock = threading.Lock()
        os.makedirs(directory, exist_ok=True)
        self.audit_path = os.path.join(directory, "audit.jsonl")
        self.security_path = os.path.join(directory, "security_events.jsonl")

    def truncate(self, text: str) -> tuple[str, bool]:
        if self.max_chars > 0 and len(text) > self.max_chars:
            return text[: self.max_chars], True
        return text, False

    def _append(self, path: str, record: dict) -> None:
        line = json.dumps(record, ensure_ascii=False)
        with self._lock, open(path, "a", encoding="utf-8") as fh:
            fh.write(line + "\n")

    def request(self, record: dict) -> None:
        self._append(self.audit_path, {"ts": now_iso(), **record})

    def security_event(
        self,
        request_id: str,
        client_ip: str,
        direction: str,
        action: str,
        finding: dict,
        model: Optional[str] = None,
    ) -> None:
        self._append(
            self.security_path,
            {
                "ts": now_iso(),
                "request_id": request_id,
                "client_ip": client_ip,
                "direction": direction,  # input | output
                "action": action,  # blocked | redacted | logged
                "model": model,
                **finding,
            },
        )
