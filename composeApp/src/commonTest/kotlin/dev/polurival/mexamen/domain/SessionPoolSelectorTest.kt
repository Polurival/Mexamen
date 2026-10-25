package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.testing.FakeQuestionStatsStore
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** #21: «Repasar errores» — пул из вопросов с `timesWrong > 0`, сессия не длиннее числа ошибок. */
class SessionPoolSelectorTest {

    private val entries: List<QuestionEntry> =
        (1..12).map { QuestionEntry(it, if (it <= 8) "Historia" else "Geografía", "Pregunta $it", "Respuesta $it") } +
            QuestionEntry(13, "Historia", "¿Cuándo fue?", "1810")

    private fun wrong(vararg ids: Int): Map<Int, QuestionStats> =
        ids.associateWith { QuestionStats(timesShown = 1, timesWrong = 1) }

    @Test
    fun mistakesKeepOnlyQuestionsWithWrongAnswers() {
        val stats = wrong(2, 5) + mapOf(
            3 to QuestionStats(timesShown = 4, timesCorrect = 4),
            7 to QuestionStats(timesShown = 3, timesCorrect = 2, timesWrong = 1),
        )
        assertEquals(listOf(2, 5, 7), SessionPoolSelector.mistakes(entries, stats).map { it.id })
    }

    @Test
    fun selectMistakesShortensSessionWhenFewerThanRequested() {
        val pool = SessionPoolSelector.select(entries, wrong(1, 4, 9), SessionPoolSelector.TOPIC_MISTAKES, requested = 10)
        assertEquals(listOf(1, 4, 9), pool.entries.map { it.id })
        assertEquals(3, pool.count)
    }

    @Test
    fun selectMistakesWithNoErrorsGivesEmptySession() {
        val stats = mapOf(1 to QuestionStats(timesShown = 2, timesCorrect = 2))
        val pool = SessionPoolSelector.select(entries, stats, SessionPoolSelector.TOPIC_MISTAKES, requested = 10)
        assertTrue(pool.entries.isEmpty())
        assertEquals(0, pool.count)
    }

    @Test
    fun selectMistakesKeepsRequestedSizeWhenEnoughErrors() {
        val stats = wrong(*(1..12).toList().toIntArray())
        val pool = SessionPoolSelector.select(entries, stats, SessionPoolSelector.TOPIC_MISTAKES, requested = 10)
        assertEquals(12, pool.entries.size)
        assertEquals(10, pool.count)
    }

    @Test
    fun selectAllTopicsAndDataTopicsIgnoreStats() {
        val stats = wrong(1)
        assertEquals(SessionPool(entries, 10), SessionPoolSelector.select(entries, stats, topic = null, requested = 10))
        val geo = SessionPoolSelector.select(entries, stats, topic = "Geografía", requested = 10)
        assertEquals((9..12).toList(), geo.entries.map { it.id })
        assertEquals(10, geo.count)
        assertEquals(listOf(13), SessionPoolSelector.select(entries, stats, "Fechas", 1).entries.map { it.id })
    }

    @Test
    fun flashcardsForMistakesUseOnlyWrongQuestionsAndShorterSession() = runTest {
        val store = FakeQuestionStatsStore()
        store.update { wrong(3, 6, 11) }
        val dataset = QuestionDataset(entries, dim = 1, vectorsById = entries.associate { it.id to floatArrayOf(1f) })

        val cards = BuildFlashcardsUseCase(Random(0))
            .build(dataset, store, topic = SessionPoolSelector.TOPIC_MISTAKES, nowMillis = 0L)

        assertEquals(setOf(3, 6, 11), cards.map { it.id }.toSet())
        assertEquals(3, cards.size)
    }

    @Test
    fun flashcardsForMistakesWithoutErrorsAreEmpty() = runTest {
        val dataset = QuestionDataset(entries, dim = 1, vectorsById = entries.associate { it.id to floatArrayOf(1f) })

        val cards = BuildFlashcardsUseCase(Random(0))
            .build(dataset, FakeQuestionStatsStore(), topic = SessionPoolSelector.TOPIC_MISTAKES, nowMillis = 0L)

        assertTrue(cards.isEmpty())
    }
}
