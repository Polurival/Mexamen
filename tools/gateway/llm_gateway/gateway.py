"""Ядро шлюза: rate limit → Input Guard → upstream → Output Guard → cost → аудит.

Не зависит от HTTP-сервера: принимает `GatewayRequest`, возвращает `GatewayResponse`;
вызов upstream инжектируется (в тестах — фейк без сети и без расходов).
"""

from __future__ import annotations

import json
import time
import uuid
from dataclasses import dataclass, field
from typing import Callable, Optional

from .anthropic_wire import message_from_events, message_to_sse, parse_sse
from .audit import AuditLog
from .config import Config
from .cost import CostTracker, Usage, compute_cost, load_prices
from .input_guard import MODE_BLOCK, VALID_MODES, InputGuard, blocked_error_body, dumps
from .output_guard import OutputGuard, system_text_of
from .rate_limit import RateLimiter

GUARDED_PATHS = frozenset({"/v1/messages", "/v1/messages/count_tokens"})

# (method, path, headers, body) -> (status, headers, body)
UpstreamFn = Callable[[str, str, dict, bytes], tuple]

# Чтение без тела не расходует лимит: служебные пробы клиента (`GET /`, `GET /json/version`) не должны съедать квоту LLM-запросов.
_RATE_EXEMPT_METHODS = frozenset({"GET", "HEAD", "OPTIONS"})

_HOP_HEADERS = frozenset(
    {"host", "content-length", "connection", "transfer-encoding", "accept-encoding", "content-encoding", "keep-alive"}
)


@dataclass
class GatewayRequest:
    method: str
    path: str
    headers: dict  # ключи в нижнем регистре
    body: bytes
    client_ip: str


@dataclass
class GatewayResponse:
    status: int
    headers: dict = field(default_factory=dict)
    body: bytes = b""


def _json_response(status: int, payload: dict, extra: Optional[dict] = None) -> GatewayResponse:
    headers = {"content-type": "application/json", **(extra or {})}
    return GatewayResponse(status, headers, json.dumps(payload, ensure_ascii=False).encode("utf-8"))


def _error(status: int, err_type: str, message: str, extra: Optional[dict] = None) -> GatewayResponse:
    return _json_response(status, {"type": "error", "error": {"type": err_type, "message": message}}, extra)


