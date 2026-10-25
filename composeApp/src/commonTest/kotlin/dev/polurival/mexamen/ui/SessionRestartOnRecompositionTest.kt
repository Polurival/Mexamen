package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.TestDatasetBytes
import dev.polurival.mexamen.domain.FlashcardGrade
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotEquals

/** В памяти, без Android. */
private class InMemoryQuestionStatsStore : QuestionStatsStore {
    private var stats: Map<Int, QuestionStats> = emptyMap()

    override suspend fun readAll(): Map<Int, QuestionStats> = stats

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        stats = transform(stats)
    }

    override suspend fun clear() {
        stats = emptyMap()
    }
}

/**
 * Регрессия issue #6: при повороте экрана `LaunchedEffect(topic)` в `QuizFlow`/`FlashcardFlow`
 * снова зовёт старт у пережившей поворот ViewModel — идущая сессия не должна сбрасываться.
 * Таймер квиза крутится на виртуальном времени, поэтому только `runCurrent()`, без `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SessionRestartOnRecompositionTest {

    private fun repository(): QuestionRepository {
        val entries = (1..24).map { id ->
            QuestionEntry(id, if (id <= 12) TOPIC else "otra", "q$id", "Answer $id")
        }
        val vectors = entries.map { e ->
            val angle = e.id * 0.15f
            floatArrayOf(cos(angle), sin(angle))
        }
        return TestDatasetBytes.repository(entries, vectors)
    }

    private fun TestScope.useTestMain() {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun repeatedQuizStartForSameTopicKeepsCurrentQuestion() = runTest {
        useTestMain()
        val viewModel = QuizViewModel(repository(), InMemoryQuestionStatsStore())
        viewModel.startQuiz(TOPIC)
        runCurrent()
        viewModel.selectAnswer(0)
        viewModel.advanceQuestion()
        runCurrent()
        val before = assertIs<QuizUiState.Question>(viewModel.state.value)
        assertEquals(1, before.index)

        // Пересоздание композиции после поворота
        viewModel.startQuiz(TOPIC)
        runCurrent()

        val after = assertIs<QuizUiState.Question>(viewModel.state.value)
        assertEquals(before.index, after.index)
        assertEquals(before.question, after.question)
    }

    @Test
    fun explicitQuizRestartResetsSession() = runTest {
        useTestMain()
        val viewModel = QuizViewModel(repository(), InMemoryQuestionStatsStore())
        viewModel.startQuiz(TOPIC)
        runCurrent()
        viewModel.selectAnswer(0)
        viewModel.advanceQuestion()
        runCurrent()
        assertEquals(1, assertIs<QuizUiState.Question>(viewModel.state.value).index)

        viewModel.startQuiz(TOPIC, restart = true)
        runCurrent()

        assertEquals(0, assertIs<QuizUiState.Question>(viewModel.state.value).index)
    }

    @Test
    fun repeatedFlashcardStartForSameTopicKeepsCurrentCard() = runTest {
        useTestMain()
        val viewModel = FlashcardViewModel(repository(), InMemoryQuestionStatsStore())
        viewModel.start(TOPIC)
        runCurrent()
        viewModel.flip()
        viewModel.grade(FlashcardGrade.GOOD)
        runCurrent()
        val before = assertIs<FlashcardUiState.Card>(viewModel.state.value)
        assertEquals(1, before.index)

        viewModel.start(TOPIC)
        runCurrent()

        val after = assertIs<FlashcardUiState.Card>(viewModel.state.value)
        assertEquals(before.index, after.index)
        assertEquals(before.entry, after.entry)
    }

    @Test
    fun explicitFlashcardRestartResetsSession() = runTest {
        useTestMain()
        val viewModel = FlashcardViewModel(repository(), InMemoryQuestionStatsStore())
        viewModel.start(TOPIC)
        runCurrent()
        viewModel.flip()
        viewModel.grade(FlashcardGrade.GOOD)
        runCurrent()
        assertNotEquals(0, assertIs<FlashcardUiState.Card>(viewModel.state.value).index)

        viewModel.start(TOPIC, restart = true)
        runCurrent()

        assertEquals(0, assertIs<FlashcardUiState.Card>(viewModel.state.value).index)
    }

    private companion object {
        const val TOPIC = "tema"
    }
}
