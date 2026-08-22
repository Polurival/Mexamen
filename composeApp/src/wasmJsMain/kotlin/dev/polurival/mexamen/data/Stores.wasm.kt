package dev.polurival.mexamen.data

class InMemoryInterviewStore : InterviewStore {
    private val answers = mutableMapOf<String, String>()
    override suspend fun readAllAnswers(): Map<String, String> = answers
    override suspend fun saveAnswer(questionId: String, answer: String) {
        answers[questionId] = answer
    }
}

actual fun defaultInterviewStore(): InterviewStore = InMemoryInterviewStore()

class InMemoryQuestionStatsStore : QuestionStatsStore {
    private var stats = mapOf<Int, QuestionStats>()
    
    override suspend fun readAll(): Map<Int, QuestionStats> = stats

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        stats = transform(stats)
    }

    override suspend fun clear() {
        stats = emptyMap()
    }
}

actual fun defaultQuestionStatsStore(): QuestionStatsStore = InMemoryQuestionStatsStore()
