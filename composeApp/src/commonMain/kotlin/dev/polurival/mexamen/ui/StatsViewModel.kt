package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.domain.StatsAggregator
import dev.polurival.mexamen.domain.StatsSummary
import dev.polurival.mexamen.domain.TopicStatsSummary
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface StatsUiState {
    data object Loading : StatsUiState
    data class Loaded(val summary: StatsSummary, val topics: List<TopicStatsSummary> = emptyList()) : StatsUiState
}

class StatsViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<StatsUiState>(StatsUiState.Loading)
    val state: StateFlow<StatsUiState> = _state.asStateFlow()

    fun load() {
        _state.value = StatsUiState.Loading
        viewModelScope.launch {
            val dataset = repository.dataset()
            val stats = statsStore.readAll()
            _state.value = StatsUiState.Loaded(
                StatsAggregator.summarize(dataset.entries.size, stats),
                StatsAggregator.byTopic(dataset.entries, stats),
            )
        }
    }

    fun resetStats() {
        viewModelScope.launch {
            statsStore.clear()
            load()
        }
    }
}
