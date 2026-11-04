package dev.polurival.mexamen.domain

import dev.polurival.mexamen.llm.LlmProvider
import dev.polurival.mexamen.llm.LlmUnavailable

/**
 * Спрашивает модель выбранного [ModelVariant] тот же вопрос с теми же вариантами, что видит пользователь.
 * Бросает [LlmUnavailable], если сервис недоступен — квиз и карточки от него не зависят.
 */
class AskModelUseCase(
    private val providers: Map<ModelVariant, LlmProvider>,
    private val clock: () -> Long = ::currentTimeMillis,
) {
    suspend fun ask(question: QuizQuestion, variant: ModelVariant): ModelAnswer {
        val provider = providers[variant] ?: throw LlmUnavailable("No provider for $variant")
        val started = clock()
        val text = provider.complete(
            system = "",
            user = QuestionPrompt.format(question.entry.question, question.options),
        )
        return ModelAnswer(
            variant = variant,
            optionIndex = AnswerParser.parse(text, question.options),
            rawText = text,
            latencyMillis = clock() - started,
        )
    }
}
