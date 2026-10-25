package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.TestDatasetBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

private class TopicsFakeStatsStore(var stats: Map<Int, QuestionStats> = emptyMap()) : QuestionStatsStore {
    override suspend fun readAll(): Map<Int, QuestionStats> = stats
    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        stats = transform(stats)
    }
    override suspend fun clear() {
        stats = emptyMap()
    }
}

/** Счётчик «Repasar errores» в списке тем (issue #21): считается из store и обновляется при повторном load(). */
@OptIn(ExperimentalCoroutinesApi::class)
class TopicsViewModelTest {

    private fun repository(): QuestionRepository {
        val entries = (1..5).map { QuestionEntry(it, "tema", "q$it", "Answer $it") }
        return TestDatasetBytes.repository(entries, entries.map { floatArrayOf(it.id.toFloat(), 1f) })
    }

    private fun TestScope.useTestMain() {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun mistakesCountIsZeroWithoutErrors() = runTest {
        useTestMain()
        val store = TopicsFakeStatsStore(mapOf(1 to QuestionStats(timesShown = 1, timesCorrect = 1)))
        val viewModel = TopicsViewModel(repository(), store)
        viewModel.load()
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.mistakesCount)
        assertEquals(5, viewModel.state.value.totalCount)
        assertFalse(viewModel.state.value.isLoading)
    }

    @Test
    fun reloadPicksUpNewMistakes() = runTest {
        useTestMain()
        val store = TopicsFakeStatsStore()
        val viewModel = TopicsViewModel(repository(), store)
        viewModel.load()
        advanceUntilIdle()
        assertEquals(0, viewModel.state.value.mistakesCount)

        store.stats = mapOf(
            2 to QuestionStats(timesShown = 1, timesWrong = 1),
            4 to QuestionStats(timesShown = 2, timesCorrect = 1, timesWrong = 1),
        )
        viewModel.load()
        advanceUntilIdle()
        assertEquals(2, viewModel.state.value.mistakesCount)
    }
}
