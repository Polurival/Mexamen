#!/usr/bin/env python3
"""Задача F2 (real-answers-plan.md): применяет ручную разметку match_review.json.

Каждая запись в match_review.json — вопрос, для которого F1 не смог однозначно
определить верный вариант среди 4 опций test-страницы (перефразировка либо
короткий ответ упомянут в нескольких вариантах — см. real-answers-plan.md, F1).

Разметка — поле "correct_index" (0..3), проставленное вручную после сверки
dataset_answer с test_options. Скрипт применяет её к questions.json той же
логикой, что и F1: вариант correct_index -> answerVariants (если отличается от
answer), остальные 3 -> realDistractors.
"""

import json
import sys
import unicodedata
import re
from pathlib import Path

OUT_DIR = Path(__file__).parent / "out"
QUESTIONS_JSON = OUT_DIR / "questions.json"
REVIEW_JSON = OUT_DIR / "match_review.json"


def fold(text: str) -> str:
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = text.lower()
    text = re.sub(r"[^a-z0-9]+", " ", text)
    return text.strip()


def main() -> None:
    questions = json.loads(QUESTIONS_JSON.read_text(encoding="utf-8"))
    by_id = {q["id"]: q for q in questions}
    review = json.loads(REVIEW_JSON.read_text(encoding="utf-8"))

    missing = [r["id"] for r in review if "correct_index" not in r]
    if missing:
        sys.exit(f"Не размечены: {missing}")

    applied = 0
    for r in review:
        entry = by_id[r["id"]]
        idx = r["correct_index"]
        options = r["test_options"]
        if not (0 <= idx < len(options)):
            sys.exit(f"id={r['id']}: correct_index {idx} вне диапазона")

        correct_text = options[idx]
        variant_folds = {fold(v) for v in entry["answerVariants"]}
        if fold(correct_text) not in variant_folds:
            entry["answerVariants"].append(correct_text)
            variant_folds.add(fold(correct_text))

        for i, option in enumerate(options):
            if i == idx or fold(option) in variant_folds:
                continue
            if option not in entry["realDistractors"]:
                entry["realDistractors"].append(option)
        applied += 1

    QUESTIONS_JSON.write_text(
        json.dumps(questions, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"Применено размеченных записей: {applied}")

    with_real = sum(1 for q in questions if len(q["realDistractors"]) >= 3)
    print(f"Вопросов с >=3 realDistractors (итого): {with_real}")


if __name__ == "__main__":
    main()
