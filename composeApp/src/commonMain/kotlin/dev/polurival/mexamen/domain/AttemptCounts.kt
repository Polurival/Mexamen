package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats

/** Счётчики попыток по одному вопросу для UI (кумулятивно за всё время), без деталей хранения. */
data class AttemptCounts(
    val timesShown: Int = 0,
    val timesCorrect: Int = 0,
) {
    companion object {
        /** Маппинг persisted-статистики в доменную модель; нет статистики — нулевые счётчики. */
        fun from(stats: QuestionStats?): AttemptCounts =
            if (stats == null) AttemptCounts() else AttemptCounts(stats.timesShown, stats.timesCorrect)
    }
}
