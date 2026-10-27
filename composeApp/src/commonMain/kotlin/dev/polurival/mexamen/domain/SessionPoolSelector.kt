package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStats

/** Пул вопросов сессии и сколько из него брать; [count] = 0 — сессию строить не из чего. */
data class SessionPool(val entries: List<QuestionEntry>, val count: Int)

/**
 * Единая точка выбора пула для квиза и карточек по теме из списка тем.
 * Темы по данным (`section`, Fechas/Personajes/Eventos) — через [QuestionRepository.filterByTopic];
 * «Repasar errores» зависит от статистики, поэтому живёт здесь, а не в репозитории.
 */
object SessionPoolSelector {
    /** Имя темы «Repasar errores» — видно в UI и передаётся как topic в маршрут сессии. */
    const val TOPIC_MISTAKES = "Repasar errores"

    /** Вопросы, на которых пользователь хоть раз ошибся (`timesWrong > 0`, кумулятивно за всё время). */
    fun mistakes(entries: List<QuestionEntry>, stats: Map<Int, QuestionStats>): List<QuestionEntry> =
        entries.filter { (stats[it.id]?.timesWrong ?: 0) > 0 }

    /**
     * Пул для [topic] (`null` — все вопросы). Для «Repasar errores» сессия укорачивается до числа
     * ошибок (в т.ч. до 0); для остальных тем запрошенный [requested] не меняется — нехватку
     * вопросов по-прежнему сигнализирует [WeightedQuestionPicker].
     */
    fun select(
        entries: List<QuestionEntry>,
        stats: Map<Int, QuestionStats>,
        topic: String?,
        requested: Int,
    ): SessionPool = when (topic) {
        null -> SessionPool(entries, requested)
        TOPIC_MISTAKES -> mistakes(entries, stats).let { SessionPool(it, minOf(requested, it.size)) }
        else -> SessionPool(QuestionRepository.filterByTopic(entries, topic), requested)
    }
}
