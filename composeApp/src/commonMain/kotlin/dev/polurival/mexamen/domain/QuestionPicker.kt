package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.random.Random

/**
 * Выбор случайных вопросов для квиза (FR-2): не более [maxPerSection] из одного
 * раздела, при возможности. Если разделов не хватает — лимит ослабляется, чтобы
 * всегда вернуть [count] вопросов.
 */
class QuestionPicker(
    private val random: Random = Random.Default,
    private val maxPerSection: Int = 2,
) {
    fun pick(entries: List<QuestionEntry>, count: Int): List<QuestionEntry> {
        require(count <= entries.size) {
            "Requested $count questions, dataset has only ${entries.size}"
        }
        val shuffled = entries.shuffled(random)
        val picked = ArrayList<QuestionEntry>(count)
        val perSection = mutableMapOf<String, Int>()
        for (entry in shuffled) {
            if (picked.size == count) break
            val used = perSection.getOrElse(entry.section) { 0 }
            if (used < maxPerSection) {
                picked += entry
                perSection[entry.section] = used + 1
            }
        }
        if (picked.size < count) {
            // Разделов слишком мало — добираем без лимита
            val chosenIds = picked.mapTo(mutableSetOf()) { it.id }
            for (entry in shuffled) {
                if (picked.size == count) break
                if (entry.id !in chosenIds) picked += entry
            }
        }
        return picked
    }
}
