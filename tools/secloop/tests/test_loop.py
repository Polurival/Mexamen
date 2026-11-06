"""Решения цикла проверяются на фейках: ни LLM, ни Gradle, ни git."""

import unittest

from secloop import loop
from secloop.loop import GenResult, LoopDeps, ReviewMaterial, ReviewOutcome, TestResult, run_task
from secloop.review import ReviewFinding, ReviewResult


def f(sev: str, file: str = "A.kt", line: int = 7, title: str = "проблема", fix: str = "поправь") -> ReviewFinding:
    return ReviewFinding(sev, "cat", file, line, title, fix=fix)


class Harness:
    """Сценарий: списки результатов по итерациям; запоминает, что вызывалось и с какими промптами."""

    def __init__(self, tests=None, reviews=None, gen_events=None) -> None:
        self.tests = list(tests or [TestResult(True, 10)])
        self.reviews = list(reviews or [ReviewOutcome(result=ReviewResult("ok", []))])
        self.gen_events = list(gen_events or [])
        self.prompts: list[tuple[str, bool]] = []
        self.review_calls = 0
        self.commits: list[str] = []
        self.logged: list[dict] = []

    def deps(self) -> LoopDeps:
        def generate(prompt: str, first: bool) -> GenResult:
            self.prompts.append((prompt, first))
            return GenResult(True, "ok", 0.1, 3)

        def run_tests() -> TestResult:
            return self.tests.pop(0) if len(self.tests) > 1 else self.tests[0]

        def review(material: ReviewMaterial) -> ReviewOutcome:
            self.review_calls += 1
            return self.reviews.pop(0) if len(self.reviews) > 1 else self.reviews[0]

        def commit(message: str) -> str:
            self.commits.append(message)
            return "abc1234"

        def drain(stage: str) -> list:
            return self.gen_events.pop(0) if stage == "gen" and self.gen_events else []

        return LoopDeps(
            generate=generate,
            run_tests=run_tests,
            review_material=lambda: ReviewMaterial("diff", {"A.kt": "val x = 1"}),
            review=review,
            commit=commit,
            drain_gateway_events=drain,
            scan_secrets=lambda text: [],
            log=self.logged.append,
        )


def ok(*findings: ReviewFinding) -> ReviewOutcome:
    return ReviewOutcome(result=ReviewResult("s", list(findings)))


