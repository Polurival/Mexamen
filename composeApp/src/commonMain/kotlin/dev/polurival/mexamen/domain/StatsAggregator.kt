package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats

data class StatsSummary(
    val totalQuestions: Int,
    val attempted: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val neverAttempted: Int,
)

/** Агрегация persisted per-question статистики для экрана статистики. Кумулятивно за всё время. */
object StatsAggregator {
    fun summarize(datasetSize: Int, stats: Map<Int, QuestionStats>): StatsSummary {
        val attempted = stats.count { (_, s) -> s.timesShown > 0 }
        return StatsSummary(
            totalQuestions = datasetSize,
            attempted = attempted,
            totalCorrect = stats.values.sumOf { it.timesCorrect },
            totalWrong = stats.values.sumOf { it.timesWrong },
            neverAttempted = (datasetSize - attempted).coerceAtLeast(0),
        )
    }
}
