package dev.polurival.mexamen.ui.navigation

import kotlin.test.Test
import kotlin.test.assertEquals

class BackStackHistoryTest {

    @Test
    fun homeAloneNeedsNoHistoryEntries() {
        // глубина 0: back браузера на Home уходит со страницы штатно
        assertEquals(0, BackStackHistory.depthFor(backStackSize = 1))
        assertEquals(0, BackStackHistory.historyStepsAfterBackStackChange(backStackSize = 1, historyDepth = 0))
    }

    @Test
    fun forwardNavigationAddsOneHistoryEntry() {
        assertEquals(1, BackStackHistory.historyStepsAfterBackStackChange(backStackSize = 2, historyDepth = 0))
        assertEquals(1, BackStackHistory.historyStepsAfterBackStackChange(backStackSize = 3, historyDepth = 1))
    }

    @Test
    fun inAppReturnHomeRewindsHistoryBySeveralEntries() {
        // Home → Legends → Detail → Result, затем «Volver al inicio» снимает всё до Home
        assertEquals(-3, BackStackHistory.historyStepsAfterBackStackChange(backStackSize = 1, historyDepth = 3))
    }

    @Test
    fun browserBackRemovesTopScreen() {
        // Home → TopicsList, back браузера встаёт на запись глубины 0
        assertEquals(1, BackStackHistory.screensToRemoveOnPopState(backStackSize = 2, poppedDepth = 0))
    }

    @Test
    fun popStateCausedByInAppBackRemovesNothing() {
        // приложение уже сняло экран и само вызвало history.go(-1)
        assertEquals(0, BackStackHistory.screensToRemoveOnPopState(backStackSize = 2, poppedDepth = 1))
    }

    @Test
    fun unknownHistoryStateIsTreatedAsHome() {
        assertEquals(3, BackStackHistory.screensToRemoveOnPopState(backStackSize = 4, poppedDepth = -1))
    }

    @Test
    fun browserForwardToStaleEntryIsRewound() {
        // после in-app возврата остались «forward»-записи; экраны для них уже не восстановить
        assertEquals(0, BackStackHistory.screensToRemoveOnPopState(backStackSize = 1, poppedDepth = 2))
        assertEquals(-2, BackStackHistory.historyStepsAfterBackStackChange(backStackSize = 1, historyDepth = 2))
    }
}
