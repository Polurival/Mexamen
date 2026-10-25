package dev.polurival.mexamen.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import dev.polurival.mexamen.domain.ThemeMode
import dev.polurival.mexamen.domain.ThemeModeRules

/**
 * Платформенная цветовая схема (NFR-6): Android 12+ — динамические цвета,
 * иначе стандартные M3-схемы.
 */
@Composable
expect fun platformColorScheme(darkTheme: Boolean): ColorScheme

/**
 * Тема приложения. Светлая/тёмная — по выбору пользователя ([themeMode]);
 * [ThemeMode.SYSTEM] (по умолчанию) следует системной настройке.
 */
@Composable
fun MexamenTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = platformColorScheme(
            darkTheme = ThemeModeRules.isDark(themeMode, systemInDarkTheme = isSystemInDarkTheme()),
        ),
        content = content,
    )
}
