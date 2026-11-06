# secloop — execution loop со security step (День 14)

Оркестратор поверх процесса Дня 5 (профиль `feature` → Gradle-проверки → коммит): после зелёных тестов код уходит на
**security review вторым вызовом LLM**, весь трафик к LLM идёт через **LLM Gateway** ([../gateway](../gateway)).
Только стандартная библиотека Python; не часть приложения Mexamen.

Отчёт с результатами трёх задач: **[docs/day14-secloop/REPORT.md](../../docs/day14-secloop/REPORT.md)**.

```
задача → генерация (claude -p --agent feature, через Gateway gen: вход redact)
       → Gradle: compileAndroidMain + compileKotlinWasmJs + testAndroidHostTest   упало → фидбек в генерацию
       → security review (claude -p, отдельный промпт, без инструментов, через Gateway review: вход block)
           Critical/High → «исправь: <что> в <file>:<line>» → генерация (та же сессия)
           только Medium/Low → warning в loop-events.jsonl → git commit
           чисто → git commit
```

## Запуск

```bash
cd tools/secloop
python3 -m unittest discover -s tests -t .          # 33 теста логики на фейках (без LLM и Gradle)

# боевой прогон: реальные вызовы claude (тратит квоту подписки), работа в git worktree вне репозитория
python3 -m secloop run --tasks T1 T2 T3 --workdir /tmp/secloop --run-id myrun --allow-email <email аккаунта>
python3 -m secloop probe --workdir /tmp/secloop --run-id myrun --allow-email <email аккаунта>   # контрольные пробы Gateway
python3 report.py myrun                              # → docs/day14-secloop/REPORT.md (+ evidence/)
```

`--allow-email`: Claude Code кладёт email аккаунта в служебный контекст каждого запроса; без allowlist review-шлюз в режиме `block`
блокирует вообще всё (так было в первом прогоне, см. отчёт §7).

## Состав

| Путь | Что |
|---|---|
| `secloop/loop.py` | цикл и решения; внешний мир — через `LoopDeps` (в тестах — фейки) |
| `secloop/review.py` | схема и разбор ответа ревьюера, решение, формат фидбека, материал для ревью |
| `secloop/runners.py` | Gateway-процессы, `claude -p`, Gradle, git worktree |
| `secloop/evidence.py`, `canaries.py` | проверка утечек (канарейки + реальные секреты), сводка по Gateway, статические проверки |
| `prompts/security_review.md` | security-промпт под стек Mexamen (Android + Compose Multiplatform/Wasm) |
| `tasks/T1…T3` | три задачи, провоцирующие небезопасный код (шаблоны с `{{JWT}}`, `{{EMAIL}}`, …) |
| `runs/` | сырые артефакты прогонов (в git не попадают: тела запросов большие) |

## Что важно знать

- Fail-closed: ревью не получено / не разобрано / блокировка шлюзом не привязалась к коду → коммита нет (`review_error`).
- Коммиты делаются в ветках `secloop/<run>-<task>` изолированного worktree; `main` не затрагивается.
- Секреты в задачах и `local.properties` рабочей копии — синтетические канарейки; реальный `local.properties` в worktree не копируется.
