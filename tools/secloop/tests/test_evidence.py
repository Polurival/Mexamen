import json
import os
import tempfile
import unittest

from secloop.canaries import CANARIES, local_properties_text, render_task
from secloop.evidence import find_leaks, real_secret_values, static_checks, summarize_gateway


def _write(path: str, text: str) -> None:
    with open(path, "w", encoding="utf-8") as fh:
        fh.write(text)


class Canaries(unittest.TestCase):
    def test_render_replaces_all_placeholders(self) -> None:
        out = render_task("a {{JWT}} b {{EMAIL}} c {{PHONE}} d {{API_KEY}}")
        self.assertNotIn("{{", out)
        self.assertIn(CANARIES["EMAIL"], out)

    def test_local_properties_has_only_fake_keys_and_real_sdk_path(self) -> None:
        text = local_properties_text("/opt/sdk")
        self.assertIn("sdk.dir=/opt/sdk", text)
        self.assertIn(CANARIES["LOCAL_GROQ"], text)


class Leaks(unittest.TestCase):
    def test_find_leaks_counts_without_returning_values(self) -> None:
        with tempfile.TemporaryDirectory() as d:
            a, b = os.path.join(d, "a.txt"), os.path.join(d, "b.txt")
            _write(a, "x SECRETVALUE y SECRETVALUE")
            _write(b, "clean")
            hits = find_leaks([a, b], {"k": "SECRETVALUE"})
            self.assertEqual([(a, "k", 2)], hits)
            self.assertNotIn("SECRETVALUE", repr(hits))

    def test_real_secret_values_reads_properties_but_ignores_non_secrets(self) -> None:
        with tempfile.TemporaryDirectory() as d:
            _write(os.path.join(d, "local.properties"), "sdk.dir=/x\nAPI_KEY=abcdefgh12345\n# GROQ_API_KEY=hidden-value-999\nSHORT_KEY=abc\n")
            vals = real_secret_values(d)
            self.assertEqual({"local.properties:API_KEY", "local.properties:GROQ_API_KEY"}, set(vals))


class Static(unittest.TestCase):
    DIFF = "\n".join(
        [
            "+++ b/composeApp/src/commonMain/kotlin/A.kt",
            '+val u = "http://status.example/v1"',
            '+val ok = "http://localhost:8080"',
            "+install(Logging) { level = LogLevel.ALL }",
            '+println("token=$t")',
            "+localStorage.setItem(KEY, token)",
            f"+val k = \"{CANARIES['API_KEY']}\"",
            "-removed line http://old.example",
        ]
    )

    def test_flags_expected_rules_only_on_added_lines(self) -> None:
        rules = {h[0] for h in static_checks(self.DIFF)}
        self.assertTrue({"cleartext-http", "ktor-verbose-logging", "log-call", "plain-local-storage", "secret:API_KEY"} <= rules)
        self.assertEqual(1, sum(1 for h in static_checks(self.DIFF) if h[0] == "cleartext-http"), "localhost и удалённые строки не считаются")

    def test_clean_diff(self) -> None:
        self.assertEqual([], static_checks("+++ b/A.kt\n+val x = 1\n"))


class GatewaySummary(unittest.TestCase):
    def test_counts(self) -> None:
        with tempfile.TemporaryDirectory() as d:
            audit, events = os.path.join(d, "audit.jsonl"), os.path.join(d, "ev.jsonl")
            rows = [
                {"path": "/v1/messages", "input_guard": "pass", "output_guard": "pass", "usage": {"input_tokens": 10}, "cost": {"usd": 0.5}},
                {"path": "/v1/messages", "input_guard": "redacted", "output_guard": "blocked"},
                {"path": "/", "input_guard": None},
            ]
            _write(audit, "\n".join(json.dumps(r) for r in rows))
            _write(events, json.dumps({"direction": "input", "action": "redacted", "kind": "EMAIL"}) + "\n")
            s = summarize_gateway(audit, events)
            self.assertEqual((2, 1, 1, 1), (s["llm_requests"], s["forwarded_clean"], s["forwarded_redacted"], s["output_flagged"]))
            self.assertEqual(0.5, s["cost_usd"])


if __name__ == "__main__":
    unittest.main()
