"""Input Guard: прогон всех INPUT_CASES в режимах redact и block."""

import json
import unittest

from llm_gateway.config import DEFAULT_EMAIL_ALLOW
from llm_gateway.input_guard import InputGuard

from .cases import INPUT_CASES, OPENAI_LONG, OPENAI_SHORT
from .helpers import build_body


def _text_of(body: dict) -> str:
    return json.dumps(body, ensure_ascii=False)


class InputGuardCases(unittest.TestCase):
    def setUp(self) -> None:
        self.guard = InputGuard(email_allow=DEFAULT_EMAIL_ALLOW)

    def test_every_case_in_redact_mode(self) -> None:
        for case in INPUT_CASES:
            with self.subTest(case.id, title=case.title):
                result = self.guard.inspect(build_body(case.messages), "redact")
                found = {lf.finding.kind for lf in result.findings}
                if case.known_miss:
                    self.assertEqual(set(), found, "известный пропуск внезапно стал находиться — обновите кейс и RESULTS.md")
                    for fragment in case.leak_fragments:
                        self.assertIn(fragment, _text_of(result.body))
                    continue
                self.assertEqual(case.expect_kinds, found)
                dump = _text_of(result.body)
                for fragment in case.leak_fragments:
                    self.assertNotIn(fragment, dump, "секрет остался в запросе после маскирования")
                if case.expect_label:
                    self.assertIn(case.expect_label, dump)
                self.assertFalse(result.blocked)

    def test_every_case_in_block_mode(self) -> None:
        for case in INPUT_CASES:
            with self.subTest(case.id, title=case.title):
                result = self.guard.inspect(build_body(case.messages), "block")
                expected_block = bool(case.expect_kinds) and not case.known_miss
                self.assertEqual(expected_block, result.blocked)

    def test_task_example_is_masked_exactly(self) -> None:
        result = self.guard.inspect(build_body([f"key={OPENAI_SHORT}"]), "redact")
        self.assertEqual("key=[REDACTED_API_KEY]", result.body["messages"][0]["content"])

    def test_original_body_is_not_mutated(self) -> None:
        body = build_body([f"key={OPENAI_LONG}"])
        self.guard.inspect(body, "redact")
        self.assertIn(OPENAI_LONG, body["messages"][0]["content"])

    def test_scans_system_tool_result_and_tool_use_input(self) -> None:
        body = {
            "model": "m",
            "system": [{"type": "text", "text": f"token={OPENAI_LONG}"}],
            "messages": [
                {"role": "assistant", "content": [{"type": "tool_use", "id": "t1", "name": "Bash", "input": {"command": f"echo {OPENAI_LONG}"}}]},
                {"role": "user", "content": [{"type": "tool_result", "tool_use_id": "t1", "content": [{"type": "text", "text": f"out {OPENAI_LONG}"}]}]},
            ],
        }
        result = self.guard.inspect(body, "redact")
        self.assertNotIn(OPENAI_LONG, _text_of(result.body))
        self.assertEqual(3, len(result.findings))
        self.assertEqual("t1", result.body["messages"][1]["content"][0]["tool_use_id"])  # служебные поля целы

    def test_thinking_and_image_blocks_are_left_alone(self) -> None:
        body = {
            "model": "m",
            "messages": [
                {"role": "assistant", "content": [{"type": "thinking", "thinking": f"{OPENAI_LONG}", "signature": "c2lnbmF0dXJl" * 4}]},
                {"role": "user", "content": [{"type": "image", "source": {"type": "base64", "media_type": "image/png", "data": "QUJD" * 50}}]},
            ],
        }
        result = self.guard.inspect(body, "redact")
        self.assertEqual([], result.findings)
        self.assertEqual(body, result.body)

    def test_split_secret_across_assistant_turn_is_caught(self) -> None:
        result = self.guard.inspect(build_body(["ключ начинается на sk-", "proj-Zq8Lm3Vx9Tb2Nw7Kc4"]), "redact")
        self.assertEqual({"API_KEY"}, {lf.finding.kind for lf in result.findings})

    def test_finding_log_never_contains_full_secret(self) -> None:
        result = self.guard.inspect(build_body([f"key {OPENAI_LONG}", "card 4111 1111 1111 1111"]), "redact")
        logged = json.dumps([lf.to_log() for lf in result.findings])
        self.assertNotIn(OPENAI_LONG, logged)
        self.assertNotIn("4111 1111", logged)
        self.assertNotIn("1111 1111", logged)

    def test_disabled_detector_is_skipped(self) -> None:
        result = InputGuard(frozenset({"EMAIL"})).inspect(build_body(["a.b@example.com"]), "redact")
        self.assertEqual([], result.findings)


class EmailAllowlist(unittest.TestCase):
    def test_noreply_is_allowed_by_default_but_real_address_is_not(self) -> None:
        guard = InputGuard(email_allow=DEFAULT_EMAIL_ALLOW)
        text = "Co-Authored-By: Bot <noreply@anthropic.com>; пиши на maria.lopez@gmail.com; alt: No-Reply@Corp.io"
        result = guard.inspect(build_body([text]), "redact")
        out = result.body["messages"][0]["content"]
        self.assertIn("noreply@anthropic.com", out)
        self.assertIn("No-Reply@Corp.io", out, "шаблоны без учёта регистра")
        self.assertNotIn("maria.lopez@gmail.com", out)
        self.assertEqual(1, len(result.findings))

    def test_without_allowlist_noreply_is_masked(self) -> None:
        result = InputGuard().inspect(build_body(["<noreply@anthropic.com>"]), "redact")
        self.assertEqual(1, len(result.findings))

    def test_custom_exact_and_domain_patterns(self) -> None:
        guard = InputGuard(email_allow=DEFAULT_EMAIL_ALLOW + ("me@mail.dev", "*@corp.io"))
        text = "me@mail.dev, ana@corp.io, bob@other.org"
        out = guard.inspect(build_body([text]), "redact").body["messages"][0]["content"]
        self.assertEqual("me@mail.dev, ana@corp.io, [REDACTED_EMAIL]", out)

    def test_allowlist_does_not_hide_a_real_email_hidden_in_base64(self) -> None:
        import base64

        blob = base64.b64encode(b"contact: maria.lopez@gmail.com please").decode()
        self.assertEqual(1, len(InputGuard(email_allow=DEFAULT_EMAIL_ALLOW).inspect(build_body([blob]), "redact").findings))


if __name__ == "__main__":
    unittest.main()
