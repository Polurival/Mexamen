package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry

/**
 * Поиск по банку вопросов («Banco de preguntas»): подстрока в тексте вопроса или ответа,
 * без учёта регистра и диакритики (нормализация общая с [RagFallback.normalize]).
 * Пустой/пробельный запрос возвращает все записи в исходном порядке.
 */
object QuestionSearch {

    fun filter(entries: List<QuestionEntry>, query: String): List<QuestionEntry> {
        val needle = RagFallback.normalize(query)
        if (needle.isEmpty()) return entries
        return entries.filter { entry ->
            RagFallback.normalize(entry.question).contains(needle) ||
                RagFallback.normalize(entry.answer).contains(needle)
        }
    }
}
