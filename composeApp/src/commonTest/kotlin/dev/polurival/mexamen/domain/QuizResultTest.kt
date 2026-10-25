package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuizResultTest {

    private fun question(id: Int, correctIndex: Int = 1) = QuizQuestion(
        entry = QuestionEntry(id, "s", "q$id", "a$id"),
        options = listOf("a", "b", "c", "d"),
        correctIndex = correctIndex,
        source = DistractorSource.FALLBACK,
    )

    @Test
    fun answeredQuestionIsCorrectOnlyWhenChosenMatchesCorrectIndex() {
        assertTrue(AnsweredQuestion(question(1, correctIndex = 2), chosenIndex = 2).isCorrect)
        assertFalse(AnsweredQuestion(question(1, correctIndex = 2), chosenIndex = 0).isCorrect)
    }

    @Test
    fun resultCountsScoreAndTotal() {
        val result = QuizResult(
            listOf(
                AnsweredQuestion(question(1), chosenIndex = 1),
                AnsweredQuestion(question(2), chosenIndex = 0),
                AnsweredQuestion(question(3), chosenIndex = 1),
            ),
        )
        assertEquals(2, result.score)
        assertEquals(3, result.total)
    }

    @Test
    fun answeredQuestionIsAnsweredOnlyForValidOptionIndex() {
        assertTrue(AnsweredQuestion(question(1), chosenIndex = 0).isAnswered)
        assertTrue(AnsweredQuestion(question(1), chosenIndex = 3).isAnswered)
        assertFalse(AnsweredQuestion(question(1), chosenIndex = AnsweredQuestion.NOT_ANSWERED).isAnswered)
        assertFalse(AnsweredQuestion(question(1), chosenIndex = 4).isAnswered)
    }

    @Test
    fun unansweredQuestionIsNeverCorrectEvenWithInvalidCorrectIndex() {
        val broken = question(1, correctIndex = AnsweredQuestion.NOT_ANSWERED)
        assertFalse(AnsweredQuestion(broken, chosenIndex = AnsweredQuestion.NOT_ANSWERED).isCorrect)
    }

    @Test
    fun resultCountsUnansweredInTotalButNotInScore() {
        val result = QuizResult(
            listOf(
                AnsweredQuestion(question(1), chosenIndex = 1),
                AnsweredQuestion(question(2), chosenIndex = AnsweredQuestion.NOT_ANSWERED),
            ),
        )
        assertEquals(1, result.score)
        assertEquals(2, result.total)
    }

    @Test
    fun emptyResultHasZeroScoreAndTotal() {
        val result = QuizResult(emptyList())
        assertEquals(0, result.score)
        assertEquals(0, result.total)
    }
}
