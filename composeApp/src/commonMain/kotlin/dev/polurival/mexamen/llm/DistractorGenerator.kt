package dev.polurival.mexamen.llm

/**
 * Батч-генерация дистракторов (FR-4).
 * Возвращает мапу: id вопроса → список из 3 дистракторов.
 */
interface DistractorGenerator {
    suspend fun generate(requests: List<DistractorRequestItem>): Map<Int, List<String>>
}
