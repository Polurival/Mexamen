package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class RagFallbackTest {

    private fun entry(id: Int, answer: String) = QuestionEntry(id, "s", "q$id", answer)

    private val target = entry(1, "Guadalajara")

    @Test
    fun prefersOwnRealDistractorsOverNeighbors() {
        val withReal = target.copy(realDistractors = listOf("Monterrey", "Mexicali", "La Paz"))
        val neighbors = listOf(entry(2, "Colima"), entry(3, "Toluca"))
        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            RagFallback.distractors(withReal, neighbors),
        )
    }

    @Test
    fun fillsFromNeighborRealDistractorsWhenOwnAreShort() {
        val withReal = target.copy(realDistractors = listOf("Monterrey"))
        val neighbors = listOf(
            entry(2, "Colima").copy(realDistractors = listOf("Mexicali", "La Paz")),
            entry(3, "Toluca").copy(realDistractors = listOf("Puebla")),
        )
        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            RagFallback.distractors(withReal, neighbors),
        )
    }

    @Test
    fun fallsBackToOwnSyntheticDistractorsWhenNoRealAnywhere() {
        val withSynthetic = target.copy(syntheticDistractors = listOf("Monterrey", "Mexicali", "La Paz"))
        val neighbors = listOf(entry(2, "Colima"), entry(3, "Toluca"))
        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            RagFallback.distractors(withSynthetic, neighbors),
        )
    }

    @Test
    fun fallsBackToNeighborAnswerVariantsWhenNoRealOrSynthetic() {
        val neighbors = listOf(
            entry(2, "Colima").copy(answerVariants = listOf("Colima", "Colima de Álvarez")),
            entry(3, "Toluca"),
        )
        assertEquals(
            listOf("Colima", "Colima de Álvarez", "Toluca"),
            RagFallback.distractors(target, neighbors),
        )
    }

    @Test
    fun takesNearestNeighborAnswersInOrder() {
        val neighbors = listOf(entry(2, "Monterrey"), entry(3, "Mexicali"), entry(4, "La Paz"), entry(5, "Colima"))
        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            RagFallback.distractors(target, neighbors),
        )
    }

    @Test
    fun skipsAnswerEqualToCorrectCaseAccentInsensitive() {
        val neighbors = listOf(entry(2, "GUADALAJARA"), entry(3, "guadalajára"), entry(4, "Monterrey"), entry(5, "Mexicali"), entry(6, "La Paz"))
        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            RagFallback.distractors(target, neighbors),
        )
    }

    @Test
    fun dedupsBetweenNeighborsAndFillsFromNext() {
        val neighbors = listOf(
            entry(2, "Monterrey"),
            entry(3, "monterrey"),
            entry(4, "MONTERREY "),
            entry(5, "Mexicali"),
            entry(6, "La Paz"),
        )
        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            RagFallback.distractors(target, neighbors),
        )
    }

    @Test
    fun skipsBlankAnswers() {
        val neighbors = listOf(entry(2, "  "), entry(3, "Monterrey"), entry(4, "Mexicali"), entry(5, "La Paz"))
        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            RagFallback.distractors(target, neighbors),
        )
    }

    @Test
    fun returnsFewerWhenNotEnoughDistinctAnswers() {
        val neighbors = listOf(entry(2, "Monterrey"), entry(3, "Monterrey"))
        assertEquals(
            listOf("Monterrey"),
            RagFallback.distractors(target, neighbors),
        )
    }

    @Test
    fun normalizeHandlesCaseAccentsAndWhitespace() {
        assertEquals("mexico", RagFallback.normalize("  MÉXICO "))
        assertEquals("nueva espana", RagFallback.normalize("Nueva   España"))
        assertEquals("benito juarez", RagFallback.normalize("Benito\tJuárez"))
    }
}
