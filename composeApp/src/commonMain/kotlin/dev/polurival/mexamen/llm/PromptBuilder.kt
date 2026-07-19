package dev.polurival.mexamen.llm

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Один вопрос батча для генерации дистракторов (FR-4). */
@Serializable
data class DistractorRequestItem(
    val id: Int,
    val question: String,
    @SerialName("correct_answer") val correctAnswer: String,
    /** Ответы RAG-соседей — контекст для правдоподобных дистракторов. */
    val context: List<String>,
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
        estilo que la respuesta correcta. Usa el campo "context" (respuestas a
        preguntas similares) como inspiración para distractores realistas.
        Reglas estrictas:
        - Ningún distractor puede coincidir con la respuesta correcta ni repetirse.
        - Distractores cortos, como en un examen real de opción múltiple.
        - Responde SOLO con JSON válido, sin markdown ni texto adicional, con el formato:
        {"items":[{"id":1,"distractors":["...","...","..."]}]}
    """.trimIndent()

    private val json = Json

    fun buildUserPrompt(items: List<DistractorRequestItem>): String =
        json.encodeToString(items)
}
