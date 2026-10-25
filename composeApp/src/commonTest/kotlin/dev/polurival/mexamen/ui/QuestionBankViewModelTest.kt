package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.TestDatasetBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/** «Banco de preguntas» (issue #22): загрузка всего гида, поиск и раскрытие explicación. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestionBankViewModelTest {

    private val entries = listOf(
        QuestionEntry(1, "HISTORIA", "¿Quién fue el Benemérito de las Américas?", "Benito Juárez", explanation = "e1"),
        QuestionEntry(2, "ESTADOS Y CAPITALES", "¿Cuál es la capital de Yucatán?", "Mérida"),
        QuestionEntry(3, "ESTADOS Y CAPITALES", "¿Cuál es la capital de Sonora?", "Hermosillo"),
    )

    private fun viewModel() = QuestionBankViewModel(
        TestDatasetBytes.repository(entries, entries.map { floatArrayOf(it.id.toFloat(), 1f) }),
    )

    private fun TestScope.useTestMain() {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun QuestionBankViewModel.loaded(): QuestionBankUiState.Loaded =
        assertIs<QuestionBankUiState.Loaded>(state.value)

    @Test
    fun loadShowsWholeGuideWithEmptyQuery() = runTest {
        useTestMain()
        val vm = viewModel()
        assertIs<QuestionBankUiState.Loading>(vm.state.value)
        vm.load()
        advanceUntilIdle()
        assertEquals("", vm.loaded().query)
        assertEquals(entries, vm.loaded().results)
        assertEquals(3, vm.loaded().totalCount)
    }

    @Test
    fun updateQueryFiltersAndEmptyQueryRestoresAll() = runTest {
        useTestMain()
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        vm.updateQuery("MERIDA")
        assertEquals("MERIDA", vm.loaded().query)
        assertEquals(listOf(2), vm.loaded().results.map { it.id })
        vm.updateQuery("")
        assertEquals(entries, vm.loaded().results)
        assertEquals(3, vm.loaded().totalCount)
    }

    @Test
    fun toggleExpandedOpensAndClosesExplanation() = runTest {
        useTestMain()
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        vm.toggleExpanded(1)
        assertEquals(setOf(1), vm.loaded().expandedIds)
        vm.toggleExpanded(1)
        assertTrue(vm.loaded().expandedIds.isEmpty())
    }

    @Test
    fun repeatedLoadKeepsQueryAndExpandedCards() = runTest {
        useTestMain()
        val vm = viewModel()
        vm.load()
        advanceUntilIdle()
        vm.updateQuery("capital")
        vm.toggleExpanded(3)
        vm.load()
        advanceUntilIdle()
        assertEquals("capital", vm.loaded().query)
        assertEquals(listOf(2, 3), vm.loaded().results.map { it.id })
        assertEquals(setOf(3), vm.loaded().expandedIds)
    }
}
