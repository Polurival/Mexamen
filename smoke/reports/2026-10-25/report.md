# /test-all — 2026-10-25, ветка `day5-run2` (PR #25)

| Уровень | Результат |
|---|---|
| 1 · код (`domain.*`, `data.*`, LLM-тесты) | **153 теста, 0 упало** |
| 1 · полный `testAndroidHostTest` (в рамках loop, для справки) | 205 тестов, 0 упало |
| Wasm (`compileKotlinWasmJs`) | ✅ BUILD SUCCESSFUL |
| Сборка/установка (`assembleDebug`, `adb install -r`) | ✅ moto g54 5G (ZY22KK5KSC) |
| 2 · smoke, первый прогон | 7 / 8 PASS, S8 FAIL |
| 2 · smoke после фикса S8 | **8 / 8 PASS** (S8 перепрогнан: 19/19 шагов) |

## Разбор FAIL: S8 · тема оформления

- **Шаг:** 6 `expect: Tema: Claro` после `tap: Tema: Sistema` — скриншот [S8-005.png](S8-005.png) / [S8-006.png](S8-006.png).
- **Что на самом деле:** нажатие работает — цвета приложения меняются (после двух нажатий весь экран тёмный, т.е. режим Oscuro применён), но иконка и `contentDescription` на Home остаются «Tema: Sistema». `logcat -b crash` пуст.
- **Где проблема:** `composeApp/src/commonMain/kotlin/dev/polurival/mexamen/App.kt:132` — `NavEntry(Route.Home) { HomeScreen(themeMode = themeMode, ...) }` захватывает параметр `themeMode` из `AppContent` по значению. `NavDisplay` хранит содержимое entry по ключу и не пересоздаёт лямбду при новом `themeMode`, поэтому Home видит устаревший режим. `MexamenTheme` на уровне `App()` (`App.kt:93`) получает актуальный режим — поэтому цвета верные.
- **Почему не поймали тесты:** юнит-тесты `ThemeViewModel`/`ThemeModeRules` зелёные — VM работает; баг в связке Compose/Navigation3, это видно только на устройстве.
- **Фикс (применён, коммит «fix: theme toggle icon…»):** в `AppContent` значение обёрнуто в State и читается внутри entry:
  ```kotlin
  val currentThemeMode by rememberUpdatedState(themeMode)
  ...
  Route.Home -> NavEntry(key) {
      HomeScreen(themeMode = currentThemeMode, onCycleThemeMode = onCycleThemeMode, ...)
  }
  ```
  (не брать `koinViewModel<ThemeViewModel>()` внутри entry — получится отдельный VM в сторе entry).
- **Перепрогон S8 после фикса:** ✅ PASS, 19/19 — иконка и `contentDescription` меняются Sistema → Claro → Oscuro, режим переживает `launch`, Estadísticas открываются в тёмной теме (скриншот S8-008: тёмная схема, иконка луны). Повторно: `compileAndroidMain` + `compileKotlinWasmJs` + `testAndroidHostTest` → 205 тестов, 0 упало; `assembleDebug` + `adb install -r` ✅.
- Скриншоты в git не коммитятся (локально в `smoke/reports/2026-10-25/` и `2026-10-25-s8-rerun/`).

