package dev.polurival.mexamen.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.TextContent
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.fail

class DeepSeekProviderTest {

    private val validResponse = """
        {
          "id": "chatcmpl-1",
          "choices": [
            {"index": 0, "message": {"role": "assistant", "content": "hola"}, "finish_reason": "stop"}
          ],
          "usage": {"prompt_tokens": 10, "completion_tokens": 1}
        }
    """.trimIndent()

    private fun provider(
        apiKey: String? = "test-key",
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = validResponse,
        capture: (HttpRequestData) -> Unit = {},
    ) = DeepSeekProvider(
        apiKey = apiKey,
        client = HttpClient(
            MockEngine { request ->
                capture(request)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    @Test
    fun sendsCorrectRequestAndParsesResponse() = runTest {
        var captured: HttpRequestData? = null
        val result = provider(capture = { captured = it }).complete("system prompt", "user prompt")

        assertEquals("hola", result)
        val request = captured ?: fail("Request was not sent")
        assertEquals(DeepSeekProvider.API_URL, request.url.toString())
        assertEquals(HttpMethod.Post, request.method)
        assertEquals("Bearer test-key", request.headers[HttpHeaders.Authorization])

        val sent = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals(DeepSeekProvider.MODEL, sent.getValue("model").jsonPrimitive.content)
        assertEquals(DeepSeekProvider.TEMPERATURE, sent.getValue("temperature").jsonPrimitive.content.toDouble())
        val messages = sent.getValue("messages").jsonArray.map { it.jsonObject }
        assertEquals(listOf("system", "user"), messages.map { it.getValue("role").jsonPrimitive.content })
        assertEquals("system prompt", messages[0].getValue("content").jsonPrimitive.content)
        assertEquals("user prompt", messages[1].getValue("content").jsonPrimitive.content)
    }

    @Test
    fun missingKeyFailsFastWithoutHttpCall() = runTest {
        var called = false
        val failure = assertFailsWith<LlmUnavailable> {
            provider(apiKey = null, capture = { called = true }).complete("s", "u")
        }
        assertTrue(!called, "HTTP must not be called without key")
        assertTrue(failure.message!!.contains("key"))

        assertFailsWith<LlmUnavailable> {
            provider(apiKey = "  ", capture = { called = true }).complete("s", "u")
        }
        assertTrue(!called)
    }

    @Test
    fun httpErrorBecomesLlmUnavailable() = runTest {
        val failure = assertFailsWith<LlmUnavailable> {
            provider(status = HttpStatusCode.InternalServerError).complete("s", "u")
        }
        assertTrue(failure.message!!.contains("500"))
    }

    @Test
    fun unparseableBodyBecomesLlmUnavailable() = runTest {
        assertFailsWith<LlmUnavailable> {
            provider(body = "not a json at all {").complete("s", "u")
        }
    }

    @Test
    fun emptyChoicesBecomesLlmUnavailable() = runTest {
        assertFailsWith<LlmUnavailable> {
            provider(body = """{"choices": []}""").complete("s", "u")
        }
    }
}
