"""HTTP-слой на стандартной библиотеке (без внешних зависимостей): сервер + вызов upstream."""

from __future__ import annotations

import argparse
import http.client
import ssl
import sys
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from urllib.parse import urlparse

from .config import Config
from .gateway import Gateway, GatewayRequest, UpstreamFn


def make_http_upstream(base_url: str, timeout: float) -> UpstreamFn:
    """Вызов реального upstream (HTTPS/HTTP). Тело читается целиком — Output Guard проверяет ответ до отдачи."""
    parsed = urlparse(base_url)
    https = parsed.scheme == "https"
    host = parsed.hostname or ""
    port = parsed.port or (443 if https else 80)
    prefix = parsed.path.rstrip("/")

    def call(method: str, path: str, headers: dict, body: bytes):
        if https:
            conn: http.client.HTTPConnection = http.client.HTTPSConnection(
                host, port, timeout=timeout, context=ssl.create_default_context()
            )
        else:
            conn = http.client.HTTPConnection(host, port, timeout=timeout)
        try:
            send = dict(headers)
            send["accept-encoding"] = "identity"
            send["host"] = parsed.netloc
            conn.request(method, prefix + path, body=body or None, headers=send)
            resp = conn.getresponse()
            data = resp.read()
            return resp.status, {k.lower(): v for k, v in resp.getheaders()}, data
        finally:
            conn.close()

    return call


def _read_body(handler: BaseHTTPRequestHandler) -> bytes:
    if handler.headers.get("Transfer-Encoding", "").lower() == "chunked":
        chunks = []
        while True:
            size = int(handler.rfile.readline().strip().split(b";")[0] or b"0", 16)
            if size == 0:
                handler.rfile.readline()
                break
            chunks.append(handler.rfile.read(size))
            handler.rfile.readline()
        return b"".join(chunks)
    length = int(handler.headers.get("Content-Length") or 0)
    return handler.rfile.read(length) if length else b""


def make_handler(gateway: Gateway, trust_xff: bool):
    class Handler(BaseHTTPRequestHandler):
        protocol_version = "HTTP/1.1"

        def _handle(self) -> None:
            ip = self.client_address[0]
            if trust_xff:
                forwarded = self.headers.get("X-Forwarded-For", "")
                if forwarded:
                    ip = forwarded.split(",")[0].strip()
            req = GatewayRequest(
                method=self.command,
                path=self.path,
                headers={k.lower(): v for k, v in self.headers.items()},
                body=_read_body(self),
                client_ip=ip,
            )
            resp = gateway.handle(req)
            self.send_response(resp.status)
            for k, v in resp.headers.items():
                self.send_header(k, v)
            self.send_header("Content-Length", str(len(resp.body)))
            self.end_headers()
            self.wfile.write(resp.body)

        do_GET = do_POST = do_PUT = do_DELETE = do_PATCH = _handle

        def log_message(self, fmt: str, *args) -> None:  # аудит ведёт Gateway, не stderr
            return

    return Handler


def build_server(config: Config, upstream: UpstreamFn | None = None) -> tuple[ThreadingHTTPServer, Gateway]:
    gateway = Gateway(config, upstream or make_http_upstream(config.upstream_url, config.upstream_timeout))
    server = ThreadingHTTPServer((config.host, config.port), make_handler(gateway, config.trust_forwarded_for))
    server.daemon_threads = True
    return server, gateway


def main(argv: list[str] | None = None) -> int:
    cfg = Config.from_env()
    p = argparse.ArgumentParser(description="LLM Gateway: прокси для Anthropic API с Input/Output Guard")
    p.add_argument("--host", default=cfg.host)
    p.add_argument("--port", type=int, default=cfg.port)
    p.add_argument("--upstream", default=cfg.upstream_url)
    p.add_argument("--input-mode", choices=["block", "redact"], default=cfg.input_mode)
    p.add_argument("--output-mode", choices=["block", "redact", "log"], default=cfg.output_mode)
    p.add_argument("--rate-limit", type=int, default=cfg.rate_limit_per_min, help="запросов/мин с IP (0 — выкл.)")
    p.add_argument("--log-dir", default=cfg.log_dir)
    a = p.parse_args(argv)
    cfg.host, cfg.port, cfg.upstream_url = a.host, a.port, a.upstream
    cfg.input_mode, cfg.output_mode, cfg.rate_limit_per_min, cfg.log_dir = a.input_mode, a.output_mode, a.rate_limit, a.log_dir

    server, _ = build_server(cfg)
    print(
        f"LLM Gateway: http://{cfg.host}:{cfg.port} -> {cfg.upstream_url} | input={cfg.input_mode} "
        f"output={cfg.output_mode} rate={cfg.rate_limit_per_min}/min | logs: {cfg.log_dir}",
        file=sys.stderr,
    )
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
    finally:
        server.server_close()
    return 0
