package dev.polurival.mexamen.ui

import dev.polurival.mexamen.domain.FlashcardGrade
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
}
