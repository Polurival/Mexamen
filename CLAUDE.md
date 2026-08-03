# Mexamen

Android-приложение (Compose Multiplatform, Kotlin) — квиз для подготовки к экзамену на натурализацию в Мексике.

## Архитектура (читать перед любой реализацией)

```
UI (Compose M3)      Domain (commonMain)                Data (commonMain)
HomeScreen    ◄────  QuizViewModel
QuizScreen           └─ StartQuizUseCase ──► QuestionRepository (questions.json + embeddings.bin)
ResultScreen              │                  VectorStore (cosine top-K, in-memory, без vector-DB)
                           └──► DistractorGenerator
                                  ├─ LlmProvider (interface) ──► DeepSeekProvider (Ktor)
                                  └─ RagFallback (ответы RAG-соседей)
```

- Поток: `StartQuizUseCase` берёт 10 случайных вопросов → `VectorStore.topK` даёт RAG-контекст → `DistractorGenerator` собирает батч-промпт → `LlmProvider.complete()` → парсинг/валидация → дыры закрывает `RagFallback` → `QuizViewModel` отдаёт UI готовые `QuizQuestion`.
- Модель данных: `QuestionEntry(id, section, question, answer)`, `QuizQuestion(entry, options, correctIndex, source: LLM|FALLBACK)`, `QuizResult(answers, score)`.
- Бизнес-логика — чистый Kotlin в `commonMain` (задел на iOS/Desktop), тестируется без Android-зависимостей.

## Ключевые решения (зафиксированы, не пересматривать без запроса пользователя)

- LLM: DeepSeek API (`deepseek-chat`) за интерфейсом `LlmProvider`; ключ в `local.properties` (`DEEPSEEK_API_KEY`), не в git.
- Квиз только на испанском.
- RAG: `questions.json` + `embeddings.bin` в ассетах (оффлайн-пайплайн `tools/`, Ollama `nomic-embed-text` 768-dim), косинусный top-K в памяти. Без vector-DB и LLM-фреймворков.
- Квиз обязан работать без сети/ключа: fallback-дистракторы из ответов RAG-соседей.
- DI: Koin (`koin-core`/`koin-compose`/`koin-compose-viewmodel`), модули в `commonMain/di/` (`dataModule`, `llmModule`, `viewModelModule` → `appModules`), граф стартует через `KoinApplication` в `App()`.
- Не рефакторить вне текущей задачи.

## Spec-Driven Development (история MVP)

`mvp-spec.md` и `mvp-plan.md` описывают уже реализованный MVP (задачи A1…E2, все выполнены). Читать их только при работе, затрагивающей конкретную область MVP-решений, или по запросу пользователя — не перед каждой задачей.

## Команды

- Сборка: `./gradlew :composeApp:assembleDebug`
- Тесты: `./gradlew :composeApp:testDebugUnitTest`
- Пайплайн данных: `python tools/scrape_guia.py`, затем `python tools/build_embeddings.py` (нужен запущенный Ollama)

## Референсы паттернов

`/media/polurival/Data/AI-Projects/AI-Playground/`: `test_deepseek.py` (DeepSeek HTTP), `week_5_RAG/embedder.py` (Ollama-эмбеддинги), `week_6_local_LLM_service/` (формат spec/plan).