class LoopDecisions(unittest.TestCase):
    def test_clean_code_is_committed_first_try(self) -> None:
        h = Harness()
        r = run_task("T", "title", "сделай X", h.deps())
        self.assertEqual(loop.COMMITTED_CLEAN, r.status)
        self.assertEqual(1, len(r.iterations))
        self.assertEqual("abc1234", r.commit_sha)
        self.assertEqual([("сделай X", True)], h.prompts)

    def test_high_finding_goes_back_to_generation_with_exact_feedback(self) -> None:
        h = Harness(reviews=[ok(f("high", "Store.kt", 42, "Токен в DataStore", "Keystore")), ok()])
        r = run_task("T", "title", "сделай X", h.deps())
        self.assertEqual(loop.COMMITTED_CLEAN, r.status)
        self.assertEqual(2, len(r.iterations))
        followup, first = h.prompts[1]
        self.assertFalse(first, "повторный вызов продолжает сессию, а не начинает новую")
        self.assertIn("[HIGH] Токен в DataStore в Store.kt:42 — Keystore", followup)
        self.assertEqual(1, len(h.commits), "коммит один — после исправления")

    def test_medium_and_low_pass_with_warning_logged(self) -> None:
        h = Harness(reviews=[ok(f("medium", title="нет pinning"), f("low", title="мелочь"))])
        r = run_task("T", "title", "X", h.deps())
        self.assertEqual(loop.COMMITTED_WITH_WARNINGS, r.status)
        self.assertEqual(1, len(r.iterations), "Medium/Low не возвращают на генерацию")
        warns = [e for e in h.logged if e["event"] == "security_warning"]
        self.assertEqual(["medium", "low"], [w["severity"] for w in warns])
        self.assertIn("[MEDIUM] нет pinning", h.commits[0])

    def test_persistent_critical_is_never_committed(self) -> None:
        h = Harness(reviews=[ok(f("critical", title="ключ в коде"))])
        r = run_task("T", "title", "X", h.deps(), max_iterations=3)
        self.assertEqual(loop.SECURITY_FAILED, r.status)
        self.assertEqual(3, len(r.iterations))
        self.assertEqual([], h.commits)
        self.assertEqual("ключ в коде", r.final_blocking[0].title)

    def test_failing_tests_skip_security_and_feed_output_back(self) -> None:
        h = Harness(tests=[TestResult(False, 5, 1, 0, "e: A.kt:3 Unresolved reference"), TestResult(True, 6)])
        r = run_task("T", "title", "X", h.deps())
        self.assertEqual(loop.COMMITTED_CLEAN, r.status)
        self.assertIn("Unresolved reference", h.prompts[1][0])
        self.assertEqual(1, h.review_calls, "ревью — только после зелёных тестов")

    def test_tests_never_green_is_failure_without_review_or_commit(self) -> None:
        h = Harness(tests=[TestResult(False, 1, 1, 0, "boom")])
        r = run_task("T", "title", "X", h.deps(), max_iterations=2)
        self.assertEqual(loop.TESTS_FAILED, r.status)
        self.assertEqual((0, []), (h.review_calls, h.commits))

    def test_review_error_is_fail_closed(self) -> None:
        h = Harness(reviews=[ReviewOutcome(error="невалидный JSON")])
        r = run_task("T", "title", "X", h.deps())
        self.assertEqual(loop.REVIEW_ERROR, r.status)
        self.assertEqual([], h.commits)

    def test_gateway_block_on_review_becomes_critical_finding_with_location(self) -> None:
        h = Harness(reviews=[ReviewOutcome(gateway_blocked_kinds=["API_KEY"]), ok()])
        deps = h.deps()

        class F:
            kind, start = "API_KEY", 4

        deps.scan_secrets = lambda text: [F()]
        r = run_task("T", "title", "X", deps)
        self.assertEqual(loop.COMMITTED_CLEAN, r.status)
        self.assertEqual(2, len(r.iterations))
        self.assertIn("A.kt:1", h.prompts[1][0])
        self.assertEqual("gateway", r.iterations[0].findings[0].source)
        self.assertTrue(any(e["event"] == "review_blocked_by_gateway" for e in h.logged))

    def test_gateway_block_not_caused_by_code_is_review_error_not_a_generator_loop(self) -> None:
        h = Harness(reviews=[ReviewOutcome(gateway_blocked_kinds=["EMAIL"])])
        r = run_task("T", "title", "X", h.deps())
        self.assertEqual(loop.REVIEW_ERROR, r.status)
        self.assertEqual(1, len(h.prompts), "генератор не гоняют по кругу из-за сбоя конфигурации")
        self.assertEqual([], h.commits)

    def test_gateway_output_block_during_generation_is_reported_in_test_feedback(self) -> None:
        event = {"direction": "output", "action": "blocked", "category": "SECRET"}
        h = Harness(tests=[TestResult(False, 0, 0, 1, "compile error"), TestResult(True, 3)], gen_events=[[event], []])
        run_task("T", "title", "X", h.deps())
        self.assertIn("LLM Gateway заблокировал", h.prompts[1][0])

    def test_empty_diff_is_not_reviewed_or_committed(self) -> None:
        h = Harness()
        deps = h.deps()
        deps.review_material = lambda: ReviewMaterial("  \n", {})
        r = run_task("T", "title", "X", deps)
        self.assertEqual(loop.NO_CHANGES, r.status)
        self.assertEqual((0, []), (h.review_calls, h.commits))

    def test_every_decision_is_logged(self) -> None:
        h = Harness(reviews=[ok(f("high")), ok()])
        run_task("T", "title", "X", h.deps())
        events = [e["event"] for e in h.logged]
        self.assertEqual(2, events.count("security_decision"))
        self.assertIn("commit", events)


if __name__ == "__main__":
    unittest.main()
