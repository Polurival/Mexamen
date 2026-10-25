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

    companion object {
        /**
         * Итог по всему [quiz]: вопросы без ответа (напр. истёк таймер) добавляются после [answers]
         * как «Sin respuesta» (chosenIndex = -1) и считаются неверными, поэтому total = размер квиза.
         */
        fun forQuiz(quiz: List<QuizQuestion>, answers: List<AnsweredQuestion>): QuizResult {
            val answeredIds = answers.map { it.question.entry.id }.toSet()
            val unanswered = quiz.filterNot { it.entry.id in answeredIds }
                .map { AnsweredQuestion(it, chosenIndex = -1) }
            return QuizResult(answers + unanswered)
        }
    }
}
