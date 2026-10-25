package dev.polurival.mexamen.ui

import dev.polurival.mexamen.domain.ThemeMode
import dev.polurival.mexamen.testing.FakeThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** #23: выбор темы на Home — чтение сохранённого режима, цикл и запись между запусками. */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun startsWithSystemWhenNothingStored() = runTest {
        val vm = ThemeViewModel(FakeThemeModeStore())
        assertIs<ThemeUiState.Loading>(vm.state.value)
        assertEquals(ThemeMode.SYSTEM, vm.state.value.mode)

        runCurrent()

        assertEquals(ThemeUiState.Loaded(ThemeMode.SYSTEM), vm.state.value)
    }

    @Test
    fun restoresStoredModeOnStart() = runTest {
        val vm = ThemeViewModel(FakeThemeModeStore(value = "DARK"))
        runCurrent()

        assertEquals(ThemeUiState.Loaded(ThemeMode.DARK), vm.state.value)
    }

    @Test
    fun cycleModeGoesSystemLightDarkSystemAndPersistsEachChoice() = runTest {
        val store = FakeThemeModeStore()
        val vm = ThemeViewModel(store)
        runCurrent()

        vm.cycleMode()
        runCurrent()
        assertEquals(ThemeMode.LIGHT, vm.state.value.mode)
        assertEquals("LIGHT", store.value)

        vm.cycleMode()
        runCurrent()
        assertEquals(ThemeMode.DARK, vm.state.value.mode)
        assertEquals("DARK", store.value)

        vm.cycleMode()
        runCurrent()
        assertEquals(ThemeMode.SYSTEM, vm.state.value.mode)
        assertEquals("SYSTEM", store.value)
    }

    @Test
    fun choiceSurvivesRestartWithSameStore() = runTest {
        val store = FakeThemeModeStore()
        val first = ThemeViewModel(store)
        runCurrent()
        first.cycleMode()
        first.cycleMode()
        runCurrent()

        val second = ThemeViewModel(store)
        runCurrent()

        assertEquals(ThemeUiState.Loaded(ThemeMode.DARK), second.state.value)
    }

    @Test
    fun cycleIsIgnoredWhileStoredModeIsStillLoading() = runTest {
        val store = FakeThemeModeStore(value = "DARK", gatedRead = true)
        val vm = ThemeViewModel(store)
        runCurrent()

        vm.cycleMode()
        runCurrent()
        assertIs<ThemeUiState.Loading>(vm.state.value)
        assertEquals(emptyList(), store.writes)

        store.openRead()
        runCurrent()

        // Прочитанный выбор не затёрт «ранним» нажатием и не перезаписан в store
        assertEquals(ThemeUiState.Loaded(ThemeMode.DARK), vm.state.value)
        assertEquals(emptyList(), store.writes)
    }

    @Test
    fun stateChangesImmediatelyWhileWriteIsPendingAndWritesKeepOrder() = runTest {
        val store = FakeThemeModeStore(gatedWrite = true)
        val vm = ThemeViewModel(store)
        runCurrent()

        vm.cycleMode()
        runCurrent()
        // Запись «зависла», но UI уже показывает новый режим, и следующее нажатие не блокируется
        assertEquals(ThemeMode.LIGHT, vm.state.value.mode)
        assertNull(store.value)

        vm.cycleMode()
        runCurrent()
        assertEquals(ThemeMode.DARK, vm.state.value.mode)

        store.openWrite()
        runCurrent()

        assertEquals(listOf("LIGHT", "DARK"), store.writes)
        assertEquals("DARK", store.value)
    }
}
