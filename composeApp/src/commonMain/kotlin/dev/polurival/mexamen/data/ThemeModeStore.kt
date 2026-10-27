package dev.polurival.mexamen.data

/**
 * Персистентность выбранной темы оформления между запусками.
 * Хранит сырую строку: data не знает про доменный `ThemeMode`, разбор — в `domain/ThemeModeRules`.
 */
interface ThemeModeStore {
    /** Сохранённое значение или `null`, если пользователь ещё не выбирал тему. */
    suspend fun read(): String?
    suspend fun write(value: String)
}

/** Продакшн-инстанс; платформенное хранилище (DataStore на Android, localStorage в wasm). */
expect fun defaultThemeModeStore(): ThemeModeStore
