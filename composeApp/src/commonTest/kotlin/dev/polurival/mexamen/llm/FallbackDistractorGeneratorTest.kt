package dev.polurival.mexamen.llm

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FallbackDistractorGeneratorTest {

    @Test
    fun picksFromContextAndValidates() = runTest {
        val requests = listOf(
            DistractorRequestItem(
                id = 1,
                question = "¿?",
                correctAnswer = "Guadalajara",
                context = listOf("Monterrey", "Mexicali", "La Paz", "Guadalajara"),
                answerVariants = listOf("Guadalajara", "Gdl")
            ),
            DistractorRequestItem(
                id = 2,
                question = "¿?",
                correctAnswer = "Hermosillo",
                context = listOf("Culiacán", "Mazatlán"), // Only 2 candidates
                answerVariants = listOf("Hermosillo")
            )
        )

        val result = FallbackDistractorGenerator().generate(requests)

        // id=1: "Guadalajara" filtered out (matches answer), 3 left: Monterrey, Mexicali, La Paz
        assertEquals(listOf("Monterrey", "Mexicali", "La Paz"), result[1])
        
        // id=2: Only 2 candidates -> less than 3 -> returns null for this ID (omitted from map)
        assertEquals(null, result[2])
    }
    
    @Test
    fun filtersDuplicatesAndEmpty() = runTest {
        val requests = listOf(
            DistractorRequestItem(
                id = 1,
                question = "¿?",
                correctAnswer = "A",
                context = listOf("B", "B", " ", "C", "D"),
                answerVariants = listOf("A")
            )
        )
        val result = FallbackDistractorGenerator().generate(requests)
        assertEquals(listOf("B", "C", "D"), result[1])
    }
}
