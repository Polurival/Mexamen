package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.ThemeMode
import dev.polurival.mexamen.data.ThemeModeStore
import dev.polurival.mexamen.domain.ThemeModeResolver
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Выбранный режим темы на уровне всего приложения. Источник правды — [ThemeModeStore];
 * до первого чтения хранилища действует [ThemeMode.SYSTEM].
 */
class ThemeViewModel(
    private val themeModeStore: ThemeModeStore,
) : ViewModel() {

    val state: StateFlow<ThemeMode> = themeModeStore.themeMode
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /** Следующий режим считается внутри атомарного update — от сохранённого значения, а не от UI. */
    fun cycle() {
        viewModelScope.launch { themeModeStore.update(ThemeModeResolver::next) }
    }
}
