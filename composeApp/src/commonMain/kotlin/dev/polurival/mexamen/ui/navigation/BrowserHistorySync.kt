package dev.polurival.mexamen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * Связывает backStack с историей браузера, чтобы системный «назад» (кнопка браузера,
 * свайп в PWA) снимал верхний экран, а не уходил со страницы. На Android — no-op:
 * там back обрабатывает NavDisplay.
 */
@Composable
expect fun BrowserHistorySync(backStack: MutableList<NavKey>)
