# Эксперимент: правила v1 → v2 (День 1, Rules)

## Тестовый промпт (один и тот же для обеих генераций)

> Помоги улучшить статистику в приложении: на экране StatsScreen нужно показывать количество пройденных вопросов и количество верных ответов для каждой из тем вопросов. На FlashcardScreen на карточке с правильным ответом, нужно показывать сколько раз этот вопрос юзер уже проходил и сколько раз дал верный ответ. Тоже самое для QuizScreen, точнее в ResultScreen - для каждого вопроса показать сколько раз был пройден, и количество верных ответов

## Протокол (чистое сравнение)

Файлы: `CLAUDE.v1.md` — правила ДО (копия из `HEAD` на момент старта), `../../CLAUDE.md` + `.claude/` — v2.

```bash
# Генерация 1 (v1): чистый worktree от коммита c1aae01 — там старый CLAUDE.md, нет .claude/skills|agents
git worktree add ../Mexamen-gen1 c1aae01
cp local.properties ../Mexamen-gen1/          # sdk.dir
cd ../Mexamen-gen1 && claude                   # вставить промпт, БЕЗ уточнений; потом:
git diff > ../gen1.diff && git diff --stat

# Генерация 2 (v2): worktree от коммита с новыми правилами
git add CLAUDE.md .claude docs/rules-experiment .gitignore && git commit -m "rules v2"
git worktree add ../Mexamen-gen2 HEAD
cp local.properties ../Mexamen-gen2/
cd ../Mexamen-gen2 && claude                   # тот же промпт
git diff > ../gen2.diff && git diff --stat
```

Для каждой генерации прогнать: `./gradlew :composeApp:compileDebugKotlinAndroid :composeApp:testDebugUnitTest --console=plain`, сделать скриншоты StatsScreen / FlashcardScreen (карточка с ответом) / ResultScreen.

## Чеклист оценки

| Критерий | Gen 1 (v1) | Gen 2 (v2) |
|---|---|---|
| Компилируется | | |
| Тесты зелёные | | |
| Добавлены тесты на новую логику (`commonTest`) | | |
| Агрегация по темам в `domain/`, не в Composable | | |
| Screen stateless, store не из Composable | | |
| Нет второго источника статистики / новых полей в `QuestionStats` | | |
| Тема = `section` (+ учтены виртуальные темы или явно оговорено) | | |
| Учтена асинхронная запись (в Result не «потерян» последний ответ) | | |
| Тексты на испанском, термины как в приложении | | |
| `App.kt`/DI обновлены, нет лишнего рефакторинга | | |
| Размер diff (файлы / строки) | | |

## Что изменено в правилах v1 → v2 (и почему)

| Изменение | Ожидаемое влияние (гипотеза — проверить по таблице) |
|---|---|
| v1 описывал устаревшую архитектуру (`StartQuizUseCase`, только Android, нет Flashcards/Stats/Navigation3/wasm). v2 — актуальная карта папок и потока данных | Ассистент не тратит ходы на угадывание структуры, правит правильные файлы |
| Раздел «Статистика»: `QuestionStats` per id, кто пишет, кумулятивность, тема = `section`, виртуальные темы | Главный фактор: данные для задачи уже есть (`timesShown/Correct`) — не придумывает новый стор/поля |
| Предупреждение об асинхронной записи в `selectAnswer` | Корректные счётчики в ResultScreen (иначе последний ответ не учтён / off-by-one) |
| 5 эталонных паттернов из кода (object-агрегатор, sealed UiState+VM, stateless Screen, store, Fake-тест) | Единый стиль, логика в `domain/`, тест рядом |
| 7 антипаттернов (логика в Composable, store в Screen, платформенный API в commonMain, дубль фильтра тем, ...) | Меньше нарушений слоёв и wasm-поломок |
| «Definition of done» с конкретными gradle-командами и проводкой `App.kt`/DI | Проверка компиляции/тестов без напоминаний, нет забытых вызовов |
| Скилл `new-screen-feature`, `verify`, субагент `rules-reviewer`, `CLAUDE.local.md` | Процесс по слоям + самопроверка |

## Итог (заполнить после прогонов)

- Разница gen1 → gen2: _…_
- Что больше всего повлияло на качество (топ-3): _…_
- Что ассистент всё ещё не учёл → правка v3: _…_