## Не проверено
- Web/wasm в браузере (в т.ч. #5 back/swipe, тема в веб-рамке `main.kt`).
- `*SmokeTest` (реальная сеть/Groq) — по правилам не запускались.
- Цвета тем на экранах кроме Home сверялись только глазами по скриншотам; раннер цвета не сравнивает.
- S7 `type:` проверен (сценарий прошёл), но только ASCII-ввод.
- Экраны результата легенды и блок «Por tema» статистики smoke не покрывают (нет шага прокрутки).

---

# Smoke-отчёт Mexamen — 2026-10-25 18:26

Устройство: moto g54 5G (физическое, USB). Прогнано сценариев: 8, упало: 1.

| Сценарий | Статус | Шагов | Причина |
|---|---|---|---|
| S1 · Quiz completo por todos los temas | ✅ PASS | 47 |  |
| S2 · Tarjetas didácticas (flashcards) | ✅ PASS | 43 |  |
| S3 · Estadísticas reflejan respuestas y se reinician | ✅ PASS | 19 |  |
| S4 · Cancelar reinicio de estadísticas | ✅ PASS | 14 |  |
| S5 · Leyendas: список и детальный экран | ✅ PASS | 17 |  |
| S6 · Repasar errores (tarjetas solo con errores) | ✅ PASS | 32 |  |
| S7 · Banco de preguntas: lista, explicación y búsqueda | ✅ PASS | 29 |  |
| S8 · Tema: Sistema → Claro → Oscuro con guardado | ❌ FAIL | 19 | шаг 6 `expect: Tema: Claro`: ожидался текст «Tema: Claro», на экране его нет |

### S1 · Quiz completo por todos los temas — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S1-001.png) |
| 2 | `launch` | ✅ | ![](S1-002.png) |
| 3 | `expect: Comenzar quiz` | ✅ | ![](S1-003.png) |
| 4 | `tap: Comenzar quiz` | ✅ | ![](S1-004.png) |
| 5 | `expect: Todos los temas` | ✅ | ![](S1-005.png) |
| 6 | `expect: Fechas` | ✅ | ![](S1-006.png) |
| 7 | `expect: Personajes` | ✅ | ![](S1-007.png) |
| 8 | `expect: Eventos` | ✅ | ![](S1-008.png) |
| 9 | `expect_absent: Даты` | ✅ | ![](S1-009.png) |
| 10 | `tap: Todos los temas` | ✅ | ![](S1-010.png) |
| 11 | `expect: Pregunta 1 de 10` | ✅ | ![](S1-011.png) |
| 12 | `tap_option: 1` | ✅ | ![](S1-012.png) |
| 13 | `expect: Siguiente` | ✅ | ![](S1-013.png) |
| 14 | `tap: Siguiente` | ✅ | ![](S1-014.png) |
| 15 | `tap_option: 1` | ✅ | ![](S1-015.png) |
| 16 | `expect: Siguiente` | ✅ | ![](S1-016.png) |
| 17 | `tap: Siguiente` | ✅ | ![](S1-017.png) |
| 18 | `tap_option: 1` | ✅ | ![](S1-018.png) |
| 19 | `expect: Siguiente` | ✅ | ![](S1-019.png) |
| 20 | `tap: Siguiente` | ✅ | ![](S1-020.png) |
| 21 | `tap_option: 1` | ✅ | ![](S1-021.png) |
| 22 | `expect: Siguiente` | ✅ | ![](S1-022.png) |
| 23 | `tap: Siguiente` | ✅ | ![](S1-023.png) |
| 24 | `tap_option: 1` | ✅ | ![](S1-024.png) |
| 25 | `expect: Siguiente` | ✅ | ![](S1-025.png) |
| 26 | `tap: Siguiente` | ✅ | ![](S1-026.png) |
| 27 | `tap_option: 1` | ✅ | ![](S1-027.png) |
| 28 | `expect: Siguiente` | ✅ | ![](S1-028.png) |
| 29 | `tap: Siguiente` | ✅ | ![](S1-029.png) |
| 30 | `tap_option: 1` | ✅ | ![](S1-030.png) |
| 31 | `expect: Siguiente` | ✅ | ![](S1-031.png) |
| 32 | `tap: Siguiente` | ✅ | ![](S1-032.png) |
| 33 | `tap_option: 1` | ✅ | ![](S1-033.png) |
| 34 | `expect: Siguiente` | ✅ | ![](S1-034.png) |
| 35 | `tap: Siguiente` | ✅ | ![](S1-035.png) |
| 36 | `tap_option: 1` | ✅ | ![](S1-036.png) |
| 37 | `expect: Siguiente` | ✅ | ![](S1-037.png) |
| 38 | `tap: Siguiente` | ✅ | ![](S1-038.png) |
| 39 | `tap_option: 1` | ✅ | ![](S1-039.png) |
| 40 | `expect: Siguiente` | ✅ | ![](S1-040.png) |
| 41 | `tap: Siguiente` | ✅ | ![](S1-041.png) |
| 42 | `expect: Resultado:` | ✅ | ![](S1-042.png) |
| 43 | `expect: Volver al inicio` | ✅ | ![](S1-043.png) |
| 44 | `tap: Volver al inicio` | ✅ | ![](S1-044.png) |
| 45 | `expect: Temas del` | ✅ | ![](S1-045.png) |
| 46 | `back` | ✅ | ![](S1-046.png) |
| 47 | `expect: Prepárate para el examen de naturalización` | ✅ | ![](S1-047.png) |

### S2 · Tarjetas didácticas (flashcards) — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S2-001.png) |
| 2 | `launch` | ✅ | ![](S2-002.png) |
| 3 | `tap: Comenzar tarjetas didácticas` | ✅ | ![](S2-003.png) |
| 4 | `expect: Todos los temas` | ✅ | ![](S2-004.png) |
| 5 | `tap: Todos los temas` | ✅ | ![](S2-005.png) |
| 6 | `expect_absent: Generando preguntas…` | ✅ | ![](S2-006.png) |
| 7 | `expect: Tarjeta 1 de 10` | ✅ | ![](S2-007.png) |
| 8 | `expect: Toca para ver la respuesta` | ✅ | ![](S2-008.png) |
| 9 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-009.png) |
| 10 | `expect: Bien` | ✅ | ![](S2-010.png) |
| 11 | `tap: Bien` | ✅ | ![](S2-011.png) |
| 12 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-012.png) |
| 13 | `expect: Bien` | ✅ | ![](S2-013.png) |
| 14 | `tap: Bien` | ✅ | ![](S2-014.png) |
| 15 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-015.png) |
| 16 | `expect: Bien` | ✅ | ![](S2-016.png) |
| 17 | `tap: Bien` | ✅ | ![](S2-017.png) |
| 18 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-018.png) |
| 19 | `expect: Bien` | ✅ | ![](S2-019.png) |
| 20 | `tap: Bien` | ✅ | ![](S2-020.png) |
| 21 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-021.png) |
| 22 | `expect: Bien` | ✅ | ![](S2-022.png) |
| 23 | `tap: Bien` | ✅ | ![](S2-023.png) |
| 24 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-024.png) |
| 25 | `expect: Bien` | ✅ | ![](S2-025.png) |
| 26 | `tap: Bien` | ✅ | ![](S2-026.png) |
| 27 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-027.png) |
| 28 | `expect: Bien` | ✅ | ![](S2-028.png) |
| 29 | `tap: Bien` | ✅ | ![](S2-029.png) |
| 30 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-030.png) |
| 31 | `expect: Bien` | ✅ | ![](S2-031.png) |
| 32 | `tap: Bien` | ✅ | ![](S2-032.png) |
| 33 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-033.png) |
| 34 | `expect: Bien` | ✅ | ![](S2-034.png) |
| 35 | `tap: Bien` | ✅ | ![](S2-035.png) |
| 36 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-036.png) |
| 37 | `expect: Bien` | ✅ | ![](S2-037.png) |
| 38 | `tap: Bien` | ✅ | ![](S2-038.png) |
| 39 | `expect: ¡Sesión completada!` | ✅ | ![](S2-039.png) |
| 40 | `tap: Volver al inicio` | ✅ | ![](S2-040.png) |
| 41 | `expect: Temas de Tarjetas` | ✅ | ![](S2-041.png) |
| 42 | `back` | ✅ | ![](S2-042.png) |
| 43 | `expect: Prepárate para el examen de naturalización` | ✅ | ![](S2-043.png) |

