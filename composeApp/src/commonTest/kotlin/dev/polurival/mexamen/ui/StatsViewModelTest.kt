package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionStats
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
import kotlin.test.assertTrue

/** #15: StatsScreen стал stateless — загрузка и сброс живут в VM, поведение фиксируем тестом. */
@OptIn(ExperimentalCoroutinesApi::class)
class StatsViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadShowsSummaryFromStore() = runTest {
        val store = FakeQuestionStatsStore()
        store.update { mapOf(1 to QuestionStats(timesShown = 2, timesCorrect = 1, timesWrong = 1)) }
        val vm = StatsViewModel(testQuestionRepository(size = 12), store)
        assertIs<StatsUiState.Loading>(vm.state.value)

        vm.load()
        runCurrent()

        val loaded = assertIs<StatsUiState.Loaded>(vm.state.value)
        assertEquals(12, loaded.summary.totalQuestions)
        assertEquals(1, loaded.summary.attempted)
        assertEquals(1, loaded.summary.totalCorrect)
        assertEquals(1, loaded.summary.totalWrong)
        assertTrue(loaded.topics.isNotEmpty())
    }

    @Test
    fun resetStatsClearsStoreAndReloads() = runTest {
        val store = FakeQuestionStatsStore()
        store.update { mapOf(1 to QuestionStats(timesShown = 1, timesCorrect = 1)) }
        val vm = StatsViewModel(testQuestionRepository(size = 12), store)
        vm.load()
        runCurrent()

        vm.resetStats()
        runCurrent()

        assertTrue(store.readAll().isEmpty())
        val loaded = assertIs<StatsUiState.Loaded>(vm.state.value)
        assertEquals(0, loaded.summary.attempted)
        assertEquals(12, loaded.summary.neverAttempted)
    }
}
