package dev.polurival.mexamen.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/** Сборка байтов questions.json / embeddings.bin для тестов репозитория без реальных ассетов. */
internal object TestDatasetBytes {
    fun questions(entries: List<QuestionEntry>): ByteArray =
        Json.encodeToString(entries).encodeToByteArray()

    /** Формат как в проде: int32 LE count, int32 LE dim, затем count×dim float32 LE. */
    fun embeddings(vectors: List<FloatArray>, dim: Int = vectors.firstOrNull()?.size ?: 0): ByteArray {
        val out = ArrayList<Byte>()
        fun putInt(v: Int) {
            for (shift in 0 until 32 step 8) out += ((v shr shift) and 0xFF).toByte()
        }
        putInt(vectors.size)
        putInt(dim)
        vectors.forEach { vec -> vec.forEach { putInt(it.toBits()) } }
        return out.toByteArray()
    }

    fun repository(entries: List<QuestionEntry>, vectors: List<FloatArray>): QuestionRepository =
        QuestionRepository(
            readQuestions = { questions(entries) },
            readEmbeddings = { embeddings(vectors) },
        )
}
