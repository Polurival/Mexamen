package dev.polurival.mexamen.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * Платформенная цветовая схема (NFR-6): Android 12+ — динамические цвета,
 * иначе стандартные M3-схемы. Светлая/тёмная — от системной настройки.
 */
@Composable
expect fun platformColorScheme(darkTheme: Boolean): ColorScheme

@Composable
fun MexamenTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = platformColorScheme(darkTheme = isSystemInDarkTheme()),
        content = content,
    )
}
