package dev.polurival.mexamen.testing

import dev.polurival.mexamen.llm.LlmProvider

/** LLM-заглушка: возвращает [reply] или бросает [failure]; запоминает последний запрос. */
internal class FakeLlmProvider(
    private val reply: String = "",
    private val failure: Exception? = null,
) : LlmProvider {
    var lastSystem: String? = null
        private set
    var lastUser: String? = null
        private set

    override suspend fun complete(system: String, user: String): String {
        lastSystem = system
        lastUser = user
        failure?.let { throw it }
        return reply
    }
}
