package dev.polurival.mexamen.domain

/** Ответ модели на вопрос квиза; [optionIndex] == null — ответ не разобран. */
data class ModelAnswer(
    val variant: ModelVariant,
    val optionIndex: Int?,
    val rawText: String,
    val latencyMillis: Long,
)