### S3 · Estadísticas reflejan respuestas y se reinician — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S3-001.png) |
| 2 | `launch` | ✅ | ![](S3-002.png) |
| 3 | `tap: Comenzar quiz` | ✅ | ![](S3-003.png) |
| 4 | `tap: Todos los temas` | ✅ | ![](S3-004.png) |
| 5 | `expect: Pregunta 1 de 10` | ✅ | ![](S3-005.png) |
| 6 | `tap_option: 1` | ✅ | ![](S3-006.png) |
| 7 | `expect: Siguiente` | ✅ | ![](S3-007.png) |
| 8 | `tap: Cerrar quiz` | ✅ | ![](S3-008.png) |
| 9 | `launch` | ✅ | ![](S3-009.png) |
| 10 | `tap: Estadísticas` | ✅ | ![](S3-010.png) |
| 11 | `expect: Preguntas totales` | ✅ | ![](S3-011.png) |
| 12 | `expect: Preguntas intentadas` | ✅ | ![](S3-012.png) |
| 13 | `expect: Respuestas correctas` | ✅ | ![](S3-013.png) |
| 14 | `expect_text_value: Preguntas intentadas = 1` | ✅ | ![](S3-014.png) |
| 15 | `tap: Reiniciar estadísticas` | ✅ | ![](S3-015.png) |
| 16 | `expect: ¿Reiniciar estadísticas?` | ✅ | ![](S3-016.png) |
| 17 | `tap: Reiniciar` | ✅ | ![](S3-017.png) |
| 18 | `expect: Preguntas intentadas` | ✅ | ![](S3-018.png) |
| 19 | `expect_text_value: Preguntas intentadas = 0` | ✅ | ![](S3-019.png) |

