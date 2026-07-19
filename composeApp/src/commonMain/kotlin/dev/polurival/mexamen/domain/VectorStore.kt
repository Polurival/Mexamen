package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import kotlin.math.sqrt

data class Neighbor(val id: Int, val score: Float)

/**
 * Косинусный top-K полным перебором по датасету (FR-3).
 * ~800 векторов × 768 — миллисекунды, индекс не нужен.
 */
class VectorStore(private val dataset: QuestionDataset) {

    /** K ближайших соседей вопроса [id], сам вопрос исключён, по убыванию близости. */
    fun topK(id: Int, k: Int): List<Neighbor> {
        require(k > 0) { "k must be positive, got $k" }
        val query = dataset.embedding(id)
        return dataset.entries.asSequence()
            .filter { it.id != id }
            .map { Neighbor(it.id, cosine(query, dataset.embedding(it.id))) }
            .sortedByDescending { it.score }
            .take(k)
            .toList()
    }

    companion object {
        fun cosine(a: FloatArray, b: FloatArray): Float {
            require(a.size == b.size) { "Vector size mismatch: ${a.size} vs ${b.size}" }
            var dot = 0f
            var normA = 0f
            var normB = 0f
            for (i in a.indices) {
                dot += a[i] * b[i]
                normA += a[i] * a[i]
                normB += b[i] * b[i]
            }
            val denominator = sqrt(normA) * sqrt(normB)
            return if (denominator == 0f) 0f else dot / denominator
        }
    }
}
