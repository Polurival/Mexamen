package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.TestDatasetBytes
import dev.polurival.mexamen.testing.FakeQuestionStatsStore
import kotlinx.coroutines.test.runTest
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Регрессия #10: виртуальные темы показываются по-испански, и имя из списка тем
 * фильтрует вопросы одинаково в репозитории, квизе и карточках.
 */
class VirtualTopicsFilteringTest {

    private val entries = listOf(
        QuestionEntry(1, "Historia", "¿Cuándo fue la Independencia?", "1810"),
        QuestionEntry(2, "Historia", "¿Quién fue Hidalgo?", "Un cura"),
        QuestionEntry(3, "Historia", "La Batalla de Puebla", "Ganó México"),
    ) + (4..9).map { QuestionEntry(it, "Geografía", "q$it", "Answer $it") }

    private val vectors = entries.map { e ->
        val angle = e.id * 0.15f
        floatArrayOf(cos(angle), sin(angle))
    }

    private val repository = TestDatasetBytes.repository(entries, vectors)

    @Test
    fun virtualTopicNamesAreSpanish() = runTest {
        val names = repository.getVirtualTopics().map { it.name }
        assertEquals(listOf("Fechas", "Personajes", "Eventos"), names)
    }

    @Test
    fun quizFiltersByEveryVirtualTopicName() = runTest {
        val dataset = repository.dataset()
        repository.getVirtualTopics().forEach { topic ->
            val expected = repository.getQuestionsByTopic(topic.name).map { it.id }.toSet()
            assertTrue(expected.isNotEmpty(), "Empty topic ${topic.name}")
            val quiz = BuildQuizUseCase(dataset, Random(0), nowMillis = 0L)
                .build(count = topic.questionCount, topic = topic.name)
            assertEquals(expected, quiz.map { it.entry.id }.toSet(), "Topic ${topic.name}")
        }
    }

    @Test
    fun flashcardsFilterByEveryVirtualTopicName() = runTest {
        val dataset = repository.dataset()
        repository.getVirtualTopics().forEach { topic ->
            val expected = repository.getQuestionsByTopic(topic.name).map { it.id }.toSet()
            assertTrue(expected.isNotEmpty(), "Empty topic ${topic.name}")
            val cards = BuildFlashcardsUseCase(Random(0))
                .build(dataset, FakeQuestionStatsStore(), count = topic.questionCount, topic = topic.name, nowMillis = 0L)
            assertEquals(expected, cards.map { it.id }.toSet(), "Topic ${topic.name}")
        }
    }
}
