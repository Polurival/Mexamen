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

/** [mistakes] — пункт «Repasar errores»: число вопросов с `timesWrong > 0`; при 0 пункт неактивен. */
data class TopicsUiState(
    val topics: List<Topic> = emptyList(),
    val virtualTopics: List<Topic> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
    val mistakes: Topic = Topic(SessionPoolSelector.TOPIC_MISTAKES, 0),
) {
    val hasMistakes: Boolean get() = mistakes.questionCount > 0
}

class TopicsViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
) : ViewModel() {

    private val _state = MutableStateFlow(TopicsUiState(isLoading = true))
    val state: StateFlow<TopicsUiState> = _state.asStateFlow()

    /**
     * Вызывается при каждом показе списка: число ошибок меняется после сессии, а VM переживает
     * возврат с квиза/карточек. Шиммер — только при первой загрузке.
     */
    fun load() {
        viewModelScope.launch {
            val topics = repository.getTopics()
            val virtualTopics = repository.getVirtualTopics()
            val totalCount = repository.getTotalQuestionsCount()
            val mistakesCount = SessionPoolSelector.mistakes(repository.dataset().entries, statsStore.readAll()).size
            _state.value = TopicsUiState(
                topics = topics,
                virtualTopics = virtualTopics,
                totalCount = totalCount,
                isLoading = false,
                mistakes = Topic(SessionPoolSelector.TOPIC_MISTAKES, mistakesCount),
            )
        }
    }
}
