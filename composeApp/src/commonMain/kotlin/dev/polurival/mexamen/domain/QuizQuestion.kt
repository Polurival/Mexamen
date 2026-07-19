package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry

enum class DistractorSource { LLM, FALLBACK }

/**
 * Готовый вопрос квиза: 4 перемешанных варианта, индекс верного, источник дистракторов.
 */
data class QuizQuestion(
    val entry: QuestionEntry,
    val options: List<String>,
    val correctIndex: Int,
    val source: DistractorSource,
)
