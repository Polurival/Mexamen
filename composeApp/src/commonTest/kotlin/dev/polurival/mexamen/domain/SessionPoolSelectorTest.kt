package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import kotlinx.coroutines.test.runTest
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private class MistakesStatsStore(private val stats: Map<Int, QuestionStats>) : QuestionStatsStore {
    override suspend fun readAll(): Map<Int, QuestionStats> = stats
    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) = error("not used")
    override suspend fun clear() = error("not used")
}

/** Режим «Repasar errores» (issue #21): отбор по `timesWrong > 0`, короткая/пустая сессия. */
class SessionPoolSelectorTest {

    private val entries: List<QuestionEntry> =
        (1..20).map { QuestionEntry(it, "section-${it % 4}", "q$it", "Answer $it") } +
            QuestionEntry(21, "Historia", "¿Cuándo fue?", "1810")

    private val dataset = QuestionDataset(
        entries = entries,
        dim = 2,
        vectorsById = entries.associate { it.id to floatArrayOf(cos(it.id * 0.15f), sin(it.id * 0.15f)) },
    )

    private fun wrong(vararg ids: Int): Map<Int, QuestionStats> =
        ids.associateWith { QuestionStats(timesShown = 1, timesWrong = 1) }

    @Test
    fun mistakesKeepOnlyQuestionsWithWrongAnswers() {
        val stats = wrong(3, 7) +
            mapOf(
                5 to QuestionStats(timesShown = 2, timesCorrect = 2),
                9 to QuestionStats(timesShown = 3, timesCorrect = 2, timesWrong = 1),
            )
        assertEquals(listOf(3, 7, 9), SessionPoolSelector.mistakes(entries, stats).map { it.id })
    }

    @Test
    fun mistakesTopicWithFewerThanRequestedGivesShorterSession() {
        val pool = SessionPoolSelector.select(dataset, SessionPoolSelector.TOPIC_MISTAKES, wrong(2, 4, 6), requested = 10)
        assertEquals(listOf(2, 4, 6), pool.entries.map { it.id })
        assertEquals(3, pool.sessionSize)
    }

    @Test
    fun mistakesTopicWithMoreThanRequestedKeepsRequestedSize() {
        val pool = SessionPoolSelector.select(dataset, SessionPoolSelector.TOPIC_MISTAKES, wrong(*(1..12).toList().toIntArray()), requested = 10)
        assertEquals(12, pool.entries.size)
        assertEquals(10, pool.sessionSize)
    }

    @Test
    fun mistakesTopicWithoutErrorsIsEmpty() {
        val stats = mapOf(1 to QuestionStats(timesShown = 1, timesCorrect = 1))
        val pool = SessionPoolSelector.select(dataset, SessionPoolSelector.TOPIC_MISTAKES, stats, requested = 10)
        assertTrue(pool.entries.isEmpty())
        assertEquals(0, pool.sessionSize)
    }

    @Test
    fun nullTopicIsWholeDatasetAndRegularTopicsDelegateToDataset() {
        val stats = wrong(1)
        assertEquals(entries, SessionPoolSelector.select(dataset, null, stats, 10).entries)
        listOf("section-1", QuestionEntry.TOPIC_DATES).forEach { topic ->
            val pool = SessionPoolSelector.select(dataset, topic, stats, 3)
            assertEquals(dataset.entriesByTopic(topic), pool.entries, "topic=$topic")
            assertEquals(3, pool.sessionSize)
        }
    }

    @Test
    fun quizInMistakesModeUsesOnlyErrorsAndIsShorter() = runTest {
        val store = MistakesStatsStore(wrong(2, 5, 8, 11))
        val quiz = BuildQuizUseCase(dataset, Random(0), statsStore = store, nowMillis = 0L)
            .build(topic = SessionPoolSelector.TOPIC_MISTAKES)
        assertEquals(setOf(2, 5, 8, 11), quiz.map { it.entry.id }.toSet())
    }

    @Test
    fun quizInMistakesModeWithoutErrorsIsEmpty() = runTest {
        val quiz = BuildQuizUseCase(dataset, Random(0), statsStore = MistakesStatsStore(emptyMap()), nowMillis = 0L)
            .build(topic = SessionPoolSelector.TOPIC_MISTAKES)
        assertTrue(quiz.isEmpty())
    }

    @Test
    fun flashcardsInMistakesModeUseOnlyErrorsAndAreShorter() = runTest {
        val cards = BuildFlashcardsUseCase(Random(0))
            .build(dataset, MistakesStatsStore(wrong(1, 21)), topic = SessionPoolSelector.TOPIC_MISTAKES, nowMillis = 0L)
        assertEquals(setOf(1, 21), cards.map { it.id }.toSet())
    }

    @Test
    fun flashcardsInMistakesModeWithoutErrorsAreEmpty() = runTest {
        val cards = BuildFlashcardsUseCase(Random(0))
            .build(dataset, MistakesStatsStore(emptyMap()), topic = SessionPoolSelector.TOPIC_MISTAKES, nowMillis = 0L)
        assertTrue(cards.isEmpty())
    }

    @Test
    fun mistakesModeIsWeightedTowardHigherWeight() = runTest {
        // 12 ошибок, сессия 10: вопросы с минимальным весом попадают в сессию реже, чем с максимальным
        val lowIds = setOf(1, 2)
        val stats = (1..12).associateWith { id ->
            val weight = if (id in lowIds) QuestionWeighting.MIN_WEIGHT else QuestionWeighting.MAX_WEIGHT
            QuestionStats(weight = weight, timesShown = 1, timesWrong = 1)
        }
        var lowPicked = 0
        var highPicked = 0
        repeat(RUNS) { seed ->
            val cards = BuildFlashcardsUseCase(Random(seed))
                .build(dataset, MistakesStatsStore(stats), topic = SessionPoolSelector.TOPIC_MISTAKES, nowMillis = 0L)
            assertEquals(10, cards.size)
            lowPicked += cards.count { it.id in lowIds }
            highPicked += cards.count { it.id !in lowIds }
        }
        val lowRate = lowPicked.toDouble() / (lowIds.size * RUNS)
        val highRate = highPicked.toDouble() / ((12 - lowIds.size) * RUNS)
        assertTrue(lowRate < highRate, "lowRate=$lowRate highRate=$highRate")
    }

    private companion object {
        const val RUNS = 50
    }
}
