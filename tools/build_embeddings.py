#!/usr/bin/env python3
"""Задача A2: эмбеддинги для questions.json -> tools/out/embeddings.bin.

Локальный Ollama, модель nomic-embed-text (768-dim). Эмбеддится строка
"вопрос + ответ". Формат выходного файла: заголовок из двух uint32
little-endian (count, dim), затем count*dim float32 little-endian подряд
в порядке id (id из JSON последовательны, 1..count).

Идемпотентен: если embeddings.bin уже существует и заголовок согласован
с questions.json — пересчёт пропускается (форс: --force).
Падает с понятной ошибкой, если Ollama не запущен.
"""

import argparse
import json
import math
import re
import struct
import sys
import unicodedata
from pathlib import Path

import requests

OLLAMA_URL = "http://localhost:11434/api/embeddings"
MODEL = "nomic-embed-text"
DIM = 768

OUT_DIR = Path(__file__).parent / "out"
QUESTIONS_JSON = OUT_DIR / "questions.json"
EMBEDDINGS_BIN = OUT_DIR / "embeddings.bin"


def normalize_for_embedding(text: str) -> str:
    """BERT-uncased нормализация: lowercase + срез диакритики + удаление ¿¡.

    Ollama (проверено на 0.24.0) не нормализует вход для uncased-модели
    nomic-embed-text: токены с заглавными или акцентами ("México", "cuál")
    уходят в [UNK], из-за чего разные вопросы получают идентичные векторы.
    """
    text = unicodedata.normalize("NFD", text.lower())
    text = "".join(ch for ch in text if unicodedata.category(ch) != "Mn")
    text = text.replace("¿", " ").replace("¡", " ")
    return re.sub(r"\s+", " ", text).strip()


def get_embedding(text: str) -> list[float]:
    """768-dim вектор от Ollama; SystemExit с понятным текстом, если Ollama недоступен."""
    try:
        resp = requests.post(
            OLLAMA_URL, json={"model": MODEL, "prompt": text}, timeout=60
        )
        resp.raise_for_status()
    except requests.exceptions.ConnectionError:
        sys.exit(
            f"Ollama не отвечает на {OLLAMA_URL}.\n"
            f"Запустить: `ollama serve`, модель: `ollama pull {MODEL}`."
        )
    vec = resp.json()["embedding"]
    if len(vec) != DIM:
        sys.exit(f"Ожидалась размерность {DIM}, получено {len(vec)} (модель {MODEL}?)")
    return vec


def load_questions() -> list[dict]:
    if not QUESTIONS_JSON.exists():
        sys.exit(f"Нет {QUESTIONS_JSON} — сначала `python tools/scrape_guia.py`")
    data = json.loads(QUESTIONS_JSON.read_text(encoding="utf-8"))
    ids = [e["id"] for e in data]
    if ids != list(range(1, len(data) + 1)):
        sys.exit("id в questions.json не последовательны — порядок векторов сломается")
    return data


def existing_is_current(count: int) -> bool:
    """True, если embeddings.bin существует и согласован по заголовку и размеру."""
    if not EMBEDDINGS_BIN.exists():
        return False
    raw = EMBEDDINGS_BIN.read_bytes()
    if len(raw) < 8:
        return False
    file_count, file_dim = struct.unpack("<II", raw[:8])
    return (
        file_count == count
        and file_dim == DIM
        and len(raw) == 8 + count * DIM * 4
    )


def build(entries: list[dict]) -> None:
    count = len(entries)
    vectors: list[list[float]] = []
    for i, e in enumerate(entries, 1):
        text = normalize_for_embedding(f"{e['question']}\n{e['answer']}")
        vectors.append(get_embedding(text))
        if i % 25 == 0 or i == count:
            print(f"\rЭмбеддинг {i}/{count}", end="", flush=True)
    print()

    with open(EMBEDDINGS_BIN, "wb") as f:
        f.write(struct.pack("<II", count, DIM))
        for vec in vectors:
            f.write(struct.pack(f"<{DIM}f", *vec))
    size = EMBEDDINGS_BIN.stat().st_size
    expected = 8 + count * DIM * 4
    assert size == expected, f"размер {size} != ожидаемого {expected}"
    print(f"Сохранено: {EMBEDDINGS_BIN} ({size} байт = 8 + {count}×{DIM}×4)")


def read_vectors() -> list[list[float]]:
    raw = EMBEDDINGS_BIN.read_bytes()
    count, dim = struct.unpack("<II", raw[:8])
    floats = struct.unpack(f"<{count * dim}f", raw[8:])
    return [list(floats[i * dim : (i + 1) * dim]) for i in range(count)]


def cosine(a: list[float], b: list[float]) -> float:
    dot = sum(x * y for x, y in zip(a, b))
    na = math.sqrt(sum(x * x for x in a))
    nb = math.sqrt(sum(x * x for x in b))
    return dot / (na * nb)


def find_id(entries: list[dict], needle: str) -> int | None:
    """id первой записи, чей вопрос содержит подстроку (без регистра)."""
    for e in entries:
        if needle.casefold() in e["question"].casefold():
            return e["id"]
    return None


def self_check(entries: list[dict], vectors: list[list[float]]) -> None:
    """Семантическая самопроверка (DoD A2) + top-K соседи для выборочных вопросов."""
    print("\n— Самопроверка —")
    id_jal = find_id(entries, "capital de Jalisco")
    id_nl = find_id(entries, "capital de Nuevo León")
    id_lit = find_id(entries, "muralismo")
    if not all([id_jal, id_nl, id_lit]):
        sys.exit(f"Не нашлись опорные вопросы: jal={id_jal} nl={id_nl} lit={id_lit}")

    v = lambda qid: vectors[qid - 1]
    sim_capitals = cosine(v(id_jal), v(id_nl))
    sim_far = cosine(v(id_jal), v(id_lit))
    print(f"cos(capital Jalisco, capital Nuevo León) = {sim_capitals:.4f}")
    print(f"cos(capital Jalisco, muralismo)          = {sim_far:.4f}")
    if sim_capitals <= sim_far + 0.05:
        sys.exit("ПРОВАЛ: близкие вопросы не ближе далёких — эмбеддинги подозрительны")
    print("OK: столицы заметно ближе друг к другу, чем к литературе")

    for qid in [id_jal, find_id(entries, "Plan de Iguala"), id_lit]:
        e = entries[qid - 1]
        sims = sorted(
            ((cosine(v(qid), vectors[i]), i + 1) for i in range(len(vectors)) if i + 1 != qid),
            reverse=True,
        )[:5]
        print(f"\nTop-5 соседей [{qid}] {e['question'][:70]}")
        for s, nid in sims:
            n = entries[nid - 1]
            print(f"  {s:.4f} [{nid}] {n['question'][:60]} -> {n['answer'][:40]}")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--force", action="store_true", help="пересчитать, даже если файл актуален")
    args = parser.parse_args()

    entries = load_questions()
    if not args.force and existing_is_current(len(entries)):
        print(f"{EMBEDDINGS_BIN} актуален ({len(entries)} векторов) — пересчёт пропущен (--force для форса)")
    else:
        build(entries)
    self_check(entries, read_vectors())


if __name__ == "__main__":
    main()
