---
name: verify
description: Проверка изменений Mexamen перед сдачей — компиляция, тесты, поиск антипаттернов в diff. Используй после любой правки кода.
---

1. `./gradlew :composeApp:compileAndroidMain --console=plain`
2. `./gradlew :composeApp:testAndroidHostTest --console=plain`
3. По `git diff` проверь антипаттерны из CLAUDE.md §6:
   - `grep -nE 'java\.|android\.|System\.currentTimeMillis|!!|println|GlobalScope|runBlocking' ` по изменённым файлам в `commonMain`;
   - в `ui/*Screen.kt` нет `statsStore`/`repository`/логики агрегации;
   - новые строки UI — на испанском;
   - у новых полей `QuestionStats` есть дефолты.
4. Новая логика покрыта тестом в `commonTest`; изменение сигнатур отражено в `App.kt` и `di/`.
5. Отчёт: что прошло, что упало (с текстом ошибки), что не проверено.
