package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore

/** Read-modify-write склейка [QuestionStatsStore] и чистой математики [QuestionWeighting]. */
class RecordAnswerUseCase(
    private val statsStore: QuestionStatsStore,
    private val clock: () -> Long = ::currentTimeMillis,
) {
    suspend fun recordQuizAnswer(questionId: Int, correct: Boolean) {
        val outcome = if (correct) AnswerOutcome.CORRECT else AnswerOutcome.WRONG
        val now = clock()
        statsStore.update { stats -> stats.updated(questionId) { QuestionWeighting.applyOutcome(it, outcome, now) } }
    }

    suspend fun recordFlashcardGrade(questionId: Int, grade: FlashcardGrade) {
        val now = clock()
        statsStore.update { stats -> stats.updated(questionId) { QuestionWeighting.applyGrade(it, grade, now) } }
    }

    private fun Map<Int, QuestionStats>.updated(
        id: Int,
        transform: (QuestionStats) -> QuestionStats,
    ): Map<Int, QuestionStats> = this + (id to transform(this[id] ?: QuestionStats()))
}
