package dev.polurival.mexamen.domain

/** Пороги «сдал/не сдал»: решение принимается здесь, экраны получают готовый флаг. */
object PassCriteria {
    /** Минимум верных ответов в квизе натурализации. */
    const val QUIZ_PASS_SCORE = 8

    /** Минимум верных ответов в тесте по испанскому (легенды). */
    const val LEGEND_PASS_SCORE = 5

    fun isQuizPassed(result: QuizResult): Boolean = result.score >= QUIZ_PASS_SCORE

    fun isLegendPassed(result: QuizResult): Boolean = result.score >= LEGEND_PASS_SCORE
}
