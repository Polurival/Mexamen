# Mexamen

Приложение на Compose Multiplatform (Kotlin) для подготовки к экзамену на натурализацию в Мексике (гражданство через SRE): квиз, карточки, статистика, тексты для чтения и практика собеседования. Таргеты — **Android** и **Web (Kotlin/Wasm)**. Экзамен сдаётся на испанском; интерфейс приложения — тоже полностью на испанском.

| Квиз | Тёмная тема | Результат |
|---|---|---|
| ![Quiz](docs/screenshots/quiz_llm_light.png) | ![Dark](docs/screenshots/quiz_offline_dark.png) | ![Result](docs/screenshots/result.png) |

## Какая задача решается

Официальный гид подготовки — ~800 пар «вопрос → ответ» сплошным текстом ([источник](https://ruspansky.com/guia-de-estudios/)). По такому списку неудобно готовиться: нет самопроверки и формата «выбери ответ», как на реальном экзамене. Mexamen превращает гид (776 вопросов в 11 разделах) в квиз и карточки с интервальным повторением: чаще показываются вопросы, на которых пользователь ошибается.

## Возможности

Главный экран (`HomeScreen`) — три блока и переключатель темы оформления. Все экраны:

| Экран | Где в UI | Что делает |
|---|---|---|
| **Главный** (`HomeScreen`) | старт | Блоки «Historia y cultura», «Idioma», «Entrevista»; иконка темы циклически переключает **Sistema / Claro / Oscuro** (выбор сохраняется). |
| **Выбор темы** (`TopicsListScreen`) | «Comenzar quiz» / «Comenzar tarjetas didácticas» | «Todos los temas», **«Repasar errores»** (вопросы с хотя бы одной ошибкой; неактивен с подписью «Sin errores todavía»), «Por Tipo» — виртуальные темы **Fechas / Personajes / Eventos**, «Por Sección» — разделы гида. У каждой темы — число вопросов. |
| **Квиз** (`QuizScreen`) | после выбора темы | 10 вопросов × 4 варианта, таймер 10 мин на сессию, после ответа — подсветка, «💡 Explicación» и «🔗 ¿Sabías que...?» (ближайший по эмбеддингу вопрос). |
| **Результат квиза** (`ResultScreen`) | конец квиза или таймаут | Счёт X/10, сдал/не сдал (порог 8/10, `PassCriteria`), разбор с верными ответами и счётчиками попыток по каждому вопросу; «Intentar de nuevo» / «Volver al inicio». |
| **Карточки** (`FlashcardCardScreen`) | после выбора темы | 10 карточек: вопрос → тап → ответ, объяснение, связанный вопрос и счётчики попыток; оценка **Otra vez / Difícil / Bien / Fácil**. 100% оффлайн. |
| **Итог карточек** (`FlashcardDoneScreen`) | конец сессии | «¡Sesión completada!» и число оценок каждого вида. |
| **Статистика** (`StatsScreen`) | «Estadísticas» | Preguntas totales / intentadas / sin intentar, Respuestas correctas / incorrectas (кумулятивно); блок «Por tema» — точность по темам, слабые темы первыми; сброс статистики с подтверждением. |
| **Банк вопросов** (`QuestionBankScreen`) | «Banco de preguntas» | Все вопросы гида с поиском по вопросу или ответу без учёта регистра и диакритики; карточки раскрываются. |
| **Легенды** (`LegendsListScreen` → `LegendDetailScreen` → `LegendResultScreen`) | «Leyendas» | 10 мексиканских легенд для чтения (часть экзамена по испанскому): текст с аудио и подсветкой слов, 6 вопросов на понимание, таймер 10 мин, порог сдачи 5. |
| **Собеседование** (`InterviewScreen`) | «Entrevista» → «Preguntas» | 16 типовых вопросов интервью; свой ответ на каждый вводится и сохраняется локально. |

**Статистика и хранение.** Для каждого вопроса хранится один `QuestionStats` (вес, показы, верные/неверные, время последнего показа) — в DataStore на Android и в `localStorage` в вебе. Квиз и карточки пишут в него через `RecordAnswerUseCase`; оценки карточек «Otra vez»/«Difícil» считаются ошибкой, «Bien»/«Fácil» — верным ответом.

## Как выбираются вопросы

1. **Пул** — `SessionPoolSelector`: весь датасет, тема (раздел или виртуальная тема, единый фильтр `QuestionDataset.entriesByTopic`) или «Repasar errores» (`timesWrong > 0`; сессия может быть короче 10).
2. **Выбор** — `WeightedQuestionPicker` (заменил старый случайный `QuestionPicker`): взвешенная выборка без возврата (A-ES, ключ `U^(1/weight)`). Вес из `QuestionWeighting`:
   - ответ меняет вес умножением: верно ×0.5, неверно ×1.5, «Difícil» ×1.15, «Fácil» ×0.3, в пределах 1…8;
   - забывание: чем дольше вопрос не показывался, тем выше вес (+100% за каждые 14 дней, максимум ×3 и не выше 8);
   - вопросы, показанные менее 4 ч назад, — в cooldown;
   - не больше 2 вопросов из одного раздела.

   Cooldown и лимит по разделу ослабляются каскадом, если кандидатов не хватает.

## Как участвует AI

У вопросов гида есть только правильный ответ — неверные варианты (дистракторы) приходится добывать отдельно:

- **Реальные дистракторы** — с test-страниц ruspansky.com, сматченные с гидом (657 вопросов), плюс **синтетические**, разово сгенерированные LLM оффлайн для оставшихся 120 (см. «Реальные варианты ответов» ниже). Итого у всех 776 вопросов есть ≥3 дистрактора без рантайм-LLM.
- **RAG** — собственный, без фреймворков и vector-DB. Эмбеддинги всех вопросов посчитаны оффлайн (Ollama `nomic-embed-text`, 768-dim) и лежат в ассетах (`embeddings.bin`). На устройстве — косинусный top-K полным перебором в памяти (`VectorStore`). Соседи нужны для дистракторов-кандидатов и для блока «¿Sabías que...?».
- **LLM в рантайме** — Groq `llama-3.3-70b-versatile` (`GroqProvider`, таймаут 30 с) за интерфейсом `LlmProvider`; `LlmDistractorGenerator` отправляет один батч-запрос на весь квиз (вопрос, все формулировки верного ответа, реальные дистракторы как примеры стиля) и валидирует ответ поэлементно (ровно 3, без совпадений с верным ответом, примерами и между собой, case/accent-insensitive). LLM генерирует только неверные варианты.

> **Текущее состояние кода:** в `di/LlmModule.kt` как `DistractorGenerator` зарегистрирован `FallbackDistractorGenerator` — квиз **не обращается к LLM**, дистракторы берутся из реальных/синтетических вариантов вопроса и его RAG-соседей. `GroqProvider` зарегистрирован как `LlmProvider`, но рантайм-квизом не используется; LLM-путь (`LlmDistractorGenerator` + `GroqProvider`) покрыт юнит-тестами и ручными smoke-тестами и включается заменой регистрации на `LlmDistractorGenerator(get())`. Бейдж «⚡ Modo offline» в квизе сейчас закомментирован.

```
┌────────────── Оффлайн (машина разработчика) ──────────────┐
│ scrape_guia.py ─► questions.json ─► build_embeddings.py   │
│ match_real_answers.py / apply_review.py (реальные)        │
│ generate_synthetic_distractors.py (DeepSeek, разово)      │
│                                     ─► embeddings.bin     │
└──────────────────────────┬────────────────────────────────┘
                           │ commit в composeResources/files
┌──────────── Приложение (commonMain) ─▼────────────────────┐
│ QuizViewModel ─► BuildQuizUseCase                         │
│    ├─ SessionPoolSelector (тема / Repasar errores)        │
│    ├─ WeightedQuestionPicker ◄─ QuestionStatsStore        │
│    ├─ VectorStore (cosine top-8) ─► PromptBuilder.context │
│    ├─ DistractorGenerator                                 │
│    │    ├─ FallbackDistractorGenerator  (в DI сейчас)     │
│    │    └─ LlmDistractorGenerator ─► LlmProvider ─► Groq  │
│    └─ RagFallback (если кандидатов < 3)                   │
│ FlashcardViewModel ─► BuildFlashcardsUseCase (без LLM)    │
│ Ответы ─► RecordAnswerUseCase ─► QuestionWeighting ─► store│
└───────────────────────────────────────────────────────────┘
```

**Graceful degradation:** квиз и карточки работают без сети и без ключа. Если LLM-путь включён, при любой ошибке (нет сети, нет ключа, таймаут, HTTP 429, кривой JSON) `LlmUnavailable` уводит квиз целиком в `RagFallback`; частично невалидный ответ LLM закрывается fallback-ом поэлементно.

## Запуск пайплайна данных

Одноразово, на машине разработчика (артефакты закоммичены):

```bash
pip install -r tools/requirements.txt
python tools/scrape_guia.py        # гид → tools/out/questions.json (+ кэш HTML)
ollama pull nomic-embed-text       # нужен запущенный Ollama
python tools/build_embeddings.py   # → tools/out/embeddings.bin (+ самопроверка top-K)
# затем скопировать оба файла в composeApp/src/commonMain/composeResources/files/
```

Доп. скрипты (разовые, результат закоммичен): `match_real_answers.py` + `apply_review.py` — реальные дистракторы; `generate_synthetic_distractors.py` и `generate_explanations.py` — DeepSeek API (`DEEPSEEK_API_KEY`), только оффлайн-генерация данных; `generate_audio.py` — озвучка легенд (Google Cloud TTS, mp3 + тайминги слов в `composeResources/files/audio/`).

## Сборка и запуск

```bash
# Ключ Groq (опционально — квиз и без него работает полностью):
echo 'GROQ_API_KEY=gsk_...' >> local.properties

./gradlew :androidApp:assembleDebug                  # APK: androidApp/build/outputs/apk/debug/
./gradlew :composeApp:compileAndroidMain             # компиляция shared-модуля (Android)
./gradlew :composeApp:compileKotlinWasmJs            # компиляция shared-модуля (Wasm)
./gradlew :composeApp:testAndroidHostTest            # тесты (commonTest + androidHostTest) на JVM
```

Ключ не хранится в git: `local.properties` (или env `GROQ_API_KEY`) → `BuildConfig` → `AppConfig`. В веб-сборке ключа нет. Требования: Android 8.0+ (API 26).

### Web (Wasm / PWA)

Приложение также собирается под Web (Kotlin/Wasm) — можно пользоваться в браузере или «установить» на iOS как Progressive Web App без App Store.

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun    # локальный dev-сервер
./gradlew :composeApp:wasmJsBrowserDistribution      # сборка для публикации
```

Результат продакшен-сборки (статические файлы): `composeApp/build/dist/wasmJs/productionExecutable` (для Vercel — `vercel.json` с SPA-rewrite).

Для «установки» на iOS:
1. Откройте URL приложения в **Safari**.
2. Нажмите кнопку **«Поделиться»** (Share).
3. Выберите **«На экран „Домой“»** (Add to Home Screen).

## Тестирование

**Уровень 1 — код.** `./gradlew :composeApp:testAndroidHostTest` гоняет `commonTest` (domain, llm, data, ViewModel-ы с in-memory fake-store и фиксированными часами, Ktor `MockEngine`) и `androidHostTest` (реальные ассеты: репозиторий, семантика эмбеддингов). Текущий прогон (`composeApp/build/test-results/testAndroidHostTest`, 2026-10-25): **179 тестов в 36 классах, 0 падений, 2 пропущено** — это smoke-тесты с реальной сетью, они пропускаются без `RUN_GROQ_SMOKE=1`.

Ручные smoke-тесты с реальным ключом Groq и сетью (не для CI):

```bash
RUN_GROQ_SMOKE=1 ./gradlew :composeApp:testAndroidHostTest --tests '*SmokeTest*'
```

**Уровень 2 — UI smoke** на физическом Android-устройстве по USB: сценарии `smoke/scenarios/S1…S7` (квиз, карточки, статистика и сброс, отмена сброса, легенды, банк вопросов, тема оформления). Прогоняет агент через MCP `mobile` или раннер `smoke/tools/run_smoke.py` — см. [smoke/README.md](smoke/README.md).

## Реальные варианты ответов (real-answers-plan, F1…I1)

Дистракторы, где возможно, — не генерация, а реальные неверные варианты, взятые с test-страниц ruspansky.com (WATU-квиз, test-1…test-10) и сматченные с `questions.json` по тексту вопроса. Формулировка верного ответа иногда различается между гидом и test-страницей — обе хранятся, и при сборке квиза показывается случайная (`answerVariants`, G2); дистракторы дедуплицируются против всех вариантов.

Покрытие (776 вопросов, по `questions.json`):

| Источник дистракторов | Вопросов | Комментарий |
|---|---|---|
| `realDistractors` (реальные, с test-страниц) | 657 | у 656 — ≥3 варианта (F1 авто + F2 ручная разметка) |
| `syntheticDistractors` (LLM, оффлайн, разово) | 120 | нет test-страницы для вопроса или реальных < 3 (F3) |
| **Итого с ≥3 дистракторами без обращения к рантайм-LLM** | **776 / 776** | |
| `answerVariants` с альтернативной формулировкой (>1 варианта) | 155 | остальные 621 — один вариант (совпадает с гидом) |

Кандидаты для `FallbackDistractorGenerator` (`PromptBuilder.buildContext`): свои `realDistractors` + `realDistractors` RAG-соседей; если их нет — ответы соседей. Если валидных кандидатов меньше трёх, срабатывает `RagFallback` (G3): свои `realDistractors` → соседские `realDistractors` → свои `syntheticDistractors` → ответы RAG-соседей (с расширением top-K до всего датасета).

## Структура

- `composeApp/` — KMP-модуль: `ui` (экраны + ViewModel), `domain` (чистая логика), `data` (датасет, хранилища), `llm`, `di` (Koin); `androidMain`/`wasmJsMain` — только `actual`-реализации.
- `androidApp/` — Android application: точка входа `MainActivity`, ресурсы, проброс ключа.
- `composeApp/src/commonTest`, `androidHostTest` — тесты (kotlin-test, coroutines-test, Ktor MockEngine).
- `smoke/` — UI smoke-сценарии и раннер для физического устройства.
- `tools/` — Python-пайплайн данных (скрейпер, матчинг, эмбеддинги, генерация, аудио).
- `CLAUDE.md` — правила проекта (архитектура, паттерны, «готово»).
- История: `mvp-spec.md`, `mvp-plan.md` (MVP 2026-07; тогда провайдером LLM был DeepSeek, позже заменён на Groq), `real-answers-plan.md`.
