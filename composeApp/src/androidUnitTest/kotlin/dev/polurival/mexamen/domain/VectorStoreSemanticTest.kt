package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionRepository
import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Смоук на реальном датасете: соседи должны быть семантически осмысленными.
 * Печатает top-5 для трёх вопросов (ручная проверка из plan.md C1).
 */
class VectorStoreSemanticTest {

    private val resourcesDir = File("src/commonMain/composeResources/files")

    private fun repository() = QuestionRepository(
        readQuestions = { resourcesDir.resolve("questions.json").readBytes() },
        readEmbeddings = { resourcesDir.resolve("embeddings.bin").readBytes() },
    )

    @Test
    fun neighborsAreSemanticallyMeaningful() = runBlocking {
        val dataset = repository().dataset()
        val store = VectorStore(dataset)
        val byId = dataset.entries.associateBy { it.id }

        val samples = listOf(
            dataset.entries.first { it.question.contains("Jalisco") },
            dataset.entries.first { it.section != dataset.entries.first().section },
            dataset.entries.last(),
        )

        samples.forEach { entry ->
            val neighbors = store.topK(entry.id, k = 5)
            println("[${entry.section}] ${entry.question} -> ${entry.answer}")
            neighbors.forEach { n ->
                val e = byId.getValue(n.id)
                println("    %.3f [%s] %s -> %s".format(n.score, e.section, e.question, e.answer))
            }
            assertTrue(
                neighbors.all { it.score > 0.5f },
                "Top-5 neighbors of '${entry.question}' suspiciously distant: $neighbors",
            )
        }

        // Узкий раздел столиц: соседи вопроса о столице — другие столицы
        val jalisco = samples.first()
        val jaliscoNeighbors = store.topK(jalisco.id, k = 5)
        val sameSection = jaliscoNeighbors.count { byId.getValue(it.id).section == jalisco.section }
        assertTrue(
            sameSection >= 3,
            "Expected >= 3 of top-5 capital neighbors from '${jalisco.section}', got $sameSection",
        )
    }

    @Test
    fun capitalQuestionsCloserToEachOtherThanToUnrelated() = runBlocking {
        val dataset = repository().dataset()
        val jalisco = dataset.entries.first { it.question.contains("Jalisco") }
        val nuevoLeon = dataset.entries.first { it.question.contains("Nuevo León") }
        val unrelated = dataset.entries.first { it.section != jalisco.section }

        val related = VectorStore.cosine(dataset.embedding(jalisco.id), dataset.embedding(nuevoLeon.id))
        val distant = VectorStore.cosine(dataset.embedding(jalisco.id), dataset.embedding(unrelated.id))
        println("cosine(Jalisco, Nuevo León) = $related, cosine(Jalisco, '${unrelated.question}') = $distant")
        assertTrue(
            related > distant + 0.1f,
            "Capital questions should be notably closer: related=$related, distant=$distant",
        )
    }
}
