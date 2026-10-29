#!/usr/bin/env python3
"""Задача F1 (real-answers-plan.md): сопоставление test-1..test-10 с questions.json.

https://ruspansky.com/test-1/ ... /test-10/ (WATU quiz plugin) содержат по 4
варианта ответа на вопрос, но не помечают верный в HTML (грейдинг server-side,
через AJAX). Верный вариант вычисляется сравнением с уже известным `answer` из
questions.json; оставшиеся варианты — реальные, написанные человеком дистракторы.

Однозначные случаи (ровно один вариант совпадает с answer) размечаются
автоматически. Неоднозначные (0 или >=2 совпадений — перефразировка либо короткий
ответ упомянут в нескольких вариантах) выгружаются в match_review.json для
ручной разметки (задача F2): угадывать эвристикой нельзя — цена ошибки — верный
ответ попадёт в дистракторы.

Результат: questions.json дополняется полями
  answerVariants: [str, ...]   — формулировки правильного ответа (>= 1, всегда содержит answer)
  realDistractors: [str, ...]  — реальные неверные варианты с test-страниц (может быть пустым)
"""

import json
import re
import sys
import unicodedata
from pathlib import Path

import requests
from bs4 import BeautifulSoup

OUT_DIR = Path(__file__).parent / "out"
APP_DATA_DIR = Path(__file__).parent.parent / "composeApp" / "src" / "commonMain" / "composeResources" / "files"
PAGES_CACHE_DIR = OUT_DIR / "test_pages_cache"
QUESTIONS_JSON = APP_DATA_DIR / "questions.json"
REVIEW_JSON = OUT_DIR / "match_review.json"

TEST_URLS = [f"https://ruspansky.com/test-{i}/" for i in range(1, 11)]

USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0 Safari/537.36"
)  # без него ruspansky.com отвечает 406 (Cloudflare/ModSecurity)


def fetch_page(url: str) -> str:
    """Возвращает HTML страницы: из кэша, либо скачивает и кэширует."""
    cache_file = PAGES_CACHE_DIR / (url.rstrip("/").rsplit("/", 1)[-1] + ".html")
    if cache_file.exists():
        return cache_file.read_text(encoding="utf-8")
    print(f"Скачиваю {url} ...")
    resp = requests.get(url, headers={"User-Agent": USER_AGENT}, timeout=30)
    resp.raise_for_status()
    PAGES_CACHE_DIR.mkdir(parents=True, exist_ok=True)
    cache_file.write_text(resp.text, encoding="utf-8")
    return resp.text


def fold(text: str) -> str:
    """Нормализация для сравнения текста: без диакритики, регистра, пунктуации."""
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = text.lower()
    text = re.sub(r"[^a-z0-9]+", " ", text)
    return text.strip()


def parse_test_page(html: str) -> list[dict]:
    """Возвращает [{"question": str, "options": [str, str, str, str]}, ...]."""
    soup = BeautifulSoup(html, "html.parser")
    result = []
    for block in soup.find_all(class_="watu-question"):
        content = block.find(class_="question-content")
        p = content.find("p") if content else None
        if p is None:
            continue
        qtext = re.sub(r"^\d+\.\s*", "", p.get_text(" ").strip())
        options = [span.get_text(" ").strip() for span in block.find_all("span", class_="answer")]
        if qtext and len(options) == 4:
            result.append({"question": qtext, "options": options})
    return result


def match_correct_index(answer: str, options: list[str]) -> int | None:
    """Индекс варианта, равного answer; None если неоднозначно (0 или >=2 кандидатов)."""
    ans_f = fold(answer)
    opt_f = [fold(o) for o in options]
    if ans_f in opt_f:
        return opt_f.index(ans_f)
    candidates = [i for i, of in enumerate(opt_f) if of and (of in ans_f or ans_f in of)]
    return candidates[0] if len(candidates) == 1 else None


def main() -> None:
    questions = json.loads(QUESTIONS_JSON.read_text(encoding="utf-8"))
    by_fold = {fold(q["question"]): q for q in questions}
    if len(by_fold) != len(questions):
        sys.exit("Дубли нормализованного текста вопроса в questions.json — проверить")

    for q in questions:
        q.setdefault("answerVariants", [q["answer"]])
        q.setdefault("realDistractors", [])

    review: list[dict] = []
    page_matches = 0

    for url in TEST_URLS:
        html = fetch_page(url)
        for item in parse_test_page(html):
            entry = by_fold.get(fold(item["question"]))
            if entry is None:
                continue
            page_matches += 1

            idx = match_correct_index(entry["answer"], item["options"])
            if idx is None:
                review.append(
                    {
                        "id": entry["id"],
                        "question": entry["question"],
                        "dataset_answer": entry["answer"],
                        "test_options": item["options"],
                        "source_url": url,
                    }
                )
                continue

            correct_text = item["options"][idx]
            variant_folds = {fold(v) for v in entry["answerVariants"]}
            if fold(correct_text) not in variant_folds:
                entry["answerVariants"].append(correct_text)
                variant_folds.add(fold(correct_text))

            for i, option in enumerate(item["options"]):
                if i == idx or fold(option) in variant_folds:
                    continue
                if option not in entry["realDistractors"]:
                    entry["realDistractors"].append(option)

    QUESTIONS_JSON.write_text(
        json.dumps(questions, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    REVIEW_JSON.write_text(
        json.dumps(review, ensure_ascii=False, indent=2), encoding="utf-8"
    )

    with_real = sum(1 for q in questions if len(q["realDistractors"]) >= 3)
    with_variants = sum(1 for q in questions if len(q["answerVariants"]) > 1)

    print(f"Вопросы датасета: {len(questions)}")
    print(f"Инстансов вопросов на test-страницах, сматченных с датасетом: {page_matches}")
    print(f"Вопросов с >=3 realDistractors: {with_real}")
    print(f"Вопросов с альтернативной формулировкой ответа (answerVariants > 1): {with_variants}")
    print(f"Неоднозначных (в match_review.json, требуют ручной разметки — F2): {len(review)}")
    print(f"Сохранено: {QUESTIONS_JSON}")
    print(f"Сохранено: {REVIEW_JSON}")


if __name__ == "__main__":
    main()
