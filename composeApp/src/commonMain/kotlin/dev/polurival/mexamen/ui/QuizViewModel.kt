package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.domain.AnsweredQuestion
import dev.polurival.mexamen.domain.BuildQuizUseCase
import dev.polurival.mexamen.domain.QuizQuestion
import dev.polurival.mexamen.domain.QuizResult
import dev.polurival.mexamen.domain.RecordAnswerUseCase
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
        val timeLeftSeconds: Int = 600,
    ) : QuizUiState

    data class Result(val result: QuizResult) : QuizUiState
}

class QuizViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
    private val distractorGenerator: DistractorGenerator? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Home())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private val recordAnswer = RecordAnswerUseCase(statsStore)
    private var quiz: List<QuizQuestion> = emptyList()
    private var answers = mutableListOf<AnsweredQuestion>()
    private var advanceJob: Job? = null
    private var timerJob: Job? = null

    fun startQuiz(topic: String? = null) {
        if (_state.value is QuizUiState.Loading) return
        advanceJob?.cancel()
        timerJob?.cancel()
        _state.value = QuizUiState.Loading
        viewModelScope.launch {
            try {
                val dataset = repository.dataset()
                quiz = BuildQuizUseCase(
                    dataset,
                    statsStore = statsStore,
                    distractorGenerator = distractorGenerator
                ).build(topic = topic)
                answers = mutableListOf()
                _state.value = QuizUiState.Question(0, quiz.size, quiz.first(), selectedIndex = null, timeLeftSeconds = 600)
                startTimer()
            } catch (e: Exception) {
                _state.value = QuizUiState.Home(error = e.message ?: "Error al cargar las preguntas")
            }
        }
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (true) {
                delay(1000L)
                val current = _state.value as? QuizUiState.Question ?: break
                if (current.timeLeftSeconds > 0) {
                    _state.value = current.copy(timeLeftSeconds = current.timeLeftSeconds - 1)
                } else {
                    finishQuiz()
                    break
                }
            }
        }
    }

    private fun finishQuiz() {
        timerJob?.cancel()
        advanceJob?.cancel()
        // If timed out, we might have fewer answers than quiz size.
        // QuizResult handles a list of AnsweredQuestion.
        _state.value = QuizUiState.Result(QuizResult(answers.toList()))
    }

    fun selectAnswer(optionIndex: Int) {
        val current = _state.value as? QuizUiState.Question ?: return
        if (current.selectedIndex != null) return
        val answered = AnsweredQuestion(current.question, optionIndex)
        answers += answered
        _state.value = current.copy(selectedIndex = optionIndex)
        viewModelScope.launch { recordAnswer.recordQuizAnswer(current.question.entry.id, answered.isCorrect) }
        advanceJob = viewModelScope.launch {
            delay(ANSWER_REVEAL_MILLIS)
            advance(current.index)
        }
    }

    private fun advance(answeredIndex: Int) {
        val next = answeredIndex + 1
        val current = _state.value as? QuizUiState.Question
        _state.value = if (next < quiz.size) {
            QuizUiState.Question(
                index = next,
                total = quiz.size,
                question = quiz[next],
                selectedIndex = null,
                timeLeftSeconds = current?.timeLeftSeconds ?: 600
            )
        } else {
            timerJob?.cancel()
            QuizUiState.Result(QuizResult(answers.toList()))
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
        advanceJob?.cancel()
    }

    companion object {
        const val ANSWER_REVEAL_MILLIS = 1200L
    }
}
