package dev.polurival.mexamen.domain

import kotlin.test.Test
import kotlin.test.assertEquals

class TopicAccuracyFormatterTest {

    @Test
    fun formatShowsRoundedPercent() {
        assertEquals("Aciertos: 67 %", TopicAccuracyFormatter.format(2.0 / 3))
    }

    @Test
    fun formatShowsZeroAndFullPercent() {
        assertEquals("Aciertos: 0 %", TopicAccuracyFormatter.format(0.0))
        assertEquals("Aciertos: 100 %", TopicAccuracyFormatter.format(1.0))
    }

    @Test
    fun formatShowsSinRespuestasWhenRatioIsNull() {
        assertEquals("Sin respuestas", TopicAccuracyFormatter.format(null))
    }
}
