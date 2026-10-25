package dev.polurival.mexamen.ui

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.TestDatasetBytes
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
import kotlin.test.assertIs

/** #22: «Banco de preguntas» — загрузка всего гида, поиск и раскрытие explicación. */
@OptIn(ExperimentalCoroutinesApi::class)
class QuestionBankViewModelTest {

    private val entries = listOf(
        QuestionEntry(1, "HISTORIA", "¿Quién encabezó la Revolución?", "Madero", explanation = "Plan de San Luis."),
        QuestionEntry(2, "GEOGRAFÍA", "¿Dónde está el Cañón del Sumidero?", "Chiapas"),
        QuestionEntry(3, "HISTORIA", "¿En qué año inició la Independencia de México?", "1810"),
    )

    private fun viewModel() = QuestionBankViewModel(
        TestDatasetBytes.repository(entries, entries.map { floatArrayOf(it.id.toFloat(), 1f) }),
    )

    private fun QuestionBankViewModel.loaded() = assertIs<QuestionBankUiState.Loaded>(state.value)

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun loadShowsAllQuestionsWithEmptyQuery() = runTest {
        val vm = viewModel()
        assertIs<QuestionBankUiState.Loading>(vm.state.value)

        vm.load()
        runCurrent()

        val state = vm.loaded()
        assertEquals("", state.query)
        assertEquals(listOf(1, 2, 3), state.results.map { it.id })
        assertEquals(3, state.totalCount)
    }

    @Test
    fun queryFiltersIgnoringCaseAndAccentsAndEmptyQueryRestoresAll() = runTest {
        val vm = viewModel()
        vm.load()
        runCurrent()

        vm.updateQuery("CANON")
        assertEquals(listOf(2), vm.loaded().results.map { it.id })
        assertEquals("CANON", vm.loaded().query)
        assertEquals(3, vm.loaded().totalCount)

        vm.updateQuery("")
        assertEquals(listOf(1, 2, 3), vm.loaded().results.map { it.id })
    }

    @Test
    fun toggleExpandedOpensAndClosesExplanation() = runTest {
        val vm = viewModel()
        vm.load()
        runCurrent()

        vm.toggleExpanded(1)
        assertEquals(setOf(1), vm.loaded().expandedIds)
        vm.toggleExpanded(1)
        assertEquals(emptySet(), vm.loaded().expandedIds)
    }

    @Test
    fun repeatedLoadKeepsQueryAndExpandedCards() = runTest {
        val vm = viewModel()
        vm.load()
        runCurrent()
        vm.updateQuery("revolucion")
        vm.toggleExpanded(1)

        vm.load()
        runCurrent()

        assertEquals("revolucion", vm.loaded().query)
        assertEquals(listOf(1), vm.loaded().results.map { it.id })
        assertEquals(setOf(1), vm.loaded().expandedIds)
    }

    @Test
    fun queryBeforeLoadIsIgnored() = runTest {
        val vm = viewModel()
        vm.updateQuery("madero")
        vm.toggleExpanded(1)
        assertIs<QuestionBankUiState.Loading>(vm.state.value)

        vm.load()
        runCurrent()
        assertEquals("", vm.loaded().query)
        assertEquals(3, vm.loaded().results.size)
    }
}
