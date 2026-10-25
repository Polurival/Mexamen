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
    fun answeredQuestionIsAnsweredOnlyWhenChosenIndexIsValidOption() {
        assertTrue(AnsweredQuestion(question(1), chosenIndex = 0).isAnswered)
        assertFalse(AnsweredQuestion(question(1), chosenIndex = -1).isAnswered)
        assertFalse(AnsweredQuestion(question(1), chosenIndex = 4).isAnswered)
    }

    @Test
    fun unansweredQuestionIsNeverCorrectAndNotScored() {
        // Легенда с некорректным correctIndex (-1) не должна засчитывать пропущенный вопрос.
        val unanswered = AnsweredQuestion(question(1, correctIndex = -1), chosenIndex = -1)
        assertFalse(unanswered.isCorrect)
        val result = QuizResult(listOf(unanswered, AnsweredQuestion(question(2), chosenIndex = -1)))
        assertEquals(0, result.score)
        assertEquals(2, result.total)
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
    fun forQuizAddsUnansweredQuestionsAsWrong() {
        val quiz = listOf(question(1), question(2), question(3), question(4))
        val answers = listOf(
            AnsweredQuestion(quiz[0], chosenIndex = 1),
            AnsweredQuestion(quiz[1], chosenIndex = 0),
        )
        val result = QuizResult.forQuiz(quiz, answers)
        assertEquals(4, result.total)
        assertEquals(1, result.score)
        assertEquals(listOf(1, 2, 3, 4), result.answers.map { it.question.entry.id })
        assertTrue(result.answers.drop(2).none { it.isAnswered || it.isCorrect })
    }

    @Test
    fun forQuizWithAllAnsweredKeepsAnswersUnchanged() {
        val quiz = listOf(question(1), question(2))
        val answers = listOf(
            AnsweredQuestion(quiz[0], chosenIndex = 1),
            AnsweredQuestion(quiz[1], chosenIndex = 1),
        )
        assertEquals(QuizResult(answers), QuizResult.forQuiz(quiz, answers))
    }

    @Test
    fun emptyResultHasZeroScoreAndTotal() {
        val result = QuizResult(emptyList())
        assertEquals(0, result.score)
        assertEquals(0, result.total)
    }
}
