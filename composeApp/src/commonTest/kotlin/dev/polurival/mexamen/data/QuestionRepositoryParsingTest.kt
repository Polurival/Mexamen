package dev.polurival.mexamen.data

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class QuestionRepositoryParsingTest {

    private val entries = listOf(
        QuestionEntry(1, "Historia", "¿Cuándo fue la Independencia?", "1810"),
        QuestionEntry(2, "Historia", "¿Quién fue Hidalgo?", "Un cura"),
        QuestionEntry(3, "Geografía", "¿Cuál es la capital?", "Ciudad de México"),
    )
    private val vectors = listOf(
        floatArrayOf(1f, 0f),
        floatArrayOf(0.5f, -2.25f),
        floatArrayOf(0f, 1f),
    )

    @Test
    fun parseEmbeddingsRoundTripsHeaderAndFloats() {
        val (dim, parsed) = QuestionRepository.parseEmbeddings(TestDatasetBytes.embeddings(vectors))
        assertEquals(2, dim)
        assertEquals(3, parsed.size)
        vectors.zip(parsed).forEach { (expected, actual) -> assertContentEquals(expected, actual) }
    }

    @Test
    fun parseEmbeddingsRejectsTruncatedHeader() {
        assertFailsWith<IllegalStateException> {
            QuestionRepository.parseEmbeddings(ByteArray(4))
        }
    }

    @Test
    fun parseEmbeddingsRejectsSizeMismatch() {
        val bytes = TestDatasetBytes.embeddings(vectors)
        assertFailsWith<IllegalStateException> {
            QuestionRepository.parseEmbeddings(bytes.copyOf(bytes.size - 1))
        }
    }

    @Test
    fun parseQuestionsIgnoresUnknownFieldsAndAppliesDefaults() {
        val json = """[{"id":7,"section":"S","question":"Q","answer":"A","extra":"x"}]"""
        val parsed = QuestionRepository.parseQuestions(json.encodeToByteArray())
        assertEquals(1, parsed.size)
        assertEquals(listOf("A"), parsed.single().answerVariants)
        assertEquals(emptyList(), parsed.single().realDistractors)
    }

    @Test
    fun datasetAlignsEmbeddingsByQuestionId() = runTest {
        val dataset = TestDatasetBytes.repository(entries, vectors).dataset()
        assertContentEquals(vectors[1], dataset.embedding(2))
        assertEquals(2, dataset.dim)
    }

    @Test
    fun datasetFailsWhenCountsDiffer() = runTest {
        val repo = TestDatasetBytes.repository(entries, vectors.take(2))
        assertFailsWith<IllegalStateException> { repo.dataset() }
    }

    @Test
    fun datasetFailsOnDuplicateIds() = runTest {
        val dup = entries.map { it.copy(id = 1) }
        assertFailsWith<IllegalStateException> {
            TestDatasetBytes.repository(dup, vectors).dataset()
        }
    }

    @Test
    fun getTopicsGroupsBySectionSortedByName() = runTest {
        val topics = TestDatasetBytes.repository(entries, vectors).getTopics()
        assertEquals(listOf(Topic("Geografía", 1), Topic("Historia", 2)), topics)
    }

    @Test
    fun getQuestionsByTopicHandlesVirtualAndRealTopics() = runTest {
        val repo = TestDatasetBytes.repository(entries, vectors)
        assertEquals(listOf(1), repo.getQuestionsByTopic("Fechas").map { it.id })
        assertEquals(listOf(2), repo.getQuestionsByTopic("Personajes").map { it.id })
        assertEquals(listOf(3), repo.getQuestionsByTopic("Geografía").map { it.id })
        assertEquals(3, repo.getTotalQuestionsCount())
    }

    @Test
    fun getVirtualTopicsReportsCounts() = runTest {
        val topics = TestDatasetBytes.repository(entries, vectors).getVirtualTopics()
        assertEquals(listOf("Fechas", "Personajes", "Eventos"), topics.map { it.name })
        assertEquals(listOf(1, 1, 1), topics.map { it.questionCount })
    }
}
