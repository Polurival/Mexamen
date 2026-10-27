package dev.polurival.mexamen.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavKey

/**
 * Связывает backStack с историей браузера, чтобы back браузера/жест снимал верхний экран.
 * [backStack] — snapshot-список из `rememberNavBackStack`. На Android back обрабатывает NavDisplay — no-op.
 */
@Composable
expect fun BrowserHistorySync(backStack: MutableList<NavKey>)