### S4 · Cancelar reinicio de estadísticas — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S4-001.png) |
| 2 | `launch` | ✅ | ![](S4-002.png) |
| 3 | `tap: Comenzar quiz` | ✅ | ![](S4-003.png) |
| 4 | `tap: Todos los temas` | ✅ | ![](S4-004.png) |
| 5 | `tap_option: 1` | ✅ | ![](S4-005.png) |
| 6 | `expect: Siguiente` | ✅ | ![](S4-006.png) |
| 7 | `tap: Cerrar quiz` | ✅ | ![](S4-007.png) |
| 8 | `launch` | ✅ | ![](S4-008.png) |
| 9 | `tap: Estadísticas` | ✅ | ![](S4-009.png) |
| 10 | `tap: Reiniciar estadísticas` | ✅ | ![](S4-010.png) |
| 11 | `expect: ¿Reiniciar estadísticas?` | ✅ | ![](S4-011.png) |
| 12 | `tap: Cancelar` | ✅ | ![](S4-012.png) |
| 13 | `expect_absent: ¿Reiniciar estadísticas?` | ✅ | ![](S4-013.png) |
| 14 | `expect_text_value: Preguntas intentadas = 1` | ✅ | ![](S4-014.png) |

### S5 · Leyendas: список и детальный экран — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S5-001.png) |
| 2 | `launch` | ✅ | ![](S5-002.png) |
| 3 | `tap: Leyendas` | ✅ | ![](S5-003.png) |
| 4 | `expect: Leyendas Mexicanas` | ✅ | ![](S5-004.png) |
| 5 | `expect_absent: Back` | ✅ | ![](S5-005.png) |
| 6 | `tap: El cenote Zací` | ✅ | ![](S5-006.png) |
| 7 | `expect_absent: Leyendas Mexicanas` | ✅ | ![](S5-007.png) |
| 8 | `expect: Reproducir audio` | ✅ | ![](S5-008.png) |
| 9 | `expect_absent: Back` | ✅ | ![](S5-009.png) |
| 10 | `back` | ✅ | ![](S5-010.png) |
| 11 | `expect: Leyendas Mexicanas` | ✅ | ![](S5-011.png) |
| 12 | `tap: El cenote Zací` | ✅ | ![](S5-012.png) |
| 13 | `expect_absent: Leyendas Mexicanas` | ✅ | ![](S5-013.png) |
| 14 | `tap: Volver` | ✅ | ![](S5-014.png) |
| 15 | `expect: Leyendas Mexicanas` | ✅ | ![](S5-015.png) |
| 16 | `back` | ✅ | ![](S5-016.png) |
| 17 | `expect: Comenzar quiz` | ✅ | ![](S5-017.png) |

### S6 · Repasar errores (tarjetas solo con errores) — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S6-001.png) |
| 2 | `launch` | ✅ | ![](S6-002.png) |
| 3 | `tap: Comenzar tarjetas didácticas` | ✅ | ![](S6-003.png) |
| 4 | `expect: Repasar errores` | ✅ | ![](S6-004.png) |
| 5 | `expect: Sin errores todavía` | ✅ | ![](S6-005.png) |
| 6 | `tap: Repasar errores` | ✅ | ![](S6-006.png) |
| 7 | `expect: Temas de Tarjetas` | ✅ | ![](S6-007.png) |
| 8 | `expect_absent: Tarjeta 1 de` | ✅ | ![](S6-008.png) |
| 9 | `tap: Todos los temas` | ✅ | ![](S6-009.png) |
| 10 | `expect: Tarjeta 1 de 10` | ✅ | ![](S6-010.png) |
| 11 | `tap: Toca para ver la respuesta` | ✅ | ![](S6-011.png) |
| 12 | `expect: Otra vez` | ✅ | ![](S6-012.png) |
| 13 | `tap: Otra vez` | ✅ | ![](S6-013.png) |
| 14 | `tap: Toca para ver la respuesta` | ✅ | ![](S6-014.png) |
| 15 | `expect: Otra vez` | ✅ | ![](S6-015.png) |
| 16 | `tap: Otra vez` | ✅ | ![](S6-016.png) |
| 17 | `tap: Toca para ver la respuesta` | ✅ | ![](S6-017.png) |
| 18 | `expect: Otra vez` | ✅ | ![](S6-018.png) |
| 19 | `tap: Otra vez` | ✅ | ![](S6-019.png) |
| 20 | `expect: Tarjeta 4 de 10` | ✅ | ![](S6-020.png) |
| 21 | `tap: Cerrar tarjetas` | ✅ | ![](S6-021.png) |
| 22 | `expect: Temas de Tarjetas` | ✅ | ![](S6-022.png) |
| 23 | `expect_absent: Sin errores todavía` | ✅ | ![](S6-023.png) |
| 24 | `tap: Repasar errores` | ✅ | ![](S6-024.png) |
| 25 | `expect: Tarjeta 1 de 3` | ✅ | ![](S6-025.png) |
| 26 | `tap: Cerrar tarjetas` | ✅ | ![](S6-026.png) |
| 27 | `back` | ✅ | ![](S6-027.png) |
| 28 | `expect: Comenzar quiz` | ✅ | ![](S6-028.png) |
| 29 | `tap: Comenzar quiz` | ✅ | ![](S6-029.png) |
| 30 | `expect: Temas del Quiz` | ✅ | ![](S6-030.png) |
| 31 | `tap: Repasar errores` | ✅ | ![](S6-031.png) |
| 32 | `expect: Pregunta 1 de 3` | ✅ | ![](S6-032.png) |

