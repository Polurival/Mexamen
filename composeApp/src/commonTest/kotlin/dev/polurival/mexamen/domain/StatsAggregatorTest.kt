package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats
import kotlin.test.Test
import kotlin.test.assertEquals

class StatsAggregatorTest {

    @Test
    fun freshInstallHasZeroedSummary() {
        val summary = StatsAggregator.summarize(datasetSize = 770, stats = emptyMap())
        assertEquals(770, summary.totalQuestions)
        assertEquals(0, summary.attempted)
        assertEquals(0, summary.totalCorrect)
        assertEquals(0, summary.totalWrong)
        assertEquals(770, summary.neverAttempted)
    }

    @Test
    fun aggregatesAcrossAttemptedQuestions() {
        val stats = mapOf(
            1 to QuestionStats(timesShown = 3, timesCorrect = 2, timesWrong = 1),
            2 to QuestionStats(timesShown = 1, timesCorrect = 0, timesWrong = 1),
            3 to QuestionStats(timesShown = 0), // never actually answered, just present in map
        )
        val summary = StatsAggregator.summarize(datasetSize = 10, stats = stats)
        assertEquals(10, summary.totalQuestions)
        assertEquals(2, summary.attempted)
        assertEquals(2, summary.totalCorrect)
        assertEquals(2, summary.totalWrong)
        assertEquals(8, summary.neverAttempted)
    }
}

class StatsAggregatorTopicTest {
    private fun e(id: Int, s: String) = dev.polurival.mexamen.data.QuestionEntry(id, s, "q$id", "a$id")

    @Test
    fun groupsByTopicAndAttempts() {
        val entries = listOf(e(1, "A"), e(2, "A"), e(3, "B"))
        val stats = mapOf(
            1 to QuestionStats(timesShown = 3, timesCorrect = 2, timesWrong = 1),
            2 to QuestionStats(timesShown = 1, timesCorrect = 1),
        )
        val topics = StatsAggregator.byTopic(entries, stats)
        assertEquals(listOf(TopicStats("A", 2, 4, 3), TopicStats("B", 1, 0, 0)), topics)
        val c = StatsAggregator.attemptCounts(listOf(1, 3), stats)
        assertEquals(AttemptCounts(3, 2), c[1])
        assertEquals(AttemptCounts.ZERO, c[3])
    }
}
