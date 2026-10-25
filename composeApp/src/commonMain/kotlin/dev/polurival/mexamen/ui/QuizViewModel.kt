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
import dev.polurival.mexamen.domain.PassCriteria
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
        val timeLeftSeconds: Int = QuizViewModel.TIME_LIMIT_SECONDS,
        val relatedEntry: QuestionEntry? = null,
    ) : QuizUiState

    /**
     * [attempts] — счётчики попыток по id вопроса (только для вопросов с сохранённой статистикой);
     * [passed] — решение «сдал/не сдал» по [PassCriteria].
     */
    data class Result(
        val result: QuizResult,
        val passed: Boolean,
        val attempts: Map<Int, AttemptCounts> = emptyMap(),
    ) : QuizUiState
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
    /** Итог зафиксирован (последний ответ или таймаут) — ответы и переходы больше не принимаются. */
    private var finished = false
    private var startedTopic: String? = null

    /**
     * Повторный вызов для того же [topic] при уже начатой сессии игнорируется: ViewModel
     * переживает поворот, а `LaunchedEffect` в `QuizFlow` срабатывает снова. [restart] — явный рестарт.
     */
    fun startQuiz(topic: String? = null, restart: Boolean = false) {
        if (_state.value is QuizUiState.Loading) return
        if (!restart && started && startedTopic == topic) return
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
                    timeLeftSeconds = TIME_LIMIT_SECONDS,
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
                if (finished) return@launch
                _state.value = QuizUiState.Question(
                    index = next,
                    total = quiz.size,
                    question = nextQuestion,
                    selectedIndex = null,
                    timeLeftSeconds = current?.timeLeftSeconds ?: TIME_LIMIT_SECONDS,
                    relatedEntry = related
                )
            }
        } else {
            timerJob?.cancel()
            showResult()
        }
    }

    /**
     * Ждёт запись последних ответов, чтобы счётчики в результате включали текущую попытку.
     * Итог — по всему квизу: при таймауте неотвеченные идут как «Sin respuesta» и в статистику не пишутся.
     */
    private fun showResult() {
        finished = true
        val result = QuizResult.forQuiz(quiz, answers.toList())
        val pending = recordJobs.toList()
        viewModelScope.launch {
            pending.forEach { it.join() }
            val attempts = statsStore.readAll().mapValues { (_, stats) -> AttemptCounts.from(stats) }
            _state.value = QuizUiState.Result(result, PassCriteria.isQuizPassed(result), attempts)
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        /** Лимит времени на квиз (10 минут). */
        const val TIME_LIMIT_SECONDS = 600
    }
}