### S7 · Banco de preguntas: lista, explicación y búsqueda — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S7-001.png) |
| 2 | `launch` | ✅ | ![](S7-002.png) |
| 3 | `tap: Banco de preguntas` | ✅ | ![](S7-003.png) |
| 4 | `expect: Buscar` | ✅ | ![](S7-004.png) |
| 5 | `expect: Preguntas: 776 de 776` | ✅ | ![](S7-005.png) |
| 6 | `expect: ¿Cuál es la capital de Aguascalientes?` | ✅ | ![](S7-006.png) |
| 7 | `expect_absent: Su nombre se debe a las aguas termales` | ✅ | ![](S7-007.png) |
| 8 | `tap: ¿Cuál es la capital de Aguascalientes?` | ✅ | ![](S7-008.png) |
| 9 | `expect: Explicación` | ✅ | ![](S7-009.png) |
| 10 | `expect: Su nombre se debe a las aguas termales` | ✅ | ![](S7-010.png) |
| 11 | `tap: ¿Cuál es la capital de Aguascalientes?` | ✅ | ![](S7-011.png) |
| 12 | `expect_absent: Su nombre se debe a las aguas termales` | ✅ | ![](S7-012.png) |
| 13 | `tap: Buscar` | ✅ | ![](S7-013.png) |
| 14 | `type: canon` | ✅ | ![](S7-014.png) |
| 15 | `expect: Preguntas: 2 de 776` | ✅ | ![](S7-015.png) |
| 16 | `expect: Cañón del Sumidero` | ✅ | ![](S7-016.png) |
| 17 | `expect: Chiapas` | ✅ | ![](S7-017.png) |
| 18 | `expect: Coro del himno mexicano` | ✅ | ![](S7-018.png) |
| 19 | `tap: Volver` | ✅ | ![](S7-019.png) |
| 20 | `expect: Comenzar quiz` | ✅ | ![](S7-020.png) |
| 21 | `tap: Banco de preguntas` | ✅ | ![](S7-021.png) |
| 22 | `expect: Preguntas: 776 de 776` | ✅ | ![](S7-022.png) |
| 23 | `tap: Buscar` | ✅ | ![](S7-023.png) |
| 24 | `type: SUMIDERO` | ✅ | ![](S7-024.png) |
| 25 | `expect: Preguntas: 1 de 776` | ✅ | ![](S7-025.png) |
| 26 | `expect: Cañón del Sumidero` | ✅ | ![](S7-026.png) |
| 27 | `expect_absent: Coro del himno mexicano` | ✅ | ![](S7-027.png) |
| 28 | `tap: Volver` | ✅ | ![](S7-028.png) |
| 29 | `expect: Comenzar quiz` | ✅ | ![](S7-029.png) |

### S8 · Tema: Sistema → Claro → Oscuro con guardado — **FAIL**

> Причина: шаг 6 `expect: Tema: Claro`: ожидался текст «Tema: Claro», на экране его нет

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S8-001.png) |
| 2 | `launch` | ✅ | ![](S8-002.png) |
| 3 | `expect: Comenzar quiz` | ✅ | ![](S8-003.png) |
| 4 | `expect: Tema: Sistema` | ✅ | ![](S8-004.png) |
| 5 | `tap: Tema: Sistema` | ✅ | ![](S8-005.png) |
| 6 | `expect: Tema: Claro` | ❌ | ![](S8-006.png) |
