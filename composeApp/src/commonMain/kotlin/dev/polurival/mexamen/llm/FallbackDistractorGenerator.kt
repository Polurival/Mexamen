package dev.polurival.mexamen.llm

import dev.polurival.mexamen.domain.RagFallback

/**
 * Локальная генерация дистракторов (без LLM).
 * Использует кандидатов из [DistractorRequestItem.context], которые UseCase
 * подбирает из реальных дистракторов вопроса и соседей.
 */
class FallbackDistractorGenerator : DistractorGenerator {

    override suspend fun generate(requests: List<DistractorRequestItem>): Map<Int, List<String>> {
        return buildMap {
            for (request in requests) {
                val valid = validate(request, request.context)
                if (valid != null) {
                    put(request.id, valid)
                }
            }
        }
    }

    private fun validate(request: DistractorRequestItem, candidates: List<String>): List<String>? {
        val seen = mutableSetOf<String>()
        request.answerVariants.forEach { seen += RagFallback.normalize(it) }

        val valid = candidates
            .map { it.trim() }
            .filter { it.isNotEmpty() && seen.add(RagFallback.normalize(it)) }

        return if (valid.size >= DISTRACTOR_COUNT) valid.take(DISTRACTOR_COUNT) else null
    }

    companion object {
        private const val DISTRACTOR_COUNT = 3
    }
}
