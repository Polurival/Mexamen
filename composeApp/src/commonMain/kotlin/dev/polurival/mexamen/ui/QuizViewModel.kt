package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.domain.AnsweredQuestion
import dev.polurival.mexamen.domain.BuildQuizUseCase
import dev.polurival.mexamen.domain.QuizQuestion
import dev.polurival.mexamen.domain.QuizResult
import dev.polurival.mexamen.llm.DistractorGenerator
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed interface QuizUiState {
    data class Home(val error: String? = null) : QuizUiState
    data object Loading : QuizUiState

    /** Вопрос [index] из [total]; [selectedIndex] != null — показана подсветка, идёт автопереход. */
    data class Question(
        val index: Int,
        val total: Int,
        val question: QuizQuestion,
        val selectedIndex: Int?,
    ) : QuizUiState

    data class Result(val result: QuizResult) : QuizUiState
}

class QuizViewModel(
    private val repository: QuestionRepository,
    private val distractorGenerator: DistractorGenerator? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Home())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private var quiz: List<QuizQuestion> = emptyList()
    private var answers = mutableListOf<AnsweredQuestion>()
    private var advanceJob: Job? = null

    fun startQuiz() {
        if (_state.value is QuizUiState.Loading) return
        advanceJob?.cancel()
        _state.value = QuizUiState.Loading
        viewModelScope.launch {
            try {
                val dataset = repository.dataset()
                quiz = BuildQuizUseCase(dataset, distractorGenerator = distractorGenerator).build()
                answers = mutableListOf()
                _state.value = QuizUiState.Question(0, quiz.size, quiz.first(), selectedIndex = null)
            } catch (e: Exception) {
                _state.value = QuizUiState.Home(error = e.message ?: "Error al cargar las preguntas")
            }
        }
    }

    fun selectAnswer(optionIndex: Int) {
        val current = _state.value as? QuizUiState.Question ?: return
        if (current.selectedIndex != null) return
        answers += AnsweredQuestion(current.question, optionIndex)
        _state.value = current.copy(selectedIndex = optionIndex)
        advanceJob = viewModelScope.launch {
            delay(ANSWER_REVEAL_MILLIS)
            advance(current.index)
        }
    }

    private fun advance(answeredIndex: Int) {
        val next = answeredIndex + 1
        _state.value = if (next < quiz.size) {
            QuizUiState.Question(next, quiz.size, quiz[next], selectedIndex = null)
        } else {
            QuizUiState.Result(QuizResult(answers.toList()))
        }
    }

    companion object {
        const val ANSWER_REVEAL_MILLIS = 1200L
    }
}
