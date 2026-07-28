package dev.polurival.mexamen.llm

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class PromptBuilderTest {

    private fun entry(
        id: Int,
        answer: String,
        realDistractors: List<String> = emptyList(),
    ) = QuestionEntry(id, "s", "q$id", answer, realDistractors = realDistractors)

    @Test
    fun usesOwnRealDistractorsWhenPresent() {
        val target = entry(1, "Guadalajara", realDistractors = listOf("Monterrey", "Mexicali", "La Paz"))
        val neighbors = listOf(entry(2, "Colima"), entry(3, "Toluca"))

        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz"),
            PromptBuilder.buildContext(target, neighbors),
        )
    }

    @Test
    fun fillsFromNeighborRealDistractorsWhenOwnAreMissing() {
        val target = entry(1, "Guadalajara")
        val neighbors = listOf(
            entry(2, "Colima", realDistractors = listOf("Mexicali", "La Paz")),
            entry(3, "Toluca", realDistractors = listOf("Puebla")),
        )

        assertEquals(
            listOf("Mexicali", "La Paz", "Puebla"),
            PromptBuilder.buildContext(target, neighbors),
        )
    }

    @Test
    fun fallsBackToNeighborAnswersWhenNoRealDistractorsAnywhere() {
        val target = entry(1, "Guadalajara")
        val neighbors = listOf(entry(2, "Colima"), entry(3, "Toluca"))

        assertEquals(
            listOf("Colima", "Toluca"),
            PromptBuilder.buildContext(target, neighbors),
        )
    }

    @Test
    fun dedupsBetweenOwnAndNeighborRealDistractors() {
        val target = entry(1, "Guadalajara", realDistractors = listOf("Monterrey", "Mexicali", "La Paz"))
        val neighbors = listOf(entry(2, "Colima", realDistractors = listOf("Monterrey", "Puebla")))

        assertEquals(
            listOf("Monterrey", "Mexicali", "La Paz", "Puebla"),
            PromptBuilder.buildContext(target, neighbors),
        )
    }
}
