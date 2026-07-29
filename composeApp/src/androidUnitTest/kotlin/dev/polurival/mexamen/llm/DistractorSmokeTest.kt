package dev.polurival.mexamen.llm

import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.domain.BuildQuizUseCase
import dev.polurival.mexamen.domain.DistractorSource
import java.io.File
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Ручной интеграционный smoke (DoD D2): с реальным ключом ≥ 8/10 вопросов
 * получают LLM-дистракторы. Запуск:
 * RUN_GROQ_SMOKE=1 ./gradlew :composeApp:testDebugUnitTest --tests '*DistractorSmokeTest*'
 */
class DistractorSmokeTest {

    private val resourcesDir = File("src/commonMain/composeResources/files")

    @Test
    fun realQuizGetsLlmDistractorsForMostQuestions() {
        assumeTrue(System.getenv("RUN_GROQ_SMOKE") == "1")
        val repository = QuestionRepository(
            readQuestions = { resourcesDir.resolve("questions.json").readBytes() },
            readEmbeddings = { resourcesDir.resolve("embeddings.bin").readBytes() },
        )
        val quiz = runBlocking {
            BuildQuizUseCase(
                dataset = repository.dataset(),
                distractorGenerator = DistractorGenerator(GroqProvider()),
            ).build()
        }
        quiz.forEach { q ->
            println("[${q.source}] ${q.entry.question}")
            q.options.forEachIndexed { i, opt ->
                println("    ${if (i == q.correctIndex) "*" else " "} $opt")
            }
        }
        val llmCount = quiz.count { it.source == DistractorSource.LLM }
        println("LLM distractors: $llmCount/10")
        assertTrue(llmCount >= 8, "Expected >= 8/10 LLM questions, got $llmCount")
    }
}
