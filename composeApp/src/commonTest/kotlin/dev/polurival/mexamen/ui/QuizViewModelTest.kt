package dev.polurival.mexamen.ui

import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.testing.FakeQuestionStatsStore
import dev.polurival.mexamen.testing.GatedQuestionStatsStore
import dev.polurival.mexamen.testing.testQuestionRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
 * Регрессия #6: пересоздание композиции (поворот) не должно перезапускать идущую сессию.
 * Регрессия #8: при таймауте неотвеченные вопросы входят в итог и не пишутся в статистику.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class QuizViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel() = QuizViewModel(testQuestionRepository(), FakeQuestionStatsStore())

    @Test
    fun repeatedEnsureStartedKeepsCurrentQuestion() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        vm.selectAnswer(0)
        vm.advanceQuestion()
        runCurrent()
        val before = assertIs<QuizUiState.Question>(vm.state.value)
        assertEquals(1, before.index)

        vm.ensureStarted(null)
        runCurrent()

        val after = assertIs<QuizUiState.Question>(vm.state.value)
        assertEquals(1, after.index)
        assertEquals(before.question, after.question)
    }

    @Test
    fun ensureStartedOnResultKeepsResult() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        while (vm.state.value is QuizUiState.Question) {
            vm.selectAnswer(0)
            vm.advanceQuestion()
            vm.ensureStarted(null)
            runCurrent()
        }
        val result = assertIs<QuizUiState.Result>(vm.state.value)

        vm.ensureStarted(null)
        runCurrent()

        assertEquals(result, vm.state.value)
    }

    @Test
    fun explicitStartQuizRestartsSession() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        vm.selectAnswer(0)
        vm.advanceQuestion()
        runCurrent()
        assertEquals(1, assertIs<QuizUiState.Question>(vm.state.value).index)

        vm.startQuiz(null)
        runCurrent()

        val restarted = assertIs<QuizUiState.Question>(vm.state.value)
        assertEquals(0, restarted.index)
        assertEquals(null, restarted.selectedIndex)
    }

    @Test
    fun timeoutResultIncludesUnansweredQuestionsWithoutRecordingThem() = runTest {
        val store = FakeQuestionStatsStore()
        val vm = QuizViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        val quizSize = assertIs<QuizUiState.Question>(vm.state.value).total
        repeat(3) {
            vm.selectAnswer(0)
            vm.advanceQuestion()
            runCurrent()
        }

        advanceTimeBy(QUIZ_TIMEOUT_MILLIS)
        runCurrent()

        val result = assertIs<QuizUiState.Result>(vm.state.value).result
        assertEquals(quizSize, result.total)
        assertEquals(quizSize, result.answers.size)
        assertTrue(result.answers.take(3).all { it.isAnswered })
        assertTrue(result.answers.drop(3).none { it.isAnswered || it.isCorrect })
        val answeredIds = result.answers.take(3).map { it.question.entry.id }.toSet()
        assertEquals(answeredIds, store.readAll().keys)
    }

    @Test
    fun answerAfterTimeoutBeforeResultIsIgnored() = runTest {
        val store = GatedQuestionStatsStore()
        val vm = QuizViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        vm.selectAnswer(0) // запись висит на gate — результат ждёт её, окно открыто
        advanceTimeBy(QUIZ_TIMEOUT_MILLIS)
        runCurrent()
        assertFalse(vm.state.value is QuizUiState.Result)

        vm.advanceQuestion()
        runCurrent()
        vm.selectAnswer(1)
        runCurrent()
        store.open()
        runCurrent()

        val result = assertIs<QuizUiState.Result>(vm.state.value).result
        assertTrue(result.answers.first().isAnswered)
        assertTrue(result.answers.drop(1).none { it.isAnswered })
        assertEquals(setOf(result.answers.first().question.entry.id), store.readAll().keys)
    }

    @Test
    fun resultAttemptsIncludeAnswersOfFinishedSession() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        while (vm.state.value is QuizUiState.Question) {
            vm.selectAnswer(0)
            vm.advanceQuestion()
            runCurrent()
        }

        val state = assertIs<QuizUiState.Result>(vm.state.value)
        state.result.answers.forEach { answered ->
            val expected = AttemptCounts(timesShown = 1, timesCorrect = if (answered.isCorrect) 1 else 0)
            assertEquals(expected, state.attempts[answered.question.entry.id])
        }
    }

    private companion object {
        /** 600 тиков таймера + тик, на котором срабатывает завершение. */
        const val QUIZ_TIMEOUT_MILLIS = 601_000L
    }
}
