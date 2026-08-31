package dev.polurival.mexamen.data

import kotlinx.serialization.Serializable

@Serializable
data class QuestionEntry(
    val id: Int,
    val section: String,
    val question: String,
    val answer: String,
    val answerVariants: List<String> = listOf(answer),
    val realDistractors: List<String> = emptyList(),
    val syntheticDistractors: List<String> = emptyList(),
    val explanation: String? = null,
) {
    companion object {
        fun isDate(entry: QuestionEntry): Boolean {
            val dateRegex = Regex("\\d{4}")
            return entry.question.contains("¿Cuándo", ignoreCase = true) ||
                entry.answer.contains(dateRegex)
        }

        fun isPeople(entry: QuestionEntry): Boolean {
            val keywords = listOf("Quién", "Presidente", "Hidalgo", "Morelos", "Juárez", "Porfirio", "Madero")
            return keywords.any { entry.question.contains(it, ignoreCase = true) }
        }

        fun isEvent(entry: QuestionEntry): Boolean {
            val keywords = listOf("Batalla", "Tratado", "Independencia", "Revolución", "Constitución", "Plan de")
            return keywords.any { entry.question.contains(it, ignoreCase = true) }
        }
    }
}
