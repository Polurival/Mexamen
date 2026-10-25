package dev.polurival.mexamen.domain

import kotlinx.serialization.Serializable

@Serializable
data class AnsweredQuestion(
    val question: QuizQuestion,
    val chosenIndex: Int,
) {
    /** Пользователь выбрал существующий вариант (иначе — вопрос остался без ответа, напр. в тесте по легенде). */
    val isAnswered: Boolean get() = chosenIndex in question.options.indices

    val isCorrect: Boolean get() = isAnswered && chosenIndex == question.correctIndex

    companion object {
        /** Значение [chosenIndex] для вопроса без ответа. */
        const val NOT_ANSWERED = -1
    }
}

@Serializable
data class QuizResult(val answers: List<AnsweredQuestion>) {
    val score: Int = answers.count { it.isCorrect }
    val total: Int = answers.size

    companion object {
        /**
         * Итог сессии квиза: [answers] идут по порядку [questions], оставшиеся вопросы (таймаут)
         * добавляются как [AnsweredQuestion.NOT_ANSWERED] — total = размер квиза, они считаются неверными.
         */
        fun of(questions: List<QuizQuestion>, answers: List<AnsweredQuestion>): QuizResult =
            QuizResult(answers + questions.drop(answers.size).map { AnsweredQuestion(it, AnsweredQuestion.NOT_ANSWERED) })
    }
}
