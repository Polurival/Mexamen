# Mexamen

Compose Multiplatform (Kotlin) — квиз/flashcards для подготовки к экзамену на натурализацию в Мексике. Таргеты: **Android** (`androidMain`) и **wasmJs** (веб, `wasmJsMain`). Весь UI и тексты — **на испанском**; KDoc/комментарии в коде — на русском.

> Правила v2. Конфиги: глобальный `~/.claude/CLAUDE.md` (личные предпочтения), этот файл (проект), `CLAUDE.local.md` (локальное, в gitignore), скиллы `.claude/skills/`, субагент `.claude/agents/`.

## 1. Стек

Kotlin Multiplatform + Compose Multiplatform + Material3 · Navigation3 (`Route : NavKey`) · Koin (`koin-core/compose/compose-viewmodel`) · Ktor client (Groq) · kotlinx-serialization · kotlinx-coroutines · DataStore (Android) / localStorage (wasm) · тесты: `kotlin.test` + `kotlinx-coroutines-test` + `ktor-client-mock`. Линтера (ktlint/detekt) в проекте **нет** — «прошло линтер» = чистая компиляция без новых warnings + соответствие этому файлу.

## 2. Архитектура и структура папок

```
composeApp/src/
  commonMain/kotlin/dev/polurival/mexamen/
    App.kt                 NavDisplay + *Flow-composable'ы (связка VM ↔ Screen)
    ui/                    *Screen.kt (stateless), *ViewModel.kt, navigation/Routes.kt, Theme.kt
    domain/                чистый Kotlin: *UseCase, object-хелперы (StatsAggregator, QuestionWeighting), модели
    data/                  QuestionEntry, QuestionStats, *Store (interface + expect fun default*), QuestionRepository
    llm/                   LlmProvider, GroqProvider, *DistractorGenerator, PromptBuilder
    di/                    DataModule, LlmModule, ViewModelModule → appModules
  commonMain/composeResources/files/   questions.json, embeddings.bin
  androidMain/ wasmJsMain/             только actual-реализации (Store, Clock, AudioPlayer, Theme)
  commonTest/                          тесты domain/llm (запускаются без Android)
  androidHostTest/                     smoke-тесты с реальной сетью/ассетами
tools/                                 Python-пайплайн данных (scrape → embeddings), не часть приложения
```

Поток зависимостей строго: `ui → domain → data`. `domain`/`data`/`llm` не знают про Compose.

**Поток данных UI:** `Screen(state, callbacks)` ← `StateFlow<UiState>` ← `ViewModel` → `UseCase` / `Store`. `App.kt` содержит `XxxFlow` (берёт `koinViewModel()`, `collectAsState`, `when(state)` → нужный Screen).

**Статистика (важно, на ней строятся задачи):** `QuestionStats(weight, timesShown, timesCorrect, timesWrong, lastSeenEpochMillis)` хранится **per question id** в `QuestionStatsStore` (единый JSON-блоб). Quiz и Flashcards пишут через `RecordAnswerUseCase` → `QuestionWeighting` (flashcard AGAIN/HARD = wrong, GOOD/EASY = correct). Счётчики **кумулятивны за всё время**. Тема вопроса = `QuestionEntry.section`; есть ещё «виртуальные» темы (`Fechas`/`Personajes`/`Eventos`) — фильтрация по теме **только** через `QuestionRepository.getQuestionsByTopic`.

**Запись асинхронна:** `QuizViewModel.selectAnswer` пишет в store через `viewModelScope.launch` — чтение статистики сразу после может не увидеть последний ответ. Новые счётчики для UI считай явно (прочитанное + текущая попытка) или читай после завершения записи; не полагайся на гонку.

## 3. Зафиксированные решения (не пересматривать без запроса)

- LLM Groq `llama-3.3-70b-versatile` за `LlmProvider`; ключ в `local.properties` (`GROQ_API_KEY`), не в git. LLM генерирует только неверные варианты.
- RAG: `questions.json` + `embeddings.bin` (768-dim), косинусный top-K в памяти, без vector-DB и LLM-фреймворков.
- Квиз/flashcards работают **без сети и ключа**.
- DI только Koin; новый ViewModel/Store регистрируется в `di/*Module.kt`.
- Не рефакторить вне текущей задачи (в т.ч. не чинить попутно найденное дублирование — упомяни в ответе).

