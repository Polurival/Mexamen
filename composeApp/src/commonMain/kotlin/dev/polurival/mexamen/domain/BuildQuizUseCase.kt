package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.llm.DistractorGenerator
import dev.polurival.mexamen.llm.DistractorRequestItem
import dev.polurival.mexamen.llm.LlmUnavailable
import dev.polurival.mexamen.llm.PromptBuilder
import kotlin.random.Random

/**
 * Сборка квиза. LLM-путь (FR-4/5): один батч-запрос дистракторов с RAG-контекстом,
 * невалидные элементы и полный отказ LLM закрываются fallback-дистракторами (FR-6).
 * `source` каждого вопроса отражает реальный источник. Без [distractorGenerator]
 * (нет ключа/оффлайн-режим по построению) — сразу fallback.
 */
class BuildQuizUseCase(
    private val dataset: QuestionDataset,
    private val random: Random = Random.Default,
    private val distractorGenerator: DistractorGenerator? = null,
    private val statsStore: QuestionStatsStore? = null,
    private val nowMillis: Long = currentTimeMillis(),
) {
    private val store = VectorStore(dataset)
    private val byId = dataset.entries.associateBy { it.id }

    suspend fun build(count: Int = QUIZ_SIZE, topic: String? = null): List<QuizQuestion> {
        val stats = statsStore?.readAll() ?: emptyMap()
        val pool = if (topic == null) dataset.entries else dataset.entriesByTopic(topic)
        val picked = WeightedQuestionPicker(random).pick(pool, stats, count, nowMillis)
        val ragNeighbors = picked.associate { entry ->
            entry.id to store.topK(entry.id, RAG_TOP_K).map { byId.getValue(it.id) }
        }
        val llmDistractors = generateLlmDistractors(picked, ragNeighbors)
        return picked.map { entry ->
            val llm = llmDistractors[entry.id]
            if (llm != null) {
                toQuizQuestion(entry, llm, DistractorSource.LLM)
            } else {
                toQuizQuestion(entry, fallbackDistractors(entry), DistractorSource.FALLBACK)
            }
        }
    }

    private suspend fun generateLlmDistractors(
        picked: List<QuestionEntry>,
        ragNeighbors: Map<Int, List<QuestionEntry>>,
    ): Map<Int, List<String>> {
        val generator = distractorGenerator ?: return emptyMap()
        val requests = picked.map { entry ->
            DistractorRequestItem(
                id = entry.id,
                question = entry.question,
                correctAnswer = entry.answer,
                context = PromptBuilder.buildContext(entry, ragNeighbors.getValue(entry.id)),
                answerVariants = entry.answerVariants,
            )
        }
        return try {
            generator.generate(requests)
        } catch (e: LlmUnavailable) {
            // FR-6: квиз никогда не блокируется из-за LLM — целиком fallback
            emptyMap()
        }
    }

    /**
     * Отображаемый верный ответ — случайный элемент [QuestionEntry.answerVariants] (G2).
     * Дистракторы дедуплицируются против ВСЕХ вариантов, а не только выбранного.
     */
    private fun toQuizQuestion(
        entry: QuestionEntry,
        distractors: List<String>,
        source: DistractorSource,
    ): QuizQuestion {
        val variant = entry.answerVariants.random(random)
        val variantKeys = entry.answerVariants.mapTo(mutableSetOf(), RagFallback::normalize)
        val filtered = distractors.filterNot { variantKeys.contains(RagFallback.normalize(it)) }
        val options = (filtered + variant).shuffled(random)
        return QuizQuestion(
            entry = entry,
            options = options,
            correctIndex = options.indexOf(variant),
            source = source,
        )
    }

    /**
     * Дистракторы из top-K соседей; если после дедупликации их меньше трёх —
     * расширяем K вплоть до всего датасета.
     */
    private fun fallbackDistractors(entry: QuestionEntry): List<String> {
        val maxK = dataset.entries.size - 1
        var k = RAG_TOP_K
        while (true) {
            val neighbors = store.topK(entry.id, k).map { byId.getValue(it.id) }
            val distractors = RagFallback.distractors(entry, neighbors, DISTRACTOR_COUNT)
            if (distractors.size == DISTRACTOR_COUNT) return distractors
            check(k < maxK) {
                "Not enough distinct answers in dataset for question id=${entry.id}"
            }
            k = minOf(k * 4, maxK)
        }
    }

    companion object {
        const val QUIZ_SIZE = 10
        const val DISTRACTOR_COUNT = 3
        const val RAG_TOP_K = 8
    }
}
