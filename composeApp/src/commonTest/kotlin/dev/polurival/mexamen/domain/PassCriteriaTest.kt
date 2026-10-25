package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class PassCriteriaTest {

    private fun resultWithScore(correct: Int, total: Int = 10): QuizResult {
        val answers = (0 until total).map { id ->
            AnsweredQuestion(
                question = QuizQuestion(
                    entry = QuestionEntry(id, "s", "q$id", "a$id"),
                    options = listOf("a", "b", "c", "d"),
                    correctIndex = 0,
                    source = DistractorSource.FALLBACK,
                ),
                chosenIndex = if (id < correct) 0 else 1,
            )
        }
        return QuizResult(answers)
    }

    @Test
    fun quizIsPassedExactlyAtThreshold() {
        assertTrue(PassCriteria.isQuizPassed(resultWithScore(PassCriteria.QUIZ_PASS_SCORE)))
        assertFalse(PassCriteria.isQuizPassed(resultWithScore(PassCriteria.QUIZ_PASS_SCORE - 1)))
    }

    @Test
    fun legendIsPassedExactlyAtThreshold() {
        assertTrue(PassCriteria.isLegendPassed(resultWithScore(PassCriteria.LEGEND_PASS_SCORE)))
        assertFalse(PassCriteria.isLegendPassed(resultWithScore(PassCriteria.LEGEND_PASS_SCORE - 1)))
    }

    @Test
    fun emptyResultIsNotPassed() {
        assertFalse(PassCriteria.isQuizPassed(QuizResult(emptyList())))
        assertFalse(PassCriteria.isLegendPassed(QuizResult(emptyList())))
    }
}
