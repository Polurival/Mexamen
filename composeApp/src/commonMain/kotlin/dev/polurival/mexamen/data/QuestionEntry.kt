package dev.polurival.mexamen.data

import kotlinx.serialization.Serializable

@Serializable
data class QuestionEntry(
    val id: Int,
    val section: String,
    val question: String,
    val answer: String,
)
