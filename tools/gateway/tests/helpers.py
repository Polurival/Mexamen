"""Общие хелперы тестов: сборка запросов, фейковый upstream (без сети и без расходов), временные логи."""

from __future__ import annotations

import json
import os
import shutil
import tempfile
from typing import Optional

from llm_gateway.anthropic_wire import message_to_sse
from llm_gateway.config import Config
from llm_gateway.gateway import Gateway, GatewayRequest

from .cases import message_with


def build_body(user_texts: list[str], stream: bool = False, system: Optional[str] = None, model: str = "claude-sonnet-5-5") -> dict:
    """Диалог: user, assistant «Принято.», user, … (как в настоящем чате)."""
    messages: list[dict] = []
    for i, text in enumerate(user_texts):
        if i:
            messages.append({"role": "assistant", "content": "Принято."})
        messages.append({"role": "user", "content": text})
    body: dict = {"model": model, "max_tokens": 256, "stream": stream, "messages": messages}
    if system is not None:
        body["system"] = system
    return body


def make_request(body: dict, path: str = "/v1/messages", ip: str = "10.0.0.1", headers: Optional[dict] = None) -> GatewayRequest:
    h = {"content-type": "application/json", "x-api-key": "SECRET-UPSTREAM-AUTH", "anthropic-version": "2023-06-01"}
    h.update(headers or {})
    return GatewayRequest("POST", path, h, json.dumps(body, ensure_ascii=False).encode(), ip)


class FakeUpstream:
    """Возвращает заранее заданное сообщение в JSON или SSE — в зависимости от поля stream запроса."""

    def __init__(self, message: Optional[dict] = None, status: int = 200, raise_exc: Optional[Exception] = None) -> None:
        self.message = message or message_with([{"type": "text", "text": "Hola, esta es la respuesta."}])
        self.status = status
        self.raise_exc = raise_exc
        self.calls: list[dict] = []

    def __call__(self, method: str, path: str, headers: dict, body: bytes):
        self.calls.append({"method": method, "path": path, "headers": headers, "body": body})
        if self.raise_exc:
            raise self.raise_exc
        if path.startswith("/v1/messages/count_tokens"):
            return 200, {"content-type": "application/json"}, b'{"input_tokens": 42}'
        if self.status != 200:
            return self.status, {"content-type": "application/json"}, b'{"type":"error","error":{"type":"overloaded_error","message":"x"}}'
        wants_stream = False
        try:
            wants_stream = bool(json.loads(body).get("stream"))
        except ValueError:
            pass
        if wants_stream:
            return 200, {"content-type": "text/event-stream"}, message_to_sse(self.message)
        return 200, {"content-type": "application/json"}, json.dumps(self.message).encode()

    @property
    def last_body(self) -> dict:
        return json.loads(self.calls[-1]["body"])


class TempLogs:
    def __enter__(self) -> str:
        self.path = tempfile.mkdtemp(prefix="gw-logs-")
        return self.path

    def __exit__(self, *exc) -> None:
        shutil.rmtree(self.path, ignore_errors=True)


def make_gateway(upstream: FakeUpstream, log_dir: str, **overrides) -> Gateway:
    cfg = Config(**{"log_dir": log_dir, "rate_limit_per_min": 1000, **overrides})
    return Gateway(cfg, upstream)


def read_jsonl(path: str) -> list[dict]:
    if not os.path.exists(path):
        return []
    with open(path, encoding="utf-8") as fh:
        return [json.loads(line) for line in fh if line.strip()]


def all_log_text(log_dir: str) -> str:
    out = []
    for name in os.listdir(log_dir):
        with open(os.path.join(log_dir, name), encoding="utf-8") as fh:
            out.append(fh.read())
    return "\n".join(out)
