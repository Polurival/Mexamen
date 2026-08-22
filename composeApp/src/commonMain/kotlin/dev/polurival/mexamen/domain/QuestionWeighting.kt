package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats
import kotlin.math.min

/** Правильно/неправильно в quiz (flashcard использует более гранулярный [FlashcardGrade]). */
enum class AnswerOutcome { CORRECT, WRONG }

/**
 * Чистая математика веса вопроса. Quiz и flashcards шарят один [QuestionStats] per id,
 * поэтому 4-уровневая оценка flashcard ложится на ту же шкалу веса и те же
 * correct/wrong счётчики, что бинарный quiz-ответ. Hard считается как wrong
 * (неидеальное вспоминание — не "знаю").
 */
object QuestionWeighting {
    const val NEUTRAL_WEIGHT = 1.0f
    const val MIN_WEIGHT = 1.0f
    const val MAX_WEIGHT = 8.0f

    const val CORRECT_MULTIPLIER = 0.5f
    const val WRONG_MULTIPLIER = 1.5f
    const val HARD_MULTIPLIER = 1.15f
    const val EASY_MULTIPLIER = 0.3f

    const val COOLDOWN_MILLIS = 14400000L // 4 * 60 * 60 * 1000L
    const val DECAY_HALF_LIFE_DAYS = 14.0
    const val MAX_DECAY_BONUS = 2.0
    private const val MILLIS_PER_DAY = 86400000L // 24 * 60 * 60 * 1000L

    fun applyOutcome(stats: QuestionStats, outcome: AnswerOutcome, nowMillis: Long): QuestionStats {
        val multiplier = when (outcome) {
            AnswerOutcome.CORRECT -> CORRECT_MULTIPLIER
            AnswerOutcome.WRONG -> WRONG_MULTIPLIER
        }
        val stamped = stamp(stats, nowMillis, multiplier)
        return when (outcome) {
            AnswerOutcome.CORRECT -> stamped.copy(timesCorrect = stamped.timesCorrect + 1)
            AnswerOutcome.WRONG -> stamped.copy(timesWrong = stamped.timesWrong + 1)
        }
    }

    fun applyGrade(stats: QuestionStats, grade: FlashcardGrade, nowMillis: Long): QuestionStats {
        val multiplier = when (grade) {
            FlashcardGrade.AGAIN -> WRONG_MULTIPLIER
            FlashcardGrade.HARD -> HARD_MULTIPLIER
            FlashcardGrade.GOOD -> CORRECT_MULTIPLIER
            FlashcardGrade.EASY -> EASY_MULTIPLIER
        }
        val stamped = stamp(stats, nowMillis, multiplier)
        return when (grade) {
            FlashcardGrade.AGAIN, FlashcardGrade.HARD -> stamped.copy(timesWrong = stamped.timesWrong + 1)
            FlashcardGrade.GOOD, FlashcardGrade.EASY -> stamped.copy(timesCorrect = stamped.timesCorrect + 1)
        }
    }

    fun isInCooldown(stats: QuestionStats, nowMillis: Long): Boolean {
        val lastSeen = stats.lastSeenEpochMillis ?: return false
        return nowMillis - lastSeen < COOLDOWN_MILLIS
    }

    /** Вес для выбора в сессию: чем дольше вопрос не показывался, тем выше приоритет (forgetting curve). */
    fun effectiveWeight(stats: QuestionStats, nowMillis: Long): Double {
        val lastSeen = stats.lastSeenEpochMillis ?: return stats.weight.toDouble()
        val daysSince = (nowMillis - lastSeen).coerceAtLeast(0L) / MILLIS_PER_DAY.toDouble()
        val decayBonus = min(daysSince / DECAY_HALF_LIFE_DAYS, MAX_DECAY_BONUS)
        return min(stats.weight * (1.0 + decayBonus), MAX_WEIGHT.toDouble())
    }

    private fun stamp(stats: QuestionStats, nowMillis: Long, multiplier: Float): QuestionStats =
        stats.copy(
            weight = (stats.weight * multiplier).coerceIn(MIN_WEIGHT, MAX_WEIGHT),
            timesShown = stats.timesShown + 1,
            lastSeenEpochMillis = nowMillis,
        )
}
