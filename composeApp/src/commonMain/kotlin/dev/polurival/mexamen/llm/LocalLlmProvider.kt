package dev.polurival.mexamen.llm

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Локальный сервис с дообученной моделью (OpenAI-совместимый `/v1/chat/completions`, см. fine_tuning_prod).
 * [model] — алиас сервиса ("tuned"/"base"): какая версия за ним стоит, решает registry.json на стороне
 * сервиса, поэтому выкатка и откат модели не требуют новой сборки приложения.
 * Любая ошибка (сервис не запущен, таймаут, кривой ответ) становится [LlmUnavailable].
 */
class LocalLlmProvider(
    private val model: String,
    private val baseUrl: String = DEFAULT_BASE_URL,
    private val client: HttpClient = defaultClient(),
) : LlmProvider {

    override suspend fun complete(system: String, user: String): String {
        val messages = buildList {
            if (system.isNotBlank()) add(ChatMessage(role = "system", content = system))
            add(ChatMessage(role = "user", content = user))
        }
        val request = ChatRequest(model = model, messages = messages, temperature = TEMPERATURE, maxTokens = MAX_TOKENS)
        val response = try {
            client.post("$baseUrl$PATH") {
                setBody(TextContent(json.encodeToString(request), ContentType.Application.Json))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LlmUnavailable("Local LLM request failed: ${e.message}", e)
        }
        if (!response.status.isSuccess()) {
            throw LlmUnavailable("Local LLM HTTP ${response.status.value}")
        }
        val parsed = try {
            json.decodeFromString<ChatResponse>(response.bodyAsText())
        } catch (e: Exception) {
            throw LlmUnavailable("Unparseable local LLM response: ${e.message}", e)
        }
        return parsed.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
            ?: throw LlmUnavailable("Empty local LLM response")
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
        @SerialName("max_tokens") val maxTokens: Int,
    )

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList()) {
        @Serializable
        data class Choice(val message: ChatMessage? = null)
    }

    companion object {
        const val DEFAULT_BASE_URL = "http://localhost:8090"
        const val PATH = "/v1/chat/completions"
        const val TEMPERATURE = 0.0
        const val MAX_TOKENS = 128

        /** Первый запрос после простоя включает холодную загрузку модели (секунды на CPU). */
        const val TIMEOUT_MILLIS = 90_000L

        private val json = Json { ignoreUnknownKeys = true }

        /** Один клиент на все модели сервиса (см. LlmModule). */
        fun defaultClient(): HttpClient = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MILLIS
            }
        }
    }
}
