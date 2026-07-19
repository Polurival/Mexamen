package dev.polurival.mexamen.llm

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

private class FakeLlm(
    private val response: String? = null,
    private val failure: Exception? = null,
) : LlmProvider {
    var lastSystem: String? = null
    var lastUser: String? = null

    override suspend fun complete(system: String, user: String): String {
        lastSystem = system
        lastUser = user
        failure?.let { throw it }
        return response ?: error("No canned response")
    }
}

class DistractorGeneratorTest {

    private val requests = listOf(
        DistractorRequestItem(1, "¿Capital de Jalisco?", "Guadalajara", listOf("Monterrey", "Mexicali")),
        DistractorRequestItem(2, "¿Capital de Sonora?", "Hermosillo", listOf("Chihuahua", "La Paz")),
    )

    @Test
    fun validResponseParsedAndPromptCarriesContext() = runTest {
        val llm = FakeLlm(
            response = """
                {"items":[
                  {"id":1,"distractors":["Monterrey","Mexicali","La Paz"]},
                  {"id":2,"distractors":["Chihuahua","Culiacán","Saltillo"]}
                ]}
            """.trimIndent(),
        )
        val result = DistractorGenerator(llm).generate(requests)

        assertEquals(
            mapOf(
                1 to listOf("Monterrey", "Mexicali", "La Paz"),
                2 to listOf("Chihuahua", "Culiacán", "Saltillo"),
            ),
            result,
        )
        assertEquals(PromptBuilder.SYSTEM_PROMPT, llm.lastSystem)
        val user = llm.lastUser!!
        assertTrue(
            "correct_answer" in user && "Guadalajara" in user && "Monterrey" in user,
            "User prompt must carry question, answer and context: $user",
        )
    }

    @Test
    fun markdownFencedResponseIsParsed() = runTest {
        val llm = FakeLlm(
            response = "```json\n{\"items\":[{\"id\":1,\"distractors\":[\"A\",\"B\",\"C\"]}]}\n```",
        )
        val result = DistractorGenerator(llm).generate(requests)
        assertEquals(mapOf(1 to listOf("A", "B", "C")), result)
    }

    @Test
    fun brokenJsonThrowsLlmUnavailable() = runTest {
        val llm = FakeLlm(response = "Claro, aquí tienes los distractores: 1) Monterrey…")
        assertFailsWith<LlmUnavailable> {
            DistractorGenerator(llm).generate(requests)
        }
    }

    @Test
    fun partiallyInvalidItemsAreDropped() = runTest {
        val llm = FakeLlm(
            response = """
                {"items":[
                  {"id":1,"distractors":["GUADALAJARA","Monterrey","Mexicali"]},
                  {"id":2,"distractors":["Chihuahua","chihuahua","La Paz","Culiacán"]},
                  {"id":99,"distractors":["A","B","C"]}
                ]}
            """.trimIndent(),
        )
        val result = DistractorGenerator(llm).generate(requests)

        // id=1: дубль правильного ответа → осталось 2 → элемент невалиден
        // id=2: дубль между собой отфильтрован, добор четвёртым → валиден
        // id=99: неизвестный id → игнор
        assertEquals(mapOf(2 to listOf("Chihuahua", "La Paz", "Culiacán")), result)
    }

    @Test
    fun llmFailurePropagatesAsLlmUnavailable() = runTest {
        val llm = FakeLlm(failure = LlmUnavailable("no network"))
        assertFailsWith<LlmUnavailable> {
            DistractorGenerator(llm).generate(requests)
        }
    }
}
