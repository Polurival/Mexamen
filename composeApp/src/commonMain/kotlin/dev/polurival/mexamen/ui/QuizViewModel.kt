package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.data.QuestionStatsStore
import dev.polurival.mexamen.domain.AnsweredQuestion
import dev.polurival.mexamen.domain.AttemptCounts
import dev.polurival.mexamen.domain.BuildQuizUseCase
import dev.polurival.mexamen.domain.GetRelatedQuestionUseCase
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
        val relatedEntry: QuestionEntry? = null,
    ) : QuizUiState

    /** [attempts] — кумулятивные счётчики по id вопроса, уже включают ответы этой сессии. */
    data class Result(val result: QuizResult, val attempts: Map<Int, AttemptCounts> = emptyMap()) : QuizUiState
}

class QuizViewModel(
    private val repository: QuestionRepository,
    private val statsStore: QuestionStatsStore,
    private val distractorGenerator: DistractorGenerator? = null,
) : ViewModel() {

    private val _state = MutableStateFlow<QuizUiState>(QuizUiState.Home())
    val state: StateFlow<QuizUiState> = _state.asStateFlow()

    private val recordAnswer = RecordAnswerUseCase(statsStore)
    private val getRelatedQuestion = GetRelatedQuestionUseCase(repository)
    private var quiz: List<QuizQuestion> = emptyList()
    private var answers = mutableListOf<AnsweredQuestion>()
    private var timerJob: Job? = null
    private val recordJobs = mutableListOf<Job>()
    private var started = false
    /** Сессия завершена (последний ответ или таймаут): до появления Result новые ответы/переходы игнорируются. */
    private var finished = false
    private var startedTopic: String? = null

    /**
     * Старт при входе на экран. ViewModel переживает поворот, а композиция пересоздаётся —
     * повторный вызов для того же [topic] не сбрасывает уже начатую сессию (#6).
     * Явный рестарт («Intentar de nuevo»/«Reintentar») — через [startQuiz].
     */
    fun ensureStarted(topic: String? = null) {
        if (started && startedTopic == topic) return
        startQuiz(topic)
    }

    fun startQuiz(topic: String? = null) {
        if (_state.value is QuizUiState.Loading) return
        started = true
        startedTopic = topic
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
                recordJobs.clear()
                finished = false
                val firstQuestion = quiz.first()
                val related = getRelatedQuestion(firstQuestion.entry)
                _state.value = QuizUiState.Question(
                    index = 0,
                    total = quiz.size,
                    question = firstQuestion,
                    selectedIndex = null,
                    timeLeftSeconds = 600,
                    relatedEntry = related
                )
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
        // При таймауте ответов меньше, чем вопросов — неотвеченные дополняет QuizResult.of.
        showResult()
    }

    fun selectAnswer(optionIndex: Int) {
        if (finished) return
        val current = _state.value as? QuizUiState.Question ?: return
        if (current.selectedIndex != null) return
        val answered = AnsweredQuestion(current.question, optionIndex)
        answers += answered
        _state.value = current.copy(selectedIndex = optionIndex)
        recordJobs += viewModelScope.launch { recordAnswer.recordQuizAnswer(current.question.entry.id, answered.isCorrect) }
    }

    fun advanceQuestion() {
        if (finished) return
        val current = _state.value as? QuizUiState.Question ?: return
        if (current.selectedIndex == null) return
        advance(current.index)
    }

    private fun advance(answeredIndex: Int) {
        val next = answeredIndex + 1
        val current = _state.value as? QuizUiState.Question
        if (next < quiz.size) {
            viewModelScope.launch {
                val nextQuestion = quiz[next]
                val related = getRelatedQuestion(nextQuestion.entry)
                if (finished) return@launch // таймаут, пока грузился связанный вопрос
                _state.value = QuizUiState.Question(
                    index = next,
                    total = quiz.size,
                    question = nextQuestion,
                    selectedIndex = null,
                    timeLeftSeconds = current?.timeLeftSeconds ?: 600,
                    relatedEntry = related
                )
            }
        } else {
            timerJob?.cancel()
            showResult()
        }
    }

    /** Ждёт запись последних ответов, чтобы счётчики в результате включали текущую попытку. */
    private fun showResult() {
        if (finished) return
        finished = true
        val result = QuizResult.of(quiz, answers.toList())
        val pending = recordJobs.toList()
        viewModelScope.launch {
            pending.forEach { it.join() }
            _state.value = QuizUiState.Result(result, AttemptCounts.fromAll(statsStore.readAll()))
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }
}
