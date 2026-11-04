package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.llm.LlmUnavailable
import dev.polurival.mexamen.testing.FakeLlmProvider
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AskModelUseCaseTest {
    private val question = QuizQuestion(
        entry = QuestionEntry(1, "Estados", "¿Cuál es la capital de Baja California?", "Mexicali"),
        options = listOf("Tecate", "Mexicali", "Tijuana", "Ensenada"),
        correctIndex = 1,
        source = DistractorSource.FALLBACK,
    )

    @Test
    fun sendsBareQuestionWithoutSystemPromptAndParsesAnswer() = runTest {
        val tuned = FakeLlmProvider(reply = "Respuesta: B) Mexicali")
        var now = 1_000L
        val useCase = AskModelUseCase(mapOf(ModelVariant.TUNED to tuned)) { now.also { now += 250 } }

        val answer = useCase.ask(question, ModelVariant.TUNED)

        assertEquals("", tuned.lastSystem)
        assertEquals(
            "¿Cuál es la capital de Baja California?\nA) Tecate\nB) Mexicali\nC) Tijuana\nD) Ensenada",
            tuned.lastUser,
        )
        assertEquals(ModelAnswer(ModelVariant.TUNED, 1, "Respuesta: B) Mexicali", 250L), answer)
    }

    @Test
    fun routesToProviderOfRequestedVariant() = runTest {
        val tuned = FakeLlmProvider(reply = "Respuesta: A) Tecate")
        val base = FakeLlmProvider(reply = "Respuesta: C) Tijuana")
        val useCase = AskModelUseCase(mapOf(ModelVariant.TUNED to tuned, ModelVariant.BASE to base))

        assertEquals(2, useCase.ask(question, ModelVariant.BASE).optionIndex)
        assertEquals(0, useCase.ask(question, ModelVariant.TUNED).optionIndex)
    }

    @Test
    fun providerFailurePropagatesAsLlmUnavailable() = runTest {
        val useCase = AskModelUseCase(mapOf(ModelVariant.TUNED to FakeLlmProvider(failure = LlmUnavailable("down"))))
        assertFailsWith<LlmUnavailable> { useCase.ask(question, ModelVariant.TUNED) }
    }

    @Test
    fun missingProviderIsUnavailable() = runTest {
        assertFailsWith<LlmUnavailable> { AskModelUseCase(emptyMap()).ask(question, ModelVariant.BASE) }
    }
}
