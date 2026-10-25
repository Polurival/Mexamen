package dev.polurival.mexamen.data

import kotlinx.browser.localStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json

private const val INTERVIEW_STORE_KEY = "mexamen_interview_answers"
private const val STATS_STORE_KEY = "mexamen_question_stats"
private const val THEME_MODE_KEY = "mexamen_theme_mode"

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

/**
 * localStorage + in-memory StateFlow, чтобы подписчики (App и рамка в `main.kt`) видели смену сразу.
 * Один инстанс на страницу — см. [defaultThemeModeStore].
 */
class LocalStorageThemeModeStore : ThemeModeStore {
    private val current = MutableStateFlow(ThemeMode.fromStorage(localStorage.getItem(THEME_MODE_KEY)))
    override val themeMode: Flow<ThemeMode> = current.asStateFlow()

    override suspend fun update(transform: (ThemeMode) -> ThemeMode) {
        val updated = transform(current.value)
        localStorage.setItem(THEME_MODE_KEY, updated.name)
        current.value = updated
    }
}

private val sharedThemeModeStore: ThemeModeStore by lazy { LocalStorageThemeModeStore() }

actual fun defaultThemeModeStore(): ThemeModeStore = sharedThemeModeStore
