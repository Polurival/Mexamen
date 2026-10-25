package dev.polurival.mexamen.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.polurival.mexamen.domain.QuizResult

/** Переходы легенды в backStack. */
object LegendNavigation {

    /**
     * Результат заменяет завершённую легенду [legendId], а не ложится поверх: иначе back возвращает
     * на легенду с `isFinished = true`, и она снова открывает результат (петля). Back с результата → список легенд.
     * Если сверху уже не эта легенда (результат показан) — ничего не делает.
     */
    fun showResult(backStack: MutableList<NavKey>, legendId: String, result: QuizResult) {
        if (backStack.lastOrNull() != Route.LegendDetail(legendId)) return
        backStack[backStack.lastIndex] = Route.LegendResult(result)
    }
}
