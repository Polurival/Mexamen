package dev.polurival.mexamen.domain

import kotlin.math.roundToInt

/** Единая строка точности по теме для экрана статистики: «Aciertos: NN %» или «Sin respuestas». */
object TopicAccuracyFormatter {
    const val NO_ANSWERS_TEXT = "Sin respuestas"
    private const val PERCENT = 100

    fun format(correctRatio: Double?): String =
        if (correctRatio == null) NO_ANSWERS_TEXT else "Aciertos: ${(correctRatio * PERCENT).roundToInt()} %"
}
