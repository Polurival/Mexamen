package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats

data class StatsSummary(
    val totalQuestions: Int,
    val attempted: Int,
    val totalCorrect: Int,
    val totalWrong: Int,
    val neverAttempted: Int,
)

/** Сколько раз вопрос проходили и сколько раз ответили верно. */
data class AttemptCounts(val shown: Int, val correct: Int) {
    companion object { val ZERO = AttemptCounts(0, 0) }
}

/** Статистика по теме ([section]): всего вопросов, пройденных раз, верных ответов. */
data class TopicStats(
    val section: String,
    val totalQuestions: Int,
    val shown: Int,
    val correct: Int,
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

    /** Пройдено раз / верных ответов по каждой теме (section), в порядке первого появления темы. */
    fun byTopic(entries: List<QuestionEntry>, stats: Map<Int, QuestionStats>): List<TopicStats> =
        entries.groupBy { it.section }.map { (section, list) ->
            val own = list.mapNotNull { stats[it.id] }
            TopicStats(
                section = section,
                totalQuestions = list.size,
                shown = own.sumOf { it.timesShown },
                correct = own.sumOf { it.timesCorrect },
            )
        }

    /** Счётчики конкретных вопросов по id. */
    fun attemptCounts(ids: Collection<Int>, stats: Map<Int, QuestionStats>): Map<Int, AttemptCounts> =
        ids.associateWith { id -> stats[id]?.let { AttemptCounts(it.timesShown, it.timesCorrect) } ?: AttemptCounts.ZERO }
}
