package dev.polurival.mexamen.domain

/**
 * Счёт «Tú vs IA» за сессию. [modelAnswered] — в скольких раундах модель вообще ответила:
 * раунд без ответа (сервис недоступен) не засчитывается ей как неверный.
 */
data class DuelScore(
    val user: Int = 0,
    val model: Int = 0,
    val rounds: Int = 0,
    val modelAnswered: Int = 0,
) {
    /** [modelCorrect] == null — модель не ответила в этом раунде. */
    fun plus(userCorrect: Boolean, modelCorrect: Boolean?) = DuelScore(
        user = user + if (userCorrect) 1 else 0,
        model = model + if (modelCorrect == true) 1 else 0,
        rounds = rounds + 1,
        modelAnswered = modelAnswered + if (modelCorrect != null) 1 else 0,
    )

    /** [DuelWinner.NONE], если модель ответила не во всех раундах: сравнивать нечего. */
    val winner: DuelWinner
        get() = when {
            modelAnswered < rounds -> DuelWinner.NONE
            user > model -> DuelWinner.USER
            user < model -> DuelWinner.MODEL
            else -> DuelWinner.DRAW
        }
}