## 4. Naming conventions

- Пакет `dev.polurival.mexamen.<layer>`; один публичный тип на файл, файл = имя типа (`StatsAggregator.kt`). `expect/actual`: `Foo.kt` + `Foo.android.kt` / `Foo.wasm.kt`.
- Compose: `XxxScreen` (stateless, `modifier: Modifier = Modifier` последним), приватные под-composable в том же файле (`StatCard`). `XxxViewModel`, `XxxUiState` (sealed interface), `XxxUseCase` (+ `operator fun invoke` или один метод `build/record`).
- Приватный `_state: MutableStateFlow`, публичный `state: StateFlow` через `asStateFlow()`.
- Константы — `const val UPPER_SNAKE` в `companion object`/`object`; enum-значения `UPPER_SNAKE`.
- Время — только через инжектируемый `clock: () -> Long` / `nowMillis` параметр (тестируемость).
- Тексты UI — испанские литералы прямо в composable (строковых ресурсов в проекте нет — не вводить).

## 5. Паттерны — делай так (эталонный код из проекта)

**① Чистая доменная логика без зависимостей, `object` + data class результата** (`domain/StatsAggregator.kt`):
```kotlin
data class StatsSummary(val totalQuestions: Int, val attempted: Int, val totalCorrect: Int, val totalWrong: Int, val neverAttempted: Int)

/** Агрегация persisted per-question статистики для экрана статистики. Кумулятивно за всё время. */
object StatsAggregator {
    fun summarize(datasetSize: Int, stats: Map<Int, QuestionStats>): StatsSummary { /* ... */ }
}
```

**② Sealed UiState + ViewModel с read-only StateFlow** (`ui/StatsViewModel.kt`):
```kotlin
sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Loaded(val summary: StatsSummary) : StatsUiState
}

class StatsViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
) : ViewModel() {
    private val _state = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    fun load() {
        _state.value = StatsUiState.Loading
        viewModelScope.launch {
            val stats = statsStore.readAll()
            _state.value = StatsUiState.Loaded(StatsAggregator.summarize(repository.dataset().entries.size, stats))
        }
    }
}
```

**③ Stateless Screen: state + лямбды, VM не пробрасывается глубже `*Flow`** (`ui/FlashcardScreen.kt`, `App.kt`):
```kotlin
@Composable
fun FlashcardCardScreen(
    state: FlashcardUiState.Card,
    onFlip: () -> Unit,
    onGrade: (FlashcardGrade) -> Unit,
    onExit: () -> Unit,
    modifier: Modifier = Modifier,
) { /* Column(modifier.fillMaxSize().padding(24.dp)) { ... MaterialTheme.typography / colorScheme ... } */ }
```
Отступы 24.dp у экрана, 16.dp внутри Card, `Arrangement.spacedBy(12.dp)` между карточками; цвета/шрифты только из `MaterialTheme`.

**④ Платформенное хранилище: interface + `expect fun default*()`** (`data/QuestionStatsStore.kt`). Новое персистентное состояние → расширяй `QuestionStats` (поля с дефолтами — старые JSON должны читаться) и пиши через `RecordAnswerUseCase`/`QuestionStatsStore.update` (атомарный read-modify-write), а не заводи второй store.

**⑤ Тест с in-memory Fake и фиксированным clock** (`commonTest/.../RecordAnswerUseCaseTest.kt`):
```kotlin
private class FakeQuestionStatsStore : QuestionStatsStore { /* Map в памяти */ }

class RecordAnswerUseCaseTest {
    @Test
    fun recordQuizAnswerCreatesStatsForNewQuestion() = runTest {
        val store = FakeQuestionStatsStore()
        val useCase = RecordAnswerUseCase(store, clock = { 42L })
        useCase.recordQuizAnswer(questionId = 5, correct = true)
        assertEquals(1, store.readAll().getValue(5).timesCorrect)
    }
}
```
Имена тестов — `camelCase` описанием поведения. Вся новая чистая логика (агрегаты, форматирование счётчиков) — в `domain/` **с тестом в `commonTest`**.

