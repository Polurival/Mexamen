package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.domain.BuildFlashcardsUseCase
import dev.polurival.mexamen.domain.FlashcardGrade
import dev.polurival.mexamen.domain.GetRelatedQuestionUseCase
import dev.polurival.mexamen.domain.RecordAnswerUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface FlashcardUiState {
    data object Loading : FlashcardUiState
    data class Error(val message: String) : FlashcardUiState

    /** Карта [index] из [total]; [revealed] — показан ли ответ. */
    data class Card(
        val index: Int,
        val total: Int,
        val entry: QuestionEntry,
        val revealed: Boolean,
        val relatedEntry: QuestionEntry? = null,
        val attempts: AttemptCounts = AttemptCounts(),
    ) : FlashcardUiState
    data class Done(val counts: Map<FlashcardGrade, Int>) : FlashcardUiState
}

class FlashcardViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
) : ViewModel() {

    private val _state = MutableStateFlow<FlashcardUiState>(FlashcardUiState.Loading)
    val state: StateFlow<FlashcardUiState> = _state.asStateFlow()

    private val recordAnswer = RecordAnswerUseCase(statsStore)
    private val getRelatedQuestion = GetRelatedQuestionUseCase(repository)
    private var cards: List<QuestionEntry> = emptyList()
    private val counts = mutableMapOf<FlashcardGrade, Int>()
    private var started = false
    private var startedTopic: String? = null

    /** Индекс уже оценённой карты: пока следующая не показана, повторный тап не пишет оценку дважды. */
    private var gradedIndex: Int? = null

    /**
     * Старт при входе на экран. ViewModel переживает поворот, а композиция пересоздаётся —
     * повторный вызов для того же [topic] не сбрасывает уже начатую сессию (#6).
     * Явный рестарт («Reintentar») — через [start].
     */
    fun ensureStarted(topic: String? = null) {
        if (started && startedTopic == topic) return
        start(topic)
    }

    fun start(topic: String? = null) {
        started = true
        startedTopic = topic
        gradedIndex = null
        _state.value = FlashcardUiState.Loading
        viewModelScope.launch {
            try {
                val dataset = repository.dataset()
                cards = BuildFlashcardsUseCase().build(dataset, statsStore, topic = topic)
                counts.clear()
                val firstCard = cards.firstOrNull()
                if (firstCard == null) {
                    _state.value = FlashcardUiState.Error("No hay tarjetas")
                    return@launch
                }
                val related = getRelatedQuestion(firstCard)
                val attempts = AttemptCounts.from(statsStore.readAll()[firstCard.id])
                _state.value = FlashcardUiState.Card(
                    0, cards.size, firstCard, revealed = false, relatedEntry = related, attempts = attempts,
                )
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
        if (gradedIndex == current.index) return
        gradedIndex = current.index
        counts[grade] = (counts[grade] ?: 0) + 1
        val recording = viewModelScope.launch { recordAnswer.recordFlashcardGrade(current.entry.id, grade) }
        val next = current.index + 1
        if (next < cards.size) {
            viewModelScope.launch {
                val nextCard = cards[next]
                val related = getRelatedQuestion(nextCard)
                recording.join()
                val attempts = AttemptCounts.from(statsStore.readAll()[nextCard.id])
                _state.value = FlashcardUiState.Card(
                    next, cards.size, nextCard, revealed = false, relatedEntry = related, attempts = attempts,
                )
            }
        } else {
            _state.value = FlashcardUiState.Done(counts.toMap())
        }
    }
}
