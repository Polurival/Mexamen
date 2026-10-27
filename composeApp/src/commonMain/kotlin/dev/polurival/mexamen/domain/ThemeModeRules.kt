package dev.polurival.mexamen.domain

/**
 * Правила темы оформления в одном месте: порядок переключения на Home,
 * итоговое «тёмная или нет» с учётом системной настройки и разбор сохранённого значения.
 */
object ThemeModeRules {
    /** Цикл кнопки на Home: Sistema → Claro → Oscuro → Sistema. */
    fun next(mode: ThemeMode): ThemeMode = when (mode) {
        ThemeMode.SYSTEM -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.SYSTEM
    }

    /** Тёмная ли схема: явный выбор пользователя важнее системной настройки. */
    fun isDark(mode: ThemeMode, systemInDarkTheme: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemInDarkTheme
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    /** Значение из хранилища → режим; пусто или неизвестно (старая/битая запись) → [ThemeMode.SYSTEM]. */
    fun fromStored(raw: String?): ThemeMode =
        ThemeMode.entries.firstOrNull { it.name == raw } ?: ThemeMode.SYSTEM

    /** Строка для хранилища; обратна [fromStored]. */
    fun toStored(mode: ThemeMode): String = mode.name
}
