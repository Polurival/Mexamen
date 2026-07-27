#!/usr/bin/env python3
"""Задача F3 (real-answers-plan.md): syntheticDistractors для непокрытых вопросов.

Для вопросов без realDistractors (нет соответствующей test-страницы среди
test-1..test-10, ~120 из 776) — разовая офлайн-генерация 3 неверных вариантов
через DeepSeek API. Контекст — realDistractors top-K RAG-соседей
(embeddings.bin, тот же cosine top-K, что и в build_embeddings.py); если у
соседей нет реальных примеров, LLM работает только от вопроса и верного
ответа. Тот же принцип, что и в рантайм-генерации (llm/PromptBuilder.kt):
LLM генерирует только неверные варианты, никогда не сам верный ответ.

Ключ: переменная окружения DEEPSEEK_API_KEY, либо строка
`DEEPSEEK_API_KEY=...` в local.properties (корень репозитория).

Разовый прогон, результат коммитится (как embeddings.bin). Идемпотентен:
перезапуск трогает только вопросы, у которых всё ещё меньше 3 realDistractors
и нет syntheticDistractors.
"""

import json
import os
import re
import sys
import time
import unicodedata
from pathlib import Path

import requests

sys.path.insert(0, str(Path(__file__).parent))
from build_embeddings import QUESTIONS_JSON, cosine, read_vectors  # noqa: E402

REPO_ROOT = Path(__file__).parent.parent
LOCAL_PROPERTIES = REPO_ROOT / "local.properties"

API_URL = "https://api.deepseek.com/v1/chat/completions"
MODEL = "deepseek-chat"
TEMPERATURE = 0.7
TIMEOUT = 30
TOP_K = 8
CONTEXT_LIMIT = 6
BATCH_SIZE = int(sys.argv[1]) if len(sys.argv) > 1 else 15
MAX_RETRIES = 2

SYSTEM_PROMPT = """
Eres un redactor experto de exámenes de naturalización de México.
Para cada pregunta del usuario genera exactamente 3 distractores:
respuestas incorrectas pero plausibles, en español, del mismo tipo y
estilo que la respuesta correcta. Usa el campo "context" (respuestas
incorrectas reales de preguntas similares, si las hay) como inspiración
para el estilo y formato de los distractores.
Reglas estrictas:
- Ningún distractor puede coincidir con la respuesta correcta ni repetirse.
- Distractores cortos, como en un examen real de opción múltiple.
- Responde SOLO con JSON válido, sin markdown ni texto adicional, con el formato:
{"items":[{"id":1,"distractors":["...","...","..."]}]}
""".strip()


def fold(text: str) -> str:
    text = unicodedata.normalize("NFKD", text)
    text = "".join(c for c in text if not unicodedata.combining(c))
    text = text.lower()
    text = re.sub(r"[^a-z0-9]+", " ", text)
    return text.strip()


def api_key() -> str:
    key = os.environ.get("DEEPSEEK_API_KEY")
    if key:
        return key
    if LOCAL_PROPERTIES.exists():
        for line in LOCAL_PROPERTIES.read_text(encoding="utf-8").splitlines():
            if line.strip().startswith("DEEPSEEK_API_KEY="):
                return line.split("=", 1)[1].strip()
    sys.exit(
        "DEEPSEEK_API_KEY не найден (ни в окружении, ни в local.properties).\n"
        "Задать: export DEEPSEEK_API_KEY=... либо строка в local.properties."
    )


def call_deepseek(key: str, items: list[dict]) -> dict:
    payload = {
        "model": MODEL,
        "messages": [
            {"role": "system", "content": SYSTEM_PROMPT},
            {"role": "user", "content": json.dumps(items, ensure_ascii=False)},
        ],
        "temperature": TEMPERATURE,
    }
    resp = requests.post(
        API_URL,
        headers={"Authorization": f"Bearer {key}", "Content-Type": "application/json"},
        json=payload,
        timeout=TIMEOUT,
    )
    resp.raise_for_status()
    content = resp.json()["choices"][0]["message"]["content"].strip()
    content = re.sub(r"^```(?:json)?|```$", "", content, flags=re.MULTILINE).strip()
    return json.loads(content)


def validate(distractors, forbidden_folds: set[str]) -> list[str] | None:
    if not isinstance(distractors, list) or len(distractors) != 3:
        return None
    seen: set[str] = set()
    result = []
    for d in distractors:
        if not isinstance(d, str) or not d.strip():
            return None
        f = fold(d)
        if not f or f in forbidden_folds or f in seen:
            return None
        seen.add(f)
        result.append(d.strip())
    return result


def main() -> None:
    questions = json.loads(QUESTIONS_JSON.read_text(encoding="utf-8"))
    vectors = read_vectors()

    targets = [
        q for q in questions
        if len(q.get("realDistractors", [])) < 3 and not q.get("syntheticDistractors")
    ]
    print(f"Вопросов без realDistractors/syntheticDistractors: {len(targets)}")
    if not targets:
        print("Нечего генерировать.")
        return

    key = api_key()

    def context_for(entry: dict) -> list[str]:
        idx = entry["id"] - 1
        sims = sorted(
            ((cosine(vectors[idx], vectors[j]), j) for j in range(len(vectors)) if j != idx),
            reverse=True,
        )[:TOP_K]
        ctx: list[str] = []
        for _, j in sims:
            ctx.extend(questions[j].get("realDistractors", []))
        return ctx[:CONTEXT_LIMIT]

    failures: list[int] = []
    n_batches = (len(targets) + BATCH_SIZE - 1) // BATCH_SIZE
    for b in range(n_batches):
        batch = targets[b * BATCH_SIZE : (b + 1) * BATCH_SIZE]
        items = [
            {
                "id": q["id"],
                "question": q["question"],
                "correct_answer": q["answerVariants"][0],
                "context": context_for(q),
            }
            for q in batch
        ]
        pending = {q["id"]: q for q in batch}

        for attempt in range(1, MAX_RETRIES + 2):
            if not pending:
                break
            request_items = [i for i in items if i["id"] in pending]
            print(f"Батч {b + 1}/{n_batches}, попытка {attempt}: {len(request_items)} вопросов")
            try:
                parsed = call_deepseek(key, request_items)
            except Exception as e:
                print(f"  ошибка запроса: {e}")
                continue
            by_result_id = {it.get("id"): it.get("distractors") for it in parsed.get("items", [])}
            for qid in list(pending):
                entry = pending[qid]
                forbidden = {fold(v) for v in entry["answerVariants"]}
                result = validate(by_result_id.get(qid), forbidden)
                if result is not None:
                    entry["syntheticDistractors"] = result
                    del pending[qid]
            if pending:
                time.sleep(1)

        failures.extend(pending.keys())

    QUESTIONS_JSON.write_text(json.dumps(questions, ensure_ascii=False, indent=2), encoding="utf-8")

    ok = len(targets) - len(failures)
    print(f"Сгенерировано syntheticDistractors: {ok}/{len(targets)}")
    if failures:
        print(f"Не удалось (перезапустить скрипт, идемпотентен): {sorted(failures)}")
    print(f"Сохранено: {QUESTIONS_JSON}")


if __name__ == "__main__":
    main()
