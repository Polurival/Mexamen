package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.domain.QuestionSearch
import dev.polurival.mexamen.domain.SearchableQuestion
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed interface QuestionBankUiState {
    data object Loading : QuestionBankUiState

    /** [results] — уже отфильтрованные по [query]; [expandedIds] — вопросы с раскрытой explicación. */
    data class Loaded(
        val query: String,
        val results: List<QuestionEntry>,
        val totalCount: Int,
        val expandedIds: Set<Int> = emptySet(),
    ) : QuestionBankUiState
}

/** «Banco de preguntas»: весь гид без статистики, только чтение датасета и поиск через [QuestionSearch]. */
class QuestionBankViewModel(
    private val repository: QuestionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<QuestionBankUiState>(QuestionBankUiState.Loading)
    val state: StateFlow<QuestionBankUiState> = _state.asStateFlow()

    private var index: List<SearchableQuestion> = emptyList()

    /**
     * Повторный вызов в пределах той же записи навигации (пересоздание composable) не сбрасывает
     * запрос и раскрытые карточки. Индекс поиска нормализуется один раз здесь.
     */
    fun load() {
        if (_state.value is QuestionBankUiState.Loaded) return
        viewModelScope.launch {
            val entries = repository.dataset().entries
            index = QuestionSearch.index(entries)
            _state.value = QuestionBankUiState.Loaded(query = "", results = entries, totalCount = entries.size)
        }
    }

    fun updateQuery(query: String) {
        _state.update { current ->
            if (current !is QuestionBankUiState.Loaded) current
            else current.copy(query = query, results = QuestionSearch.filter(index, query))
        }
    }

    fun toggleExpanded(id: Int) {
        _state.update { current ->
            if (current !is QuestionBankUiState.Loaded) current
            else current.copy(
                expandedIds = if (id in current.expandedIds) current.expandedIds - id else current.expandedIds + id,
            )
        }
    }
}
