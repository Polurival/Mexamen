package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.legendCenoteZaci
import dev.polurival.mexamen.testing.FakeAudioPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** Регрессия #7: неотвеченные вопросы легенды не должны ронять экран результата. */
@OptIn(ExperimentalCoroutinesApi::class)
class LegendViewModelTest {

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private suspend fun startedViewModel(): LegendViewModel {
        val vm = LegendViewModel(FakeAudioPlayer())
        vm.selectLegend(legendCenoteZaci.id)
        vm.state.first { it.legend != null }
        return vm
    }

    @Test
    fun unansweredQuestionsAreMarkedNotAnsweredInResult() = runTest {
        val vm = startedViewModel()
        val first = legendCenoteZaci.questions.first()
        vm.selectAnswer(first.id, first.correctAnswerIndex)
        vm.finishLegend()

        val result = vm.getResult()

        assertEquals(legendCenoteZaci.questions.size, result.total)
        assertEquals(1, result.score)
        assertTrue(result.answers.first().isAnswered)
        assertTrue(result.answers.drop(1).none { it.isAnswered || it.isCorrect })
    }

    @Test
    fun answerAfterFinishIsIgnored() = runTest {
        val vm = startedViewModel()
        vm.finishLegend()
        val first = legendCenoteZaci.questions.first()
        vm.selectAnswer(first.id, first.correctAnswerIndex)

        val result = vm.getResult()

        assertEquals(0, result.score)
        assertFalse(result.answers.first().isAnswered)
    }
}
