# Mexamen

Приложение на Compose Multiplatform (Kotlin) для подготовки к экзамену на натурализацию в Мексике (гражданство через SRE). Таргеты: **Android** и **Web** (Kotlin/Wasm, PWA). Экзамен сдаётся на испанском, поэтому весь интерфейс приложения тоже на испанском.

| Квиз | Тёмная тема | Результат |
|---|---|---|
| ![Quiz](docs/screenshots/quiz_llm_light.png) | ![Dark](docs/screenshots/quiz_offline_dark.png) | ![Result](docs/screenshots/result.png) |

Скриншоты сняты на версии MVP. Актуальный список экранов приведён в разделе «Возможности».

## Какая задача решается

Официальный гид подготовки ([источник](https://ruspansky.com/guia-de-estudios/)) — это сплошной текст из пар «вопрос → ответ», разбитый на тематические разделы. Готовиться по нему неудобно: нет самопроверки и нет формата «выбери ответ», как на реальном экзамене. Mexamen превращает гид в датасет из **776 вопросов в 11 разделах** (`composeResources/files/questions.json`) и строит на нём квиз, карточки, поиск и статистику. Отдельно есть тренировка испанского по легендам и подготовка к собеседованию.

## Возможности

Все экраны открываются с главного (маршруты описаны в `ui/navigation/Routes.kt`). Квиз, карточки, легенды и статистика работают без сети и без API-ключа.

| Экран (`Route`) | Как открыть | Что делает |
|---|---|---|
| Главный (`Home`) | старт приложения | Три карточки: «Historia y cultura», «Idioma», «Entrevista». Вверху справа кнопка темы оформления: Sistema → Claro → Oscuro. Выбор сохраняется (`ThemeModeStore`) |
| Список тем (`TopicsList`) | «Comenzar quiz» / «Comenzar tarjetas didácticas» | «Todos los temas»; «Repasar errores» — вопросы, в которых была хотя бы одна ошибка (без ошибок пункт неактивен: «Sin errores todavía»); «Por Tipo» — виртуальные темы Fechas / Personajes / Eventos; «Por Sección» — разделы гида. Каждая тема показывает число вопросов |
| Квиз (`Quiz`) | выбор темы для квиза | 10 вопросов по 4 варианта, таймер 10 минут (последнюю минуту подсвечен). После ответа показываются объяснение и «¿Sabías que...?» — ближайший по эмбеддингам вопрос |
| Результат квиза | конец квиза или истёк таймер | «Resultado: X/10». Квиз сдан при ≥ 8 верных (`ExamRules`). Разбор каждого вопроса: ✓/✗, «Correcto: …», накопленные счётчики «Pasada N veces · Correctas: M». Кнопки «Intentar de nuevo» и «Volver al inicio» |
| Карточки (`Flashcards`) | выбор темы для карточек | 10 карточек. По тапу открывается ответ, объяснение и «¿Sabías que...?». На обороте карточки показываются и накопленные счётчики. Оценки: Otra vez / Difícil / Bien / Fácil. В конце — «¡Sesión completada!» с количеством оценок каждого вида |
| Банк вопросов (`QuestionBank`) | «Banco de preguntas» | Все 776 вопросов с ответами. Поиск по вопросу и ответу без учёта регистра и диакритики (`QuestionSearch`): каждое слово запроса должно найтись. Счётчик «Preguntas: X de Y», по тапу на вопрос открывается «Explicación» |
| Статистика (`Stats`) | «Estadísticas» | Preguntas totales / intentadas / sin intentar, Respuestas correctas / incorrectas. Блок «Por tema» с точностью по каждому разделу («Aciertos: NN %»), самые слабые темы идут первыми. Сброс статистики с подтверждением. Счётчики считаются за всё время |
| Легенды (`LegendsList`) | «Leyendas» (карточка «Idioma») | 10 мексиканских легенд (`data/Legend.kt`) |
| Легенда (`LegendDetail`) | выбор легенды | Текст и аудио (mp3) с подсветкой текущего слова, 6 вопросов на понимание прочитанного, таймер 10 минут, кнопка «Finalizar y Calificar» |
| Результат легенды (`LegendResult`) | завершение или истёк таймер | «Resultado: X / 6». Тест сдан при ≥ 5 верных |
| Собеседование (`Interview`) | «Preguntas» (карточка «Entrevista») | 16 типичных вопросов собеседования в 4 категориях. Свой ответ можно записать и сохранить (`InterviewStore`) |

Хранилище данных: на Android — DataStore, в вебе — `localStorage`. Статистика по вопросам, ответы на собеседовании и тема оформления хранятся отдельно.

## Выбор вопросов: статистика и веса

Квиз и карточки берут вопросы не случайно, а по весам: вопросы, в которых чаще ошибаешься и которые давно не встречались, попадаются чаще.

- **Статистика.** Для каждого id вопроса хранится один `QuestionStats(weight, timesShown, timesCorrect, timesWrong, lastSeenEpochMillis)` в `QuestionStatsStore` (весь объект хранится одним JSON). Квиз и карточки записывают ответы через `RecordAnswerUseCase` → `QuestionWeighting`.
- **Обновление веса** (`QuestionWeighting`). Вес ограничен диапазоном [1, 8]. Верный ответ в квизе умножает вес на 0.5, неверный — на 1.5. Оценки карточек: Otra vez ×1.5, Difícil ×1.15 (обе считаются ошибкой), Bien ×0.5, Fácil ×0.3 (обе считаются верным ответом).
- **Эффективный вес** при выборе вопросов: `weight × (1 + min(дней с последнего показа / 14, 2))`, не больше 8. Так работает кривая забывания: давно не виденный вопрос поднимается в выборке.
- **Пул сессии** (`SessionPoolSelector`): все вопросы, «Repasar errores» (`timesWrong > 0`, сессия сокращается до числа таких вопросов) или тема. Фильтр по теме один на всё приложение: `QuestionRepository.filterByTopic`.
- **Выбор** (`WeightedQuestionPicker`): взвешенная выборка без возврата (A-ES, ключ `U^(1/вес)`). Ограничения: не больше 2 вопросов из одного раздела, пропуск вопросов, показанных менее 4 часов назад. Если вопросов не хватает, ограничения ослабляются по очереди, чтобы сессия всегда набрала нужное количество.

## Как участвует AI

У вопросов гида есть только правильный ответ, неправильных вариантов нет. Дистракторы для квиза получаются тремя способами.

- **Реальные и синтетические варианты (оффлайн, в датасете).** `realDistractors` — настоящие неверные варианты с test-страниц ruspansky.com. `syntheticDistractors` сгенерированы один раз через LLM (DeepSeek, скрипт `tools/generate_synthetic_distractors.py`) для вопросов, у которых test-страницы нет. Объяснения к вопросам тоже сгенерированы оффлайн через DeepSeek (`tools/generate_explanations.py`).
- **RAG** — собственный, без фреймворков и vector-DB. Эмбеддинги всех вопросов посчитаны оффлайн (Ollama `nomic-embed-text`, 768-dim) и лежат в ассетах (`embeddings.bin`, ~2.4 MB). На устройстве `VectorStore` считает косинусный top-K полным перебором в памяти. Соседи дают контекст для дистракторов (`PromptBuilder.buildContext`: свои и соседские `realDistractors`, а если их нет — ответы соседей) и служат резервом (`RagFallback`). Ближайший сосед показывается как «¿Sabías que...?».
- **Рантайм-LLM** — Groq `llama-3.3-70b-versatile` (`GroqProvider` за интерфейсом `LlmProvider`), используется в `LlmDistractorGenerator`. Квиз отправляет один батч-запрос, ответ проверяется поэлементно: ровно 3 варианта, без совпадений с любой формулировкой верного ответа, с примерами из контекста и между собой, сравнение без учёта регистра и акцентов. Любая ошибка (нет ключа, нет сети, HTTP 429, таймаут 30 с, некорректный JSON) превращается в `LlmUnavailable`, и квиз переходит на fallback.

**Что подключено в текущей сборке.** В `di/LlmModule.kt` `DistractorGenerator` привязан к `FallbackDistractorGenerator`. Это локальный генератор: он берёт первые 3 корректных варианта из RAG-контекста и в сеть не ходит. `GroqProvider` зарегистрирован как `LlmProvider`, но в приложении его никто не запрашивает. `LlmDistractorGenerator(GroqProvider())` используется только в тестах и в ручном `DistractorSmokeTest`. В вебе ключа нет вообще (`groqApiKey()` = `null`). Чтобы включить LLM на Android, достаточно привязать в `LlmModule.kt` `DistractorGenerator` к `LlmDistractorGenerator(get())`. Бейдж «⚡ Modo offline» (`OfflineBadge`) сейчас не показывается: его вызов в `QuizScreen.kt` закомментирован.

```
┌──────────── Оффлайн (машина разработчика, tools/) ─────────────────┐
│ scrape_guia.py ─► questions.json ─► build_embeddings.py (Ollama)    │
│                                      └─► embeddings.bin (768-dim)   │
│ match_real_answers.py + apply_review.py ─► realDistractors,         │
│                                            answerVariants           │
│ generate_synthetic_distractors.py, generate_explanations.py         │
│   (DeepSeek) ─► syntheticDistractors, explanation                   │
│ generate_audio.py (Google Cloud TTS) ─► audio/<легенда>.mp3 + .json │
└──────────────────────────────┬──────────────────────────────────────┘
                               │ копируются в composeResources/files
┌──────────── Приложение (commonMain) ─────────▼──────────────────────┐
│ QuizViewModel ─► BuildQuizUseCase.build(topic)                      │
│    ├─ QuestionStatsStore.readAll()                                  │
│    ├─ SessionPoolSelector (все / Repasar errores / тема)            │
│    ├─ WeightedQuestionPicker (A-ES по весу, ≤2 на раздел,           │
│    │                          cooldown 4 ч) ─► 10 вопросов          │
│    ├─ VectorStore (cosine top-8) ─► PromptBuilder.buildContext      │
│    ├─ DistractorGenerator                                           │
│    │    ├─ FallbackDistractorGenerator  ◄── подключён в DI          │
│    │    └─ LlmDistractorGenerator ─► LlmProvider ─► GroqProvider    │
│    │                                   (тесты / smoke)              │
│    └─ RagFallback (если генератор не дал 3 варианта)                │
│ ответ ─► RecordAnswerUseCase ─► QuestionWeighting ─► Store.update   │
│ FlashcardViewModel ─► BuildFlashcardsUseCase (тот же пул + picker)  │
└─────────────────────────────────────────────────────────────────────┘
```

## Реальные варианты ответов (real-answers-plan, F1…I1)

Дистракторы по возможности не генерируются, а берутся реальные: неверные варианты с test-страниц ruspansky.com (WATU-квиз, test-1…test-10), сопоставленные с `questions.json` по тексту вопроса. Формулировка верного ответа на test-странице иногда отличается от гида. Хранятся обе (`answerVariants`, G2), и при сборке квиза показывается случайная.

Покрытие (776 вопросов, пересчитано по текущему `questions.json`):

| Источник дистракторов | Вопросов | Комментарий |
|---|---|---|
| `realDistractors` (реальные, с test-страниц) | 657 | 656 сопоставлены автоматически или вручную (F1+F2) |
| `syntheticDistractors` (LLM, оффлайн, один раз) | 119 | для вопроса нет test-страницы (F3) |
| **Итого с ≥3 дистракторами без обращения к рантайм-LLM** | **776 / 776** | |
| `answerVariants` с альтернативной формулировкой (>1 варианта) | 155 | у остальных 621 один вариант (совпадает с гидом) |

Приоритет источников в `RagFallback` (G3): свои `realDistractors` → `realDistractors` соседей → свои `syntheticDistractors` → ответы RAG-соседей. Последний вариант — крайний резерв: при текущем покрытии 776/776 он не срабатывает, но код его поддерживает.

## Запуск пайплайна данных

Выполняется один раз на машине разработчика. Готовые артефакты уже лежат в репозитории.

```bash
pip install -r tools/requirements.txt
python tools/scrape_guia.py        # гид → tools/out/questions.json (+ кэш HTML)
ollama pull nomic-embed-text       # нужен запущенный Ollama
python tools/build_embeddings.py   # → tools/out/embeddings.bin (+ самопроверка top-K)
# затем скопировать оба файла в composeApp/src/commonMain/composeResources/files/
```

Скриптам `generate_synthetic_distractors.py` и `generate_explanations.py` нужен `DEEPSEEK_API_KEY` (переменная окружения или `local.properties`). Скрипту `generate_audio.py` нужны `google-cloud-texttospeech` (в `requirements.txt` его нет) и `GOOGLE_APPLICATION_CREDENTIALS`.

## Сборка и запуск

```bash
# Ключ Groq (необязателен; в текущей сборке не используется, см. «Как участвует AI»):
echo 'GROQ_API_KEY=gsk_...' >> local.properties

./gradlew :androidApp:assembleDebug                          # APK: androidApp/build/outputs/apk/debug/
./gradlew :composeApp:compileAndroidMain --console=plain     # компиляция Android-части shared-модуля
./gradlew :composeApp:compileKotlinWasmJs --console=plain    # компиляция Web-части (проверка совместимости с wasm)
./gradlew :composeApp:testAndroidHostTest --console=plain    # юнит-тесты shared-модуля (commonTest + androidHostTest)
```

Ключ не хранится в git: `local.properties` (или переменная окружения `GROQ_API_KEY`) → `BuildConfig` → `AppConfig.groqApiKey` (`MainActivity`). Требования: Android 8.0+ (API 26).

### Web (Wasm / PWA)

Приложение собирается и под Web (Kotlin/Wasm). Его можно открыть в браузере или «установить» на iOS как Progressive Web App, без публикации в App Store.

```bash
./gradlew :composeApp:wasmJsBrowserDevelopmentRun   # локальный dev-сервер (webpack)
./gradlew :composeApp:wasmJsBrowserDistribution     # сборка для публикации
```

Готовые статические файлы для продакшена: `composeApp/build/dist/wasmJs/productionExecutable` (SPA-rewrite для Vercel — `vercel.json`).

Как «установить» на iOS:
1. Откройте URL приложения в **Safari**.
2. Нажмите кнопку **«Поделиться»** (Share).
3. Выберите **«На экран „Домой“»** (Add to Home Screen).

## Тестирование

Тестирование идёт на двух уровнях.

**Уровень 1 — код.** `./gradlew :composeApp:testAndroidHostTest --console=plain` запускает `commonTest` (domain, llm, ViewModel'и с in-memory fake из `commonTest/.../testing/`, Ktor `MockEngine`) и `androidHostTest` (репозиторий и семантика RAG на реальных ассетах). Результат последнего прогона (2026-10-25, сумма по XML в `composeApp/build/test-results/testAndroidHostTest/`): **205 тестов, 0 failures, 0 errors**, из них 2 пропущены. Пропущены `GroqSmokeTest` и `DistractorSmokeTest`: без `RUN_GROQ_SMOKE=1` они отключаются через `assumeTrue`.

Ручные интеграционные проверки с реальным ключом Groq и сетью (не для CI):

```bash
RUN_GROQ_SMOKE=1 ./gradlew :composeApp:testAndroidHostTest --tests '*SmokeTest*'
```

**Уровень 2 — UI smoke.** Сценарии `smoke/scenarios/S1…S8` проходятся на физическом Android-устройстве по USB. Их выполняет агент через MCP `mobile` или запасной раннер `smoke/tools/run_smoke.py`. Сценарии покрывают квиз, карточки, статистику и сброс, легенды, «Repasar errores», «Banco de preguntas» и тему оформления. Подробности — в [`smoke/README.md`](smoke/README.md).

## История

- MVP принят 2026-07-18 по критериям `mvp-spec.md` §10: APK собирается, квиз 10 × 4, работа в авиарежиме, экран результата, 45 юнит-тестов. Тогда рантайм-провайдером LLM был DeepSeek, позже его заменили на Groq. `mvp-spec.md` и `mvp-plan.md` оставлены как исторические документы, в них по-прежнему упоминаются DeepSeek, `QuestionPicker` и `testDebugUnitTest`.
- Реальные варианты ответов: `real-answers-plan.md` (F1…I1).

## Структура

- `composeApp/` — KMP-модуль: `data` / `domain` / `llm` / `ui` / `di` в `commonMain`, платформенные `actual` в `androidMain` и `wasmJsMain`.
- `androidApp/` — Android application-модуль: точка входа `MainActivity`, ресурсы, `BuildConfig` с ключом.
- `composeApp/src/commonTest`, `composeApp/src/androidHostTest` — тесты (kotlin-test, kotlinx-coroutines-test, Ktor MockEngine).
- `smoke/` — UI smoke-сценарии и раннер.
- `tools/` — Python-пайплайн данных (скрейпер, эмбеддинги, сопоставление реальных ответов, генерация дистракторов, объяснений и аудио).
- `mvp-spec.md`, `mvp-plan.md`, `real-answers-plan.md` — история проектирования.
