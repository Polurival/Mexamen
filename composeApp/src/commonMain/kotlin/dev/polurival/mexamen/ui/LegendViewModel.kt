package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.Legend
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.allLegends
import dev.polurival.mexamen.domain.AnsweredQuestion
import dev.polurival.mexamen.domain.DistractorSource
import dev.polurival.mexamen.domain.PassCriteria
import dev.polurival.mexamen.domain.QuizQuestion
import dev.polurival.mexamen.domain.QuizResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.jetbrains.compose.resources.ExperimentalResourceApi
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import mexamen.composeapp.generated.resources.Res
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@Serializable
data class WordTiming(
    val start: Int,
    val end: Int,
    val time_ms: Int
)

enum class AudioState {
    STOPPED, PLAYING, PAUSED
}

data class LegendUiState(
    val legend: Legend? = null,
    val userAnswers: Map<Int, Int> = emptyMap(), // questionId to optionIndex
    val timeLeftSeconds: Int = LegendViewModel.TIME_LIMIT_SECONDS,
    val isFinished: Boolean = false,
    val audioState: AudioState = AudioState.STOPPED,
    val currentWordRange: IntRange? = null,
    val timings: List<WordTiming> = emptyList()
)

class LegendViewModel(private val audioPlayer: AudioPlayer) : ViewModel() {

    private val _state = MutableStateFlow(LegendUiState())
    val state: StateFlow<LegendUiState> = _state.asStateFlow()

    private var timerJob: Job? = null

    fun selectLegend(legendId: String) {
        val legend = allLegends.find { it.id == legendId }
        
        viewModelScope.launch {
            val timings = loadMetadata(legendId)
            _state.update { it.copy(
                legend = legend,
                userAnswers = emptyMap(),
                timeLeftSeconds = TIME_LIMIT_SECONDS,
                isFinished = false,
                audioState = AudioState.STOPPED,
                currentWordRange = null,
                timings = timings ?: emptyList()
            ) }
        }
        startTimer()
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun loadMetadata(legendId: String): List<WordTiming>? {
        return try {
            val bytes = Res.readBytes("files/audio/$legendId.json")
            val jsonString = bytes.decodeToString()
            Json.decodeFromString<List<WordTiming>>(jsonString)
        } catch (e: Exception) {
            // Нет/битые тайминги — тихий fallback: аудио играет без подсветки слов
            null
        }
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
            _state.update { it.copy(audioState = AudioState.PLAYING) }
        } else {
            audioPlayer.stop()
            audioPlayer.play(
                legendId = legend.id,
                onProgress = { positionMs ->
                    updateHighlight(positionMs)
                },
                onCompletion = {
                    _state.update { it.copy(audioState = AudioState.STOPPED, currentWordRange = null) }
                }
            )
            _state.update { it.copy(audioState = AudioState.PLAYING) }
        }
    }

    private fun updateHighlight(positionMs: Long) {
        val timings = _state.value.timings
        val legendText = _state.value.legend?.text ?: return
        if (timings.isEmpty()) return

        val activeTiming = timings.lastOrNull { it.time_ms <= positionMs }
        if (activeTiming != null) {
            val start = activeTiming.start.coerceIn(0, legendText.length)
            val end = activeTiming.end.coerceIn(0, legendText.length)
            if (start < end) {
                val newRange = start until end
                if (newRange != _state.value.currentWordRange) {
                    _state.update { it.copy(currentWordRange = newRange) }
                }
            } else {
                _state.update { it.copy(currentWordRange = null) }
            }
        }
    }

    fun pauseAudio() {
        audioPlayer.pause()
        _state.update { it.copy(audioState = AudioState.PAUSED) }
    }

    fun stopAudio() {
        audioPlayer.stop()
        _state.update { it.copy(audioState = AudioState.STOPPED, currentWordRange = null) }
    }

    /** Решение «сдал/не сдал» для итога теста по легенде — по [PassCriteria]. */
    fun isPassed(result: QuizResult): Boolean = PassCriteria.isLegendPassed(result)

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

    companion object {
        /** Лимит времени на тест по легенде (10 минут). */
        const val TIME_LIMIT_SECONDS = 600
    }
}
