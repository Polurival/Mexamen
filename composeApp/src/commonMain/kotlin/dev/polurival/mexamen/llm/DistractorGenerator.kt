package dev.polurival.mexamen.llm

import dev.polurival.mexamen.domain.RagFallback
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Батч-генерация дистракторов через LLM (FR-4) с поэлементной валидацией (FR-5).
 * Возвращает только валидные элементы; дыры закрывает вызывающий (RagFallback).
 * Полный отказ (сеть/ключ/непарсабельный ответ) — [LlmUnavailable].
 */
class DistractorGenerator(private val llm: LlmProvider) {

    /** id вопроса → ровно 3 валидных дистрактора. Невалидные/отсутствующие id опущены. */
    suspend fun generate(requests: List<DistractorRequestItem>): Map<Int, List<String>> {
        if (requests.isEmpty()) return emptyMap()
        val response = llm.complete(
            system = PromptBuilder.SYSTEM_PROMPT,
            user = PromptBuilder.buildUserPrompt(requests),
        )
        val parsed = try {
            json.decodeFromString<LlmResponse>(stripFences(response))
        } catch (e: Exception) {
            throw LlmUnavailable("Unparseable distractor JSON: ${e.message}", e)
        }
        val byId = requests.associateBy { it.id }
        return buildMap {
            for (item in parsed.items) {
                val request = byId[item.id] ?: continue
                val valid = validate(request.correctAnswer, item.distractors) ?: continue
                put(item.id, valid)
            }
        }
    }

    /** FR-5: непустые, без совпадений с верным ответом и между собой (case/accent-insensitive). */
    private fun validate(correctAnswer: String, distractors: List<String>): List<String>? {
        val seen = mutableSetOf(RagFallback.normalize(correctAnswer))
        val valid = distractors
            .map { it.trim() }
            .filter { it.isNotEmpty() && seen.add(RagFallback.normalize(it)) }
        return if (valid.size >= DISTRACTOR_COUNT) valid.take(DISTRACTOR_COUNT) else null
    }

    @Serializable
    private data class LlmResponse(val items: List<Item> = emptyList()) {
        @Serializable
        data class Item(val id: Int, val distractors: List<String> = emptyList())
    }

    companion object {
        const val DISTRACTOR_COUNT = 3

        private val json = Json { ignoreUnknownKeys = true }

        /** LLM иногда оборачивает JSON в markdown-fence вопреки промпту. */
        fun stripFences(text: String): String {
            val trimmed = text.trim()
            if (!trimmed.startsWith("```")) return trimmed
            return trimmed
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        }
    }
}
