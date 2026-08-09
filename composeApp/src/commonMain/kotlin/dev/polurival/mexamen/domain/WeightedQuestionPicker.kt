package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats
import kotlin.math.pow
import kotlin.random.Random

/**
 * Выбор вопросов для quiz/flashcards, взвешенный по [QuestionWeighting.effectiveWeight]:
 * чаще ошибаемся — чаще попадается. Заменяет старый чисто-случайный `QuestionPicker`.
 *
 * Алгоритм — weighted sampling без возврата (A-ES): каждому вопросу присваивается ключ
 * `U^(1/weight)`, где U — равномерная случайная величина; сортировка по убыванию ключа
 * даёт взвешенную перестановку (при равных весах — эквивалент обычного shuffle).
 *
 * Не более [maxPerSection] из одного раздела и не свежепоказанные (cooldown) — но обе
 * границы ослабляются каскадом, если кандидатов не хватает, чтобы [count] набралось всегда.
 */
class WeightedQuestionPicker(
    private val random: Random = Random.Default,
    private val maxPerSection: Int = 2,
) {
    fun pick(
        entries: List<QuestionEntry>,
        stats: Map<Int, QuestionStats>,
        count: Int,
        nowMillis: Long,
    ): List<QuestionEntry> {
        require(count <= entries.size) {
            "Requested $count questions, dataset has only ${entries.size}"
        }
        // Ключи считаются один раз до сортировки — random.nextDouble() внутри
        // компаратора менял бы ключ между сравнениями и ломал контракт сортировки.
        val keyed = entries.map { it to sampleKey(it, stats, nowMillis) }
        val ranked = keyed.sortedByDescending { it.second }.map { it.first }
        val nonCooldown = ranked.filterNot { isInCooldown(it, stats, nowMillis) }

        val picked = ArrayList<QuestionEntry>(count)
        val chosenIds = mutableSetOf<Int>()
        val perSection = mutableMapOf<String, Int>()

        fun fillFrom(pool: List<QuestionEntry>, respectCap: Boolean) {
            for (entry in pool) {
                if (picked.size == count) return
                if (entry.id in chosenIds) continue
                val used = perSection.getOrElse(entry.section) { 0 }
                if (!respectCap || used < maxPerSection) {
                    picked += entry
                    chosenIds += entry.id
                    perSection[entry.section] = used + 1
                }
            }
        }

        // 1) без cooldown, с лимитом по секции
        fillFrom(nonCooldown, respectCap = true)
        // 2) без cooldown, лимит ослаблен
        fillFrom(nonCooldown, respectCap = false)
        // 3) допускаем и cooldown-вопросы, без ограничений — гарантирует набор count
        fillFrom(ranked, respectCap = false)
        return picked
    }

    private fun isInCooldown(entry: QuestionEntry, stats: Map<Int, QuestionStats>, nowMillis: Long): Boolean =
        QuestionWeighting.isInCooldown(stats[entry.id] ?: QuestionStats(), nowMillis)

    private fun sampleKey(entry: QuestionEntry, stats: Map<Int, QuestionStats>, nowMillis: Long): Double {
        val weight = QuestionWeighting.effectiveWeight(stats[entry.id] ?: QuestionStats(), nowMillis)
        return random.nextDouble().pow(1.0 / weight)
    }
}
