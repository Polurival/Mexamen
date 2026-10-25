@file:OptIn(ExperimentalWasmJsInterop::class)

package dev.polurival.mexamen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavKey
import kotlinx.browser.window
import org.w3c.dom.PopStateEvent
import org.w3c.dom.events.Event

/** Глубина из `history.state`; чужое/пустое состояние → -1 (трактуется как Home). */
@JsFun("(state) => typeof state === 'number' ? state : -1")
private external fun historyDepthOf(state: JsAny?): Int

@Composable
actual fun BrowserHistorySync(backStack: MutableList<NavKey>) {
    LaunchedEffect(backStack) {
        // Переход вперёд — pushState(глубина), возврат внутри приложения — history.go(-n), popstate — снять экраны
        var historyDepth = 0
        window.history.replaceState(historyDepth.toJsNumber(), "")

        fun syncHistory() {
            val steps = BackStackHistory.historyStepsAfterBackStackChange(backStack.size, historyDepth)
            if (steps > 0) {
                repeat(steps) {
                    historyDepth += 1
                    window.history.pushState(historyDepth.toJsNumber(), "")
                }
            } else if (steps < 0) {
                historyDepth += steps
                window.history.go(steps)
            }
        }

        val onPopState: (Event) -> Unit = { event ->
            val poppedDepth = historyDepthOf((event as PopStateEvent).state)
            historyDepth = poppedDepth.coerceAtLeast(0)
            repeat(BackStackHistory.screensToRemoveOnPopState(backStack.size, poppedDepth)) {
                backStack.removeAt(backStack.size - 1)
            }
            // «вперёд» браузера на устаревшую запись — откатываем историю обратно
            syncHistory()
        }

        window.addEventListener("popstate", onPopState)
        try {
            snapshotFlow { backStack.size }.collect { syncHistory() }
        } finally {
            window.removeEventListener("popstate", onPopState)
        }
    }
}
