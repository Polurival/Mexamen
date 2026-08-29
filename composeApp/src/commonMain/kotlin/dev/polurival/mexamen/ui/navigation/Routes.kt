package dev.polurival.mexamen.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.polurival.mexamen.domain.QuizResult
import kotlinx.serialization.Serializable

@Serializable
enum class SessionType { QUIZ, FLASHCARDS }

@Serializable
sealed class Route : NavKey {
    @Serializable
    data object Home : Route()

    @Serializable
    data class TopicsList(val sessionType: SessionType) : Route()
    
    @Serializable
    data class Quiz(val topic: String? = null) : Route()
    
    @Serializable
    data class Flashcards(val topic: String? = null) : Route()
    
    @Serializable
    data object Stats : Route()

    @Serializable
    data object LegendsList : Route()

    @Serializable
    data class LegendDetail(val legendId: String) : Route()

    @Serializable
    data class LegendResult(val result: QuizResult) : Route()

    @Serializable
    data object Interview : Route()
}
