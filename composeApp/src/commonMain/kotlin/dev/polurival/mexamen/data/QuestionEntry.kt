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
)
