package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.data.QuestionStats
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WeightedQuestionPickerTest {

    private fun entries(sections: Int, perSection: Int): List<QuestionEntry> {
        var id = 0
        return (1..sections).flatMap { s ->
            (1..perSection).map {
                id++
                QuestionEntry(id, "section-$s", "q$id", "a$id")
            }
        }
    }

    @Test
    fun picksRequestedCountWithoutDuplicates() {
        val picked = WeightedQuestionPicker(Random(1)).pick(entries(11, 70), emptyMap(), 10, nowMillis = 0L)
        assertEquals(10, picked.size)
        assertEquals(10, picked.map { it.id }.toSet().size)
    }

    @Test
    fun respectsSectionLimit() {
        repeat(20) { seed ->
            val picked = WeightedQuestionPicker(Random(seed)).pick(entries(11, 70), emptyMap(), 10, nowMillis = 0L)
            val bySection = picked.groupingBy { it.section }.eachCount()
            assertTrue(
                bySection.values.all { it <= 2 },
                "Seed $seed: section overflow $bySection",
            )
        }
    }

    @Test
    fun fixedSeedIsDeterministic() {
        val data = entries(11, 70)
        val first = WeightedQuestionPicker(Random(42)).pick(data, emptyMap(), 10, nowMillis = 0L)
        val second = WeightedQuestionPicker(Random(42)).pick(data, emptyMap(), 10, nowMillis = 0L)
        assertEquals(first, second)
    }

    @Test
    fun differentSeedsGiveDifferentQuizzes() {
        val data = entries(11, 70)
        val first = WeightedQuestionPicker(Random(1)).pick(data, emptyMap(), 10, nowMillis = 0L)
        val second = WeightedQuestionPicker(Random(2)).pick(data, emptyMap(), 10, nowMillis = 0L)
        assertTrue(first != second, "Different seeds should give different picks")
    }

    @Test
    fun relaxesLimitWhenTooFewSections() {
        // 2 раздела × 6 вопросов: лимит 2×2=4 < 10, но 10 вопросов вернуть обязаны
        val picked = WeightedQuestionPicker(Random(7)).pick(entries(2, 6), emptyMap(), 10, nowMillis = 0L)
        assertEquals(10, picked.size)
        assertEquals(10, picked.map { it.id }.toSet().size)
    }

    @Test
    fun failsWhenDatasetSmallerThanRequest() {
        assertFailsWith<IllegalArgumentException> {
            WeightedQuestionPicker(Random(1)).pick(entries(1, 5), emptyMap(), 10, nowMillis = 0L)
        }
    }

    @Test
    fun highWeightQuestionsArePickedMoreOften() {
        val data = entries(11, 70)
        val heavyId = data.first().id
        val stats = mapOf(heavyId to QuestionStats(weight = QuestionWeighting.MAX_WEIGHT))
        var heavyPickedCount = 0
        val trials = 500
        repeat(trials) { seed ->
            val picked = WeightedQuestionPicker(Random(seed)).pick(data, stats, 10, nowMillis = 0L)
            if (picked.any { it.id == heavyId }) heavyPickedCount++
        }
        val baselineRate = 10.0 / data.size
        val heavyRate = heavyPickedCount.toDouble() / trials
        assertTrue(
            heavyRate > baselineRate * 2,
            "Expected heavy-weight question picked much more than baseline $baselineRate, got $heavyRate",
        )
    }

    @Test
    fun cooldownExcludesRecentlySeenQuestionsUnlessNeeded() {
        val data = entries(2, 6) // маленький датасет, чтобы релаксация была видна
        val now = 10_000_000L
        val stats = data.take(4).associate { it.id to QuestionStats(lastSeenEpochMillis = now) }
        val picked = WeightedQuestionPicker(Random(3)).pick(data, stats, count = data.size, nowMillis = now)
        // count == entries.size => обязаны выбрать вообще всё, включая cooldown-вопросы
        assertEquals(data.size, picked.size)
        assertEquals(data.map { it.id }.toSet(), picked.map { it.id }.toSet())
    }
}
