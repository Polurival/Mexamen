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
   - нет новой private-копии fake store: `grep -rln "QuestionStatsStore {" composeApp/src/commonTest` — общий fake в `testing/`;
   - новые поля `@Serializable` (`Route`, `QuizResult`) с дефолтами.
4a. Smoke: если в diff менялись строки UI/экраны/навигация — `git diff -U0 | grep '^-.*"'` по старым литералам и `grep -rn` их в `smoke/`; сценарии обновлены в том же diff.
5. Отчёт: что прошло, что упало (с текстом ошибки), что не проверено.
