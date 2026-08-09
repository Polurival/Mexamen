package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.InterviewQuestion
import dev.polurival.mexamen.data.InterviewStore
import dev.polurival.mexamen.data.interviewQuestions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InterviewUiState(
    val questions: List<InterviewQuestion> = interviewQuestions,
    val answers: Map<String, String> = emptyMap(),
    val isLoading: Boolean = false
)

class InterviewViewModel(
    private val interviewStore: InterviewStore
) : ViewModel() {

    private val _state = MutableStateFlow(InterviewUiState())
    val state: StateFlow<InterviewUiState> = _state.asStateFlow()

    init {
        loadAnswers()
    }

    private fun loadAnswers() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            val answers = interviewStore.readAllAnswers()
            _state.update { it.copy(answers = answers, isLoading = false) }
        }
    }

    fun updateAnswer(questionId: String, answer: String) {
        _state.update {
            val newAnswers = it.answers.toMutableMap()
            newAnswers[questionId] = answer
            it.copy(answers = newAnswers)
        }
    }

    fun saveAnswer(questionId: String) {
        val answer = _state.value.answers[questionId] ?: return
        viewModelScope.launch {
            interviewStore.saveAnswer(questionId, answer)
        }
    }
}
