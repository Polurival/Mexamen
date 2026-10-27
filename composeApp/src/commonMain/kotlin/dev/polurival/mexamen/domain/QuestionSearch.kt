package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry

/** Вопрос с заранее нормализованным текстом «вопрос + ответ»: нормализация один раз на загрузку, а не на каждый ввод. */
data class SearchableQuestion(val entry: QuestionEntry, val normalizedText: String)

/**
 * Поиск по «Banco de preguntas»: по тексту вопроса и ответа, без учёта регистра и диакритики
 * («canon» находит «cañón», «MEXICO» — «México»); нормализация — общая [RagFallback.normalize].
 * Запрос режется на слова, каждое должно встретиться в вопросе или ответе; пустой запрос
 * возвращает все вопросы в исходном порядке.
 */
object QuestionSearch {
    fun index(entries: List<QuestionEntry>): List<SearchableQuestion> =
        entries.map { entry ->
            SearchableQuestion(entry, RagFallback.normalize(entry.question) + " " + RagFallback.normalize(entry.answer))
        }

    fun filter(index: List<SearchableQuestion>, query: String): List<QuestionEntry> {
        val terms = RagFallback.normalize(query).split(' ').filter { it.isNotEmpty() }
        return index
            .filter { item -> terms.all { item.normalizedText.contains(it) } }
            .map { it.entry }
    }
}
