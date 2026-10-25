package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats

/**
 * Кумулятивные счётчики попыток по одному вопросу для UI (результат квиза, flashcard):
 * сколько раз вопрос был пройден и сколько раз — верно. UI не видит сырой [QuestionStats].
 */
data class AttemptCounts(
    val timesShown: Int = 0,
    val timesCorrect: Int = 0,
) {
    /** Единая подпись счётчиков для всех экранов: «Pasada 1 vez · Correctas: 1», иначе «veces». */
    fun label(): String {
        val times = if (timesShown == 1) "vez" else "veces"
        return "Pasada $timesShown $times · Correctas: $timesCorrect"
    }

    companion object {
        /** Нет статистики (вопрос ещё не встречался) — нулевые счётчики. */
        fun from(stats: QuestionStats?): AttemptCounts =
            if (stats == null) AttemptCounts() else AttemptCounts(stats.timesShown, stats.timesCorrect)

        fun fromAll(stats: Map<Int, QuestionStats>): Map<Int, AttemptCounts> =
            stats.mapValues { (_, value) -> from(value) }
    }
}
