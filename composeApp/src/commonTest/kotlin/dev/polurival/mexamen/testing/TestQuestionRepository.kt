package dev.polurival.mexamen.testing

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.TestDatasetBytes
import kotlin.math.cos
import kotlin.math.sin

/** Синтетический репозиторий: [size] вопросов с разными ответами, 2D-векторы веером по углу. */
internal fun testQuestionRepository(size: Int = 12): QuestionRepository {
    val entries = (1..size).map { QuestionEntry(it, "section-${it % 3}", "q$it", "Answer $it") }
    val vectors = entries.map { e ->
        val angle = e.id * 0.15f
        floatArrayOf(cos(angle), sin(angle))
    }
    return TestDatasetBytes.repository(entries, vectors)
}
