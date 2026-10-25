package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.TestDatasetBytes
import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.domain.BuildFlashcardsUseCase
import dev.polurival.mexamen.domain.FlashcardGrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/** В памяти, без Android. */
private class FlashcardFakeStatsStore(
    private var stats: Map<Int, QuestionStats> = emptyMap(),
) : QuestionStatsStore {
    override suspend fun readAll(): Map<Int, QuestionStats> = stats

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        stats = transform(stats)
    }

    override suspend fun clear() {
        stats = emptyMap()
    }
}

/**
 * Основной сценарий [FlashcardViewModel] (issue #19): оценка без переворота игнорируется,
 * flip → grade ведёт к следующей карте со счётчиками из store, после последней карты — `Done`.
 * Таймера нет, поэтому можно `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class FlashcardViewModelTest {

    private fun repository(): QuestionRepository {
        val entries = (1..DATASET_SIZE).map { id -> QuestionEntry(id, "tema", "q$id", "Answer $id") }
        val vectors = entries.map { e ->
            val angle = e.id * 0.15f
            floatArrayOf(cos(angle), sin(angle))
        }
        return TestDatasetBytes.repository(entries, vectors)
    }

    /** Разные счётчики на каждый id, без `lastSeen` — нет cooldown, все вопросы доступны для выборки. */
    private fun priorStats(): Map<Int, QuestionStats> =
        (1..DATASET_SIZE).associateWith { id ->
            QuestionStats(timesShown = PRIOR_SHOWN_OFFSET + id, timesCorrect = id, timesWrong = PRIOR_SHOWN_OFFSET)
        }

    private fun TestScope.useTestMain() {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun gradeWithoutFlipIsIgnored() = runTest {
        useTestMain()
        val store = FlashcardFakeStatsStore()
        val viewModel = FlashcardViewModel(repository(), store)
        viewModel.start()
        advanceUntilIdle()
        val before = assertIs<FlashcardUiState.Card>(viewModel.state.value)

        viewModel.grade(FlashcardGrade.GOOD)
        advanceUntilIdle()

        val after = assertIs<FlashcardUiState.Card>(viewModel.state.value)
        assertEquals(before, after)
        assertEquals(0, after.index)
        assertFalse(after.revealed)
        assertTrue(store.readAll().isEmpty())
    }

    @Test
    fun flipThenGradeShowsNextCardWithCountsFromStore() = runTest {
        useTestMain()
        val store = FlashcardFakeStatsStore(priorStats())
        val viewModel = FlashcardViewModel(repository(), store)
        viewModel.start()
        advanceUntilIdle()
        val first = assertIs<FlashcardUiState.Card>(viewModel.state.value)
        assertEquals(AttemptCounts(PRIOR_SHOWN_OFFSET + first.entry.id, first.entry.id), first.attempts)

        viewModel.flip()
        assertTrue(assertIs<FlashcardUiState.Card>(viewModel.state.value).revealed)
        viewModel.grade(FlashcardGrade.GOOD)
        advanceUntilIdle()

        val next = assertIs<FlashcardUiState.Card>(viewModel.state.value)
        assertEquals(1, next.index)
        assertEquals(BuildFlashcardsUseCase.FLASHCARD_SIZE, next.total)
        assertFalse(next.revealed)
        assertNotEquals(first.entry.id, next.entry.id)
        val nextId = next.entry.id
        assertEquals(AttemptCounts(PRIOR_SHOWN_OFFSET + nextId, nextId), next.attempts)
        assertEquals(AttemptCounts.from(store.readAll()[nextId]), next.attempts)
        // Оценка записана в store для оценённой карты (GOOD = correct)
        val graded = store.readAll().getValue(first.entry.id)
        assertEquals(PRIOR_SHOWN_OFFSET + first.entry.id + 1, graded.timesShown)
        assertEquals(first.entry.id + 1, graded.timesCorrect)
    }

    @Test
    fun gradingLastCardShowsDoneWithGradeCounts() = runTest {
        useTestMain()
        val store = FlashcardFakeStatsStore()
        val viewModel = FlashcardViewModel(repository(), store)
        viewModel.start()
        advanceUntilIdle()
        val grades = List(BuildFlashcardsUseCase.FLASHCARD_SIZE) { i ->
            when {
                i < 1 -> FlashcardGrade.AGAIN
                i < 3 -> FlashcardGrade.HARD
                i < 7 -> FlashcardGrade.GOOD
                else -> FlashcardGrade.EASY
            }
        }

        grades.forEachIndexed { step, grade ->
            assertEquals(step, assertIs<FlashcardUiState.Card>(viewModel.state.value).index)
            viewModel.flip()
            viewModel.grade(grade)
            advanceUntilIdle()
        }

        val done = assertIs<FlashcardUiState.Done>(viewModel.state.value)
        val expected = mapOf(
            FlashcardGrade.AGAIN to 1,
            FlashcardGrade.HARD to 2,
            FlashcardGrade.GOOD to 4,
            FlashcardGrade.EASY to 3,
        )
        assertEquals(expected, done.counts)
        val stats = store.readAll()
        assertEquals(BuildFlashcardsUseCase.FLASHCARD_SIZE, stats.size)
        assertEquals(BuildFlashcardsUseCase.FLASHCARD_SIZE, stats.values.sumOf { it.timesShown })
        assertEquals(3, stats.values.sumOf { it.timesWrong })
        assertEquals(7, stats.values.sumOf { it.timesCorrect })
    }

    private companion object {
        const val DATASET_SIZE = 24
        const val PRIOR_SHOWN_OFFSET = 100
    }
}
