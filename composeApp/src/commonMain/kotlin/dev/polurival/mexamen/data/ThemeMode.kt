package dev.polurival.mexamen.data

/** Выбранный пользователем режим темы оформления; хранится по [name]. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Разбор сохранённого значения; отсутствующее или неизвестное → [SYSTEM]. */
        fun fromStorage(raw: String?): ThemeMode = entries.firstOrNull { it.name == raw } ?: SYSTEM
    }
}
