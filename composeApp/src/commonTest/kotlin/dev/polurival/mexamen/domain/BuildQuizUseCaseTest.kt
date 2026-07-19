package dev.polurival.mexamen.domain

import dev.polurival.mexamen.data.QuestionDataset
import dev.polurival.mexamen.data.QuestionEntry
import dev.polurival.mexamen.llm.DistractorGenerator
import dev.polurival.mexamen.llm.LlmProvider
import dev.polurival.mexamen.llm.LlmUnavailable
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** LLM-заглушка: парсит user-JSON и отвечает валидными дистракторами для каждого id. */
private class EchoLlm(
    private val skipIds: Set<Int> = emptySet(),
    private val failure: Exception? = null,
) : LlmProvider {
    override suspend fun complete(system: String, user: String): String {
        failure?.let { throw it }
        val items = buildJsonArray {
            Json.parseToJsonElement(user).jsonArray
                .map { it.jsonObject.getValue("id").jsonPrimitive.int }
                .filter { it !in skipIds }
                .forEach { id ->
                    add(
                        buildJsonObject {
                            put("id", id)
                            put(
                                "distractors",
                                buildJsonArray {
                                    add(Json.parseToJsonElement("\"L1-$id\""))
                                    add(Json.parseToJsonElement("\"L2-$id\""))
                                    add(Json.parseToJsonElement("\"L3-$id\""))
                                },
                            )
                        },
                    )
                }
        }
        return buildJsonObject { put("items", items) }.toString()
    }
}

class BuildQuizUseCaseTest {

    /** Синтетика: [sections]×[perSection] записей, 2D-векторы веером по углу. */
    private fun dataset(
        sections: Int = 6,
        perSection: Int = 5,
        answer: (Int) -> String = { "Answer $it" },
    ): QuestionDataset {
        var id = 0
        val entries = (1..sections).flatMap { s ->
            (1..perSection).map {
                id++
                QuestionEntry(id, "section-$s", "q$id", answer(id))
            }
        }
        val vectors = entries.associate { e ->
            val angle = e.id * 0.15f
            e.id to floatArrayOf(cos(angle), sin(angle))
        }
        return QuestionDataset(entries, dim = 2, vectorsById = vectors)
    }

    @Test
    fun everyQuestionHasFourUniqueOptionsWithCorrectAnswer() = runTest {
        val quiz = BuildQuizUseCase(dataset(), Random(1)).build()
        assertEquals(10, quiz.size)
        quiz.forEach { q ->
            assertEquals(4, q.options.size, "id=${q.entry.id}: ${q.options}")
            assertEquals(4, q.options.map(RagFallback::normalize).toSet().size, "Duplicates in ${q.options}")
            assertEquals(q.entry.answer, q.options[q.correctIndex])
            assertEquals(DistractorSource.FALLBACK, q.source)
        }
    }

    @Test
    fun correctPositionIsUniformlyDistributed() = runTest {
        val data = dataset()
        val positions = IntArray(4)
        repeat(100) { seed ->
            BuildQuizUseCase(data, Random(seed)).build().forEach { positions[it.correctIndex]++ }
        }
        // 1000 вопросов, ожидание 250 на позицию
        positions.forEachIndexed { index, count ->
            assertTrue(
                count in 175..325,
                "Position $index got $count of 1000, distribution ${positions.toList()}",
            )
        }
    }

    @Test
    fun fixedSeedIsDeterministic() = runTest {
        val data = dataset()
        assertEquals(
            BuildQuizUseCase(data, Random(42)).build(),
            BuildQuizUseCase(data, Random(42)).build(),
        )
    }

    @Test
    fun differentSeedsGiveDifferentQuizzes() = runTest {
        val data = dataset()
        assertTrue(
            BuildQuizUseCase(data, Random(1)).build() != BuildQuizUseCase(data, Random(2)).build(),
        )
    }

    @Test
    fun escalatesNeighborCountWhenNearestAnswersDuplicate() = runTest {
        // Половина ответов — дубли: top-8 соседей часто не дают 3 уникальных, нужен добор
        val data = dataset(sections = 2, perSection = 10) { id ->
            if (id % 2 == 0) "Duplicate" else "Unique $id"
        }
        val quiz = BuildQuizUseCase(data, Random(3)).build(count = 4)
        quiz.forEach { q ->
            assertEquals(4, q.options.map(RagFallback::normalize).toSet().size, "Duplicates in ${q.options}")
        }
    }

    @Test
    fun llmPathUsesLlmDistractorsAndMarksSource() = runTest {
        val quiz = BuildQuizUseCase(
            dataset = dataset(),
            random = Random(1),
            distractorGenerator = DistractorGenerator(EchoLlm()),
        ).build()

        quiz.forEach { q ->
            assertEquals(DistractorSource.LLM, q.source)
            val id = q.entry.id
            assertEquals(
                setOf("L1-$id", "L2-$id", "L3-$id", q.entry.answer),
                q.options.toSet(),
            )
            assertEquals(q.entry.answer, q.options[q.correctIndex])
        }
    }

    @Test
    fun missingLlmItemsFallBackPerQuestion() = runTest {
        val data = dataset()
        // Первые 3 вопроса детерминированного picked-набора LLM «не осилил»
        val pickedIds = BuildQuizUseCase(data, Random(7)).build().map { it.entry.id }
        val skipped = pickedIds.take(3).toSet()

        val quiz = BuildQuizUseCase(
            dataset = data,
            random = Random(7),
            distractorGenerator = DistractorGenerator(EchoLlm(skipIds = skipped)),
        ).build()

        quiz.forEach { q ->
            val expected = if (q.entry.id in skipped) DistractorSource.FALLBACK else DistractorSource.LLM
            assertEquals(expected, q.source, "id=${q.entry.id}")
            assertEquals(4, q.options.map(RagFallback::normalize).toSet().size)
            assertEquals(q.entry.answer, q.options[q.correctIndex])
        }
        assertEquals(3, quiz.count { it.source == DistractorSource.FALLBACK })
    }

    @Test
    fun llmTotalFailureFallsBackWholeQuiz() = runTest {
        val quiz = BuildQuizUseCase(
            dataset = dataset(),
            random = Random(5),
            distractorGenerator = DistractorGenerator(EchoLlm(failure = LlmUnavailable("offline"))),
        ).build()

        assertEquals(10, quiz.size)
        quiz.forEach { q ->
            assertEquals(DistractorSource.FALLBACK, q.source)
            assertEquals(4, q.options.map(RagFallback::normalize).toSet().size)
            assertEquals(q.entry.answer, q.options[q.correctIndex])
        }
    }
}
