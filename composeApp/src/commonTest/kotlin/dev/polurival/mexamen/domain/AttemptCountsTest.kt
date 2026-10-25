package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats
import kotlin.test.Test
import kotlin.test.assertEquals

class AttemptCountsTest {

    @Test
    fun labelUsesSingularVezForOneAttempt() {
        assertEquals("Pasada 1 vez · Correctas: 1", AttemptCounts(timesShown = 1, timesCorrect = 1).label())
    }

    @Test
    fun labelUsesPluralVecesForZeroAttempts() {
        assertEquals("Pasada 0 veces · Correctas: 0", AttemptCounts().label())
    }

    @Test
    fun labelUsesPluralVecesForSeveralAttempts() {
        assertEquals("Pasada 3 veces · Correctas: 2", AttemptCounts(timesShown = 3, timesCorrect = 2).label())
    }

    @Test
    fun fromMissingStatsGivesZeroCounts() {
        assertEquals(AttemptCounts(0, 0), AttemptCounts.from(null))
    }

    @Test
    fun fromStatsTakesShownAndCorrectCounters() {
        val stats = QuestionStats(weight = 2.0f, timesShown = 5, timesCorrect = 3, timesWrong = 2, lastSeenEpochMillis = 42L)
        assertEquals(AttemptCounts(timesShown = 5, timesCorrect = 3), AttemptCounts.from(stats))
    }

    @Test
    fun fromAllMapsEveryQuestionId() {
        val stats = mapOf(
            1 to QuestionStats(timesShown = 1, timesCorrect = 0, timesWrong = 1),
            7 to QuestionStats(timesShown = 2, timesCorrect = 2),
        )
        assertEquals(
            mapOf(1 to AttemptCounts(1, 0), 7 to AttemptCounts(2, 2)),
            AttemptCounts.fromAll(stats),
        )
    }
}
