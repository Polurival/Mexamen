package dev.polurival.mexamen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.polurival.mexamen.data.QuestionRepository
import dev.polurival.mexamen.domain.AskModelUseCase
import dev.polurival.mexamen.domain.BuildQuizUseCase
import dev.polurival.mexamen.domain.DuelScore
import dev.polurival.mexamen.domain.ModelAnswer
import dev.polurival.mexamen.domain.ModelVariant
import dev.polurival.mexamen.domain.QuestionPrompt
import dev.polurival.mexamen.domain.QuizQuestion
import dev.polurival.mexamen.llm.LlmUnavailable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/** Что модель ответила на текущий вопрос. */
sealed interface ModelStatus {
    data object Thinking : ModelStatus

    /**
     * [correct] — верный ли вариант; ответ без разобранного варианта считается неверным.
     * [choice] — выбранный вариант для показа («B) Mexicali»), null если ответ не разобран.
     */
    data class Answered(val answer: ModelAnswer, val correct: Boolean, val choice: String?) : ModelStatus

    /** Сервис с моделью недоступен: квиз при этом продолжается. */
    data object Unavailable : ModelStatus
}

sealed interface DuelUiState {
    data object Loading : DuelUiState
    data class Error(val message: String) : DuelUiState

    /**
     * Ответ модели запрашивается сразу при показе вопроса, чтобы к моменту ответа пользователя он был готов.
     * [variant] можно менять, пока пользователь не ответил.
     */
    data class Question(
        val index: Int,
        val total: Int,
        val question: QuizQuestion,
        val variant: ModelVariant,
        val selectedIndex: Int?,
        val model: ModelStatus,
        val score: DuelScore,
    ) : DuelUiState {
        val userCorrect: Boolean? get() = selectedIndex?.let { it == question.correctIndex }
        val canSwitchVariant: Boolean get() = selectedIndex == null

        /** Дальше можно, когда пользователь ответил и модель дала ответ или точно не ответит. */
        val canAdvance: Boolean get() = selectedIndex != null && model !is ModelStatus.Thinking
    }

    data class Finished(val score: DuelScore) : DuelUiState
}

/** «Tú vs IA»: те же вопросы отвечают пользователь и модель сервиса; статистика вопросов не пишется. */
class DuelViewModel(
    private val repository: QuestionRepository,
    private val askModel: AskModelUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow<DuelUiState>(DuelUiState.Loading)
    val state: StateFlow<DuelUiState> = _state.asStateFlow()

    private var questions: List<QuizQuestion> = emptyList()
    private var started = false
    private var askJob: Job? = null
    private var variant = ModelVariant.TUNED

    /** Старт при входе на экран; повтор после поворота не сбрасывает идущую сессию. */
    fun ensureStarted() {
        if (started) return
        start()
    }

    fun start() {
        started = true
        askJob?.cancel()
        _state.value = DuelUiState.Loading
        viewModelScope.launch {
            try {
                questions = BuildQuizUseCase(repository.dataset()).build(count = DUEL_SIZE)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.value = DuelUiState.Error(e.message ?: "Error al cargar las preguntas")
                return@launch
            }
            if (questions.isEmpty()) {
                _state.value = DuelUiState.Error("No hay preguntas")
                return@launch
            }
            showQuestion(index = 0, score = DuelScore())
        }
    }

    fun selectVariant(newVariant: ModelVariant) {
        val current = _state.value as? DuelUiState.Question ?: return
        if (!current.canSwitchVariant || newVariant == current.variant) return
        variant = newVariant
        _state.value = current.copy(variant = newVariant, model = ModelStatus.Thinking)
        askModelFor(current.index, newVariant)
    }

    fun selectAnswer(optionIndex: Int) {
        val current = _state.value as? DuelUiState.Question ?: return
        if (current.selectedIndex != null) return
        _state.value = current.copy(selectedIndex = optionIndex)
    }

    fun next() {
        val current = _state.value as? DuelUiState.Question ?: return
        if (!current.canAdvance) return
        val userCorrect = current.userCorrect ?: return
        val modelCorrect = (current.model as? ModelStatus.Answered)?.correct
        val score = current.score.plus(userCorrect, modelCorrect)
        askJob?.cancel()
        if (current.index + 1 < questions.size) {
            showQuestion(current.index + 1, score)
        } else {
            _state.value = DuelUiState.Finished(score)
        }
    }

    private fun showQuestion(index: Int, score: DuelScore) {
        _state.value = DuelUiState.Question(
            index = index,
            total = questions.size,
            question = questions[index],
            variant = variant,
            selectedIndex = null,
            model = ModelStatus.Thinking,
            score = score,
        )
        askModelFor(index, variant)
    }

    private fun askModelFor(index: Int, forVariant: ModelVariant) {
        askJob?.cancel()
        val question = questions[index]
        askJob = viewModelScope.launch {
            val status = try {
                val answer = askModel.ask(question, forVariant)
                ModelStatus.Answered(
                    answer = answer,
                    correct = answer.optionIndex == question.correctIndex,
                    choice = answer.optionIndex?.let { "${QuestionPrompt.letter(it)}) ${question.options[it]}" },
                )
            } catch (e: LlmUnavailable) {
                ModelStatus.Unavailable
            }
            val current = _state.value as? DuelUiState.Question ?: return@launch
            if (current.index == index && current.variant == forVariant) {
                _state.value = current.copy(model = status)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        askJob?.cancel()
    }

    companion object {
        const val DUEL_SIZE = 5
    }
}
