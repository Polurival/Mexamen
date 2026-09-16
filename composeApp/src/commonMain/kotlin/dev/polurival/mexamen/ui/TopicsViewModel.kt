package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.Topic
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TopicsUiState(
    val topics: List<Topic> = emptyList(),
    val virtualTopics: List<Topic> = emptyList(),
    val totalCount: Int = 0,
    val isLoading: Boolean = false,
)

class TopicsViewModel(
    private val repository: QuestionRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TopicsUiState())
    val state: StateFlow<TopicsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            val topics = repository.getTopics()
            val virtualTopics = repository.getVirtualTopics()
            val totalCount = repository.getTotalQuestionsCount()
            _state.value = TopicsUiState(topics, virtualTopics, totalCount, isLoading = false)
        }
    }
}
