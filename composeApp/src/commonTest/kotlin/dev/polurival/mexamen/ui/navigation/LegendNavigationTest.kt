package dev.polurival.mexamen.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.polurival.mexamen.domain.QuizResult
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
