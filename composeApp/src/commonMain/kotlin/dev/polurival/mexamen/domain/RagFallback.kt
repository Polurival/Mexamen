package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry

/**
 * Fallback-дистракторы без LLM (FR-6, G3): приоритет — свои realDistractors →
 * соседские realDistractors → свои syntheticDistractors → ответы соседей (последний резерв).
 * Дедупликация case/accent-insensitive против всех answerVariants и уже набранных дистракторов.
 */
object RagFallback {

    /** До [count] дистракторов, порядок приоритета описан в шапке файла. */
    fun distractors(
        entry: QuestionEntry,
        neighbors: List<QuestionEntry>,
        count: Int = 3,
    ): List<String> {
        val seen = entry.answerVariants.mapTo(mutableSetOf(), ::normalize)
        val result = ArrayList<String>(count)

        fun addAll(candidates: Sequence<String>) {
            for (candidate in candidates) {
                if (result.size == count) return
                val trimmed = candidate.trim()
                if (trimmed.isEmpty()) continue
                if (seen.add(normalize(trimmed))) result += trimmed
            }
        }

        addAll(entry.realDistractors.asSequence())
        if (result.size < count) {
            addAll(neighbors.asSequence().flatMap { it.realDistractors.asSequence() })
        }
        if (result.size < count) {
            addAll(entry.syntheticDistractors.asSequence())
        }
        if (result.size < count) {
            addAll(neighbors.asSequence().flatMap { it.answerVariants.asSequence() })
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
