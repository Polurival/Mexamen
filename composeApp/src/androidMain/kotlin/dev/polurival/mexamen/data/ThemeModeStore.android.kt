package dev.polurival.mexamen.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStoreFile
import dev.polurival.mexamen.AppContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")

class DataStoreThemeModeStore(
    private val dataStore: DataStore<Preferences>,
) : ThemeModeStore {
    override val themeMode: Flow<ThemeMode> =
        dataStore.data.map { ThemeMode.fromStorage(it[THEME_MODE_KEY]) }.distinctUntilChanged()

    override suspend fun update(transform: (ThemeMode) -> ThemeMode) {
        dataStore.edit { prefs ->
            prefs[THEME_MODE_KEY] = transform(ThemeMode.fromStorage(prefs[THEME_MODE_KEY])).name
        }
    }
}

private val dataStore: DataStore<Preferences> by lazy {
    PreferenceDataStoreFactory.create(
        produceFile = { AppContext.instance.preferencesDataStoreFile("theme_mode") },
    )
}

actual fun defaultThemeModeStore(): ThemeModeStore = DataStoreThemeModeStore(dataStore)
