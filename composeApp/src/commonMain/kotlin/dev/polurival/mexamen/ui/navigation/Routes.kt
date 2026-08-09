package dev.polurival.mexamen.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed class Route : NavKey {
    @Serializable
    data object Home : Route()
    
    @Serializable
    data object Quiz : Route()
    
    @Serializable
    data object Flashcards : Route()
    
    @Serializable
    data object Stats : Route()
}
