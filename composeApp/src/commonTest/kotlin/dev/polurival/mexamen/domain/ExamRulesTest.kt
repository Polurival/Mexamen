package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExamRulesTest {

    /** Результат из [total] вопросов, первые [correct] из которых отвечены верно. */
    private fun result(correct: Int, total: Int = 10) = QuizResult(
        (1..total).map { id ->
            val question = QuizQuestion(
                entry = QuestionEntry(id, "s", "q$id", "a$id"),
                options = listOf("a", "b", "c", "d"),
                correctIndex = 0,
                source = DistractorSource.FALLBACK,
            )
            AnsweredQuestion(question, chosenIndex = if (id <= correct) 0 else 1)
        },
    )

    @Test
    fun quizIsPassedFromPassScoreInclusive() {
        assertTrue(ExamRules.isQuizPassed(result(correct = ExamRules.QUIZ_PASS_SCORE)))
        assertTrue(ExamRules.isQuizPassed(result(correct = 10)))
        assertFalse(ExamRules.isQuizPassed(result(correct = ExamRules.QUIZ_PASS_SCORE - 1)))
    }

    @Test
    fun legendIsPassedFromLegendPassScoreInclusive() {
        assertTrue(ExamRules.isLegendPassed(result(correct = ExamRules.LEGEND_PASS_SCORE, total = 6)))
        assertFalse(ExamRules.isLegendPassed(result(correct = ExamRules.LEGEND_PASS_SCORE - 1, total = 6)))
    }

    @Test
    fun emptyResultIsNotPassed() {
        assertFalse(ExamRules.isQuizPassed(QuizResult(emptyList())))
        assertFalse(ExamRules.isLegendPassed(QuizResult(emptyList())))
    }

    @Test
    fun timeRunsOutStrictlyBelowWarningThreshold() {
        assertTrue(ExamRules.isTimeRunningOut(ExamRules.TIME_WARNING_SECONDS - 1))
        assertTrue(ExamRules.isTimeRunningOut(0))
        assertFalse(ExamRules.isTimeRunningOut(ExamRules.TIME_WARNING_SECONDS))
        assertFalse(ExamRules.isTimeRunningOut(ExamRules.QUIZ_DURATION_SECONDS))
    }
}
