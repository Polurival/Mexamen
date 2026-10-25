package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.test.Test
import kotlin.test.assertEquals

class QuestionSearchTest {

    private val entries = listOf(
        QuestionEntry(1, "HISTORIA", "¿Quién inició la Independencia?", "Miguel Hidalgo"),
        QuestionEntry(2, "HISTORIA", "¿Quién fue el Benemérito de las Américas?", "Benito Juárez"),
        QuestionEntry(3, "ESTADOS Y CAPITALES", "¿Cuál es la capital de Yucatán?", "Mérida"),
        QuestionEntry(4, "ESTADOS Y CAPITALES", "¿Cuál es la capital de Nuevo León?", "Monterrey"),
    )

    @Test
    fun emptyQueryReturnsAllEntriesInOrder() {
        assertEquals(entries, QuestionSearch.filter(entries, ""))
    }

    @Test
    fun blankQueryReturnsAllEntries() {
        assertEquals(entries, QuestionSearch.filter(entries, "   "))
    }

    @Test
    fun matchesQuestionTextIgnoringCase() {
        assertEquals(listOf(1), QuestionSearch.filter(entries, "INDEPENDENCIA").map { it.id })
    }

    @Test
    fun matchesAnswerText() {
        assertEquals(listOf(4), QuestionSearch.filter(entries, "monterrey").map { it.id })
    }

    @Test
    fun queryWithoutAccentsMatchesAccentedText() {
        assertEquals(listOf(2), QuestionSearch.filter(entries, "juarez").map { it.id })
        assertEquals(listOf(3), QuestionSearch.filter(entries, "yucatan").map { it.id })
    }

    @Test
    fun accentedQueryMatchesTextAndIgnoresCase() {
        assertEquals(listOf(3), QuestionSearch.filter(entries, "MÉRIDA").map { it.id })
        assertEquals(listOf(4), QuestionSearch.filter(entries, "león").map { it.id })
    }

    @Test
    fun queryIsTrimmedAndMatchesSubstringAcrossEntries() {
        assertEquals(listOf(3, 4), QuestionSearch.filter(entries, "  capital ").map { it.id })
    }

    @Test
    fun noMatchReturnsEmptyList() {
        assertEquals(emptyList(), QuestionSearch.filter(entries, "zapata"))
    }
}
