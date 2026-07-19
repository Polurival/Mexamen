package dev.polurival.mexamen.llm

import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Ручной интеграционный smoke (DoD D1). Реальный вызов DeepSeek — не для CI:
 * RUN_DEEPSEEK_SMOKE=1 ./gradlew :composeApp:testDebugUnitTest --tests '*DeepSeekSmokeTest*'
 */
class DeepSeekSmokeTest {

    @Test
    fun realCallReturnsText() {
        assumeTrue(System.getenv("RUN_DEEPSEEK_SMOKE") == "1")
        val text = runBlocking {
            DeepSeekProvider().complete(
                system = "Eres un asistente conciso. Responde en una sola palabra.",
                user = "¿Cuál es la capital de Jalisco?",
            )
        }
        println("DeepSeek respondió: $text")
        assertTrue(text.isNotBlank())
    }
}
