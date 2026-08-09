package dev.polurival.mexamen.data

/**
 * Персистентность per-question статистики (quiz и flashcards пишут в один и тот же store).
 * `update` — атомарный read-modify-write, чтобы конкурентные записи (напр. quiz + flashcards
 * в разных экранах) не затирали друг друга.
 */
interface QuestionStatsStore {
    suspend fun readAll(): Map<Int, QuestionStats>
    suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>)
    suspend fun clear()
}

/** Продакшн-инстанс; платформенное хранилище (DataStore на Android). */
expect fun defaultQuestionStatsStore(): QuestionStatsStore
