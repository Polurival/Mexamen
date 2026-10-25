package dev.polurival.mexamen.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ThemeModeTest {

    @Test
    fun fromStorageRoundTripsEveryModeByName() {
        ThemeMode.entries.forEach { assertEquals(it, ThemeMode.fromStorage(it.name)) }
    }

    @Test
    fun fromStorageFallsBackToSystemForMissingOrUnknownValue() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("PURPLE"))
    }
}
