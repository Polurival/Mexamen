package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.Legend
import dev.polurival.mexamen.data.allLegends
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

data class LegendUiState(
    val legend: Legend? = null,
    val userAnswers: Map<Int, Int> = emptyMap(), // questionId to optionIndex
    val timeLeftSeconds: Int = 600, // 10 minutes
    val isFinished: Boolean = false
)

class LegendViewModel : ViewModel() {

    private val _state = MutableStateFlow(LegendUiState())
    val state: StateFlow<LegendUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun selectLegend(legendId: String) {
        val legend = allLegends.find { it.id == legendId }
        _state.update { it.copy(legend = legend, userAnswers = emptyMap(), timeLeftSeconds = 600, isFinished = false) }
        startTimer()
    }

    private fun startTimer() {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            while (_state.value.timeLeftSeconds > 0 && !_state.value.isFinished) {
                delay(1.seconds)
                _state.update { it.copy(timeLeftSeconds = it.timeLeftSeconds - 1) }
            }
            if (_state.value.timeLeftSeconds <= 0 && !_state.value.isFinished) {
                finishLegend()
            }
        }
    }

    fun selectAnswer(questionId: Int, answerIndex: Int) {
        _state.update {
            val newAnswers = it.userAnswers.toMutableMap()
            newAnswers[questionId] = answerIndex
            it.copy(userAnswers = newAnswers)
        }
    }

    fun finishLegend() {
        _state.update { it.copy(isFinished = true) }
        timerJob?.cancel()
    }

    fun getResult(): Pair<Int, Int> {
        val legend = _state.value.legend ?: return 0 to 0
        var correctCount = 0
        legend.questions.forEach { question ->
            if (_state.value.userAnswers[question.id] == question.correctAnswerIndex) {
                correctCount++
            }
        }
        return correctCount to legend.questions.size
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
