package dev.polurival.mexamen.domain

/**
 * Разбор ответа модели в индекс варианта. Тот же «мягкий» разбор, что в замерах Дня 7:
 * «Respuesta: B) …», «B)» в начале строки, «**B**» или ответ, содержащий текст ровно одного варианта.
 * null — ответ не разобран (модель не выбрала вариант или выбрала неоднозначно).
 */
object AnswerParser {
    // (?i:...) не поддерживается в JS-регэкспах wasm, поэтому регистр слова «respuesta» задан классами
    private val RESPUESTA = Regex("[Rr][Ee][Ss][Pp][Uu][Ee][Ss][Tt][Aa][^A-Za-z]{0,12}\\b([A-D])\\b")
    private val LEADING_LETTER = Regex("^\\W{0,3}([A-D])\\s*[).:]", RegexOption.MULTILINE)
    private val BOLD_LETTER = Regex("\\*\\*([A-D])\\*\\*")

    fun parse(text: String, options: List<String>): Int? {
        val trimmed = text.trim()
        for (pattern in listOf(RESPUESTA, LEADING_LETTER, BOLD_LETTER)) {
            val letter = pattern.find(trimmed)?.groupValues?.get(1) ?: continue
            return QuestionPrompt.indexOf(letter[0]).takeIf { it in options.indices }
        }
        val normalizedText = RagFallback.normalize(trimmed)
        val hits = options.indices.filter { index ->
            val option = RagFallback.normalize(options[index])
            option.isNotEmpty() && normalizedText.contains(option)
        }
        return hits.singleOrNull()
    }
}
