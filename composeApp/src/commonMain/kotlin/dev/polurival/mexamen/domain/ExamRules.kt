package dev.polurival.mexamen.domain

/**
 * Правила экзамена в одном месте: длительность таймеров, порог предупреждения о времени
 * и минимальный балл для «сдал/не сдал». UI получает готовое решение, а не считает его сам.
 */
object ExamRules {
    /** Время на квиз, секунды (10 минут). */
    const val QUIZ_DURATION_SECONDS = 600

    /** Время на тест по легенде, секунды (10 минут). */
    const val LEGEND_DURATION_SECONDS = 600

    /** Меньше стольких секунд — таймер подсвечивается как истекающий. */
    const val TIME_WARNING_SECONDS = 60

    /** Минимум верных ответов, чтобы квиз считался сданным. */
    const val QUIZ_PASS_SCORE = 8

    /** Минимум верных ответов, чтобы тест по легенде считался сданным. */
    const val LEGEND_PASS_SCORE = 5

    fun isQuizPassed(result: QuizResult): Boolean = result.score >= QUIZ_PASS_SCORE

    fun isLegendPassed(result: QuizResult): Boolean = result.score >= LEGEND_PASS_SCORE

    fun isTimeRunningOut(timeLeftSeconds: Int): Boolean = timeLeftSeconds < TIME_WARNING_SECONDS
}
