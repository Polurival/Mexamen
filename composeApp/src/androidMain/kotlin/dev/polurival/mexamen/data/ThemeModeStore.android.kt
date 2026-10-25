package dev.polurival.mexamen.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.polurival.mexamen.AppContext
import kotlinx.coroutines.flow.first

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

class DataStoreThemeModeStore(
    private val dataStore: DataStore<Preferences>,
) : ThemeModeStore {
    override suspend fun read(): String? = dataStore.data.first()[THEME_MODE_KEY]

    override suspend fun write(value: String) {
        dataStore.edit { it[THEME_MODE_KEY] = value }
    }
}

private val dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(
        produceFile = { AppContext.instance.preferencesDataStoreFile("theme_mode") },
    )
}

actual fun defaultThemeModeStore(): ThemeModeStore = DataStoreThemeModeStore(dataStore)
