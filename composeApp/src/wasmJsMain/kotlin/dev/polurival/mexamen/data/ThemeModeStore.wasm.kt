package dev.polurival.mexamen.data

import kotlinx.browser.localStorage

private const val THEME_MODE_KEY = "mexamen_theme_mode"

class LocalStorageThemeModeStore : ThemeModeStore {
    override suspend fun read(): String? = localStorage.getItem(THEME_MODE_KEY)

    override suspend fun write(value: String) {
        localStorage.setItem(THEME_MODE_KEY, value)
    }
}

actual fun defaultThemeModeStore(): ThemeModeStore = LocalStorageThemeModeStore()
