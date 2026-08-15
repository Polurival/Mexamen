package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.Legend
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.allLegends
import dev.polurival.mexamen.domain.AnsweredQuestion
import dev.polurival.mexamen.domain.DistractorSource
import dev.polurival.mexamen.domain.QuizQuestion
import dev.polurival.mexamen.domain.QuizResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

enum class AudioState {
    STOPPED, PLAYING, PAUSED
}

data class LegendUiState(
    val legend: Legend? = null,
    val userAnswers: Map<Int, Int> = emptyMap(), // questionId to optionIndex
    val timeLeftSeconds: Int = 600, // 10 minutes
    val isFinished: Boolean = false,
    val audioState: AudioState = AudioState.STOPPED
)

class LegendViewModel(private val audioPlayer: AudioPlayer) : ViewModel() {

    private val _state = MutableStateFlow(LegendUiState())
    val state: StateFlow<LegendUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun selectLegend(legendId: String) {
        audioPlayer.stop()
        val legend = allLegends.find { it.id == legendId }
        _state.update { it.copy(legend = legend, userAnswers = emptyMap(), timeLeftSeconds = 600, isFinished = false, audioState = AudioState.STOPPED) }
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
        audioPlayer.stop()
        _state.update { it.copy(isFinished = true, audioState = AudioState.STOPPED) }
        timerJob?.cancel()
    }

    fun playAudio() {
        val legend = _state.value.legend ?: return
        if (_state.value.audioState == AudioState.PAUSED) {
            audioPlayer.pause() // resumes
        } else {
            audioPlayer.play(legend.id)
        }
        _state.update { it.copy(audioState = AudioState.PLAYING) }
    }

    fun pauseAudio() {
        audioPlayer.pause()
        _state.update { it.copy(audioState = AudioState.PAUSED) }
    }

    fun stopAudio() {
        audioPlayer.stop()
        _state.update { it.copy(audioState = AudioState.STOPPED) }
    }

    fun getResult(): QuizResult {
        val legend = _state.value.legend ?: return QuizResult(emptyList())
        val answers = legend.questions.map { question ->
            val chosenIndex = _state.value.userAnswers[question.id] ?: -1
            AnsweredQuestion(
                question = QuizQuestion(
                    entry = QuestionEntry(
                        id = question.id,
                        section = "Legend",
                        question = question.question,
                        answer = if (question.correctAnswerIndex in question.options.indices)
                            question.options[question.correctAnswerIndex]
                        else "",
                    ),
                    options = question.options,
                    correctIndex = question.correctAnswerIndex,
                    source = DistractorSource.FALLBACK
                ),
                chosenIndex = chosenIndex
            )
        }
        return QuizResult(answers)
    }

    override fun onCleared() {
        super.onCleared()
        audioPlayer.release()
        timerJob?.cancel()
    }
}
