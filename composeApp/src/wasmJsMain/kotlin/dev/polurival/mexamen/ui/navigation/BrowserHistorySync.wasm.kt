package dev.polurival.mexamen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.navigation3.runtime.NavKey
import kotlinx.browser.window
import kotlin.js.ExperimentalWasmJsInterop

@Composable
actual fun BrowserHistorySync(backStack: MutableList<NavKey>) {
    val sync = remember(backStack) { HistorySync(backStack) }
    DisposableEffect(sync) {
        sync.start()
        onDispose { sync.stop() }
    }
    LaunchedEffect(sync) {
        snapshotFlow { backStack.size }.collect { size -> sync.onBackStackSize(size) }
    }
}

/**
 * Одна запись истории на экран: в `history.state` хранится глубина (индекс верхнего экрана,
 * Home = 0). Переход вперёд — pushState, выход назад внутри приложения — history.go(-n),
 * popstate от браузера — снятие экранов до глубины записи.
 */
@OptIn(ExperimentalWasmJsInterop::class)
private class HistorySync(private val backStack: MutableList<NavKey>) {
    private var depth = 0

    fun start() {
        val initialDepth = depthOf(window.history.state)
        if (initialDepth > 0) {
            // Перезагрузка на вложенном экране: backStack снова [Home] — возвращаемся к корневой записи
            window.history.go(-initialDepth)
        } else {
            window.history.replaceState(0.toJsNumber(), "")
        }
        window.onpopstate = { event -> onPopState(depthOf(event.state)) }
    }

    fun stop() {
        window.onpopstate = null
    }

    fun onBackStackSize(size: Int) {
        val target = size - 1
        while (depth < target) {
            depth++
            window.history.pushState(depth.toJsNumber(), "")
        }
        if (depth > target) {
            // Экран закрыт кнопкой в UI/Escape: убираем лишние записи; их popstate ничего не снимет
            window.history.go(target - depth)
            depth = target
        }
    }

    private fun onPopState(newDepth: Int) {
        depth = newDepth
        while (backStack.size - 1 > newDepth) {
            backStack.removeAt(backStack.size - 1)
        }
        val top = backStack.size - 1
        if (newDepth > top) {
            // «Вперёд» браузера: закрытый экран не восстановить — откатываемся к текущему
            window.history.go(top - newDepth)
            depth = top
        }
    }

    private fun depthOf(state: JsAny?): Int = (state as? JsNumber)?.toInt() ?: 0
}
