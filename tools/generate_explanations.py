#!/usr/bin/env python3
"""Генерация осмысленных объяснений для вопросов с плейсхолдерами.

Около 284 вопросов имеют плейсхолдер:
"Este es un dato fundamental sobre la historia, geografía o cultura que ayuda a comprender la identidad nacional."
Этот скрипт использует DeepSeek API для замены их на короткие, полезные объяснения (2-3 предложения) на испанском.
"""

import json
import os
import re
import sys
import time
from pathlib import Path

import requests

APP_DATA_DIR = Path(__file__).parent.parent / "composeApp" / "src" / "commonMain" / "composeResources" / "files"
QUESTIONS_JSON = APP_DATA_DIR / "questions.json"
REPO_ROOT = Path(__file__).parent.parent
LOCAL_PROPERTIES = REPO_ROOT / "local.properties"

API_URL = "https://api.deepseek.com/v1/chat/completions"
MODEL = "deepseek-chat"
TEMPERATURE = 0.7
TIMEOUT = 30
BATCH_SIZE = int(sys.argv[1]) if len(sys.argv) > 1 else 15
MAX_RETRIES = 2

PLACEHOLDER = "Este es un dato fundamental sobre la historia, geografía o cultura que ayuda a comprender la identidad nacional."

SYSTEM_PROMPT = """
Eres un profesor experto en historia y cultura de México.
Para cada pregunta y respuesta correcta proporcionada, genera una explicación breve
(máximo 2-3 frases) en español. La explicación debe aportar contexto histórico,
geográfico o cultural útil para un estudiante que se prepara para el examen de naturalización.
Reglas:
- Sé preciso y educativo.
- Responde SOLO con JSON válido, sin markdown ni texto adicional, con el formato:
{"items":[{"id":1,"explanation":"..."}]}
""".strip()

def api_key() -> str:
    key = os.environ.get("DEEPSEEK_API_KEY")
    if key:
        return key
    if LOCAL_PROPERTIES.exists():
        for line in LOCAL_PROPERTIES.read_text(encoding="utf-8").splitlines():
            if line.strip().startswith("DEEPSEEK_API_KEY="):
                return line.split("=", 1)[1].strip()
    sys.exit("DEEPSEEK_API_KEY не найден.")

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

def main() -> None:
    if not QUESTIONS_JSON.exists():
        sys.exit(f"Файл {QUESTIONS_JSON} не найден.")

    questions = json.loads(QUESTIONS_JSON.read_text(encoding="utf-8"))
    targets = [q for q in questions if q.get("explanation") == PLACEHOLDER]

    print(f"Вопросов с плейсхолдерами: {len(targets)}")
    if not targets:
        print("Ничего не нужно обновлять.")
        return

    key = api_key()
    n_batches = (len(targets) + BATCH_SIZE - 1) // BATCH_SIZE

    for b in range(n_batches):
        batch = targets[b * BATCH_SIZE : (b + 1) * BATCH_SIZE]
        items = [{"id": q["id"], "question": q["question"], "answer": q["answer"]} for q in batch]

        print(f"Батч {b + 1}/{n_batches}...")
        try:
            result = call_deepseek(key, items)
            explanations = {it["id"]: it["explanation"] for it in result.get("items", [])}

            for q in batch:
                if q["id"] in explanations:
                    q["explanation"] = explanations[q["id"]]

            # Сохраняем после каждого батча
            QUESTIONS_JSON.write_text(json.dumps(questions, ensure_ascii=False, indent=2), encoding="utf-8")
        except Exception as e:
            print(f"Ошибка в батче {b + 1}: {e}")
            time.sleep(2)

    print("Готово.")

if __name__ == "__main__":
    main()
