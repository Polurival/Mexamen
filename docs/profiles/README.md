# Профили ассистента (День 2)

Профиль = набор правил для агентного режима, между которыми можно переключаться. Реализация — subagent-файлы Claude Code в `.claude/agents/`:

| Профиль | Файл | Инструменты (`tools:`) | Запуск |
|---|---|---|---|
| Bug Fix | `.claude/agents/bug-fix.md` | Read, Grep, Glob, **Edit, Write**, Bash | `claude --agent bug-fix` |
| Research | `.claude/agents/research.md` | Read, Grep, Glob, Bash (**без Edit/Write**) | `claude --agent research` |
| Code Review (бонус) | `.claude/agents/review.md` | Read, Grep, Glob, Bash (**без Edit/Write**) | `claude --agent review` |

Переключение внутри сессии: `/agents` или `@bug-fix`, `@research`, `@review`.
«НЕ ДОЛЖЕН менять код» в Research/Review обеспечено двумя слоями: инструкцией и отсутствием `Edit`/`Write` в `tools:`.

В каждом файле: системный промпт → «Что ДОЛЖЕН делать» → «Что НЕ должен делать» → «Формат ответа».

## Проверка на реальных задачах

Запуск без участия человека: `claude --agent <профиль> -p "<задача>" --output-format stream-json --verbose`.
Логи: `logs/*.txt` (читаемые), `logs/*.jsonl` (сырые события). Прогоны сделаны 2026-10-20.

### Bug Fix

Задача: «на экране Статистики кнопка «Reiniciar estadísticas» (внизу, во всю ширину) занимает много места; нужна иконка в правом верхнем углу, действие то же (диалог + `viewModel.resetStats()`)».

| | Попытка 1 | Попытка 2 (после доработки профиля) |
|---|---|---|
| Лог | `logs/bug-fix-run1.txt` | `logs/bug-fix-run2.txt` |
| Причина найдена | да (`StatsScreen.kt`, нижний `Button`) | да |
| Фикс | рабочий: `Box` в заголовке + `IconButton(AppIcons.Refresh)`, диалог не тронут | тот же по сути |
| Проверка | **провал**: `compileDebugKotlinAndroid`/`testDebugUnitTest` → `task not found`; агент сам нашёл `compileAndroidMain`/`testAndroidHostTest`, но потратил 3 вызова Gradle | с первого вызова: `compileAndroidMain compileKotlinWasmJs testAndroidHostTest` → PASS |
| Отчёт о тестах | «число тестов не смотрел» (вывод обрезан `tail`) | 30 testsuite, failures=0, errors=0 (всего 150 тестов, из XML) |

Итоговое изменение (`git diff` на `StatsScreen.kt`): кнопка удалена, в заголовке `Box` с центрированным текстом и `IconButton` справа с `contentDescription = "Reiniciar estadísticas"`; импорты обновлены; `onClick` — прежний `showConfirmDialog = true`.

**Что доработано в профиле после попытки 1**
1. Команды проверки. Профиль (и `CLAUDE.md`) называли `compileDebugKotlinAndroid` и `testDebugUnitTest`. Проект на `com.android.kotlin.multiplatform.library`, там задач с такими именами нет. В профиль внесены реальные: `compileAndroidMain`, `testAndroidHostTest`, `compileKotlinWasmJs` + правило «задача не найдена → `gradlew tasks --all`, проверку не пропускать».
2. Число тестов берётся из `build/test-results/*.xml`, а не из обрезанного `tail`.
3. Правило про UI: иконки только из `ui/AppIcons.kt` (`material-icons` не подключён), зависимости не добавлять.
4. Fallback `grep -rn` через Bash, если `Grep`/`Glob` недоступны (в headless-режиме `-p` их не оказалось).

### Research

Вопрос: «Какие классы и use case в `domain/` и `llm/` не покрыты тестами? Покажи, чего именно не хватает.»
Проверка ответа вручную: `GetRelatedQuestionUseCase`, `BuildFlashcardsUseCase`, `QuizResult`/`AnsweredQuestion` действительно не упоминаются ни в `commonTest`, ни в `androidHostTest`; в `di/` use case не зарегистрированы.

| | Попытка 1 | Попытка 2 | Попытка 3 |
|---|---|---|---|
| Лог | `logs/research-run1.txt` | `logs/research-run2.txt` | `logs/research-run3.txt` |
| Главный вывод | верный (3 непокрытых класса) | неполный: `QuizResult` назван «data-классом без логики» | полный и верный |
| Проверка DI | пропущена («не искал») | проверена | проверена |
| Заблокированные команды | 2 (`cd … && git grep`, цикл с переменной) | 0 | 0 |
| Правок кода | 0 | 0 | 0 |

**Что доработано в профиле**
- После попытки 1: обязательная проверка DI (`grep` по `di/`) с явным «не нашёл — это результат»; запрет `cd … &&`, переменных и циклов в Bash (они блокируются) — только простые `grep -rnw` с путями от корня; для покрытия — таблица «класс → тесты» по каждому классу отдельно.
- После попытки 2: класс нельзя называть «тривиальным», не прочитав файл; вычисляемые поля (`get()`, `val x = …`) считаются логикой.

### Code Review (усиление)

Задача: отревьюить незакоммиченный дифф `StatsScreen.kt` из Bug Fix. Лог: `logs/review-run1.txt`. **Один запуск — рабочий результат**, доработка не потребовалась.
Вердикт APPROVE; замечания по существу: `Refresh` как иконка деструктивного сброса неоднозначна (смягчено диалогом), кнопка активна в `Loading`, высота шапки выросла до 48dp, не добавить случайно untracked `.claude/` и `CLAUDE.local.md`. Кода не менял, сборку не запускал, как и требует профиль.

## Замечания по проекту (вне профилей)
- `CLAUDE.md` («Команды») и `CLAUDE.local.md` ссылаются на несуществующие задачи `testDebugUnitTest` и `compileDebugKotlinAndroid`. Актуальные: `:composeApp:testAndroidHostTest`, `:composeApp:compileAndroidMain`. Эти файлы я не менял.
- Фикс `StatsScreen.kt` остаётся в рабочей копии, не закоммичен.
