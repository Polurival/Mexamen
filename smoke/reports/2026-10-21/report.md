# Smoke-отчёт Mexamen — 2026-10-21 22:38

Устройство: moto g54 5G (физическое, USB). Прогнано сценариев: 5, упало: 0.

| Сценарий | Статус | Шагов | Причина |
|---|---|---|---|
| S1 · Quiz completo por todos los temas | ✅ PASS | 43 |  |
| S2 · Tarjetas didácticas (flashcards) | ✅ PASS | 42 |  |
| S3 · Estadísticas reflejan respuestas y se reinician | ✅ PASS | 19 |  |
| S4 · Cancelar reinicio de estadísticas | ✅ PASS | 14 |  |
| S5 · Leyendas: список и детальный экран | ✅ PASS | 10 |  |

### S1 · Quiz completo por todos los temas — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S1-001.png) |
| 2 | `launch` | ✅ | ![](S1-002.png) |
| 3 | `expect: Comenzar quiz` | ✅ | ![](S1-003.png) |
| 4 | `tap: Comenzar quiz` | ✅ | ![](S1-004.png) |
| 5 | `expect: Todos los temas` | ✅ | ![](S1-005.png) |
| 6 | `tap: Todos los temas` | ✅ | ![](S1-006.png) |
| 7 | `expect: Pregunta 1 de 10` | ✅ | ![](S1-007.png) |
| 8 | `tap_option: 1` | ✅ | ![](S1-008.png) |
| 9 | `expect: Siguiente` | ✅ | ![](S1-009.png) |
| 10 | `tap: Siguiente` | ✅ | ![](S1-010.png) |
| 11 | `tap_option: 1` | ✅ | ![](S1-011.png) |
| 12 | `expect: Siguiente` | ✅ | ![](S1-012.png) |
| 13 | `tap: Siguiente` | ✅ | ![](S1-013.png) |
| 14 | `tap_option: 1` | ✅ | ![](S1-014.png) |
| 15 | `expect: Siguiente` | ✅ | ![](S1-015.png) |
| 16 | `tap: Siguiente` | ✅ | ![](S1-016.png) |
| 17 | `tap_option: 1` | ✅ | ![](S1-017.png) |
| 18 | `expect: Siguiente` | ✅ | ![](S1-018.png) |
| 19 | `tap: Siguiente` | ✅ | ![](S1-019.png) |
| 20 | `tap_option: 1` | ✅ | ![](S1-020.png) |
| 21 | `expect: Siguiente` | ✅ | ![](S1-021.png) |
| 22 | `tap: Siguiente` | ✅ | ![](S1-022.png) |
| 23 | `tap_option: 1` | ✅ | ![](S1-023.png) |
| 24 | `expect: Siguiente` | ✅ | ![](S1-024.png) |
| 25 | `tap: Siguiente` | ✅ | ![](S1-025.png) |
| 26 | `tap_option: 1` | ✅ | ![](S1-026.png) |
| 27 | `expect: Siguiente` | ✅ | ![](S1-027.png) |
| 28 | `tap: Siguiente` | ✅ | ![](S1-028.png) |
| 29 | `tap_option: 1` | ✅ | ![](S1-029.png) |
| 30 | `expect: Siguiente` | ✅ | ![](S1-030.png) |
| 31 | `tap: Siguiente` | ✅ | ![](S1-031.png) |
| 32 | `tap_option: 1` | ✅ | ![](S1-032.png) |
| 33 | `expect: Siguiente` | ✅ | ![](S1-033.png) |
| 34 | `tap: Siguiente` | ✅ | ![](S1-034.png) |
| 35 | `tap_option: 1` | ✅ | ![](S1-035.png) |
| 36 | `expect: Siguiente` | ✅ | ![](S1-036.png) |
| 37 | `tap: Siguiente` | ✅ | ![](S1-037.png) |
| 38 | `expect: Resultado:` | ✅ | ![](S1-038.png) |
| 39 | `expect: Volver al inicio` | ✅ | ![](S1-039.png) |
| 40 | `tap: Volver al inicio` | ✅ | ![](S1-040.png) |
| 41 | `expect: Temas del` | ✅ | ![](S1-041.png) |
| 42 | `back` | ✅ | ![](S1-042.png) |
| 43 | `expect: Prepárate para el examen de naturalización` | ✅ | ![](S1-043.png) |

