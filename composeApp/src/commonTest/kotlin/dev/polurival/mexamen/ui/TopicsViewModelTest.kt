package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.domain.SessionPoolSelector
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
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** #21: пункт «Repasar errores» в списке тем — число вопросов с ошибками, обновляется при каждом показе. */
@OptIn(ExperimentalCoroutinesApi::class)
class TopicsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadWithoutErrorsShowsInactiveMistakesItem() = runTest {
        val store = FakeQuestionStatsStore()
        store.update { mapOf(1 to QuestionStats(timesShown = 1, timesCorrect = 1)) }
        val vm = TopicsViewModel(testQuestionRepository(size = 12), store)
        assertTrue(vm.state.value.isLoading)

        vm.load()
        runCurrent()

        val state = vm.state.value
        assertFalse(state.isLoading)
        assertEquals(12, state.totalCount)
        assertEquals(SessionPoolSelector.TOPIC_MISTAKES, state.mistakes.name)
        assertEquals(0, state.mistakes.questionCount)
        assertFalse(state.hasMistakes)
    }

    @Test
    fun reloadPicksUpNewErrors() = runTest {
        val store = FakeQuestionStatsStore()
        val vm = TopicsViewModel(testQuestionRepository(size = 12), store)
        vm.load()
        runCurrent()

        store.update { mapOf(2 to QuestionStats(timesShown = 1, timesWrong = 1), 5 to QuestionStats(timesShown = 2, timesWrong = 2)) }
        vm.load()
        runCurrent()

        assertEquals(2, vm.state.value.mistakes.questionCount)
        assertTrue(vm.state.value.hasMistakes)
    }
}