## 6. Антипаттерны — запрещено

1. **Бизнес-логика/агрегация/фильтрация в `@Composable`** или в Screen. Считай в `domain/`, отдавай готовое через UiState.
2. **Обращение к `QuestionStatsStore`/`QuestionRepository` из Composable** или передача `ViewModel` в Screen — только state + лямбды.
3. **Платформенные API в `commonMain`** (`java.*`, `android.*`, `System.currentTimeMillis`) — ломает wasmJs. Используй `expect/actual` и `currentTimeMillis()` из `domain/Clock.kt`.
4. **Дублирование логики фильтра тем** (`when(topic) { "Fechas (Даты)" -> ... }`) в новом коде — вызывай `getQuestionsByTopic`. Не копируй счётчики/веса: **один** `QuestionStats` на вопрос, второго источника правды не создавать.
5. **Новые зависимости/библиотеки, строковые ресурсы, `!!`, `println`/`Log` в прод-коде, `GlobalScope`, `runBlocking`**, хардкод ключей, русский/английский текст в UI (только испанский).
6. Ломать обратную совместимость сохранённых данных: поле `QuestionStats` без дефолта, переименование существующих полей.
7. **Data-типы в UI-слое:** не передавай в Screen сырые `QuestionStats`/`Map<Int, QuestionStats>` — маппь в доменную модель (`domain/`, напр. `AttemptCounts`). Одинаковую строку/форматирование, нужную в 2+ местах, выноси в один хелпер (не копируй литерал).
8. «Заодно» рефакторить соседний код, менять публичные сигнатуры, не нужные задаче.

## 7. Шаблон типичного файла

```kotlin
package dev.polurival.mexamen.ui            // слой = папка

import androidx.compose.foundation.layout.* // явные импорты, без wildcard кроме layout допускается как в соседних файлах
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import dev.polurival.mexamen.domain.StatsSummary

/** KDoc по-русски: что и зачем (не «как»). */
@Composable
fun XxxScreen(state: XxxUiState, onBack: () -> Unit, modifier: Modifier = Modifier) { /* ... */ }

@Composable
private fun XxxRow(label: String, value: String) { /* ... */ }   // приватные хелперы — ниже публичного
```
ViewModel-файл: импорты → `sealed interface XxxUiState` → `class XxxViewModel(...)`. Domain-файл: импорты → data class результата → `object`/`class` с логикой.

## 8. Определение «готово» (проверяй перед ответом)

1. `./gradlew :composeApp:compileAndroidMain --console=plain` — компилируется.
2. `./gradlew :composeApp:testAndroidHostTest --console=plain` — зелёные, **включая новые тесты на новую логику**.
3. Wasm-совместимость: `./gradlew :composeApp:compileKotlinWasmJs --console=plain` зелёный (в `commonMain` нет платформенных импортов).
4. Новые ViewModel/зависимости зарегистрированы в `di/`; новые `Route` — в `Routes.kt` **и** в `navigationSerializersModule` в `App.kt`; изменение сигнатуры Screen/VM — обновлены все вызовы в `App.kt`.
5. Все новые тексты на испанском, согласованы по терминам с существующими («Preguntas», «Respuestas correctas», «incorrectas»).
6. В ответе — кратко: что изменено, какие файлы, что проверено и что **нет**.

## 9. Скиллы и субагенты

- `/new-screen-feature` (`.claude/skills/`) — чеклист добавления поля/экрана через все слои (store → domain → VM → UI → DI → тест).
- `/verify` — компиляция + тесты + проверка антипаттернов по diff.
- Субагент `rules-reviewer` (`.claude/agents/`) — ревью diff против этого файла; вызывай после крупной задачи.

## 10. Команды

- Сборка: `./gradlew :androidApp:assembleDebug` · компиляция: `./gradlew :composeApp:compileAndroidMain --console=plain`
- Тесты: `./gradlew :composeApp:testAndroidHostTest --console=plain`
- Пайплайн данных: `python tools/scrape_guia.py`, затем `python tools/build_embeddings.py` (нужен Ollama)
- История MVP: `mvp-spec.md`, `mvp-plan.md`, `real-answers-plan.md` — читать только при работе над соответствующей областью.
