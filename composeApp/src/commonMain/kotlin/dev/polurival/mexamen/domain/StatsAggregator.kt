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

/** Статистика по теме (`section`): вопросов в теме, из них intentadas, всего ответов и верных. */
data class TopicStatsSummary(
    val topic: String,
    val totalQuestions: Int,
    val attemptedQuestions: Int,
    val answersGiven: Int,
    val correctAnswers: Int,
) {
    /** Доля верных ответов по теме в диапазоне 0..1; `null`, если по теме ещё не было ответов. */
    val correctRatio: Double? =
        if (answersGiven == 0) null else correctAnswers.toDouble() / answersGiven

    /**
     * Подпись точности для карточки темы: «Aciertos: NN %» или «Sin respuestas».
     * Процент округляется вниз, чтобы не показывать 100 %, пока есть хоть одна ошибка.
     */
    fun accuracyLabel(): String =
        if (answersGiven == 0) NO_ANSWERS_LABEL else "Aciertos: ${correctAnswers * 100 / answersGiven} %"

    companion object {
        private const val NO_ANSWERS_LABEL = "Sin respuestas"
    }
}

/** Агрегация persisted per-question статистики для экрана статистики. Кумулятивно за всё время. */
object StatsAggregator {
    /**
     * Сводка по темам, от слабых к сильным: сначала темы с ответами по возрастанию доли верных
     * (при равенстве — по имени), затем темы без ответов по имени.
     */
    fun byTopic(entries: List<QuestionEntry>, stats: Map<Int, QuestionStats>): List<TopicStatsSummary> =
        entries.groupBy { it.section }.map { (topic, group) ->
            val groupStats = group.mapNotNull { stats[it.id] }
            TopicStatsSummary(
                topic = topic,
                totalQuestions = group.size,
                attemptedQuestions = groupStats.count { it.timesShown > 0 },
                answersGiven = groupStats.sumOf { it.timesShown },
                correctAnswers = groupStats.sumOf { it.timesCorrect },
            )
        }.sortedWith(WEAKEST_FIRST)

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

    private val WEAKEST_FIRST: Comparator<TopicStatsSummary> =
        compareBy<TopicStatsSummary> { it.correctRatio == null }
            .thenBy { it.correctRatio ?: 0.0 }
            .thenBy { it.topic }
}
