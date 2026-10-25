package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats

/** Пул вопросов для сессии и её фактический размер (может быть меньше запрошенного). */
data class SessionPool(val entries: List<QuestionEntry>, val sessionSize: Int)

/**
 * Единственное место, где тема сессии (квиз/карточки) превращается в пул вопросов:
 * `null` — весь датасет, [TOPIC_MISTAKES] — вопросы с ошибками по статистике,
 * остальное — [QuestionDataset.entriesByTopic] (разделы и виртуальные темы).
 */
object SessionPoolSelector {
    /** Название режима «повторить ошибки»: показывается в UI как есть и служит ключом темы. */
    const val TOPIC_MISTAKES = "Repasar errores"

    fun select(
        dataset: QuestionDataset,
        topic: String?,
        stats: Map<Int, QuestionStats>,
        requested: Int,
    ): SessionPool = when (topic) {
        null -> SessionPool(dataset.entries, requested)
        // Ошибок может быть меньше [requested] (или ноль) — сессия просто короче
        TOPIC_MISTAKES -> mistakes(dataset.entries, stats).let { SessionPool(it, minOf(requested, it.size)) }
        else -> SessionPool(dataset.entriesByTopic(topic), requested)
    }

    /** Вопросы, на которых пользователь хотя бы раз ошибся (`timesWrong > 0`, кумулятивно). */
    fun mistakes(entries: List<QuestionEntry>, stats: Map<Int, QuestionStats>): List<QuestionEntry> =
        entries.filter { (stats[it.id]?.timesWrong ?: 0) > 0 }
}
