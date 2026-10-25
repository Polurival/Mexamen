package dev.polurival.mexamen.ui.navigation

import androidx.navigation3.runtime.NavKey
import dev.polurival.mexamen.domain.ExamRules
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

    /**
     * [passed] — решение «сдал/не сдал» по [ExamRules.LEGEND_PASS_SCORE]; дефолт выводится из [result],
     * поэтому сохранённый ранее backStack без этого поля восстанавливается с верным значением.
     */
    @Serializable
    data class LegendResult(
        val result: QuizResult,
        val passed: Boolean = ExamRules.isLegendPassed(result),
    ) : Route()

    @Serializable
    data object Interview : Route()
}
