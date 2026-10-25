package dev.polurival.mexamen.ui

import dev.polurival.mexamen.testing.FakeQuestionStatsStore
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
import kotlin.test.assertIs

/** Регрессия #6: пересоздание композиции (поворот) не должно перезапускать идущую сессию. */
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
}
