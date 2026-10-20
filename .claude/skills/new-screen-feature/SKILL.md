---
name: new-screen-feature
description: Чеклист добавления фичи, затрагивающей данные и UI Mexamen (новые счётчики, поля на экране, новый экран). Используй при задачах вида «показать на экране X ...», «добавить статистику/поле/экран».
---

Идти строго по слоям снизу вверх; на каждый шаг — минимальный diff.

1. **Данные.** Нужно новое персистентное значение? Расширь `QuestionStats` полем **с дефолтом** (старые JSON должны читаться) и обнови `QuestionWeighting`/`RecordAnswerUseCase`. Если данные уже есть (`timesShown/timesCorrect/timesWrong`) — новых полей не добавляй.
2. **Domain (commonMain, чистый Kotlin).** Вся агрегация/группировка (напр. по `QuestionEntry.section`) — в `domain/` (`object` или UseCase) + data class результата. Тема = `section`; фильтр виртуальных тем — только `QuestionRepository.getQuestionsByTopic`.
3. **Тест** в `commonTest` для новой логики (Fake-store, фиксированный clock, без Android).
4. **ViewModel.** Читает `statsStore.readAll()`, строит данные для UI, кладёт в `UiState`. Помни: запись ответа асинхронна — учитывай текущую попытку явно.
5. **UI.** Screen остаётся stateless: новые данные приходят в `state`/параметрах. Тексты — испанские, стили из `MaterialTheme`. Никакой логики и обращений к store в Composable.
6. **Проводка.** Обнови вызовы в `App.kt` (`*Flow`); DI в `di/ViewModelModule.kt`, если менялся конструктор VM; новый `Route` — в `Routes.kt` + `navigationSerializersModule`.
7. Запусти `/verify`.
