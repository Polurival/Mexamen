package dev.polurival.mexamen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ThemeModeRulesTest {

    @Test
    fun nextCyclesSystemLightDarkAndBack() {
        assertEquals(ThemeMode.LIGHT, ThemeModeRules.next(ThemeMode.SYSTEM))
        assertEquals(ThemeMode.DARK, ThemeModeRules.next(ThemeMode.LIGHT))
        assertEquals(ThemeMode.SYSTEM, ThemeModeRules.next(ThemeMode.DARK))
    }

    @Test
    fun nextVisitsEveryModeOnceBeforeRepeating() {
        val visited = generateSequence(ThemeMode.SYSTEM, ThemeModeRules::next).take(ThemeMode.entries.size).toSet()
        assertEquals(ThemeMode.entries.toSet(), visited)
    }

    @Test
    fun systemModeFollowsSystemSetting() {
        assertTrue(ThemeModeRules.isDark(ThemeMode.SYSTEM, systemInDarkTheme = true))
        assertFalse(ThemeModeRules.isDark(ThemeMode.SYSTEM, systemInDarkTheme = false))
    }

    @Test
    fun explicitModeOverridesSystemSetting() {
        assertFalse(ThemeModeRules.isDark(ThemeMode.LIGHT, systemInDarkTheme = true))
        assertTrue(ThemeModeRules.isDark(ThemeMode.DARK, systemInDarkTheme = false))
    }

    @Test
    fun storedValueRoundTripsForEveryMode() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeModeRules.fromStored(ThemeModeRules.toStored(mode)))
        }
    }

    @Test
    fun missingOrUnknownStoredValueFallsBackToSystem() {
        assertEquals(ThemeMode.SYSTEM, ThemeModeRules.fromStored(null))
        assertEquals(ThemeMode.SYSTEM, ThemeModeRules.fromStored(""))
        assertEquals(ThemeMode.SYSTEM, ThemeModeRules.fromStored("dark"))
    }
}
