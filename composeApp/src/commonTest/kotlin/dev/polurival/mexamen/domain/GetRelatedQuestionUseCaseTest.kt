package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.TestDatasetBytes
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class GetRelatedQuestionUseCaseTest {

    private val entries = listOf(
        QuestionEntry(1, "s", "q1", "a1"),
        QuestionEntry(2, "s", "q2", "a2"),
        QuestionEntry(3, "s", "q3", "a3"),
    )

    @Test
    fun returnsNearestNeighborByCosine() = runTest {
        val repo = TestDatasetBytes.repository(
            entries,
            listOf(floatArrayOf(1f, 0f), floatArrayOf(0f, 1f), floatArrayOf(10f, 1f)),
        )
        assertEquals(3, GetRelatedQuestionUseCase(repo)(entries[0])?.id)
        assertEquals(entries[2], GetRelatedQuestionUseCase(repo)(entries[0]))
    }

    @Test
    fun neverReturnsTheQuestionItself() = runTest {
        val repo = TestDatasetBytes.repository(
            entries,
            listOf(floatArrayOf(1f, 0f), floatArrayOf(1f, 0f), floatArrayOf(0f, 1f)),
        )
        assertEquals(2, GetRelatedQuestionUseCase(repo)(entries[0])?.id)
    }

    @Test
    fun returnsNullWhenDatasetHasSingleQuestion() = runTest {
        val repo = TestDatasetBytes.repository(entries.take(1), listOf(floatArrayOf(1f)))
        assertNull(GetRelatedQuestionUseCase(repo)(entries[0]))
    }
}
