package dev.polurival.mexamen.data

import java.io.File
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith

class QuestionRepositoryTest {

    private val resourcesDir = File("src/commonMain/composeResources/files")

    private fun repository() = QuestionRepository(
        readQuestions = { resourcesDir.resolve("questions.json").readBytes() },
        readEmbeddings = { resourcesDir.resolve("embeddings.bin").readBytes() },
    )

    @Test
    fun `dataset loads at least 700 entries`() = runBlocking {
        val dataset = repository().dataset()
        assertTrue(dataset.entries.size >= 700, "Expected >= 700 entries, got ${dataset.entries.size}")
    }

    @Test
    fun `every entry has a 768-dim embedding`() = runBlocking {
        val dataset = repository().dataset()
        assertEquals(768, dataset.dim)
        dataset.entries.forEach { entry ->
            val vector = dataset.embedding(entry.id)
            assertEquals(768, vector.size, "Wrong embedding size for id=${entry.id}")
        }
    }

    @Test
    fun `ids are unique and fields non-blank`() = runBlocking {
        val dataset = repository().dataset()
        val ids = dataset.entries.map { it.id }
        assertEquals(ids.size, ids.toSet().size, "Duplicate ids in questions.json")
        dataset.entries.forEach { entry ->
            assertTrue(entry.section.isNotBlank(), "Blank section for id=${entry.id}")
            assertTrue(entry.question.isNotBlank(), "Blank question for id=${entry.id}")
            assertTrue(entry.answer.isNotBlank(), "Blank answer for id=${entry.id}")
        }
    }

    @Test
    fun `every entry has non-empty answerVariants`() = runBlocking {
        val dataset = repository().dataset()
        dataset.entries.forEach { entry ->
            assertTrue(entry.answerVariants.isNotEmpty(), "Empty answerVariants for id=${entry.id}")
        }
    }

    @Test
    fun `embeddings are not degenerate`() = runBlocking {
        val dataset = repository().dataset()
        val first = dataset.embedding(dataset.entries.first().id)
        assertTrue(first.any { it != 0f }, "First embedding is all zeros")
    }

    @Test
    fun `dataset is cached after first load`() = runBlocking {
        var reads = 0
        val bytesQuestions = resourcesDir.resolve("questions.json").readBytes()
        val bytesEmbeddings = resourcesDir.resolve("embeddings.bin").readBytes()
        val repo = QuestionRepository(
            readQuestions = { reads++; bytesQuestions },
            readEmbeddings = { bytesEmbeddings },
        )
        repo.dataset()
        repo.dataset()
        assertEquals(1, reads)
    }

    @Test
    fun `parseEmbeddings rejects size mismatch`() {
        // header says 2 vectors of dim 3, but payload holds only one
        val broken = byteArrayOf(
            2, 0, 0, 0, // count = 2, LE
            3, 0, 0, 0, // dim = 3, LE
            0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
        )
        assertFailsWith<IllegalStateException> {
            QuestionRepository.parseEmbeddings(broken)
        }
    }

    @Test
    fun `parseEmbeddings reads little-endian floats`() {
        val one = 1.0f.toRawBits()
        val half = 0.5f.toRawBits()
        val bytes = byteArrayOf(
            1, 0, 0, 0, // count = 1
            2, 0, 0, 0, // dim = 2
            (one and 0xFF).toByte(), ((one shr 8) and 0xFF).toByte(),
            ((one shr 16) and 0xFF).toByte(), ((one shr 24) and 0xFF).toByte(),
            (half and 0xFF).toByte(), ((half shr 8) and 0xFF).toByte(),
            ((half shr 16) and 0xFF).toByte(), ((half shr 24) and 0xFF).toByte(),
        )
        val (dim, vectors) = QuestionRepository.parseEmbeddings(bytes)
        assertEquals(2, dim)
        assertEquals(1, vectors.size)
        assertEquals(1.0f, vectors[0][0])
        assertEquals(0.5f, vectors[0][1])
    }
}
