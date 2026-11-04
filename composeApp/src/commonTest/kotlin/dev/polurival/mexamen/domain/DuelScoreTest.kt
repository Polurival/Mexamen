package dev.polurival.mexamen.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class DuelScoreTest {
    @Test
    fun plusCountsRoundAndEachSideSeparately() {
        val score = DuelScore().plus(userCorrect = true, modelCorrect = false).plus(userCorrect = true, modelCorrect = true)
        assertEquals(DuelScore(user = 2, model = 1, rounds = 2, modelAnswered = 2), score)
    }

    @Test
    fun winnerFollowsScore() {
        assertEquals(DuelWinner.USER, DuelScore(user = 3, model = 2, rounds = 5, modelAnswered = 5).winner)
        assertEquals(DuelWinner.MODEL, DuelScore(user = 1, model = 2, rounds = 5, modelAnswered = 5).winner)
        assertEquals(DuelWinner.DRAW, DuelScore(user = 2, model = 2, rounds = 5, modelAnswered = 5).winner)
    }

    @Test
    fun roundWithoutModelAnswerIsNotCountedAgainstModelAndHasNoWinner() {
        val score = DuelScore().plus(userCorrect = true, modelCorrect = null)
        assertEquals(DuelScore(user = 1, model = 0, rounds = 1, modelAnswered = 0), score)
        assertEquals(DuelWinner.NONE, score.winner)
    }
}
