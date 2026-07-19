# Mexamen

Android-приложение (Compose Multiplatform, Kotlin) — квиз для подготовки к экзамену на натурализацию в Мексике.

## Spec-Driven Development

- **`spec.md`** — что строим и зачем (FR/NFR, архитектура, критерии приёмки). Источник истины.
- **`plan.md`** — задачи по фазам (A1…E2), каждая с Definition of Done. Выполнять по одной, порядок и зависимости — в конце plan.md.

Перед любой реализацией прочитать оба файла. Не рефакторить вне текущей задачи.

## Ключевые решения (зафиксированы, не пересматривать без запроса пользователя)

- LLM: DeepSeek API (`deepseek-chat`) за интерфейсом `LlmProvider`; ключ в `local.properties` (`DEEPSEEK_API_KEY`), не в git.
- Квиз только на испанском.
- RAG: `questions.json` + `embeddings.bin` в ассетах (оффлайн-пайплайн `tools/`, Ollama `nomic-embed-text` 768-dim), косинусный top-K в памяти. Без vector-DB и LLM-фреймворков.
- Квиз обязан работать без сети/ключа: fallback-дистракторы из ответов RAG-соседей.

## Команды

- Сборка: `./gradlew :composeApp:assembleDebug`
- Тесты: `./gradlew :composeApp:testDebugUnitTest`
- Пайплайн данных: `python tools/scrape_guia.py`, затем `python tools/build_embeddings.py` (нужен запущенный Ollama)

## Референсы паттернов

`/media/polurival/Data/AI-Projects/AI-Playground/`: `test_deepseek.py` (DeepSeek HTTP), `week_5_RAG/embedder.py` (Ollama-эмбеддинги), `week_6_local_LLM_service/` (формат spec/plan).
