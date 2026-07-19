package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry

/**
 * Fallback-дистракторы без LLM (FR-6): ответы ближайших RAG-соседей.
 * Дедупликация case/accent-insensitive, правильный ответ исключён,
 * дыры добираются следующими соседями из списка.
 */
object RagFallback {

    /** До [count] дистракторов из ответов [neighbors] (в порядке убывания близости). */
    fun distractors(
        entry: QuestionEntry,
        neighbors: List<QuestionEntry>,
        count: Int = 3,
    ): List<String> {
        val seen = mutableSetOf(normalize(entry.answer))
        val result = ArrayList<String>(count)
        for (neighbor in neighbors) {
            if (result.size == count) break
            val answer = neighbor.answer.trim()
            if (answer.isEmpty()) continue
            if (seen.add(normalize(answer))) result += answer
        }
        return result
    }

    /** Нормализация для сравнения ответов: trim, lowercase, без акцентов, схлопнутые пробелы. */
    fun normalize(text: String): String = buildString(text.length) {
        var lastWasSpace = false
        for (raw in text.trim().lowercase()) {
            val char = ACCENTS[raw] ?: raw
            if (char.isWhitespace()) {
                if (!lastWasSpace) append(' ')
                lastWasSpace = true
            } else {
                append(char)
                lastWasSpace = false
            }
        }
    }

    private val ACCENTS = mapOf(
        'á' to 'a', 'à' to 'a', 'é' to 'e', 'è' to 'e', 'í' to 'i', 'ì' to 'i',
        'ó' to 'o', 'ò' to 'o', 'ú' to 'u', 'ù' to 'u', 'ü' to 'u', 'ñ' to 'n',
    )
}
