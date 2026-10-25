package dev.polurival.mexamen.data

import kotlinx.coroutines.flow.Flow

/**
 * Персистентность выбранного режима темы (переживает перезапуск).
 * [themeMode] эмитит текущее значение и все последующие изменения;
 * `update` — атомарный read-modify-write (быстрые повторные тапы не теряются).
 */
interface ThemeModeStore {
    val themeMode: Flow<ThemeMode>
    suspend fun update(transform: (ThemeMode) -> ThemeMode)
}

/** Продакшн-инстанс; платформенное хранилище (DataStore на Android, localStorage в wasm). */
expect fun defaultThemeModeStore(): ThemeModeStore
