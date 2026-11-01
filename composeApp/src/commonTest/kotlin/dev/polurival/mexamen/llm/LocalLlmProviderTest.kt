package dev.polurival.mexamen.llm

import io.ktor.client.HttpClient
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
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
import kotlin.test.fail

class LocalLlmProviderTest {

    private val validResponse = """{"choices":[{"message":{"role":"assistant","content":"Respuesta: B) Mexicali"}}]}"""

    private fun provider(
        status: HttpStatusCode = HttpStatusCode.OK,
        body: String = validResponse,
        capture: (HttpRequestData) -> Unit = {},
    ) = LocalLlmProvider(
        model = "tuned",
        baseUrl = "http://localhost:8080",
        client = HttpClient(
            MockEngine { request ->
                capture(request)
                respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
            },
        ),
    )

    @Test
    fun sendsAliasAndOmitsBlankSystemMessage() = runTest {
        var captured: HttpRequestData? = null
        val result = provider(capture = { captured = it }).complete(system = "", user = "pregunta")

        assertEquals("Respuesta: B) Mexicali", result)
        val request = captured ?: fail("Request was not sent")
        assertEquals("http://localhost:8080/v1/chat/completions", request.url.toString())
        val sent = Json.parseToJsonElement((request.body as TextContent).text).jsonObject
        assertEquals("tuned", sent.getValue("model").jsonPrimitive.content)
        assertEquals(0.0, sent.getValue("temperature").jsonPrimitive.content.toDouble())
        val messages = sent.getValue("messages").jsonArray.map { it.jsonObject }
        assertEquals(listOf("user"), messages.map { it.getValue("role").jsonPrimitive.content })
    }

    @Test
    fun sendsSystemMessageWhenGiven() = runTest {
        var captured: HttpRequestData? = null
        provider(capture = { captured = it }).complete(system = "reglas", user = "pregunta")
        val sent = Json.parseToJsonElement(((captured ?: fail("no request")).body as TextContent).text).jsonObject
        val roles = sent.getValue("messages").jsonArray.map { it.jsonObject.getValue("role").jsonPrimitive.content }
        assertEquals(listOf("system", "user"), roles)
    }

    @Test
    fun httpErrorBecomesLlmUnavailable() = runTest {
        assertFailsWith<LlmUnavailable> {
            provider(status = HttpStatusCode.ServiceUnavailable, body = "{}").complete("", "u")
        }
    }

    @Test
    fun garbageAndEmptyBodiesBecomeLlmUnavailable() = runTest {
        assertFailsWith<LlmUnavailable> { provider(body = "not json").complete("", "u") }
        assertFailsWith<LlmUnavailable> { provider(body = """{"choices":[]}""").complete("", "u") }
        assertFailsWith<LlmUnavailable> {
            provider(body = """{"choices":[{"message":{"role":"assistant","content":" "}}]}""").complete("", "u")
        }
    }

    @Test
    fun networkFailureBecomesLlmUnavailable() = runTest {
        val failing = LocalLlmProvider(
            model = "tuned",
            client = HttpClient(MockEngine { throw IllegalStateException("connection refused") }),
        )
        assertFailsWith<LlmUnavailable> { failing.complete("", "u") }
    }
}
