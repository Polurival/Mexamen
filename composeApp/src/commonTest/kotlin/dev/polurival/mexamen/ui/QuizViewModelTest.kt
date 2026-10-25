package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.TestDatasetBytes
import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.domain.BuildQuizUseCase
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
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull

/** В памяти, без Android. */
private class FakeQuestionStatsStore : QuestionStatsStore {
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
 * Основной сценарий [QuizViewModel] (issue #18): старт → вопрос 0 из 10, единственный ответ
 * на вопрос, полный проход → `Result` со счётом и статистикой по всем 10 вопросам.
 * Таймер крутится на виртуальном времени, поэтому только `runCurrent()`, без `advanceUntilIdle()`.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

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
    fun startShowsFirstQuestionOfTen() = runTest {
        useTestMain()
        val viewModel = QuizViewModel(repository(), FakeQuestionStatsStore())

        viewModel.startQuiz()
        runCurrent()

        val question = assertIs<QuizUiState.Question>(viewModel.state.value)
        assertEquals(0, question.index)
        assertEquals(BuildQuizUseCase.QUIZ_SIZE, question.total)
        assertNull(question.selectedIndex)
        assertEquals(QuizViewModel.TIME_LIMIT_SECONDS, question.timeLeftSeconds)
    }

    @Test
    fun answerSetsSelectedIndexAndSecondAnswerIsIgnored() = runTest {
        useTestMain()
        val store = FakeQuestionStatsStore()
        val viewModel = QuizViewModel(repository(), store)
        viewModel.startQuiz()
        runCurrent()
        val question = assertIs<QuizUiState.Question>(viewModel.state.value).question
        val wrongIndex = wrongIndex(question.correctIndex, question.options.size)

        viewModel.selectAnswer(question.correctIndex)
        viewModel.selectAnswer(wrongIndex)
        runCurrent()

        val state = assertIs<QuizUiState.Question>(viewModel.state.value)
        assertEquals(0, state.index)
        assertEquals(question.correctIndex, state.selectedIndex)
        val stats = store.readAll().getValue(question.entry.id)
        assertEquals(1, stats.timesShown)
        assertEquals(1, stats.timesCorrect)
        assertEquals(0, stats.timesWrong)
    }

    @Test
    fun tenAnswersShowResultWithScoreAndRecordStatsForAllQuestions() = runTest {
        useTestMain()
        val store = FakeQuestionStatsStore()
        val viewModel = QuizViewModel(repository(), store)
        viewModel.startQuiz()
        runCurrent()
        val correctIds = mutableSetOf<Int>()
        val wrongIds = mutableSetOf<Int>()
        repeat(BuildQuizUseCase.QUIZ_SIZE) { step ->
            val current = assertIs<QuizUiState.Question>(viewModel.state.value)
            assertEquals(step, current.index)
            val question = current.question
            if (step < CORRECT_ANSWERS) {
                correctIds += question.entry.id
                viewModel.selectAnswer(question.correctIndex)
            } else {
                wrongIds += question.entry.id
                viewModel.selectAnswer(wrongIndex(question.correctIndex, question.options.size))
            }
            viewModel.advanceQuestion()
            runCurrent()
        }

        val result = assertIs<QuizUiState.Result>(viewModel.state.value)
        assertEquals(BuildQuizUseCase.QUIZ_SIZE, result.result.total)
        assertEquals(CORRECT_ANSWERS, result.result.score)
        assertFalse(result.passed)
        val stats = store.readAll()
        assertEquals(correctIds + wrongIds, stats.keys)
        assertEquals(BuildQuizUseCase.QUIZ_SIZE, stats.size)
        correctIds.forEach { id ->
            assertEquals(1, stats.getValue(id).timesCorrect)
            assertEquals(0, stats.getValue(id).timesWrong)
        }
        wrongIds.forEach { id ->
            assertEquals(1, stats.getValue(id).timesWrong)
            assertEquals(0, stats.getValue(id).timesCorrect)
        }
        assertEquals(stats.mapValues { (_, s) -> AttemptCounts.from(s) }, result.attempts)
        assertEquals(BuildQuizUseCase.QUIZ_SIZE, result.attempts.values.sumOf { it.timesShown })
    }

    private fun wrongIndex(correctIndex: Int, optionsCount: Int): Int = (correctIndex + 1) % optionsCount

    private companion object {
        const val CORRECT_ANSWERS = 7
    }
}
