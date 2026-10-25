package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.ThemeMode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeModeResolverTest {

    @Test
    fun nextCyclesSystemLightDarkAndBackToSystem() {
        assertEquals(ThemeMode.LIGHT, ThemeModeResolver.next(ThemeMode.SYSTEM))
        assertEquals(ThemeMode.DARK, ThemeModeResolver.next(ThemeMode.LIGHT))
        assertEquals(ThemeMode.SYSTEM, ThemeModeResolver.next(ThemeMode.DARK))
    }

    @Test
    fun systemModeFollowsSystemSetting() {
        assertTrue(ThemeModeResolver.isDark(ThemeMode.SYSTEM, systemDark = true))
        assertFalse(ThemeModeResolver.isDark(ThemeMode.SYSTEM, systemDark = false))
    }

    @Test
    fun explicitModesIgnoreSystemSetting() {
        assertFalse(ThemeModeResolver.isDark(ThemeMode.LIGHT, systemDark = true))
        assertTrue(ThemeModeResolver.isDark(ThemeMode.DARK, systemDark = false))
    }

    @Test
    fun labelsAreSpanish() {
        assertEquals("Sistema", ThemeModeResolver.label(ThemeMode.SYSTEM))
        assertEquals("Claro", ThemeModeResolver.label(ThemeMode.LIGHT))
        assertEquals("Oscuro", ThemeModeResolver.label(ThemeMode.DARK))
    }

    @Test
    fun toggleDescriptionNamesCurrentAndNextMode() {
        assertEquals("Tema: Sistema. Cambiar a Claro", ThemeModeResolver.toggleDescription(ThemeMode.SYSTEM))
        assertEquals("Tema: Oscuro. Cambiar a Sistema", ThemeModeResolver.toggleDescription(ThemeMode.DARK))
    }
}