### S2 · Tarjetas didácticas (flashcards) — **PASS**

| # | Шаг | Результат | Скриншот |
|---|---|---|---|
| 1 | `clear` | ✅ | ![](S2-001.png) |
| 2 | `launch` | ✅ | ![](S2-002.png) |
| 3 | `tap: Comenzar tarjetas didácticas` | ✅ | ![](S2-003.png) |
| 4 | `expect: Todos los temas` | ✅ | ![](S2-004.png) |
| 5 | `tap: Todos los temas` | ✅ | ![](S2-005.png) |
| 6 | `expect: Tarjeta 1 de 10` | ✅ | ![](S2-006.png) |
| 7 | `expect: Toca para ver la respuesta` | ✅ | ![](S2-007.png) |
| 8 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-008.png) |
| 9 | `expect: Bien` | ✅ | ![](S2-009.png) |
| 10 | `tap: Bien` | ✅ | ![](S2-010.png) |
| 11 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-011.png) |
| 12 | `expect: Bien` | ✅ | ![](S2-012.png) |
| 13 | `tap: Bien` | ✅ | ![](S2-013.png) |
| 14 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-014.png) |
| 15 | `expect: Bien` | ✅ | ![](S2-015.png) |
| 16 | `tap: Bien` | ✅ | ![](S2-016.png) |
| 17 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-017.png) |
| 18 | `expect: Bien` | ✅ | ![](S2-018.png) |
| 19 | `tap: Bien` | ✅ | ![](S2-019.png) |
| 20 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-020.png) |
| 21 | `expect: Bien` | ✅ | ![](S2-021.png) |
| 22 | `tap: Bien` | ✅ | ![](S2-022.png) |
| 23 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-023.png) |
| 24 | `expect: Bien` | ✅ | ![](S2-024.png) |
| 25 | `tap: Bien` | ✅ | ![](S2-025.png) |
| 26 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-026.png) |
| 27 | `expect: Bien` | ✅ | ![](S2-027.png) |
| 28 | `tap: Bien` | ✅ | ![](S2-028.png) |
| 29 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-029.png) |
| 30 | `expect: Bien` | ✅ | ![](S2-030.png) |
| 31 | `tap: Bien` | ✅ | ![](S2-031.png) |
| 32 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-032.png) |
| 33 | `expect: Bien` | ✅ | ![](S2-033.png) |
| 34 | `tap: Bien` | ✅ | ![](S2-034.png) |
| 35 | `tap: Toca para ver la respuesta` | ✅ | ![](S2-035.png) |
| 36 | `expect: Bien` | ✅ | ![](S2-036.png) |
| 37 | `tap: Bien` | ✅ | ![](S2-037.png) |
| 38 | `expect: ¡Sesión completada!` | ✅ | ![](S2-038.png) |
| 39 | `tap: Volver al inicio` | ✅ | ![](S2-039.png) |
| 40 | `expect: Temas de Tarjetas` | ✅ | ![](S2-040.png) |
| 41 | `back` | ✅ | ![](S2-041.png) |
| 42 | `expect: Prepárate para el examen de naturalización` | ✅ | ![](S2-042.png) |

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
| 5 | `tap: El cenote Zací` | ✅ | ![](S5-005.png) |
| 6 | `expect_absent: Leyendas Mexicanas` | ✅ | ![](S5-006.png) |
| 7 | `back` | ✅ | ![](S5-007.png) |
| 8 | `expect: Leyendas Mexicanas` | ✅ | ![](S5-008.png) |
| 9 | `back` | ✅ | ![](S5-009.png) |
| 10 | `expect: Comenzar quiz` | ✅ | ![](S5-010.png) |
