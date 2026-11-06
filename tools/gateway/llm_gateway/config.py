"""Конфигурация шлюза: значения по умолчанию + переопределение через переменные окружения GATEWAY_*."""

from __future__ import annotations

import os
from dataclasses import dataclass, field


def _frozen_set(value: str) -> frozenset[str]:
    return frozenset(x.strip().upper() for x in value.split(",") if x.strip())


# Служебные адреса, не являющиеся персональными данными (напр. `Co-Authored-By: … <noreply@anthropic.com>`).
DEFAULT_EMAIL_ALLOW: tuple[str, ...] = ("noreply@*", "no-reply@*")


@dataclass
class Config:
    upstream_url: str = "https://api.anthropic.com"
    host: str = "127.0.0.1"
    port: int = 8080
    input_mode: str = "redact"  # block | redact
    output_mode: str = "block"  # block | redact | log
    rate_limit_per_min: int = 60  # 0 — выключен
    log_dir: str = os.path.join(os.path.dirname(os.path.dirname(os.path.abspath(__file__))), "logs")
    log_max_chars: int = 20000
    trust_forwarded_for: bool = False
    disabled_detectors: frozenset[str] = field(default_factory=frozenset)
    email_allowlist: tuple[str, ...] = DEFAULT_EMAIL_ALLOW  # fnmatch-шаблоны email, которые Input Guard не трогает
    canary: str = ""
    pricing_file: str = ""
    upstream_timeout: float = 600.0

    @classmethod
    def from_env(cls) -> "Config":
        env = os.environ
        base = cls()
        return cls(
            upstream_url=env.get("GATEWAY_UPSTREAM_URL", base.upstream_url),
            host=env.get("GATEWAY_HOST", base.host),
            port=int(env.get("GATEWAY_PORT", base.port)),
            input_mode=env.get("GATEWAY_INPUT_MODE", base.input_mode),
            output_mode=env.get("GATEWAY_OUTPUT_MODE", base.output_mode),
            rate_limit_per_min=int(env.get("GATEWAY_RATE_LIMIT_PER_MIN", base.rate_limit_per_min)),
            log_dir=env.get("GATEWAY_LOG_DIR", base.log_dir),
            log_max_chars=int(env.get("GATEWAY_LOG_MAX_CHARS", base.log_max_chars)),
            trust_forwarded_for=env.get("GATEWAY_TRUST_XFF", "") == "1",
            disabled_detectors=_frozen_set(env.get("GATEWAY_DISABLED_DETECTORS", "")),
            email_allowlist=DEFAULT_EMAIL_ALLOW + tuple(
                x.strip() for x in env.get("GATEWAY_EMAIL_ALLOW", "").split(",") if x.strip()
            ),
            canary=env.get("GATEWAY_CANARY", ""),
            pricing_file=env.get("GATEWAY_PRICING_FILE", ""),
            upstream_timeout=float(env.get("GATEWAY_UPSTREAM_TIMEOUT", base.upstream_timeout)),
        )
