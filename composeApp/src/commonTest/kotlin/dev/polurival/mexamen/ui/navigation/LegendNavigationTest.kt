package dev.polurival.mexamen.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.domain.AnsweredQuestion
import dev.polurival.mexamen.domain.DistractorSource
import dev.polurival.mexamen.domain.ExamRules
import dev.polurival.mexamen.domain.QuizQuestion
import dev.polurival.mexamen.domain.QuizResult
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LegendNavigationTest {

    private val result = QuizResult(emptyList())

    @Test
    fun resultReplacesFinishedLegendSoBackLeadsToLegendsList() {
        // #9: результат поверх LegendDetail → back возвращал на завершённую легенду, и та снова открывала результат
        val backStack = mutableListOf<NavKey>(Route.Home, Route.LegendsList, Route.LegendDetail("cenote-zaci"))

        LegendNavigation.showResult(backStack, legendId = "cenote-zaci", result = result)

        assertEquals(listOf<NavKey>(Route.Home, Route.LegendsList, Route.LegendResult(result)), backStack)
        backStack.removeAt(backStack.size - 1) // системный back
        assertEquals(Route.LegendsList, backStack.last())
    }

    @Test
    fun repeatedShowResultDoesNotStackResults() {
        val backStack = mutableListOf<NavKey>(Route.Home, Route.LegendsList, Route.LegendDetail("cenote-zaci"))

        LegendNavigation.showResult(backStack, legendId = "cenote-zaci", result = result)
        LegendNavigation.showResult(backStack, legendId = "cenote-zaci", result = result)

        assertEquals(listOf<NavKey>(Route.Home, Route.LegendsList, Route.LegendResult(result)), backStack)
    }

    @Test
    fun legendResultRouteCarriesPassDecisionByLegendThreshold() {
        assertTrue(Route.LegendResult(legendResult(correct = ExamRules.LEGEND_PASS_SCORE)).passed)
        assertFalse(Route.LegendResult(legendResult(correct = ExamRules.LEGEND_PASS_SCORE - 1)).passed)
    }

    @Test
    fun savedLegendResultWithoutPassedFieldRestoresDecisionFromResult() {
        val passing = legendResult(correct = ExamRules.LEGEND_PASS_SCORE)
        val oldJson = """{"result":${Json.encodeToString(QuizResult.serializer(), passing)}}"""

        val restored = Json.decodeFromString(Route.LegendResult.serializer(), oldJson)

        assertEquals(passing, restored.result)
        assertTrue(restored.passed)
    }

    /** Результат легенды из 6 вопросов, первые [correct] отвечены верно. */
    private fun legendResult(correct: Int) = QuizResult(
        (1..6).map { id ->
            val question = QuizQuestion(
                entry = QuestionEntry(id, "Legend", "q$id", "a$id"),
                options = listOf("a", "b", "c", "d"),
                correctIndex = 0,
                source = DistractorSource.FALLBACK,
            )
            AnsweredQuestion(question, chosenIndex = if (id <= correct) 0 else AnsweredQuestion.NOT_ANSWERED)
        },
    )
}
