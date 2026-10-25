package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStatsStore
import kotlin.random.Random

/**
 * Сборка flashcard-сессии: без LLM/RAG/дистракторов вообще — карта показывает только
 * вопрос и ответ, поэтому в отличие от [BuildQuizUseCase] тут нет LLM-зависимости и
 * режим безусловно 100% оффлайн.
 */
class BuildFlashcardsUseCase(
    private val random: Random = Random.Default,
) {
    suspend fun build(
        dataset: QuestionDataset,
        statsStore: QuestionStatsStore,
        count: Int = FLASHCARD_SIZE,
        topic: String? = null,
        nowMillis: Long = currentTimeMillis(),
    ): List<QuestionEntry> {
        val stats = statsStore.readAll()
        val pool = when (topic) {
            null -> dataset.entries
            QuestionEntry.TOPIC_DATES -> dataset.entries.filter { QuestionEntry.isDate(it) }
            QuestionEntry.TOPIC_PEOPLE -> dataset.entries.filter { QuestionEntry.isPeople(it) }
            QuestionEntry.TOPIC_EVENTS -> dataset.entries.filter { QuestionEntry.isEvent(it) }
            else -> dataset.entries.filter { it.section == topic }
        }
        return WeightedQuestionPicker(random).pick(pool, stats, count, nowMillis)
    }

    companion object {
        const val FLASHCARD_SIZE = 10
    }
}
