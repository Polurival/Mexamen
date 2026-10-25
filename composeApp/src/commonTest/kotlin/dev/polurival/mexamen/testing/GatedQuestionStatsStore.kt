package dev.polurival.mexamen.testing

import dev.polurival.mexamen.data.QuestionStats
import dev.polurival.mexamen.data.QuestionStatsStore
import kotlinx.coroutines.CompletableDeferred

/** Store, у которого [update] ждёт [open] — держит запись «в полёте», чтобы тест мог попасть в окно до результата. */
internal class GatedQuestionStatsStore(
    private val delegate: QuestionStatsStore = FakeQuestionStatsStore(),
) : QuestionStatsStore {
    private val gate = CompletableDeferred<Unit>()

    fun open() {
        gate.complete(Unit)
    }

    override suspend fun readAll(): Map<Int, QuestionStats> = delegate.readAll()

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        gate.await()
        delegate.update(transform)
    }

    override suspend fun clear() = delegate.clear()
}
