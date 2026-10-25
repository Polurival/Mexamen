package dev.polurival.mexamen.ui

import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.domain.BuildQuizUseCase
import dev.polurival.mexamen.domain.ExamRules
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
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Базовый поток (#18): старт → первый вопрос, единственный ответ на вопрос, итог и запись статистики.
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
    fun startShowsFirstQuestionOfFullQuiz() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()

        val question = assertIs<QuizUiState.Question>(vm.state.value)
        assertEquals(0, question.index)
        assertEquals(BuildQuizUseCase.QUIZ_SIZE, question.total)
        assertEquals(10, question.total)
        assertNull(question.selectedIndex)
        assertEquals(ExamRules.QUIZ_DURATION_SECONDS, question.timeLeftSeconds)
    }

    @Test
    fun selectAnswerSetsSelectedIndexAndIgnoresRepeatedAnswer() = runTest {
        val store = FakeQuestionStatsStore()
        val vm = QuizViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        val first = assertIs<QuizUiState.Question>(vm.state.value)
        val wrongIndex = (first.question.correctIndex + 1) % first.question.options.size

        vm.selectAnswer(first.question.correctIndex)
        runCurrent()
        vm.selectAnswer(wrongIndex)
        runCurrent()

        val answered = assertIs<QuizUiState.Question>(vm.state.value)
        assertEquals(0, answered.index)
        assertEquals(first.question.correctIndex, answered.selectedIndex)
        val stats = store.readAll().getValue(first.question.entry.id)
        assertEquals(1, stats.timesShown)
        assertEquals(1, stats.timesCorrect)
        assertEquals(0, stats.timesWrong)
    }

    @Test
    fun tenAnswersShowResultWithScoreAndRecordStatsForEachQuestion() = runTest {
        val store = FakeQuestionStatsStore()
        val vm = QuizViewModel(testQuestionRepository(), store)
        vm.ensureStarted(null)
        runCurrent()
        val expectedCorrect = mutableMapOf<Int, Boolean>()
        repeat(BuildQuizUseCase.QUIZ_SIZE) { i ->
            val question = assertIs<QuizUiState.Question>(vm.state.value)
            assertEquals(i, question.index)
            // Чётные — верно, нечётные — неверно: ожидаемый score = половина квиза.
            val correct = i % 2 == 0
            val choice = if (correct) {
                question.question.correctIndex
            } else {
                (question.question.correctIndex + 1) % question.question.options.size
            }
            expectedCorrect[question.question.entry.id] = correct
            vm.selectAnswer(choice)
            vm.advanceQuestion()
            runCurrent()
        }

        val state = assertIs<QuizUiState.Result>(vm.state.value)
        assertEquals(10, state.result.total)
        assertEquals(5, state.result.score)
        assertTrue(state.result.answers.all { it.isAnswered })
        assertFalse(state.passed)
        val stats = store.readAll()
        assertEquals(10, stats.size)
        assertEquals(expectedCorrect.keys, stats.keys)
        expectedCorrect.forEach { (id, correct) ->
            val s = stats.getValue(id)
            assertEquals(1, s.timesShown)
            assertEquals(if (correct) 1 else 0, s.timesCorrect)
            assertEquals(if (correct) 0 else 1, s.timesWrong)
        }
    }

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

    @Test
    fun allCorrectQuizResultIsPassed() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()
        while (true) {
            val question = vm.state.value as? QuizUiState.Question ?: break
            vm.selectAnswer(question.question.correctIndex)
            vm.advanceQuestion()
            runCurrent()
        }

        val state = assertIs<QuizUiState.Result>(vm.state.value)
        assertTrue(state.result.score >= ExamRules.QUIZ_PASS_SCORE)
        assertTrue(state.passed)
    }

    @Test
    fun timeoutWithoutAnswersIsNotPassed() = runTest {
        val vm = viewModel()
        vm.ensureStarted(null)
        runCurrent()

        advanceTimeBy(QUIZ_TIMEOUT_MILLIS)
        runCurrent()

        val state = assertIs<QuizUiState.Result>(vm.state.value)
        assertEquals(0, state.result.score)
        assertFalse(state.passed)
    }

    private companion object {
        /** Тики таймера на всю длительность квиза + тик, на котором срабатывает завершение. */
        const val QUIZ_TIMEOUT_MILLIS = (ExamRules.QUIZ_DURATION_SECONDS + 1) * 1_000L
    }
}
