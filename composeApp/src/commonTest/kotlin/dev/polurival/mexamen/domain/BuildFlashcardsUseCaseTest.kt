package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FlashcardsReadOnlyStore(private val stats: Map<Int, QuestionStats> = emptyMap()) : QuestionStatsStore {
    override suspend fun readAll(): Map<Int, QuestionStats> = stats
    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) = error("not used")
    override suspend fun clear() = error("not used")
}

class BuildFlashcardsUseCaseTest {

    private val entries: List<QuestionEntry> =
        (1..6).map { QuestionEntry(it, "Historia", "Pregunta $it", "Respuesta $it") } +
            (7..9).map { QuestionEntry(it, "Geografía", "Pregunta $it", "Respuesta $it") } +
            listOf(
                QuestionEntry(10, "Historia", "¿Cuándo fue?", "1810"),
                QuestionEntry(11, "Historia", "¿Quién fue?", "Alguien"),
                QuestionEntry(12, "Historia", "La Batalla de Puebla", "1862"),
            )

    private val dataset = QuestionDataset(
        entries = entries,
        dim = 1,
        vectorsById = entries.associate { it.id to floatArrayOf(1f) },
    )

    private fun useCase() = BuildFlashcardsUseCase(Random(0))

    @Test
    fun buildReturnsRequestedCountOfDistinctQuestions() = runTest {
        val cards = useCase().build(dataset, FlashcardsReadOnlyStore(), count = 8, nowMillis = 0L)
        assertEquals(8, cards.size)
        assertEquals(8, cards.map { it.id }.toSet().size)
    }

    @Test
    fun buildDefaultsToFlashcardSize() = runTest {
        val cards = useCase().build(dataset, FlashcardsReadOnlyStore(), nowMillis = 0L)
        assertEquals(BuildFlashcardsUseCase.FLASHCARD_SIZE, cards.size)
    }

    @Test
    fun buildFiltersBySectionTopic() = runTest {
        val cards = useCase().build(dataset, FlashcardsReadOnlyStore(), count = 3, topic = "Geografía", nowMillis = 0L)
        assertEquals(setOf(7, 8, 9), cards.map { it.id }.toSet())
    }

    @Test
    fun buildFiltersByVirtualDateTopic() = runTest {
        val cards = useCase().build(dataset, FlashcardsReadOnlyStore(), count = 2, topic = "Fechas (Даты)", nowMillis = 0L)
        assertEquals(setOf(10, 12), cards.map { it.id }.toSet())
    }

    @Test
    fun buildFiltersByVirtualPeopleTopic() = runTest {
        val cards = useCase().build(dataset, FlashcardsReadOnlyStore(), count = 1, topic = "Personajes (Люди)", nowMillis = 0L)
        assertEquals(listOf(11), cards.map { it.id })
    }

    @Test
    fun buildFiltersByVirtualEventTopic() = runTest {
        val cards = useCase().build(dataset, FlashcardsReadOnlyStore(), count = 1, topic = "Eventos (События)", nowMillis = 0L)
        assertEquals(listOf(12), cards.map { it.id })
    }

    @Test
    fun buildFailsWhenTopicHasFewerQuestionsThanRequested() = runTest {
        assertFailsWith<IllegalArgumentException> {
            useCase().build(dataset, FlashcardsReadOnlyStore(), count = 5, topic = "Geografía", nowMillis = 0L)
        }
    }
}
