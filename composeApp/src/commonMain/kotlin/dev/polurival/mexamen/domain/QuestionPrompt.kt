package dev.polurival.mexamen.domain

/**
 * Запрос к модели в том виде, на котором её обучали (День 7): вопрос и варианты A–D, без инструкций.
 * Инструкции и few-shot для базовой модели добавляет сервис (профиль промпта в registry.json).
 * Единственный источник алфавита вариантов — и для промпта, и для разбора ответа ([AnswerParser]).
 */
object QuestionPrompt {
    private const val LETTERS = "ABCD"

    fun format(question: String, options: List<String>): String =
        question + "\n" + options.mapIndexed { index, option -> "${LETTERS[index]}) $option" }.joinToString("\n")

    fun letter(index: Int): Char = LETTERS[index]

    /** Индекс варианта по букве; -1, если буква вне алфавита. */
    fun indexOf(letter: Char): Int = LETTERS.indexOf(letter)
}
