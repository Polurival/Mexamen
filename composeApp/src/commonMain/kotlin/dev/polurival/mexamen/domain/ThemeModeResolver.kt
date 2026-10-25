package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.ThemeMode

/** Цикл переключения темы (Sistema → Claro → Oscuro → Sistema), разрешение «тёмная ли» и подписи для UI. */
object ThemeModeResolver {
    fun next(mode: ThemeMode): ThemeMode = when (mode) {
        ThemeMode.SYSTEM -> ThemeMode.LIGHT
        ThemeMode.LIGHT -> ThemeMode.DARK
        ThemeMode.DARK -> ThemeMode.SYSTEM
    }

    /** [systemDark] учитывается только в режиме [ThemeMode.SYSTEM]. */
    fun isDark(mode: ThemeMode, systemDark: Boolean): Boolean = when (mode) {
        ThemeMode.SYSTEM -> systemDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    fun label(mode: ThemeMode): String = when (mode) {
        ThemeMode.SYSTEM -> "Sistema"
        ThemeMode.LIGHT -> "Claro"
        ThemeMode.DARK -> "Oscuro"
    }

    /** contentDescription кнопки переключения: текущий режим и тот, на который переключит тап. */
    fun toggleDescription(mode: ThemeMode): String =
        "Tema: ${label(mode)}. Cambiar a ${label(next(mode))}"
}
