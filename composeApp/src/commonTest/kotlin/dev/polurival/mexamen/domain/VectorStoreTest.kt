package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class VectorStoreTest {

    // 4 вектора в 2D: id=1 смотрит вдоль X, id=2 почти вдоль X, id=3 по диагонали, id=4 вдоль Y
    private fun dataset(): QuestionDataset {
        val entries = listOf(
            QuestionEntry(1, "s", "q1", "a1"),
            QuestionEntry(2, "s", "q2", "a2"),
            QuestionEntry(3, "s", "q3", "a3"),
            QuestionEntry(4, "s", "q4", "a4"),
        )
        val vectors = mapOf(
            1 to floatArrayOf(1f, 0f),
            2 to floatArrayOf(10f, 1f),
            3 to floatArrayOf(1f, 1f),
            4 to floatArrayOf(0f, 1f),
        )
        return QuestionDataset(entries, dim = 2, vectorsById = vectors)
    }

    @Test
    fun topKExcludesSelfAndSortsDescending() {
        val neighbors = VectorStore(dataset()).topK(id = 1, k = 3)
        assertEquals(3, neighbors.size)
        assertTrue(neighbors.none { it.id == 1 }, "Self must be excluded")
        assertEquals(listOf(2, 3, 4), neighbors.map { it.id })
        assertTrue(
            neighbors.zipWithNext().all { (a, b) -> a.score >= b.score },
            "Scores must be descending: $neighbors",
        )
    }

    @Test
    fun topKReturnsAtMostDatasetSizeMinusOne() {
        val neighbors = VectorStore(dataset()).topK(id = 1, k = 100)
        assertEquals(3, neighbors.size)
    }

    @Test
    fun topKRejectsNonPositiveK() {
        assertFailsWith<IllegalArgumentException> {
            VectorStore(dataset()).topK(id = 1, k = 0)
        }
    }

    @Test
    fun cosineKnownValues() {
        assertEquals(1f, VectorStore.cosine(floatArrayOf(1f, 0f), floatArrayOf(2f, 0f)), 1e-6f)
        assertEquals(0f, VectorStore.cosine(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)), 1e-6f)
        assertEquals(-1f, VectorStore.cosine(floatArrayOf(1f, 0f), floatArrayOf(-1f, 0f)), 1e-6f)
        assertEquals(0f, VectorStore.cosine(floatArrayOf(0f, 0f), floatArrayOf(1f, 1f)), 1e-6f)
    }
}
