package dev.polurival.mexamen.domain

import kotlinx.serialization.Serializable

@Serializable
data class AnsweredQuestion(
    val question: QuizQuestion,
    val chosenIndex: Int,
) {
    /** Ответ выбран: индекс указывает на существующий вариант (легенда передаёт -1 для пропущенного вопроса). */
    val isAnswered: Boolean get() = chosenIndex in question.options.indices
    val isCorrect: Boolean get() = isAnswered && chosenIndex == question.correctIndex
}

@Serializable
data class QuizResult(val answers: List<AnsweredQuestion>) {
    val score: Int = answers.count { it.isCorrect }
    val total: Int = answers.size
}
