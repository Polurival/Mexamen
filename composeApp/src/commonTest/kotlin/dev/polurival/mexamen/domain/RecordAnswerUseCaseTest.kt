package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/** В памяти, без Android — доказывает что запись ответа не зависит от платформы. */
private class FakeQuestionStatsStore : QuestionStatsStore {
    private var stats: Map<Int, QuestionStats> = emptyMap()

    override suspend fun readAll(): Map<Int, QuestionStats> = stats

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        stats = transform(stats)
    }

    override suspend fun clear() {
        stats = emptyMap()
    }
}

class RecordAnswerUseCaseTest {

    @Test
    fun recordQuizAnswerCreatesStatsForNewQuestion() = runTest {
        val store = FakeQuestionStatsStore()
        val useCase = RecordAnswerUseCase(store, clock = { 42L })

        useCase.recordQuizAnswer(questionId = 5, correct = true)

        val stats = store.readAll().getValue(5)
        assertEquals(1, stats.timesShown)
        assertEquals(1, stats.timesCorrect)
        assertEquals(42L, stats.lastSeenEpochMillis)
    }

    @Test
    fun recordQuizAnswerAccumulatesOnExistingQuestion() = runTest {
        val store = FakeQuestionStatsStore()
        val useCase = RecordAnswerUseCase(store, clock = { 1L })

        useCase.recordQuizAnswer(questionId = 5, correct = false)
        useCase.recordQuizAnswer(questionId = 5, correct = false)

        val stats = store.readAll().getValue(5)
        assertEquals(2, stats.timesShown)
        assertEquals(2, stats.timesWrong)
    }

    @Test
    fun recordFlashcardGradeUpdatesOnlyTargetedQuestion() = runTest {
        val store = FakeQuestionStatsStore()
        val useCase = RecordAnswerUseCase(store, clock = { 7L })

        useCase.recordFlashcardGrade(questionId = 1, grade = FlashcardGrade.EASY)

        val stats = store.readAll()
        assertEquals(1, stats.size)
        assertEquals(1, stats.getValue(1).timesCorrect)
    }
}
