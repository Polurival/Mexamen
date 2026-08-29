package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.domain.BuildFlashcardsUseCase
import dev.polurival.mexamen.domain.FlashcardGrade
import dev.polurival.mexamen.domain.RecordAnswerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FlashcardUiState {
    data object Loading : FlashcardUiState
    data class Error(val message: String) : FlashcardUiState

    /** Карта [index] из [total]; [revealed] — показан ли ответ. */
    data class Card(val index: Int, val total: Int, val entry: QuestionEntry, val revealed: Boolean) : FlashcardUiState
    data class Done(val counts: Map<FlashcardGrade, Int>) : FlashcardUiState
}

class FlashcardViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<FlashcardUiState>(FlashcardUiState.Loading)
    val state: StateFlow<FlashcardUiState> = _state.asStateFlow()

    private val recordAnswer = RecordAnswerUseCase(statsStore)
    private var cards: List<QuestionEntry> = emptyList()
    private val counts = mutableMapOf<FlashcardGrade, Int>()

    fun start(topic: String? = null) {
        _state.value = FlashcardUiState.Loading
        viewModelScope.launch {
            try {
                val dataset = repository.dataset()
                cards = BuildFlashcardsUseCase().build(dataset, statsStore, topic = topic)
                counts.clear()
                _state.value = FlashcardUiState.Card(0, cards.size, cards.first(), revealed = false)
            } catch (e: Exception) {
                _state.value = FlashcardUiState.Error(e.message ?: "Error al cargar las tarjetas")
            }
        }
    }

    fun flip() {
        val current = _state.value as? FlashcardUiState.Card ?: return
        if (current.revealed) return
        _state.value = current.copy(revealed = true)
    }

    fun grade(grade: FlashcardGrade) {
        val current = _state.value as? FlashcardUiState.Card ?: return
        if (!current.revealed) return
        counts[grade] = (counts[grade] ?: 0) + 1
        viewModelScope.launch { recordAnswer.recordFlashcardGrade(current.entry.id, grade) }
        val next = current.index + 1
        _state.value = if (next < cards.size) {
            FlashcardUiState.Card(next, cards.size, cards[next], revealed = false)
        } else {
            FlashcardUiState.Done(counts.toMap())
        }
    }
}
