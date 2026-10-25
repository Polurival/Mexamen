package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.ThemeModeStore
import dev.polurival.mexamen.domain.ThemeMode
import dev.polurival.mexamen.domain.ThemeModeRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface ThemeUiState {
    /** Режим, который применяется к `MexamenTheme` и показывается на кнопке Home. */
    val mode: ThemeMode

    /** Сохранённый выбор ещё читается — показываем системную тему, переключение игнорируется. */
    data object Loading : ThemeUiState {
        override val mode: ThemeMode = ThemeMode.SYSTEM
    }

    data class Loaded(override val mode: ThemeMode) : ThemeUiState
}

/**
 * Выбор темы оформления уровня приложения (живёт над NavDisplay, один на всё приложение).
 * Сохранённый режим читается сразу при создании, а не из `LaunchedEffect`: иначе повторный
 * вызов при пересоздании Activity на миг сбрасывал бы тему в Loading.
 */
class ThemeViewModel(
    private val store: ThemeModeStore,
) : ViewModel() {
    private val _state = MutableStateFlow<ThemeUiState>(ThemeUiState.Loading)
    val state: StateFlow<ThemeUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = ThemeUiState.Loaded(ThemeModeRules.fromStored(store.read()))
        }
    }

    /**
     * Следующий режим цикла. UI меняется сразу, запись — асинхронно. До окончания чтения
     * нажатие игнорируется, чтобы прочитанное значение не затёрло свежий выбор пользователя.
     */
    fun cycleMode() {
        val current = _state.value as? ThemeUiState.Loaded ?: return
        val next = ThemeModeRules.next(current.mode)
        _state.value = ThemeUiState.Loaded(next)
        viewModelScope.launch { store.write(ThemeModeRules.toStored(next)) }
    }
}
