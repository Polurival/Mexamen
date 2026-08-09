package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionWeightingTest {

    @Test
    fun correctOutcomeHalvesWeightAndIncrementsCorrect() {
        val stats = QuestionStats(weight = 4.0f)
        val updated = QuestionWeighting.applyOutcome(stats, AnswerOutcome.CORRECT, nowMillis = 100L)
        assertEquals(2.0f, updated.weight)
        assertEquals(1, updated.timesCorrect)
        assertEquals(0, updated.timesWrong)
        assertEquals(1, updated.timesShown)
        assertEquals(100L, updated.lastSeenEpochMillis)
    }

    @Test
    fun wrongOutcomeIncreasesWeightAndIncrementsWrong() {
        val stats = QuestionStats(weight = 2.0f)
        val updated = QuestionWeighting.applyOutcome(stats, AnswerOutcome.WRONG, nowMillis = 100L)
        assertEquals(3.0f, updated.weight)
        assertEquals(1, updated.timesWrong)
        assertEquals(0, updated.timesCorrect)
    }

    @Test
    fun weightClampsToMinAndMax() {
        val low = QuestionStats(weight = QuestionWeighting.MIN_WEIGHT)
        val afterCorrect = QuestionWeighting.applyOutcome(low, AnswerOutcome.CORRECT, nowMillis = 0L)
        assertEquals(QuestionWeighting.MIN_WEIGHT, afterCorrect.weight)

        val high = QuestionStats(weight = QuestionWeighting.MAX_WEIGHT)
        val afterWrong = QuestionWeighting.applyOutcome(high, AnswerOutcome.WRONG, nowMillis = 0L)
        assertEquals(QuestionWeighting.MAX_WEIGHT, afterWrong.weight)
    }

    @Test
    fun flashcardGradeMappingMatchesAgreedTable() {
        val base = QuestionStats(weight = 4.0f)
        val again = QuestionWeighting.applyGrade(base, FlashcardGrade.AGAIN, nowMillis = 0L)
        val hard = QuestionWeighting.applyGrade(base, FlashcardGrade.HARD, nowMillis = 0L)
        val good = QuestionWeighting.applyGrade(base, FlashcardGrade.GOOD, nowMillis = 0L)
        val easy = QuestionWeighting.applyGrade(base, FlashcardGrade.EASY, nowMillis = 0L)

        assertEquals(6.0f, again.weight)
        assertEquals(1, again.timesWrong)
        assertEquals(4.0f * 1.15f, hard.weight)
        assertEquals(1, hard.timesWrong)
        assertEquals(2.0f, good.weight)
        assertEquals(1, good.timesCorrect)
        assertEquals(4.0f * 0.3f, easy.weight)
        assertEquals(1, easy.timesCorrect)
    }

    @Test
    fun cooldownIsTrueImmediatelyAfterAnswerAndFalseLater() {
        val stats = QuestionStats(lastSeenEpochMillis = 1_000_000L)
        assertTrue(QuestionWeighting.isInCooldown(stats, nowMillis = 1_000_000L + 1))
        assertFalse(
            QuestionWeighting.isInCooldown(stats, nowMillis = 1_000_000L + QuestionWeighting.COOLDOWN_MILLIS + 1),
        )
    }

    @Test
    fun neverSeenQuestionHasNoCooldownAndNeutralEffectiveWeight() {
        val stats = QuestionStats()
        assertFalse(QuestionWeighting.isInCooldown(stats, nowMillis = 1_000L))
        assertEquals(stats.weight.toDouble(), QuestionWeighting.effectiveWeight(stats, nowMillis = 1_000L))
    }

    @Test
    fun effectiveWeightGrowsWithTimeSinceLastSeenAndCapsAtMax() {
        val stats = QuestionStats(weight = 4.0f, lastSeenEpochMillis = 0L)
        val soon = QuestionWeighting.effectiveWeight(stats, nowMillis = 1_000L)
        val afterHalfLife = QuestionWeighting.effectiveWeight(
            stats,
            nowMillis = (QuestionWeighting.DECAY_HALF_LIFE_DAYS * 24 * 60 * 60 * 1000L).toLong(),
        )
        val muchLater = QuestionWeighting.effectiveWeight(
            stats,
            nowMillis = (QuestionWeighting.DECAY_HALF_LIFE_DAYS * 24 * 60 * 60 * 1000L * 100).toLong(),
        )
        assertTrue(soon < afterHalfLife, "expected weight to grow over time: $soon vs $afterHalfLife")
        assertEquals(QuestionWeighting.MAX_WEIGHT.toDouble(), muchLater)
    }
}
