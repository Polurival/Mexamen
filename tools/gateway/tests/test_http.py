"""Реальный HTTP-сервер шлюза на localhost + фейковый upstream: проверяем сам HTTP-слой и SSE."""

import json
import threading
import unittest
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import urllib.error
import urllib.request

from llm_gateway.config import Config
from llm_gateway.server import build_server, make_http_upstream

from .cases import OPENAI_LONG, message_with
from .helpers import FakeUpstream, TempLogs, build_body


class HttpLayer(unittest.TestCase):
    def _serve(self, logs: str, upstream: FakeUpstream, **cfg):
        config = Config(log_dir=logs, host="127.0.0.1", port=0, rate_limit_per_min=1000, **cfg)
        server, gateway = build_server(config, upstream)
        threading.Thread(target=server.serve_forever, daemon=True).start()
        self.addCleanup(server.server_close)
        self.addCleanup(server.shutdown)
        return f"http://127.0.0.1:{server.server_address[1]}", gateway

    @staticmethod
    def _post(url: str, body: dict, headers=None):
        req = urllib.request.Request(url, json.dumps(body).encode(), {"content-type": "application/json", **(headers or {})})
        try:
            with urllib.request.urlopen(req, timeout=10) as resp:
                return resp.status, resp.headers, resp.read()
        except urllib.error.HTTPError as err:
            return err.code, err.headers, err.read()

    def test_redact_and_stream_over_real_http(self) -> None:
        with TempLogs() as logs:
            up = FakeUpstream()
            base, _ = self._serve(logs, up)
            status, headers, body = self._post(base + "/v1/messages", build_body([f"ключ {OPENAI_LONG}"], stream=True))
            self.assertEqual(200, status)
            self.assertIn("text/event-stream", headers["content-type"])
            self.assertIn(b"event: message_start", body)
            self.assertNotIn(OPENAI_LONG.encode(), up.calls[0]["body"])
            self.assertEqual("redacted", headers["x-guard-input"])

    def test_block_over_real_http(self) -> None:
        with TempLogs() as logs:
            up = FakeUpstream()
            base, _ = self._serve(logs, up, input_mode="block")
            status, _, body = self._post(base + "/v1/messages", build_body([OPENAI_LONG]))
            self.assertEqual(400, status)
            self.assertEqual([], up.calls)
            self.assertEqual("error", json.loads(body)["type"])

    def test_health_and_stats_endpoints(self) -> None:
        with TempLogs() as logs:
            base, _ = self._serve(logs, FakeUpstream())
            with urllib.request.urlopen(base + "/_gateway/health", timeout=10) as resp:
                self.assertEqual({"status": "ok"}, json.loads(resp.read()))
            self._post(base + "/v1/messages", build_body(["hola"]))
            with urllib.request.urlopen(base + "/_gateway/stats", timeout=10) as resp:
                self.assertEqual(1, json.loads(resp.read())["cost"]["requests_with_usage"])


class RealUpstreamForwarding(unittest.TestCase):
    """make_http_upstream ходит по настоящему HTTP: заглушка-сервер записывает, что до неё дошло."""

    def test_headers_path_and_redacted_body_reach_upstream(self) -> None:
        seen: dict = {}

        class Stub(BaseHTTPRequestHandler):
            def do_POST(self) -> None:  # noqa: N802
                seen["path"] = self.path
                seen["headers"] = {k.lower(): v for k, v in self.headers.items()}
                seen["body"] = self.rfile.read(int(self.headers["Content-Length"]))
                payload = json.dumps(message_with([{"type": "text", "text": "ok"}])).encode()
                self.send_response(200)
                self.send_header("Content-Type", "application/json")
                self.send_header("Content-Length", str(len(payload)))
                self.end_headers()
                self.wfile.write(payload)

            def log_message(self, *args) -> None:
                return

        stub = ThreadingHTTPServer(("127.0.0.1", 0), Stub)
        threading.Thread(target=stub.serve_forever, daemon=True).start()
        self.addCleanup(stub.server_close)
        self.addCleanup(stub.shutdown)

        call = make_http_upstream(f"http://127.0.0.1:{stub.server_address[1]}", 10)
        body = json.dumps(build_body(["hola"])).encode()
        status, headers, data = call("POST", "/v1/messages?beta=true", {"x-api-key": "k-123", "content-type": "application/json"}, body)

        self.assertEqual(200, status)
        self.assertEqual("/v1/messages?beta=true", seen["path"])
        self.assertEqual("k-123", seen["headers"]["x-api-key"])
        self.assertEqual("identity", seen["headers"]["accept-encoding"])
        self.assertEqual(body, seen["body"])
        self.assertEqual("message", json.loads(data)["type"])
        self.assertEqual("application/json", headers["content-type"])


if __name__ == "__main__":
    unittest.main()
