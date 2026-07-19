package dev.polurival.mexamen.llm

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.isSuccess
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DeepSeek chat API (OpenAI-совместимый), паттерн AI-Playground/test_deepseek.py.
 * JSON собирается/парсится вручную — клиенту не нужны плагины, MockEngine-тесты просты.
 */
class DeepSeekProvider(
    private val apiKey: String? = deepSeekApiKey(),
    private val client: HttpClient = defaultClient(),
) : LlmProvider {

    override suspend fun complete(system: String, user: String): String {
        val key = apiKey?.takeIf { it.isNotBlank() }
            ?: throw LlmUnavailable("DeepSeek API key is not configured")
        val request = ChatRequest(
            model = MODEL,
            messages = listOf(
                ChatMessage(role = "system", content = system),
                ChatMessage(role = "user", content = user),
            ),
            temperature = TEMPERATURE,
        )
        val response = try {
            client.post(API_URL) {
                header(HttpHeaders.Authorization, "Bearer $key")
                setBody(TextContent(json.encodeToString(request), ContentType.Application.Json))
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw LlmUnavailable("DeepSeek request failed: ${e.message}", e)
        }
        if (!response.status.isSuccess()) {
            throw LlmUnavailable("DeepSeek HTTP ${response.status.value}")
        }
        val body = response.bodyAsText()
        val parsed = try {
            json.decodeFromString<ChatResponse>(body)
        } catch (e: Exception) {
            throw LlmUnavailable("Unparseable DeepSeek response: ${e.message}", e)
        }
        return parsed.choices.firstOrNull()?.message?.content?.takeIf { it.isNotBlank() }
            ?: throw LlmUnavailable("Empty DeepSeek response")
    }

    @Serializable
    private data class ChatRequest(
        val model: String,
        val messages: List<ChatMessage>,
        val temperature: Double,
    )

    @Serializable
    private data class ChatMessage(val role: String, val content: String)

    @Serializable
    private data class ChatResponse(val choices: List<Choice> = emptyList()) {
        @Serializable
        data class Choice(val message: ChatMessage? = null)
    }

    companion object {
        const val API_URL = "https://api.deepseek.com/v1/chat/completions"
        const val MODEL = "deepseek-chat"
        const val TEMPERATURE = 0.7
        const val TIMEOUT_MILLIS = 30_000L

        private val json = Json { ignoreUnknownKeys = true }

        private fun defaultClient(): HttpClient = HttpClient {
            install(HttpTimeout) {
                requestTimeoutMillis = TIMEOUT_MILLIS
            }
        }
    }
}
