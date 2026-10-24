"""Замер скорости локальных моделей Ollama на правилах проекта (stdlib only).

Запуск: python3 docs/local-llm/bench.py [model ...]
Меряет: (1) чат с CLAUDE.md в system prompt — prefill tok/s, генерация tok/s, TTFT;
(2) FIM-автокомплит (только для *coder*) — латентность подсказки.
"""
import json
import pathlib
import sys
import time
import urllib.request

OLLAMA = "http://localhost:11434"
ROOT = pathlib.Path(__file__).resolve().parents[2]
RULES = (ROOT / ".continue/rules/01-mexamen.md").read_text(encoding="utf-8")
OPTIONS = {"temperature": 0.2, "top_p": 0.9, "num_ctx": 16384}

CHAT_PROMPT = (
    "В каком слое и в каком файле должна жить агрегация статистики по темам? "
    "Ответь в 3 пунктах, со ссылкой на правило."
)
FIM_PREFIX = """package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats

object StatsAggregator {
    fun accuracyPercent(stats: QuestionStats): Int {
        val total = stats.timesCorrect + stats.timesWrong
"""
FIM_SUFFIX = """
    }
}
"""


def post(path, body):
    req = urllib.request.Request(
        OLLAMA + path, data=json.dumps(body).encode(), headers={"Content-Type": "application/json"}
    )
    t0 = time.time()
    with urllib.request.urlopen(req, timeout=1800) as r:
        data = json.loads(r.read())
    return data, time.time() - t0


def rate(count, ns):
    return count / (ns / 1e9) if ns else 0.0


def bench_chat(model):
    post("/api/generate", {"model": model, "prompt": "", "keep_alive": "30m"})  # загрузка в RAM
    data, wall = post(
        "/api/chat",
        {
            "model": model,
            "stream": False,
            "options": OPTIONS,
            "messages": [{"role": "system", "content": RULES}, {"role": "user", "content": CHAT_PROMPT}],
        },
    )
    return {
        "prompt_tokens": data.get("prompt_eval_count"),
        "prefill_tok_s": round(rate(data.get("prompt_eval_count", 0), data.get("prompt_eval_duration", 0)), 1),
        "gen_tokens": data.get("eval_count"),
        "gen_tok_s": round(rate(data.get("eval_count", 0), data.get("eval_duration", 0)), 1),
        "ttft_s": round(data.get("prompt_eval_duration", 0) / 1e9, 1),
        "wall_s": round(wall, 1),
        "answer": data["message"]["content"],
    }


def bench_fim(model):
    runs = []
    for _ in range(3):
        data, wall = post(
            "/api/generate",
            {
                "model": model,
                "prompt": FIM_PREFIX,
                "suffix": FIM_SUFFIX,
                "stream": False,
                "options": {"temperature": 0.1, "num_predict": 64, "num_ctx": 2048},
            },
        )
        runs.append(wall)
    return {"latency_s": [round(x, 2) for x in runs], "completion": data["response"]}


def main():
    models = sys.argv[1:] or ["qwen2.5-coder:1.5b", "qwen2.5:3b", "qwen3:4b"]
    for m in models:
        print(f"=== {m}")
        chat = bench_chat(m)
        answer = chat.pop("answer")
        print(json.dumps(chat, ensure_ascii=False))
        print("--- answer:\n" + answer.strip()[:1500])
        if "coder" in m:
            fim = bench_fim(m)
            print("--- FIM:", json.dumps(fim, ensure_ascii=False))
        post("/api/generate", {"model": m, "keep_alive": 0})  # выгрузить, чтобы не делить RAM
        print()


if __name__ == "__main__":
    main()
