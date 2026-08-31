package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository

class GetRelatedQuestionUseCase(
    private val repository: QuestionRepository
) {
    suspend operator fun invoke(question: QuestionEntry): QuestionEntry? {
        val dataset = repository.dataset()
        val vectorStore = VectorStore(dataset)
        
        // Находим ближайшего соседа (k=1)
        val neighbors = vectorStore.topK(question.id, k = 1)
        if (neighbors.isEmpty()) return null
        
        val relatedId = neighbors.first().id
        return dataset.entries.find { it.id == relatedId }
    }
}
