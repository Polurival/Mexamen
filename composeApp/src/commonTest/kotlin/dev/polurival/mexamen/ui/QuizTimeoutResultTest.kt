package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.TestDatasetBytes
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.math.cos
import kotlin.math.sin
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** В памяти, без Android. */
private class TimeoutStatsStore : QuestionStatsStore {
    private var stats: Map<Int, QuestionStats> = emptyMap()

    override suspend fun readAll(): Map<Int, QuestionStats> = stats

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        stats = transform(stats)
    }

    override suspend fun clear() {
        stats = emptyMap()
    }
}

/** Запись в store висит до [gate] — имитирует незавершённую запись ответа на момент таймаута. */
private class GatedStatsStore : QuestionStatsStore {
    val gate = CompletableDeferred<Unit>()
    private var stats: Map<Int, QuestionStats> = emptyMap()

    override suspend fun readAll(): Map<Int, QuestionStats> = stats

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        gate.await()
        stats = transform(stats)
    }

    override suspend fun clear() {
        stats = emptyMap()
    }
}

/**
 * Регрессия issue #8: по истечении таймера результат должен включать весь квиз —
 * неотвеченные вопросы идут в разбор как «Sin respuesta» (неверные), но в статистику не пишутся.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizTimeoutResultTest {

    private fun repository(): QuestionRepository {
        val entries = (1..24).map { id -> QuestionEntry(id, "tema", "q$id", "Answer $id") }
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
    fun timeoutResultIncludesUnansweredQuestionsAsWrongWithoutRecordingStats() = runTest {
        useTestMain()
        val store = TimeoutStatsStore()
        val viewModel = QuizViewModel(repository(), store)
        viewModel.startQuiz()
        runCurrent()
        val quizSize = assertIs<QuizUiState.Question>(viewModel.state.value).total
        val answeredIds = mutableListOf<Int>()
        repeat(ANSWERED) {
            val question = assertIs<QuizUiState.Question>(viewModel.state.value).question
            answeredIds += question.entry.id
            viewModel.selectAnswer(question.correctIndex)
            viewModel.advanceQuestion()
            runCurrent()
        }

        advanceTimeBy(TIMEOUT_MILLIS)
        runCurrent()

        val result = assertIs<QuizUiState.Result>(viewModel.state.value).result
        assertEquals(quizSize, result.total)
        assertEquals(ANSWERED, result.score)
        val unanswered = result.answers.drop(ANSWERED)
        assertEquals(quizSize - ANSWERED, unanswered.size)
        assertTrue(unanswered.none { it.isAnswered || it.isCorrect })
        assertEquals(quizSize, result.answers.map { it.question.entry.id }.toSet().size)
        assertEquals(answeredIds.toSet(), store.readAll().keys)
    }

    @Test
    fun timeoutKeepsAnswerSelectedOnCurrentQuestion() = runTest {
        useTestMain()
        val store = TimeoutStatsStore()
        val viewModel = QuizViewModel(repository(), store)
        viewModel.startQuiz()
        runCurrent()
        val current = assertIs<QuizUiState.Question>(viewModel.state.value)
        // Ответ выбран, но автопереход ещё не случился
        viewModel.selectAnswer(current.question.correctIndex)
        runCurrent()

        advanceTimeBy(TIMEOUT_MILLIS)
        runCurrent()

        val result = assertIs<QuizUiState.Result>(viewModel.state.value).result
        assertEquals(current.total, result.total)
        assertEquals(1, result.score)
        assertFalse(result.answers.drop(1).any { it.isAnswered })
        assertEquals(setOf(current.question.entry.id), store.readAll().keys)
    }

    @Test
    fun answerAfterTimeoutIsIgnoredWhileRecordIsPending() = runTest {
        useTestMain()
        val store = GatedStatsStore()
        val viewModel = QuizViewModel(repository(), store)
        viewModel.startQuiz()
        runCurrent()
        val first = assertIs<QuizUiState.Question>(viewModel.state.value)
        viewModel.selectAnswer(first.question.correctIndex)
        viewModel.advanceQuestion()
        runCurrent()
        val second = assertIs<QuizUiState.Question>(viewModel.state.value)
        assertEquals(1, second.index)

        // Таймаут, а запись первого ответа ещё не завершена — Result пока не выставлен
        advanceTimeBy(TIMEOUT_MILLIS)
        runCurrent()
        viewModel.selectAnswer(second.question.correctIndex)
        viewModel.advanceQuestion()
        runCurrent()
        store.gate.complete(Unit)
        runCurrent()

        val result = assertIs<QuizUiState.Result>(viewModel.state.value).result
        assertEquals(first.total, result.total)
        assertEquals(1, result.score)
        assertFalse(result.answers[1].isAnswered)
        assertEquals(setOf(first.question.entry.id), store.readAll().keys)
    }

    private companion object {
        const val ANSWERED = 3
        const val TIMEOUT_MILLIS = 601_000L
    }
}
