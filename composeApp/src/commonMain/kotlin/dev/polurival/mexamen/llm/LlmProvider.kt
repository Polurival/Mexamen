package dev.polurival.mexamen.llm

/**
 * Абстракция LLM (spec §2): MVP — Groq, замена провайдера не трогает остальной код.
 */
interface LlmProvider {
    /**
     * Один chat-запрос. Бросает [LlmUnavailable] при любой проблеме
     * (нет ключа, сеть, таймаут, кривой ответ) — вызывающий уходит в fallback (FR-6).
     */
    suspend fun complete(system: String, user: String): String
}

class LlmUnavailable(message: String, cause: Throwable? = null) : Exception(message, cause)

/** Ключ Groq из платформенной конфигурации; null — ключ не задан. */
expect fun groqApiKey(): String?

/** Debug-сборка ли — включает HTTP-логирование (Logcat) в [GroqProvider]. */
expect fun isDebugBuild(): Boolean
