package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

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

    @Test
    fun byTopicGroupsBySection() {
        fun e(id: Int, section: String) = QuestionEntry(id = id, section = section, question = "q$id", answer = "a$id")
        val entries = listOf(e(1, "A"), e(2, "A"), e(3, "B"))
        val stats = mapOf(
            1 to QuestionStats(timesShown = 3, timesCorrect = 2, timesWrong = 1),
            3 to QuestionStats(timesShown = 1, timesCorrect = 1),
        )
        val topics = StatsAggregator.byTopic(entries, stats).associateBy { it.topic }
        assertEquals(TopicStatsSummary("A", 2, 1, 3, 2), topics["A"])
        assertEquals(TopicStatsSummary("B", 1, 1, 1, 1), topics["B"])
    }

    @Test
    fun correctRatioIsShareOfCorrectAnswers() {
        assertEquals(0.75, TopicStatsSummary("A", 4, 2, answersGiven = 4, correctAnswers = 3).correctRatio)
        assertEquals(0.0, TopicStatsSummary("A", 4, 1, answersGiven = 2, correctAnswers = 0).correctRatio)
    }

    @Test
    fun correctRatioIsNullWhenTopicHasNoAnswers() {
        assertNull(TopicStatsSummary("A", 4, 0, answersGiven = 0, correctAnswers = 0).correctRatio)
    }

    @Test
    fun byTopicSortsWeakestFirstThenUnansweredByName() {
        fun e(id: Int, section: String) = QuestionEntry(id = id, section = section, question = "q$id", answer = "a$id")
        val entries = listOf(
            e(1, "Fuerte"), e(2, "Débil"), e(3, "Zeta sin"), e(4, "Alfa sin"), e(5, "Media"), e(6, "Empate"),
        )
        val stats = mapOf(
            1 to QuestionStats(timesShown = 4, timesCorrect = 4),
            2 to QuestionStats(timesShown = 4, timesCorrect = 1, timesWrong = 3),
            5 to QuestionStats(timesShown = 2, timesCorrect = 1, timesWrong = 1),
            6 to QuestionStats(timesShown = 4, timesCorrect = 2, timesWrong = 2),
            4 to QuestionStats(timesShown = 0), // запись есть, но ответов не было
        )
        val order = StatsAggregator.byTopic(entries, stats).map { it.topic }
        assertEquals(listOf("Débil", "Empate", "Media", "Fuerte", "Alfa sin", "Zeta sin"), order)
    }
}
