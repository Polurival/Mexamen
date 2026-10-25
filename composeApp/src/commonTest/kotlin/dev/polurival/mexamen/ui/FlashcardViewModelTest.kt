package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.domain.FlashcardGrade
import dev.polurival.mexamen.domain.SessionPoolSelector
import dev.polurival.mexamen.testing.FakeQuestionStatsStore
import dev.polurival.mexamen.testing.GatedQuestionStatsStore
import dev.polurival.mexamen.testing.testQuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Базовый поток (#19): оценка только после переворота, переход на следующую карту со счётчиками
 * из store, итог `Done` с количеством оценок.
 * Регрессия #6: пересоздание композиции (поворот) не должно перезапускать идущую сессию.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = FlashcardViewModel(testQuestionRepository(), FakeQuestionStatsStore())

    @Test
    fun repeatedEnsureStartedKeepsCurrentCard() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        vm.flip()
        vm.grade(FlashcardGrade.GOOD)
        runCurrent()
        val before = assertIs<FlashcardUiState.Card>(vm.state.value)
        assertEquals(1, before.index)

        vm.ensureStarted(null)
        runCurrent()

        val after = assertIs<FlashcardUiState.Card>(vm.state.value)
        assertEquals(1, after.index)
        assertEquals(before.entry, after.entry)
    }

    @Test
    fun ensureStartedOnDoneKeepsDone() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        while (vm.state.value is FlashcardUiState.Card) {
            vm.flip()
            vm.grade(FlashcardGrade.GOOD)
            vm.ensureStarted(null)
            runCurrent()
        }
        val done = assertIs<FlashcardUiState.Done>(vm.state.value)

        vm.ensureStarted(null)
        runCurrent()

        assertEquals(done, vm.state.value)
    }

    @Test
    fun explicitStartRestartsSession() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        vm.flip()
        vm.grade(FlashcardGrade.GOOD)
        runCurrent()
        assertEquals(1, assertIs<FlashcardUiState.Card>(vm.state.value).index)

        vm.start(null)
        runCurrent()

        assertEquals(0, assertIs<FlashcardUiState.Card>(vm.state.value).index)
    }

    @Test
    fun cardCarriesPersistedAttemptCounts() = runTest {
        val store = FakeQuestionStatsStore()
        store.update { (1..12).associateWith { QuestionStats(timesShown = 3, timesCorrect = 2, timesWrong = 1) } }
        val vm = FlashcardViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()

        assertEquals(AttemptCounts(timesShown = 3, timesCorrect = 2), assertIs<FlashcardUiState.Card>(vm.state.value).attempts)
    }

    @Test
    fun gradeWithoutFlipIsIgnored() = runTest {
        val store = FakeQuestionStatsStore()
        val vm = FlashcardViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        val before = assertIs<FlashcardUiState.Card>(vm.state.value)
        assertFalse(before.revealed)

        vm.grade(FlashcardGrade.GOOD)
        runCurrent()

        assertEquals(before, vm.state.value)
        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun flipThenGradeShowsNextCardWithCountsFromStore() = runTest {
        val store = FakeQuestionStatsStore()
        // У каждого вопроса свои счётчики — по ним видно, что взяты именно данные следующей карты.
        store.update { (1..12).associateWith { id -> QuestionStats(timesShown = id + 10, timesCorrect = id) } }
        val vm = FlashcardViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        val first = assertIs<FlashcardUiState.Card>(vm.state.value)

        vm.flip()
        assertTrue(assertIs<FlashcardUiState.Card>(vm.state.value).revealed)
        vm.grade(FlashcardGrade.GOOD)
        runCurrent()

        val next = assertIs<FlashcardUiState.Card>(vm.state.value)
        assertEquals(1, next.index)
        assertEquals(first.total, next.total)
        assertFalse(next.revealed)
        assertTrue(next.entry.id != first.entry.id)
        val id = next.entry.id
        assertEquals(AttemptCounts(timesShown = id + 10, timesCorrect = id), next.attempts)
        val graded = store.readAll().getValue(first.entry.id)
        assertEquals(first.entry.id + 11, graded.timesShown)
        assertEquals(first.entry.id + 1, graded.timesCorrect)
    }

    @Test
    fun nextCardWaitsForPendingWriteAndRepeatedGradeIsIgnored() = runTest {
        val store = GatedQuestionStatsStore()
        val vm = FlashcardViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        val first = assertIs<FlashcardUiState.Card>(vm.state.value)
        vm.flip()

        vm.grade(FlashcardGrade.AGAIN) // запись висит на gate — следующая карта её ждёт, окно открыто
        runCurrent()
        assertEquals(0, assertIs<FlashcardUiState.Card>(vm.state.value).index)
        vm.grade(FlashcardGrade.EASY) // повторный тап в окне до следующей карты
        runCurrent()
        store.open()
        runCurrent()

        val next = assertIs<FlashcardUiState.Card>(vm.state.value)
        assertEquals(1, next.index)
        val graded = store.readAll().getValue(first.entry.id)
        assertEquals(1, graded.timesShown)
        assertEquals(1, graded.timesWrong)
        assertEquals(0, graded.timesCorrect)
    }

    @Test
    fun lastCardGradeShowsDoneWithGradeCountsAndRecordsEveryCard() = runTest {
        val store = FakeQuestionStatsStore()
        val vm = FlashcardViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        val total = assertIs<FlashcardUiState.Card>(vm.state.value).total
        val grades = FlashcardGrade.entries
        val expectedGrades = mutableMapOf<Int, FlashcardGrade>()
        repeat(total) { i ->
            val card = assertIs<FlashcardUiState.Card>(vm.state.value)
            assertEquals(i, card.index)
            val grade = grades[i % grades.size]
            expectedGrades[card.entry.id] = grade
            vm.flip()
            vm.grade(grade)
            runCurrent()
        }

        val done = assertIs<FlashcardUiState.Done>(vm.state.value)
        // 10 карт по кругу AGAIN, HARD, GOOD, EASY.
        assertEquals(10, total)
        assertEquals(
            mapOf(FlashcardGrade.AGAIN to 3, FlashcardGrade.HARD to 3, FlashcardGrade.GOOD to 2, FlashcardGrade.EASY to 2),
            done.counts,
        )
        val stats = store.readAll()
        assertEquals(expectedGrades.keys, stats.keys)
        expectedGrades.forEach { (id, grade) ->
            val correct = grade == FlashcardGrade.GOOD || grade == FlashcardGrade.EASY
            assertEquals(AttemptCounts(timesShown = 1, timesCorrect = if (correct) 1 else 0), AttemptCounts.from(stats[id]))
        }

        vm.flip()
        vm.grade(FlashcardGrade.GOOD)
        runCurrent()
        assertEquals(done, vm.state.value)
    }

    @Test
    fun mistakesTopicWithoutErrorsShowsNoCards() = runTest {
        val vm = viewModel()
        vm.ensureStarted(SessionPoolSelector.TOPIC_MISTAKES)
        runCurrent()

        assertEquals("No hay tarjetas", assertIs<FlashcardUiState.Error>(vm.state.value).message)
    }
}
