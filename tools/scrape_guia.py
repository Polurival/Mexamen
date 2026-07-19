#!/usr/bin/env python3
"""Задача A1: скрейпинг https://ruspansky.com/guia-de-estudios/ -> tools/out/questions.json.

Скачивает страницу (кэширует сырой HTML в tools/out/guia_raw.html, повторно сайт
не дёргает), парсит тематические разделы (на странице их 11), извлекает пары
"вопрос -> ответ",
чистит и сохраняет JSON: [{"id": 1, "section": "...", "question": "...", "answer": "..."}].
"""

import json
import re
import sys
import unicodedata
from pathlib import Path

import requests
from bs4 import BeautifulSoup

URL = "https://ruspansky.com/guia-de-estudios/"
OUT_DIR = Path(__file__).parent / "out"
RAW_HTML = OUT_DIR / "guia_raw.html"
QUESTIONS_JSON = OUT_DIR / "questions.json"

USER_AGENT = (
    "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 "
    "(KHTML, like Gecko) Chrome/126.0 Safari/537.36"
)


def fetch_html() -> str:
    """Возвращает HTML страницы: из кэша, либо скачивает и кэширует."""
    if RAW_HTML.exists():
        print(f"Кэш найден: {RAW_HTML}")
        return RAW_HTML.read_text(encoding="utf-8")
    print(f"Скачиваю {URL} ...")
    resp = requests.get(URL, headers={"User-Agent": USER_AGENT}, timeout=30)
    resp.raise_for_status()
    resp.encoding = "utf-8"  # у страницы нет charset в заголовках, apparent_encoding ошибается
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    RAW_HTML.write_text(resp.text, encoding="utf-8")
    print(f"Сохранён кэш: {RAW_HTML} ({len(resp.text)} символов)")
    return resp.text


def clean_text(text: str) -> str:
    """Нормализация: юникод-нормализация, тире/кавычки, пробелы."""
    text = unicodedata.normalize("NFC", text)
    replacements = {
        "–": "-",  # en dash
        "—": "-",  # em dash
        "−": "-",  # minus
        "«": '"',  # «
        "»": '"',  # »
        "“": '"',
        "”": '"',
        "‘": "'",
        "’": "'",
        " ": " ",  # nbsp
    }
    for src, dst in replacements.items():
        text = text.replace(src, dst)
    text = re.sub(r"\s+", " ", text)
    return text.strip()


def strip_leading_number(text: str) -> str:
    """Убирает нумерацию в начале вопроса: '12. ', '12) ', '12.- '."""
    return re.sub(r"^\d+\s*[.)-]*\s*", "", text).strip()


def parse_qa_pairs(html: str) -> list[dict]:
    """Извлекает пары вопрос->ответ из Elementor-аккордеона.

    Разметка страницы: секции — `div.elementor-accordion-item`
    (заголовок в `.elementor-accordion-title`), внутри каждой — `<table>`
    со строками `<tr><td>вопрос</td><td>ответ</td></tr>`.

    Секция "ESTADOS Y CAPITALES" — не вопросы, а пары "штат -> столица";
    преобразуется в вопросы вида "¿Cuál es la capital de X?".
    """
    soup = BeautifulSoup(html, "html.parser")
    items = soup.find_all(class_="elementor-accordion-item")
    if not items:
        sys.exit("Не найдены секции аккордеона (elementor-accordion-item)")

    entries: list[dict] = []
    for item in items:
        title_el = item.find(class_="elementor-accordion-title")
        if title_el is None:
            continue
        section = clean_text(title_el.get_text(" "))
        is_capitals = "ESTADOS Y CAPITALES" in section.upper()
        for tr in item.find_all("tr"):
            tds = tr.find_all("td")
            if len(tds) != 2:
                continue
            left = clean_text(tds[0].get_text(" "))
            right = clean_text(tds[1].get_text(" "))
            if not left or not right:
                continue
            if is_capitals:
                question = f"¿Cuál es la capital de {left}?"
            else:
                question = strip_leading_number(left)
            entries.append(
                {"section": section, "question": question, "answer": right}
            )
    return entries


def dedupe_and_finalize(entries: list[dict]) -> list[dict]:
    """Отсев пустых и дублей (по нормализованному вопросу), проставление id."""
    seen: set[str] = set()
    result: list[dict] = []
    for e in entries:
        if not e["question"] or not e["answer"] or not e["section"]:
            continue
        key = e["question"].casefold()
        if key in seen:
            continue
        seen.add(key)
        result.append(
            {
                "id": len(result) + 1,
                "section": e["section"],
                "question": e["question"],
                "answer": e["answer"],
            }
        )
    return result


def main() -> None:
    html = fetch_html()
    raw_entries = parse_qa_pairs(html)
    entries = dedupe_and_finalize(raw_entries)

    sections = sorted({e["section"] for e in entries})
    print(f"Извлечено записей: {len(entries)}")
    print(f"Разделов: {len(sections)}")
    for s in sections:
        count = sum(1 for e in entries if e["section"] == s)
        print(f"  {count:4d}  {s}")

    OUT_DIR.mkdir(parents=True, exist_ok=True)
    QUESTIONS_JSON.write_text(
        json.dumps(entries, ensure_ascii=False, indent=2), encoding="utf-8"
    )
    print(f"Сохранено: {QUESTIONS_JSON}")

    if len(entries) < 700:
        sys.exit(f"ВНИМАНИЕ: записей меньше 700 ({len(entries)}) — проверить парсинг")


if __name__ == "__main__":
    main()
