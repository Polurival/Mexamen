package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.domain.QuestionSearch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuestionBankUiState {
    data object Loading : QuestionBankUiState

    /**
     * [results] — записи, подходящие под [query] (при пустом запросе — весь гид);
     * [totalCount] — размер гида; [expandedIds] — id вопросов с раскрытым explicación.
     */
    data class Loaded(
        val query: String,
        val results: List<QuestionEntry>,
        val totalCount: Int,
        val expandedIds: Set<Int> = emptySet(),
    ) : QuestionBankUiState
}

/** «Banco de preguntas»: просмотр всего гида с поиском; работает без сети и без статистики. */
class QuestionBankViewModel(
    private val repository: QuestionRepository,
) : ViewModel() {

    private val _state = MutableStateFlow<QuestionBankUiState>(QuestionBankUiState.Loading)
    val state: StateFlow<QuestionBankUiState> = _state.asStateFlow()

    private var entries: List<QuestionEntry> = emptyList()

    /** Повторный вызов (рекомпозиция, возврат на экран) не сбрасывает запрос и раскрытые карточки. */
    fun load() {
        if (_state.value is QuestionBankUiState.Loaded) return
        viewModelScope.launch {
            entries = repository.dataset().entries
            _state.value = QuestionBankUiState.Loaded(query = "", results = entries, totalCount = entries.size)
        }
    }

    fun updateQuery(query: String) {
        val current = _state.value as? QuestionBankUiState.Loaded ?: return
        _state.value = current.copy(query = query, results = QuestionSearch.filter(entries, query))
    }

    fun toggleExpanded(questionId: Int) {
        val current = _state.value as? QuestionBankUiState.Loaded ?: return
        val expanded = current.expandedIds
        _state.value = current.copy(
            expandedIds = if (questionId in expanded) expanded - questionId else expanded + questionId,
        )
    }
}
