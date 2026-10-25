package dev.polurival.mexamen.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
actual fun platformColorScheme(darkTheme: Boolean): ColorScheme {
    return if (darkTheme) darkColorScheme() else lightColorScheme()
}

/** В браузере системных баров нет. */
@Composable
actual fun PlatformSystemBarsAppearance(darkTheme: Boolean) = Unit
