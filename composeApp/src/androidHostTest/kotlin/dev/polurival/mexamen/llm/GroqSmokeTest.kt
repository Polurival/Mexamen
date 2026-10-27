package dev.polurival.mexamen.llm

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Ручной интеграционный smoke (DoD D1). Реальный вызов Groq — не для CI:
 * RUN_GROQ_SMOKE=1 ./gradlew :composeApp:testAndroidHostTest --tests '*GroqSmokeTest*'
 */
class GroqSmokeTest {

    @Test
    fun realCallReturnsText() {
        assumeTrue(System.getenv("RUN_GROQ_SMOKE") == "1")
        val text = runBlocking {
            GroqProvider().complete(
                system = "Eres un asistente conciso. Responde en una sola palabra.",
                user = "¿Cuál es la capital de Jalisco?",
            )
        }
        println("Groq respondió: $text")
        assertTrue(text.isNotBlank())
    }
}
