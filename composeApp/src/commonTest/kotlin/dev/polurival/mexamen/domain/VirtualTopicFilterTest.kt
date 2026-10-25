package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.TestDatasetBytes
import kotlinx.coroutines.test.runTest
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals

private class EmptyStatsStore : QuestionStatsStore {
    override suspend fun readAll(): Map<Int, QuestionStats> = emptyMap()
    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) = error("not used")
    override suspend fun clear() = error("not used")
}

/**
 * Виртуальные темы: названия показываются в UI как есть (только испанский) и служат ключом
 * фильтра в репозитории, квизе и карточках — все три места должны понимать одно и то же имя.
 */
class VirtualTopicFilterTest {

    private val entries = listOf(
        QuestionEntry(1, "Historia", "¿Cuándo se fundó Tenochtitlan?", "1325"),
        QuestionEntry(2, "Historia", "¿Quién fue Hidalgo?", "Un cura"),
        QuestionEntry(3, "Historia", "¿Qué fue la Batalla de Puebla?", "Una victoria"),
        QuestionEntry(4, "Geografía", "¿Cuál es la capital?", "Ciudad de México"),
        QuestionEntry(5, "Geografía", "¿Cuál es el río más largo?", "Río Bravo"),
    )
    private val vectors = entries.map { floatArrayOf(it.id.toFloat(), 1f) }

    private fun repository() = TestDatasetBytes.repository(entries, vectors)

    @Test
    fun virtualTopicNamesAreSpanishOnly() = runTest {
        val names = repository().getVirtualTopics().map { it.name }
        assertEquals(listOf("Fechas", "Personajes", "Eventos"), names)
    }

    @Test
    fun repositoryFiltersBySpanishVirtualTopicNames() = runTest {
        val repo = repository()
        assertEquals(listOf(1), repo.getQuestionsByTopic("Fechas").map { it.id })
        assertEquals(listOf(2), repo.getQuestionsByTopic("Personajes").map { it.id })
        assertEquals(listOf(3), repo.getQuestionsByTopic("Eventos").map { it.id })
    }

    @Test
    fun quizFiltersByEveryVirtualTopicFromRepository() = runTest {
        val repo = repository()
        val dataset = repo.dataset()
        repo.getVirtualTopics().forEach { topic ->
            val quiz = BuildQuizUseCase(dataset, Random(0)).build(count = topic.questionCount, topic = topic.name)
            assertEquals(
                repo.getQuestionsByTopic(topic.name).map { it.id }.toSet(),
                quiz.map { it.entry.id }.toSet(),
                "topic=${topic.name}",
            )
        }
    }

    @Test
    fun flashcardsFilterByEveryVirtualTopicFromRepository() = runTest {
        val repo = repository()
        val dataset = repo.dataset()
        repo.getVirtualTopics().forEach { topic ->
            val cards = BuildFlashcardsUseCase(Random(0))
                .build(dataset, EmptyStatsStore(), count = topic.questionCount, topic = topic.name, nowMillis = 0L)
            assertEquals(
                repo.getQuestionsByTopic(topic.name).map { it.id }.toSet(),
                cards.map { it.id }.toSet(),
                "topic=${topic.name}",
            )
        }
    }
}
