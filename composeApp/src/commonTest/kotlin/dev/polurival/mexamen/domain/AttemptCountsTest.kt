package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats
import kotlin.test.Test
import kotlin.test.assertEquals

class AttemptCountsTest {

    @Test
    fun formatUsesSingularVezForOneAttempt() {
        assertEquals("Pasada 1 vez · Correctas: 1", AttemptCountsFormatter.format(AttemptCounts(1, 1)))
    }

    @Test
    fun formatUsesPluralVecesForZeroAttempts() {
        assertEquals("Pasada 0 veces · Correctas: 0", AttemptCountsFormatter.format(AttemptCounts()))
    }

    @Test
    fun formatUsesPluralVecesForSeveralAttempts() {
        assertEquals("Pasada 5 veces · Correctas: 3", AttemptCountsFormatter.format(AttemptCounts(5, 3)))
    }

    @Test
    fun fromCopiesShownAndCorrectCounters() {
        val stats = QuestionStats(timesShown = 4, timesCorrect = 2, timesWrong = 2)
        assertEquals(AttemptCounts(timesShown = 4, timesCorrect = 2), AttemptCounts.from(stats))
    }

    @Test
    fun fromNullStatsGivesZeroCounts() {
        assertEquals(AttemptCounts(0, 0), AttemptCounts.from(null))
    }
}