class Gateway:
    def __init__(self, config: Config, upstream: UpstreamFn, clock: Callable[[], float] = time.monotonic) -> None:
        self.config = config
        self.upstream = upstream
        self.input_guard = InputGuard(config.disabled_detectors, config.email_allowlist)
        self.output_guard = OutputGuard(config.disabled_detectors, config.canary)
        self.limiter = RateLimiter(config.rate_limit_per_min)
        self.audit = AuditLog(config.log_dir, config.log_max_chars)
        self.prices = load_prices(config.pricing_file)
        self.costs = CostTracker()
        self._clock = clock

    # ------------------------------------------------------------------ вход

    def handle(self, req: GatewayRequest) -> GatewayResponse:
        path = req.path.split("?", 1)[0]
        if path.startswith("/_gateway/"):
            return self._local(path)

        rid = uuid.uuid4().hex[:12]
        started = self._clock()
        rec: dict = {
            "request_id": rid,
            "client_ip": req.client_ip,
            "method": req.method,
            "path": path,
        }

        allowed, retry_after = (True, 0) if req.method in _RATE_EXEMPT_METHODS else self.limiter.check(req.client_ip)
        if not allowed:
            rec.update(action="rate_limited", status=429, retry_after_s=retry_after)
            self._finish(rec, started)
            return _error(
                429,
                "rate_limit_error",
                f"[LLM Gateway] Превышен лимит {self.config.rate_limit_per_min} запросов/мин с одного IP. "
                f"Повторите через {retry_after} с. request_id={rid}",
                {"retry-after": str(retry_after), "x-gateway-request-id": rid},
            )

        guarded = req.method == "POST" and path in GUARDED_PATHS
        if not guarded:
            return self._passthrough(req, rid, rec, started)
        return self._guarded(req, path, rid, rec, started)

    # ------------------------------------------------------------------ guarded POST

    def _guarded(self, req: GatewayRequest, path: str, rid: str, rec: dict, started: float) -> GatewayResponse:
        try:
            parsed = json.loads(req.body)
            if not isinstance(parsed, dict):
                raise ValueError("тело запроса должно быть JSON-объектом")
        except ValueError as exc:
            rec.update(action="bad_request", status=400, error=str(exc))
            self._finish(rec, started)
            return _error(400, "invalid_request_error", f"[LLM Gateway] Некорректный JSON: {exc}", {"x-gateway-request-id": rid})

        model = parsed.get("model") if isinstance(parsed.get("model"), str) else None
        stream = bool(parsed.get("stream"))
        rec.update(model=model, stream=stream)

        # Клиент может только УЖЕСТОЧИТЬ режим (redact -> block), но не ослабить.
        mode = self.config.input_mode if self.config.input_mode in VALID_MODES else MODE_BLOCK
        if req.headers.get("x-guard-mode", "").lower() == MODE_BLOCK:
            mode = MODE_BLOCK

        result = self.input_guard.inspect(parsed, mode)
        in_action = "blocked" if result.blocked else "redacted" if result.redacted else "pass"
        rec["input_mode"] = mode
        rec["input_guard"] = in_action
        rec["input_findings"] = [lf.to_log() for lf in result.findings]
        rec["request"], rec["request_truncated"] = self.audit.truncate(json.dumps(result.body, ensure_ascii=False))
        for lf in result.findings:
            self.audit.security_event(rid, req.client_ip, "input", in_action, lf.to_log(), model)

        if result.blocked:
            rec.update(action="blocked_input", status=400, cost={"usd": 0.0, "note": "в LLM не отправлено"})
            rec["estimated_input_tokens_not_sent"] = len(rec["request"]) // 4
            self._finish(rec, started)
            return _json_response(400, blocked_error_body(result, rid), {"x-gateway-request-id": rid, "x-guard-input": "blocked"})

        body = dumps(result.body) if result.findings else req.body
        status, up_headers, up_body = self._call_upstream(req, body, rid, rec, started)
        if status is None:
            return up_headers  # type: ignore[return-value]  # готовый 502-ответ

        out_headers = self._client_headers(up_headers, rid)
        out_headers["x-guard-input"] = in_action
        rec["status"] = status
        rec["action"] = "forwarded_redacted" if result.redacted else "forwarded"

        if path == "/v1/messages/count_tokens" or status != 200:
            self._log_plain_response(rec, up_body, status)
            self._finish(rec, started)
            return GatewayResponse(status, out_headers, up_body)

        return self._guard_response(req, parsed, stream, up_headers, up_body, out_headers, rid, rec, started, model)

    def _guard_response(
        self, req, parsed, stream, up_headers, up_body, out_headers, rid, rec, started, model
    ) -> GatewayResponse:
        message = self._parse_message(up_headers, up_body)
        if message is None:
            # Не смогли разобрать ответ → не можем проверить → не отдаём (fail-closed).
            rec.update(action="guard_error", status=502, error="не удалось разобрать ответ upstream для Output Guard")
            self._finish(rec, started)
            return _error(502, "api_error", f"[LLM Gateway] Output Guard не смог проверить ответ. request_id={rid}", {"x-gateway-request-id": rid})

        usage = Usage.from_dict(message.get("usage"))
        cost_model = message.get("model") or model or ""
        cost = compute_cost(cost_model, usage, self.prices)
        cumulative = self.costs.add(cost_model, usage, cost)
        rec["usage"] = usage.to_log()
        rec["cost"] = cost.to_log()
        rec["cumulative_usd"] = round(cumulative, 8)

        result = self.output_guard.inspect(message, system_text_of(parsed), self.config.output_mode)
        rec["output_guard"] = result.action
        rec["output_findings"] = [f.to_log() for f in result.findings]
        for f in result.findings:
            self.audit.security_event(rid, req.client_ip, "output", result.action, f.to_log(), cost_model)

        out_headers["x-guard-output"] = result.action
        if cost.usd is not None:
            out_headers["x-cost-usd"] = f"{cost.usd:.6f}"

        rec["response"], rec["response_truncated"] = self.audit.truncate(
            json.dumps(_loggable(result.message), ensure_ascii=False)
        )
        if result.action in ("blocked", "redacted"):
            rec["action"] = "blocked_output" if result.action == "blocked" else "redacted_output"
            if stream:
                payload, ctype = message_to_sse(result.message), "text/event-stream"
            else:
                payload, ctype = json.dumps(result.message, ensure_ascii=False).encode("utf-8"), "application/json"
            out_headers["content-type"] = ctype
            self._finish(rec, started)
            return GatewayResponse(200, out_headers, payload)

        self._finish(rec, started)
        return GatewayResponse(200, out_headers, up_body)

    # ------------------------------------------------------------------ вспомогательное

    def _passthrough(self, req: GatewayRequest, rid: str, rec: dict, started: float) -> GatewayResponse:
        status, up_headers, up_body = self._call_upstream(req, req.body, rid, rec, started)
        if status is None:
            return up_headers  # type: ignore[return-value]
        rec.update(action="passthrough", status=status)
        self._finish(rec, started)
        return GatewayResponse(status, self._client_headers(up_headers, rid), up_body)

    def _call_upstream(self, req: GatewayRequest, body: bytes, rid: str, rec: dict, started: float):
        headers = {k: v for k, v in req.headers.items() if k not in _HOP_HEADERS and k != "x-guard-mode"}
        try:
            return self.upstream(req.method, req.path, headers, body)
        except Exception as exc:  # noqa: BLE001 — любая ошибка сети → 502, не падаем
            rec.update(action="upstream_error", status=502, error=f"{type(exc).__name__}: {exc}")
            self._finish(rec, started)
            return None, _error(502, "api_error", f"[LLM Gateway] Upstream недоступен: {type(exc).__name__}", {"x-gateway-request-id": rid}), None

    @staticmethod
    def _client_headers(up_headers: dict, rid: str) -> dict:
        headers = {k.lower(): v for k, v in up_headers.items() if k.lower() not in _HOP_HEADERS}
        headers["x-gateway-request-id"] = rid
        return headers

    @staticmethod
    def _parse_message(headers: dict, body: bytes) -> Optional[dict]:
        ctype = {k.lower(): v for k, v in headers.items()}.get("content-type", "")
        if "text/event-stream" in ctype:
            return message_from_events(parse_sse(body))
        try:
            data = json.loads(body)
        except ValueError:
            return None
        return data if isinstance(data, dict) and data.get("type") == "message" else None

    def _log_plain_response(self, rec: dict, body: bytes, status: int) -> None:
        text, truncated = self.audit.truncate(body.decode("utf-8", errors="replace"))
        rec["response"], rec["response_truncated"] = text, truncated
        if status == 200:
            try:
                tokens = json.loads(body).get("input_tokens")
                if isinstance(tokens, int):
                    rec["usage"] = {"input_tokens": tokens}
                    rec["cost"] = {"usd": 0.0, "note": "count_tokens бесплатен"}
            except ValueError:
                pass

    def _finish(self, rec: dict, started: float) -> None:
        rec["latency_ms"] = int((self._clock() - started) * 1000)
        self.audit.request(rec)

    def _local(self, path: str) -> GatewayResponse:
        if path == "/_gateway/health":
            return _json_response(200, {"status": "ok"})
        if path == "/_gateway/stats":
            return _json_response(
                200,
                {
                    "cost": self.costs.snapshot(),
                    "config": {
                        "upstream": self.config.upstream_url,
                        "input_mode": self.config.input_mode,
                        "output_mode": self.config.output_mode,
                        "rate_limit_per_min": self.config.rate_limit_per_min,
                    },
                },
            )
        return _error(404, "not_found_error", "unknown gateway endpoint")


def _loggable(message: dict) -> dict:
    """Сообщение для аудита без thinking-блоков (не нужны и очень длинные)."""
    return {**message, "content": [b for b in message.get("content", []) if b.get("type") not in ("thinking", "redacted_thinking")]}
