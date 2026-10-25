package dev.polurival.mexamen.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.polurival.mexamen.data.ThemeMode
import dev.polurival.mexamen.domain.ThemeModeResolver

/**
 * Платформенная цветовая схема (NFR-6): Android 12+ — динамические цвета,
 * иначе стандартные M3-схемы. Светлая/тёмная — по [ThemeMode] (см. [MexamenTheme]).
 */
@Composable
expect fun platformColorScheme(darkTheme: Boolean): ColorScheme

/** Цвет иконок системных баров под фактическую тему (Android); в wasm — no-op. */
@Composable
expect fun PlatformSystemBarsAppearance(darkTheme: Boolean)

/** [themeMode] — выбор пользователя; [ThemeMode.SYSTEM] следует системной настройке. */
@Composable
fun MexamenTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    val darkTheme = ThemeModeResolver.isDark(themeMode, systemDark = isSystemInDarkTheme())
    PlatformSystemBarsAppearance(darkTheme)
    MaterialTheme(
        colorScheme = platformColorScheme(darkTheme = darkTheme),
        content = content,
    )
}
