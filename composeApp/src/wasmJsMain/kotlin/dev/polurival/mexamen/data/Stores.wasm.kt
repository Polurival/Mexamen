package dev.polurival.mexamen.data

import kotlinx.browser.localStorage
import kotlinx.serialization.json.Json

private const val INTERVIEW_STORE_KEY = "mexamen_interview_answers"
private const val STATS_STORE_KEY = "mexamen_question_stats"

private val json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

class LocalStorageInterviewStore : InterviewStore {
    override suspend fun readAllAnswers(): Map<String, String> {
        val data = localStorage.getItem(INTERVIEW_STORE_KEY) ?: return emptyMap()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun saveAnswer(questionId: String, answer: String) {
        val current = readAllAnswers().toMutableMap()
        current[questionId] = answer
        localStorage.setItem(INTERVIEW_STORE_KEY, json.encodeToString(current))
    }
}

actual fun defaultInterviewStore(): InterviewStore = LocalStorageInterviewStore()

class LocalStorageQuestionStatsStore : QuestionStatsStore {
    override suspend fun readAll(): Map<Int, QuestionStats> {
        val data = localStorage.getItem(STATS_STORE_KEY) ?: return emptyMap()
        return try {
            json.decodeFromString(data)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        val current = readAll()
        val updated = transform(current)
        localStorage.setItem(STATS_STORE_KEY, json.encodeToString(updated))
    }

    override suspend fun clear() {
        localStorage.removeItem(STATS_STORE_KEY)
    }
}

actual fun defaultQuestionStatsStore(): QuestionStatsStore = LocalStorageQuestionStatsStore()
