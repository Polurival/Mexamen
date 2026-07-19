package dev.polurival.mexamen.domain

data class AnsweredQuestion(
    val question: QuizQuestion,
    val chosenIndex: Int,
) {
    val isCorrect: Boolean get() = chosenIndex == question.correctIndex
}

data class QuizResult(val answers: List<AnsweredQuestion>) {
    val score: Int = answers.count { it.isCorrect }
    val total: Int = answers.size
}
