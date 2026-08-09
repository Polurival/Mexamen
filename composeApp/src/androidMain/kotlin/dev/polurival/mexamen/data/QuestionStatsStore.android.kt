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

private val STATS_KEY = stringPreferencesKey("question_stats_json")

/**
 * DataStore хранит один JSON-блоб (вся карта id->[QuestionStats]) под одним ключом —
 * проще редких точечных ключей на вопрос, и совместимо с тем, как questions.json
 * парсится целиком в [QuestionRepository].
 */
class DataStoreQuestionStatsStore(
    private val dataStore: DataStore<Preferences>,
) : QuestionStatsStore {
    override suspend fun readAll(): Map<Int, QuestionStats> {
        val raw = dataStore.data.first()[STATS_KEY] ?: return emptyMap()
        return decode(raw)
    }

    override suspend fun update(transform: (Map<Int, QuestionStats>) -> Map<Int, QuestionStats>) {
        dataStore.edit { prefs ->
            val current = prefs[STATS_KEY]?.let(::decode) ?: emptyMap()
            prefs[STATS_KEY] = encode(transform(current))
        }
    }

    override suspend fun clear() {
        dataStore.edit { it.remove(STATS_KEY) }
    }

    companion object {
        private val json = Json { ignoreUnknownKeys = true }

        private fun decode(raw: String): Map<Int, QuestionStats> =
            json.decodeFromString(raw)

        private fun encode(stats: Map<Int, QuestionStats>): String =
            json.encodeToString(stats)
    }
}

private val dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(
        produceFile = { AppContext.instance.preferencesDataStoreFile("question_stats") },
    )
}

actual fun defaultQuestionStatsStore(): QuestionStatsStore = DataStoreQuestionStatsStore(dataStore)
