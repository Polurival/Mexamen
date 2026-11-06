"""Разбор ответа ревьюера, решение и формат фидбека."""

import json
import unittest

from secloop.review import (
    ReviewFinding,
    ReviewParseError,
    build_review_input,
    decide,
    gateway_block_findings,
    number_lines,
    parse_review,
    security_feedback,
)


def finding(sev: str, file: str = "A.kt", line: int = 1, title: str = "t", fix: str = "f") -> dict:
    return {"severity": sev, "category": "c", "file": file, "line": line, "title": title, "explanation": "e", "fix": fix}


class ParseReview(unittest.TestCase):
    def test_structured_output_dict(self) -> None:
        result = parse_review({"summary": "s", "findings": [finding("high", line=42)]})
        self.assertEqual(42, result.findings[0].line)
        self.assertTrue(result.findings[0].blocking)

    def test_fenced_json_and_prose_around(self) -> None:
        text = "Вот результат:\n```json\n" + json.dumps({"summary": "x", "findings": [finding("low")]}) + "\n```\nготово"
        self.assertEqual("low", parse_review(text).findings[0].severity)

    def test_sorted_by_severity(self) -> None:
        result = parse_review({"summary": "", "findings": [finding("low"), finding("critical"), finding("medium")]})
        self.assertEqual(["critical", "medium", "low"], [f.severity for f in result.findings])

    def test_unknown_severity_is_treated_as_high_not_skipped(self) -> None:
        result = parse_review({"summary": "", "findings": [finding("catastrophic")]})
        self.assertEqual("high", result.findings[0].severity)

    def test_aliases(self) -> None:
        result = parse_review({"summary": "", "findings": [finding("Blocker"), finding("Moderate"), finding("Info")]})
        self.assertEqual({"critical", "medium", "low"}, {f.severity for f in result.findings})

    def test_garbage_is_a_parse_error(self) -> None:
        for bad in ("no json here", "{not json}", '{"summary": "x"}', json.dumps([1, 2])):
            with self.subTest(bad=bad), self.assertRaises(ReviewParseError):
                parse_review(bad)

    def test_bad_line_becomes_zero(self) -> None:
        f = finding("high")
        f["line"] = "n/a"
        self.assertEqual(0, parse_review({"summary": "", "findings": [f]}).findings[0].line)


class Decisions(unittest.TestCase):
    def _f(self, sev: str) -> ReviewFinding:
        return ReviewFinding(sev, "c", "A.kt", 1, "t")

    def test_clean(self) -> None:
        self.assertEqual("commit_clean", decide([]).action)

    def test_critical_or_high_returns_to_generation(self) -> None:
        for sev in ("critical", "high"):
            with self.subTest(sev):
                d = decide([self._f("low"), self._f(sev)])
                self.assertEqual("fix", d.action)
                self.assertEqual(1, len(d.blocking))
                self.assertEqual(1, len(d.warnings))

    def test_only_medium_low_passes_with_warnings(self) -> None:
        d = decide([self._f("medium"), self._f("low")])
        self.assertEqual("commit_with_warnings", d.action)
        self.assertEqual(2, len(d.warnings))

    def test_feedback_names_file_and_line(self) -> None:
        text = security_feedback([ReviewFinding("high", "c", "Store.kt", 42, "Токен в открытом виде", fix="Keystore")])
        self.assertIn("Исправь:", text)
        self.assertIn("[HIGH] Токен в открытом виде в Store.kt:42 — Keystore", text)


class ReviewInput(unittest.TestCase):
    def test_numbering_and_caps(self) -> None:
        self.assertEqual("   1| a\n   2| b", number_lines("a\nb"))
        material = build_review_input("DIFF", {"a.kt": "x\ny", "big.kt": "z" * 500}, total_cap=300)
        self.assertIn("   2| y", material)
        self.assertIn("пропущен", material)

    def test_gateway_block_is_located_to_file_and_line(self) -> None:
        class F:  # минимальный двойник Finding детектора
            kind, start = "API_KEY", 12

        files = {"A.kt": "line1\nline2\nval k = SECRET", "B.kt": "clean"}
        found = gateway_block_findings(files, ["API_KEY"], scan=lambda text: [F()] if "SECRET" in text else [])
        self.assertEqual(1, len(found))
        self.assertEqual(("A.kt", 3, "critical", "gateway"), (found[0].file, found[0].line, found[0].severity, found[0].source))

    def test_gateway_block_without_location_yields_nothing(self) -> None:
        self.assertEqual([], gateway_block_findings({"A.kt": "x"}, ["EMAIL"], scan=lambda text: []))


if __name__ == "__main__":
    unittest.main()
