package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionEntry
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class QuestionPickerTest {

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
        val picked = QuestionPicker(Random(1)).pick(entries(11, 70), 10)
        assertEquals(10, picked.size)
        assertEquals(10, picked.map { it.id }.toSet().size)
    }

    @Test
    fun respectsSectionLimit() {
        repeat(20) { seed ->
            val picked = QuestionPicker(Random(seed)).pick(entries(11, 70), 10)
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
        val first = QuestionPicker(Random(42)).pick(data, 10)
        val second = QuestionPicker(Random(42)).pick(data, 10)
        assertEquals(first, second)
    }

    @Test
    fun differentSeedsGiveDifferentQuizzes() {
        val data = entries(11, 70)
        val first = QuestionPicker(Random(1)).pick(data, 10)
        val second = QuestionPicker(Random(2)).pick(data, 10)
        assertTrue(first != second, "Different seeds should give different picks")
    }

    @Test
    fun relaxesLimitWhenTooFewSections() {
        // 2 раздела × 6 вопросов: лимит 2×2=4 < 10, но 10 вопросов вернуть обязаны
        val picked = QuestionPicker(Random(7)).pick(entries(2, 6), 10)
        assertEquals(10, picked.size)
        assertEquals(10, picked.map { it.id }.toSet().size)
    }

    @Test
    fun failsWhenDatasetSmallerThanRequest() {
        assertFailsWith<IllegalArgumentException> {
            QuestionPicker(Random(1)).pick(entries(1, 5), 10)
        }
    }
}
