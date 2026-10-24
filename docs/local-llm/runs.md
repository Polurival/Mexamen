# Журнал прогонов (локальные модели в Continue/Cursor)

## Задача 2 (День 2, Bug Fix: кнопка → иконка)

### Прогон 1 — `qwen2.5:3b`, Agent, без подсказок
- Время: 20:53 → 21:06 (**13 мин**). Лог: `logs/day2-qwen2.5-3b-run1.json`.
- Вызовы инструментов: **1** — `edit_existing_file` на `StatsViewModel.kt` с `changes: "fun xxxViewModel() { // ... viewModel.resetStats() // ... }"` (плейсхолдер из шаблона CLAUDE.md §7). Файлы перед правкой **не читал**, `StatsScreen.kt` не открывал.
- Реальный diff: +1 пустая строка в `StatsViewModel.kt`. `StatsScreen.kt` не тронут → **задача не решена**.
- Финальный ответ: выдумал «новый» `StatsViewModel.kt` + `StatsScreen` в одном файле (нарушение «один тип на файл»), `stringResource(R.string…)` (антипаттерн §6.5: строковых ресурсов нет), `Icons.Default.Refresh` (material-icons не подключён), `LocalContext` (android-only в commonMain, §6.3), `mutableStateOf` вместо `MutableStateFlow`, `viewModel` внутри Screen (§6.2).
- Сборку **не запускал** — предложил пользователю. Заявил успех («This change introduces a reset button…») — **ложный отчёт**.
- Итог: с первого раза — нет; качество — неприменимо; контекст проекта — не понял (слова из правил скопировал, смысл нет).

### Прогон 2 — `qwen2.5-coder:1.5b`, Agent, без подсказок
- Завершён 21:17 (сессия). Лог: `logs/day2-qwen2.5-coder-1.5b-run2.json`.
- Модель **не умеет нативный tool calling**: выдала вызов текстом (```json {"name": "single_find_and_replace", ...}```); Continue распарсил и применил.
- Понадобилось вмешательство: после первого ответа агент встал, пользователь написал «продолжи выполнение поставленной задачи» → модель **повторила тот же вызов** дословно (зацикливание).
- Правка: переименовала `resetStats()` → `resetStatsDialog()` в `StatsViewModel.kt`. Вызов `viewModel.resetStats()` в `StatsScreen.kt:74` остался → **сборка сломана** (`Unresolved reference`, по grep; gradle не запускался).
- Файлы не читал, `StatsScreen.kt` не трогал, сборку не запускал.
- Итог: с первого раза — нет; вредный diff (ломает компиляцию); контекст — не понял.

### Прогон 3 — `qwen2.5:3b`, Agent, **контекст подан вручную** (`@StatsScreen.kt`, `@AppIcons.kt`)
- Время: 21:20 → 21:46 (**26 мин**, остановлен вручную). Лог: `logs/day2-qwen2.5-3b-run3-context.json`.
- Контекст: правила ~3.8k ток. + файлы ~9.9k символов (~3k ток.) + системный промпт агента Continue с описанием tools ≈ 10k ток.
- Вызов 1: `edit_existing_file` на **правильный** `StatsScreen.kt`, но `changes` — копия исходного файла **без изменений** (кнопка `Button(...) { Text("Reiniciar estadísticas") }` на месте), обёрнутая в ```kotlin → Continue: «Failed to edit file».
- Дальше: модель начала печатать файл целиком в чат, смешала импорты двух файлов (`PathFillType`, `SolidColor` из `AppIcons.kt`) и **зациклилась** на `import ...ImageVector.Companion.asSymbolIcon` (несуществующий API) до ручной остановки.
- diff: пустой. Сборка не запускалась.
- Итог: подача контекста помогла найти файл, но не решить задачу; при `temperature 0.2` без `repeat_penalty` — вырожденный повтор.

### Прогон 4 — «лучшая связка»: `qwen2.5-3b-mexamen` (repeat_penalty 1.15, num_ctx 8192) + compact rules (~0.5k ток.), режим **Edit** (выделена функция `StatsScreen`)
- Время: 21:58 → 22:00 (**2 мин**). Diff: `logs/day2-tuned-run4-edit.diff`.
- Промпт конкретный: что убрать, что добавить, какую иконку (`AppIcons.Refresh`), где (заголовок, `Box`).
- ✅ Нижняя `Button «Reiniciar estadísticas»` удалена, диалог не тронут, испанский текст сохранён, повтора нет.
- ❌ `IconButton` поставлен **внизу**, а не в заголовке; заголовок сдвинут вправо (`Alignment.End`), лишняя правка `Spacer 24→16.dp`.
- ❌ `AppIcons.Refresh(contentDescription = …)` — вызов `ImageVector` как функции (нужно `Icon(AppIcons.Refresh, …)`); не добавлен импорт `IconButton`.
- Сборка (запускал я): **FAILED** — `Unresolved reference 'IconButton'`, `Cannot infer type for type parameter 'T'` (StatsScreen.kt:60).
- Итог: с первого раза — нет; быстро (в 6–13× быстрее агентных прогонов), правки в нужном месте и по смыслу близко; нужна ручная доводка ~3 строк.

## Задача 1 (День 1, фича статистики по темам/вопросам)

### Прогон 5 — «лучшая связка» (`qwen2.5-3b-mexamen` + compact rules), Agent, без подсказок
- Конец: 22:11 (сессия). Лог: `logs/day1-tuned-run5.json`.
- Вызовы инструментов: **0**. Файлы не читал, ничего не изменил (`git status` чистый).
- Ответ — текстом в чате: выдуманный путь `src/main/composables/stats/StatsScreen.kt`, `Scaffold`/`TopAppBar` вместо существующего экрана, захардкоженные «Тема 1» 50/20 вместо данных из `QuestionStats`, **русский UI** («Статистика», «Показать все») при прямом правиле «только испанский», несуществующий `state.asState()`.
- FlashcardScreen/ResultScreen не тронуты: «нужно будет сделать аналогичные изменения… предоставьте код».
- Итог: с первого раза — нет; кода нет; контекст проекта не использован вовсе.
