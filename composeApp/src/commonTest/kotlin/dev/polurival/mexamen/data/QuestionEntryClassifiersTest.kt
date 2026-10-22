package dev.polurival.mexamen.data

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class QuestionEntryClassifiersTest {

    private fun entry(question: String, answer: String = "x") =
        QuestionEntry(id = 1, section = "s", question = question, answer = answer)

    @Test
    fun isDateMatchesCuandoQuestionCaseInsensitive() {
        assertTrue(QuestionEntry.isDate(entry("¿cuándo se firmó?")))
    }

    @Test
    fun isDateMatchesFourDigitYearInAnswer() {
        assertTrue(QuestionEntry.isDate(entry("Año de la Constitución", answer = "En 1917")))
    }

    @Test
    fun isDateRejectsShortNumbersAndPlainText() {
        assertFalse(QuestionEntry.isDate(entry("¿Cuántos estados?", answer = "32")))
    }

    @Test
    fun isPeopleMatchesKeywordsIgnoringCase() {
        assertTrue(QuestionEntry.isPeople(entry("¿QUIÉN fue el primero?").copy(question = "¿Quién fue?")))
        assertTrue(QuestionEntry.isPeople(entry("Vida de benito juárez")))
    }

    @Test
    fun isPeopleRejectsUnrelatedQuestion() {
        assertFalse(QuestionEntry.isPeople(entry("¿Cuál es la capital?")))
    }

    @Test
    fun isEventMatchesKeywords() {
        assertTrue(QuestionEntry.isEvent(entry("La batalla de Puebla")))
        assertTrue(QuestionEntry.isEvent(entry("Firma del Plan de Ayala")))
    }

    @Test
    fun isEventRejectsUnrelatedQuestion() {
        assertFalse(QuestionEntry.isEvent(entry("¿Cuál es la bandera?")))
    }
}
