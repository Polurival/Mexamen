package dev.polurival.mexamen.testing

import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore

/** In-memory store для тестов ViewModel/use case'ов без платформенного хранилища. */
internal class FakeQuestionStatsStore : QuestionStatsStore {
    private var stats: Map<Int, QuestionStats> = emptyMap()

    override suspend fun readAll(): Map<Int, QuestionStats> = stats

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        stats = transform(stats)
    }

    override suspend fun clear() {
        stats = emptyMap()
    }
}
