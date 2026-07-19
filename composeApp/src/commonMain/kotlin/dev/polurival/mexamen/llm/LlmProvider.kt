package dev.polurival.mexamen.llm

/**
 * Абстракция LLM (spec §2): MVP — DeepSeek, замена провайдера не трогает остальной код.
 */
interface LlmProvider {
    /**
     * Один chat-запрос. Бросает [LlmUnavailable] при любой проблеме
     * (нет ключа, сеть, таймаут, кривой ответ) — вызывающий уходит в fallback (FR-6).
     */
    suspend fun complete(system: String, user: String): String
}

class LlmUnavailable(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Ключ DeepSeek из платформенной конфигурации; null — ключ не задан. */
expect fun deepSeekApiKey(): String?
