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
        assertEquals(0.25, TopicStatsSummary("A", 5, 2, 4, 1).correctRatio)
        assertEquals(1.0, TopicStatsSummary("A", 5, 1, 2, 2).correctRatio)
    }

    @Test
    fun correctRatioIsNullWithoutAnswers() {
        assertNull(TopicStatsSummary("A", 5, 0, 0, 0).correctRatio)
    }

    @Test
    fun accuracyLabelShowsPercentRoundedDown() {
        assertEquals("Aciertos: 66 %", TopicStatsSummary("A", 3, 3, 3, 2).accuracyLabel())
        assertEquals("Aciertos: 99 %", TopicStatsSummary("A", 200, 200, 200, 199).accuracyLabel())
        assertEquals("Aciertos: 100 %", TopicStatsSummary("A", 1, 1, 4, 4).accuracyLabel())
        assertEquals("Aciertos: 0 %", TopicStatsSummary("A", 1, 1, 2, 0).accuracyLabel())
    }

    @Test
    fun accuracyLabelWithoutAnswersSaysSinRespuestas() {
        assertEquals("Sin respuestas", TopicStatsSummary("A", 4, 0, 0, 0).accuracyLabel())
    }

    @Test
    fun byTopicSortsWeakestFirstThenUnansweredByName() {
        fun e(id: Int, section: String) = QuestionEntry(id = id, section = section, question = "q$id", answer = "a$id")
        val entries = listOf(
            e(1, "Zeta sin respuestas"),
            e(2, "Fuerte"),
            e(3, "Debil"),
            e(4, "Alfa sin respuestas"),
            e(5, "Media B"),
            e(6, "Media A"),
        )
        val stats = mapOf(
            2 to QuestionStats(timesShown = 4, timesCorrect = 4),
            3 to QuestionStats(timesShown = 4, timesCorrect = 1, timesWrong = 3),
            5 to QuestionStats(timesShown = 2, timesCorrect = 1, timesWrong = 1),
            6 to QuestionStats(timesShown = 4, timesCorrect = 2, timesWrong = 2),
            4 to QuestionStats(timesShown = 0), // запись есть, но ответов нет
        )
        val order = StatsAggregator.byTopic(entries, stats).map { it.topic }
        assertEquals(
            listOf("Debil", "Media A", "Media B", "Fuerte", "Alfa sin respuestas", "Zeta sin respuestas"),
            order,
        )
    }
}
