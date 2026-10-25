package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.data.Topic
import dev.polurival.mexamen.domain.SessionPoolSelector
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** [mistakesCount] — вопросов с `timesWrong > 0` для пункта «Repasar errores»; 0 — пункт неактивен. */
data class TopicsUiState(
    val topics: List<Topic> = emptyList(),
    val virtualTopics: List<Topic> = emptyList(),
    val totalCount: Int = 0,
    val mistakesCount: Int = 0,
    val isLoading: Boolean = false,
)

class TopicsViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TopicsUiState())
    val state: StateFlow<TopicsUiState> = _state.asStateFlow()

    /**
     * Вызывается при каждом показе списка: счётчик ошибок меняется после квиза/карточек,
     * а ViewModel переживает уход на сессию и возврат. Шиммер — только при первой загрузке.
     */
    fun load() {
        if (_state.value.topics.isEmpty()) _state.value = _state.value.copy(isLoading = true)
        viewModelScope.launch {
            val topics = repository.getTopics()
            val virtualTopics = repository.getVirtualTopics()
            val totalCount = repository.getTotalQuestionsCount()
            val mistakesCount = SessionPoolSelector.mistakes(repository.dataset().entries, statsStore.readAll()).size
            _state.value = TopicsUiState(topics, virtualTopics, totalCount, mistakesCount, isLoading = false)
        }
    }
}
