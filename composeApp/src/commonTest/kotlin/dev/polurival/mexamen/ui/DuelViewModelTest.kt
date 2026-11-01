package dev.polurival.mexamen.ui

import dev.polurival.mexamen.domain.AskModelUseCase
import dev.polurival.mexamen.domain.DuelScore
import dev.polurival.mexamen.domain.DuelWinner
import dev.polurival.mexamen.domain.ModelVariant
import dev.polurival.mexamen.llm.LlmProvider
import dev.polurival.mexamen.llm.LlmUnavailable
import dev.polurival.mexamen.testing.FakeLlmProvider
import dev.polurival.mexamen.testing.testQuestionRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/** Модель-заглушка, которая всегда выбирает вариант A: правильность зависит от перемешивания, не от теста. */
private fun askModel(
    tuned: LlmProvider = FakeLlmProvider(reply = "Respuesta: A)"),
    base: LlmProvider = FakeLlmProvider(reply = "Respuesta: B)"),
) = AskModelUseCase(mapOf(ModelVariant.TUNED to tuned, ModelVariant.BASE to base))

@OptIn(ExperimentalCoroutinesApi::class)
class DuelViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(askModel: AskModelUseCase = askModel()) =
        DuelViewModel(testQuestionRepository(), askModel)

    @Test
    fun startShowsFirstQuestionWithTunedModelAndAnswersIt() = runTest {
        val vm = viewModel()
        vm.ensureStarted()
        runCurrent()

        val question = assertIs<DuelUiState.Question>(vm.state.value)
        assertEquals(0, question.index)
        assertEquals(DuelViewModel.DUEL_SIZE, question.total)
        assertEquals(ModelVariant.TUNED, question.variant)
        assertNull(question.selectedIndex)
        val model = assertIs<ModelStatus.Answered>(question.model)
        assertEquals(0, model.answer.optionIndex)
        assertEquals(model.answer.optionIndex == question.question.correctIndex, model.correct)
    }

    @Test
    fun switchingVariantReasksOtherModelUntilUserAnswers() = runTest {
        val vm = viewModel()
        vm.ensureStarted()
        runCurrent()

        vm.selectVariant(ModelVariant.BASE)
        assertEquals(ModelStatus.Thinking, assertIs<DuelUiState.Question>(vm.state.value).model)
        runCurrent()
        val afterSwitch = assertIs<DuelUiState.Question>(vm.state.value)
        assertEquals(ModelVariant.BASE, afterSwitch.variant)
        assertEquals(1, assertIs<ModelStatus.Answered>(afterSwitch.model).answer.optionIndex)

        vm.selectAnswer(0)
        vm.selectVariant(ModelVariant.TUNED)
        assertEquals(ModelVariant.BASE, assertIs<DuelUiState.Question>(vm.state.value).variant)
    }

    @Test
    fun staleAnswerOfPreviousVariantIsIgnored() = runTest {
        val slowTuned = CompletableDeferred<String>()
        val vm = viewModel(
            askModel(tuned = object : LlmProvider {
                override suspend fun complete(system: String, user: String) = slowTuned.await()
            }),
        )
        vm.ensureStarted()
        runCurrent()
        vm.selectVariant(ModelVariant.BASE)
        runCurrent()
        slowTuned.complete("Respuesta: A)")
        runCurrent()

        val question = assertIs<DuelUiState.Question>(vm.state.value)
        assertEquals(ModelVariant.BASE, question.variant)
        assertEquals(ModelVariant.BASE, assertIs<ModelStatus.Answered>(question.model).answer.variant)
    }

    @Test
    fun unavailableModelDoesNotBlockTheDuel() = runTest {
        val down = FakeLlmProvider(failure = LlmUnavailable("down"))
        val vm = viewModel(askModel(tuned = down, base = down))
        vm.ensureStarted()
        runCurrent()

        val question = assertIs<DuelUiState.Question>(vm.state.value)
        assertEquals(ModelStatus.Unavailable, question.model)
        vm.selectAnswer(question.question.correctIndex)
        vm.next()
        runCurrent()
        val second = assertIs<DuelUiState.Question>(vm.state.value)
        assertEquals(1, second.index)
        assertEquals(DuelScore(user = 1, model = 0, rounds = 1, modelAnswered = 0), second.score)
        assertEquals(DuelWinner.NONE, second.score.winner)
    }

    @Test
    fun nextWaitsForPendingModelAnswerInsteadOfCountingItWrong() = runTest {
        val pending = CompletableDeferred<String>()
        val vm = viewModel(
            askModel(tuned = object : LlmProvider {
                override suspend fun complete(system: String, user: String) = pending.await()
            }),
        )
        vm.ensureStarted()
        runCurrent()
        val first = assertIs<DuelUiState.Question>(vm.state.value)
        vm.selectAnswer(first.question.correctIndex)

        assertFalse(assertIs<DuelUiState.Question>(vm.state.value).canAdvance)
        vm.next()
        assertEquals(0, assertIs<DuelUiState.Question>(vm.state.value).index)

        pending.complete("Respuesta: A)")
        runCurrent()
        assertTrue(assertIs<DuelUiState.Question>(vm.state.value).canAdvance)
        vm.next()
        runCurrent()
        val second = assertIs<DuelUiState.Question>(vm.state.value)
        assertEquals(1, second.index)
        assertEquals(1, second.score.modelAnswered)
    }

    @Test
    fun callsAfterFinishAreIgnored() = runTest {
        val vm = viewModel()
        vm.ensureStarted()
        runCurrent()
        repeat(DuelViewModel.DUEL_SIZE) {
            val question = assertIs<DuelUiState.Question>(vm.state.value)
            vm.selectAnswer(question.question.correctIndex)
            vm.next()
            runCurrent()
        }
        val finished = assertIs<DuelUiState.Finished>(vm.state.value)

        vm.selectAnswer(0)
        vm.selectVariant(ModelVariant.BASE)
        vm.next()
        runCurrent()

        assertEquals(finished, vm.state.value)
    }

    @Test
    fun lateModelAnswerAfterFinishDoesNotReviveSession() = runTest {
        val pending = CompletableDeferred<String>()
        val vm = viewModel(
            askModel(tuned = object : LlmProvider {
                override suspend fun complete(system: String, user: String) = pending.await()
            }),
        )
        vm.ensureStarted()
        runCurrent()
        vm.selectVariant(ModelVariant.BASE)
        runCurrent()
        repeat(DuelViewModel.DUEL_SIZE) {
            val question = assertIs<DuelUiState.Question>(vm.state.value)
            vm.selectAnswer(question.question.correctIndex)
            vm.next()
            runCurrent()
        }
        assertIs<DuelUiState.Finished>(vm.state.value)

        pending.complete("Respuesta: A)")
        runCurrent()

        assertIs<DuelUiState.Finished>(vm.state.value)
    }

    @Test
    fun repeatedAnswerIsIgnoredAndNextNeedsAnAnswer() = runTest {
        val vm = viewModel()
        vm.ensureStarted()
        runCurrent()
        val first = assertIs<DuelUiState.Question>(vm.state.value)

        vm.next()
        assertEquals(0, assertIs<DuelUiState.Question>(vm.state.value).index)

        val wrong = (first.question.correctIndex + 1) % first.question.options.size
        vm.selectAnswer(wrong)
        vm.selectAnswer(first.question.correctIndex)
        val answered = assertIs<DuelUiState.Question>(vm.state.value)
        assertEquals(wrong, answered.selectedIndex)
        assertEquals(false, answered.userCorrect)
    }

    @Test
    fun fullSessionEndsWithScoreOfAllRounds() = runTest {
        val vm = viewModel()
        vm.ensureStarted()
        runCurrent()
        repeat(DuelViewModel.DUEL_SIZE) {
            val question = assertIs<DuelUiState.Question>(vm.state.value)
            vm.selectAnswer(question.question.correctIndex)
            vm.next()
            runCurrent()
        }

        val finished = assertIs<DuelUiState.Finished>(vm.state.value)
        assertEquals(DuelViewModel.DUEL_SIZE, finished.score.rounds)
        assertEquals(DuelViewModel.DUEL_SIZE, finished.score.user)
        assertTrue(finished.score.model <= finished.score.rounds)
    }

    @Test
    fun ensureStartedDoesNotRestartRunningSession() = runTest {
        val vm = viewModel()
        vm.ensureStarted()
        runCurrent()
        val first = assertIs<DuelUiState.Question>(vm.state.value)
        vm.selectAnswer(0)

        vm.ensureStarted()
        runCurrent()

        assertEquals(0, assertIs<DuelUiState.Question>(vm.state.value).selectedIndex)
        assertEquals(first.question, assertIs<DuelUiState.Question>(vm.state.value).question)
    }
}
