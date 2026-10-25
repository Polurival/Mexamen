package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.ThemeMode
import dev.polurival.mexamen.data.ThemeModeStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals

private class FakeThemeModeStore(initial: ThemeMode = ThemeMode.SYSTEM) : ThemeModeStore {
    val saved = MutableStateFlow(initial)
    override val themeMode: Flow<ThemeMode> = saved

    override suspend fun update(transform: (ThemeMode) -> ThemeMode) {
        saved.value = transform(saved.value)
    }
}

/** Выбор темы (issue #23): чтение сохранённого режима и цикл Sistema → Claro → Oscuro с записью в store. */
@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private fun TestScope.useTestMain() {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun stateReflectsPersistedModeAfterRestart() = runTest {
        useTestMain()
        val vm = ThemeViewModel(FakeThemeModeStore(initial = ThemeMode.DARK))
        advanceUntilIdle()
        assertEquals(ThemeMode.DARK, vm.state.value)
    }

    @Test
    fun cyclePersistsNextModeAndWrapsAround() = runTest {
        useTestMain()
        val store = FakeThemeModeStore()
        val vm = ThemeViewModel(store)
        advanceUntilIdle()

        vm.cycle()
        advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, store.saved.value)
        assertEquals(ThemeMode.LIGHT, vm.state.value)

        vm.cycle()
        vm.cycle()
        advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, store.saved.value)
        assertEquals(ThemeMode.SYSTEM, vm.state.value)
    }
}
