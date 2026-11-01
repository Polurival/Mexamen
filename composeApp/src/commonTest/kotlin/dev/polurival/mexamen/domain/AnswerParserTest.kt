package dev.polurival.mexamen.domain

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class AnswerParserTest {
    private val options = listOf("Tecate", "Mexicali", "Tijuana", "Ensenada")

    @Test
    fun parsesStrictFormat() {
        assertEquals(1, AnswerParser.parse("Respuesta: B) Mexicali", options))
    }

    @Test
    fun parsesRespuestaWithoutOptionTextAndAnyCase() {
        assertEquals(2, AnswerParser.parse("respuesta: C", options))
        assertEquals(3, AnswerParser.parse("RESPUESTA - D", options))
    }

    @Test
    fun parsesLeadingLetterAndBoldLetter() {
        assertEquals(0, AnswerParser.parse("A) Tecate", options))
        assertEquals(1, AnswerParser.parse("La opción correcta es **B**.", options))
    }

    @Test
    fun fallsBackToSingleOptionTextIgnoringCaseAndAccents() {
        assertEquals(3, AnswerParser.parse("Creo que es ENSENADA", options))
    }

    @Test
    fun ambiguousOrEmptyAnswerIsNull() {
        assertNull(AnswerParser.parse("Tecate o Tijuana", options))
        assertNull(AnswerParser.parse("No lo sé", options))
        assertNull(AnswerParser.parse("", options))
    }

    @Test
    fun letterOutsideOptionsIsNull() {
        assertNull(AnswerParser.parse("Respuesta: D", options.take(3)))
    }
}
