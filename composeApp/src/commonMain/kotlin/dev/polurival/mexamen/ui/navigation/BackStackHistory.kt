package dev.polurival.mexamen.ui.navigation

/**
 * Чистый расчёт синхронизации backStack ↔ история браузера (wasm).
 * Глубина истории = число записей, добавленных приложением поверх исходной; на Home она 0,
 * поэтому back браузера на Home уходит со страницы штатно.
 */
object BackStackHistory {

    /** Глубина истории, соответствующая backStack данного размера. */
    fun depthFor(backStackSize: Int): Int = (backStackSize - 1).coerceAtLeast(0)

    /** Сдвиг истории, чтобы догнать backStack: >0 — добавить записей, <0 — `history.go(n)`, 0 — ничего. */
    fun historyStepsAfterBackStackChange(backStackSize: Int, historyDepth: Int): Int =
        depthFor(backStackSize) - historyDepth

    /** Сколько верхних экранов снять, когда браузер встал на запись глубины [poppedDepth] (неизвестная = Home). */
    fun screensToRemoveOnPopState(backStackSize: Int, poppedDepth: Int): Int =
        (depthFor(backStackSize) - poppedDepth.coerceAtLeast(0)).coerceAtLeast(0)
}
