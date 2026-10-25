package dev.polurival.mexamen.domain

/** Единая строка «Pasada N vez/veces · Correctas: M» для результата квиза и flashcards. */
object AttemptCountsFormatter {
    fun format(counts: AttemptCounts): String {
        val times = if (counts.timesShown == 1) "vez" else "veces"
        return "Pasada ${counts.timesShown} $times · Correctas: ${counts.timesCorrect}"
    }
}
