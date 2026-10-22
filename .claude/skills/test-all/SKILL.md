---
name: test-all
description: Полный прогон тестов Mexamen — код-тесты (уровень 1) + smoke на физическом Android-устройстве (уровень 2) — и единый отчёт. Используй после PR/крупной правки или по просьбе «прогони все тесты».
---

Не запускай `*SmokeTest` (реальная сеть/Groq). Эмулятор не используем — только физический телефон по USB.

1. **Уровень 1 — код.**
   `./gradlew :composeApp:testAndroidHostTest --console=plain --tests 'dev.polurival.mexamen.domain.*' --tests 'dev.polurival.mexamen.data.*' --tests '*GroqProviderTest' --tests '*PromptBuilderTest' --tests '*FallbackDistractorGeneratorTest' --tests '*LlmDistractorGeneratorTest'`
   Суммируй tests/failures из `composeApp/build/test-results/testAndroidHostTest/*.xml`. Для упавших — имя теста + текст ошибки + вероятный файл причины.
2. **Wasm-совместимость.** `./gradlew :composeApp:compileKotlinWasmJs --console=plain`.
3. **Устройство.** `adb devices` → должен быть ровно один `device`. Иначе остановись и попроси подключить/разрешить отладку (smoke пропусти, отметь «не проверено»).
4. **Сборка и установка.** `./gradlew :androidApp:assembleDebug --console=plain`, затем `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`.
   Предупреди пользователя: сценарии делают `pm clear` — сохранённая статистика на телефоне сотрётся.
5. **Уровень 2 — smoke.** Предпочтительно через MCP `mobile` (claude-in-mobile, см. `.mcp.json`): для каждого `smoke/scenarios/S*.md` выполни шаги из блока ```steps``` (словарь шагов — `smoke/README.md`), скриншот после каждого шага.
   Если MCP-инструментов нет в сессии — запасной путь: `ANDROID_SERIAL=<serial> python3 -I smoke/tools/run_smoke.py --out smoke/reports/<YYYY-MM-DD>`.
6. **Единый отчёт** (в ответ и `smoke/reports/<YYYY-MM-DD>/report.md`): таблица «Уровень 1: N тестов, M упало», «Smoke: сценарий → PASS/FAIL». Для каждого FAIL: шаг, скриншот, **где проблема** (экран → код: файл:строка, смотри `logcat -b crash` и `App.kt`/`ui/*`). Не чини молча — предложи фикс.
7. Явно перечисли, что не проверено.
