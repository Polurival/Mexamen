package dev.polurival.mexamen.data

import dev.polurival.mexamen.domain.QuestionWeighting
import kotlinx.serialization.Serializable

/**
 * Персистентная per-question статистика: вес для выборки + счётчики для экрана статистики.
 * `weight` растёт при неверном ответе/грейде и падает при верном (см. [QuestionWeighting]).
 */
@Serializable
data class QuestionStats(
    val weight: Float = QuestionWeighting.NEUTRAL_WEIGHT,
    val timesShown: Int = 0,
    val timesCorrect: Int = 0,
    val timesWrong: Int = 0,
    val lastSeenEpochMillis: Long? = null,
)
