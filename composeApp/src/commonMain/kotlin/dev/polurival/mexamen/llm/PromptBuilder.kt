package dev.polurival.mexamen.llm

import dev.polurival.mexamen.data.QuestionEntry
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import kotlinx.serialization.json.Json

/** Один вопрос батча для генерации дистракторов (FR-4). */
@Serializable
data class DistractorRequestItem(
    val id: Int,
    val question: String,
    @SerialName("correct_answer") val correctAnswer: String,
    /** Реальные неверные варианты (свои/соседские) или, если их нет нигде, ответы RAG-соседей. */
    val context: List<String>,
    /**
     * Все формулировки верного ответа (H2, не отправляется LLM — только для локальной
     * валидации в [DistractorGenerator], чтобы дистрактор не совпал с альтернативным вариантом).
     */
    @Transient val answerVariants: List<String> = listOf(correctAnswer),
)

/**
 * Промпт по контракту spec §6: system — роль составителя экзамена, испанский,
 * строгий JSON; user — JSON-массив вопросов с RAG-контекстом.
 */
object PromptBuilder {

    val SYSTEM_PROMPT = """
        Eres un redactor experto de exámenes de naturalización de México.
        Para cada pregunta del usuario genera exactamente 3 distractores:
        respuestas incorrectas pero plausibles, en español, del mismo tipo y
        estilo que la respuesta correcta. El campo "context" contiene ejemplos
        de respuestas incorrectas reales para preguntas similares — úsalos como
        referencia de estilo, pero genera distractores NUEVOS, no los repitas.
        Reglas estrictas:
        - Ningún distractor puede coincidir con la respuesta correcta ni repetirse.
        - Ningún distractor puede coincidir con ningún ejemplo del campo "context".
        - Distractores cortos, como en un examen real de opción múltiple.
        - Responde SOLO con JSON válido, sin markdown ni texto adicional, con el formato:
        {"items":[{"id":1,"distractors":["...","...","..."]}]}
    """.trimIndent()

    private val json = Json

    fun buildUserPrompt(items: List<DistractorRequestItem>): String =
        json.encodeToString(items)

    /**
     * Контекст для [DistractorRequestItem.context] (H1): свои realDistractors + соседские
     * realDistractors (реальные неверные варианты). Если ни у вопроса, ни у соседей их нет —
     * старый fallback-контекст: ответы соседей (FR-4 до real-answers-plan).
     */
    fun buildContext(entry: QuestionEntry, neighbors: List<QuestionEntry>): List<String> {
        val real = LinkedHashSet<String>()
        real += entry.realDistractors
        neighbors.forEach { real += it.realDistractors }
        if (real.isNotEmpty()) return real.toList()
        return neighbors.map { it.answer }
    }
}
