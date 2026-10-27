package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertEquals

/** #22: поиск «Banco de preguntas» — регистр, диакритика, пустой запрос = все. */
class QuestionSearchTest {

    private val entries = listOf(
        QuestionEntry(1, "ESTADOS Y CAPITALES", "¿Cuál es la capital de Aguascalientes?", "Aguascalientes"),
        QuestionEntry(2, "HISTORIA", "¿Quién encabezó la Revolución?", "Francisco I. Madero"),
        QuestionEntry(3, "GEOGRAFÍA", "¿Dónde está el Cañón del Sumidero?", "Chiapas"),
        QuestionEntry(4, "HISTORIA", "¿En qué año inició la Independencia de México?", "1810"),
    )

    private val index = QuestionSearch.index(entries)

    private fun ids(query: String) = QuestionSearch.filter(index, query).map { it.id }

    @Test
    fun emptyQueryReturnsAllInOriginalOrder() {
        assertEquals(listOf(1, 2, 3, 4), ids(""))
    }

    @Test
    fun blankQueryReturnsAll() {
        assertEquals(listOf(1, 2, 3, 4), ids("   "))
    }

    @Test
    fun searchIgnoresCase() {
        assertEquals(listOf(1), ids("AGUASCALIENTES"))
        assertEquals(listOf(2), ids("madero"))
    }

    @Test
    fun queryWithoutAccentsMatchesAccentedText() {
        assertEquals(listOf(2), ids("revolucion"))
        assertEquals(listOf(3), ids("canon"))
        assertEquals(listOf(4), ids("mexico"))
    }

    @Test
    fun accentedQueryMatchesTextAndIgnoresAccentCase() {
        assertEquals(listOf(4), ids("MÉXICO"))
        assertEquals(listOf(1), ids("cuál"))
    }

    @Test
    fun searchMatchesAnswerText() {
        assertEquals(listOf(3), ids("chiapas"))
        assertEquals(listOf(4), ids("1810"))
    }

    @Test
    fun everyWordMustMatchQuestionOrAnswer() {
        assertEquals(listOf(1), ids("capital  aguascalientes"))
        assertEquals(listOf(2), ids("revolucion madero"))
        assertEquals(emptyList(), ids("revolucion chiapas"))
    }

    @Test
    fun sectionIsNotSearched() {
        assertEquals(emptyList(), ids("geografia"))
    }

    @Test
    fun indexPrecomputesNormalizedQuestionAndAnswer() {
        val item = index[2]
        assertEquals(3, item.entry.id)
        assertEquals("¿donde esta el canon del sumidero? chiapas", item.normalizedText)
    }

    @Test
    fun uppercaseAccentsAndEnyeInQueryAreFolded() {
        assertEquals(listOf(3), ids("CAÑÓN"))
    }
}
