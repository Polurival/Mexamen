package dev.polurival.mexamen.domain

import kotlinx.serialization.Serializable

@Serializable
data class AnsweredQuestion(
    val question: QuizQuestion,
    val chosenIndex: Int,
) {
    val isCorrect: Boolean get() = chosenIndex == question.correctIndex
}

@Serializable
data class QuizResult(val answers: List<AnsweredQuestion>) {
    val score: Int = answers.count { it.isCorrect }
    val total: Int = answers.size
}
