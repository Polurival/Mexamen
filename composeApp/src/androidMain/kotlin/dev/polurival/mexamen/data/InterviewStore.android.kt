package dev.polurival.mexamen.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.polurival.mexamen.AppContext
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json

private val ANSWERS_KEY = stringPreferencesKey("interview_answers_json")

class DataStoreInterviewStore(
    private val dataStore: DataStore<Preferences>,
) : InterviewStore {
    override suspend fun readAllAnswers(): Map<String, String> {
        val raw = dataStore.data.first()[ANSWERS_KEY] ?: return emptyMap()
        return Json.decodeFromString(raw)
    }

    override suspend fun saveAnswer(questionId: String, answer: String) {
        dataStore.edit { prefs ->
            val current = prefs[ANSWERS_KEY]?.let { Json.decodeFromString<Map<String, String>>(it) } ?: emptyMap()
            val updated = current.toMutableMap().apply { put(questionId, answer) }
            prefs[ANSWERS_KEY] = Json.encodeToString(updated)
        }
    }
}

private val dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(
        produceFile = { AppContext.instance.preferencesDataStoreFile("interview_answers") },
    )
}

actual fun defaultInterviewStore(): InterviewStore = DataStoreInterviewStore(dataStore)
