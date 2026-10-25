package dev.polurival.mexamen.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json

/**
 * Загруженный в память датасет: записи гида и их эмбеддинги (768-dim), выровненные по id.
 */
class QuestionDataset(
    val entries: List<QuestionEntry>,
    val dim: Int,
    private val vectorsById: Map<Int, FloatArray>,
) {
    fun embedding(id: Int): FloatArray =
        vectorsById[id] ?: error("No embedding for question id=$id")
}

/**
 * Ленивая загрузка questions.json + embeddings.bin из ресурсов приложения.
 * Источники байтов инжектируются, чтобы parsing был тестируем без Android/Compose.
 */
class QuestionRepository(
    private val readQuestions: suspend () -> ByteArray,
    private val readEmbeddings: suspend () -> ByteArray,
) {
    private val mutex = Mutex()
    private var cached: QuestionDataset? = null

    suspend fun dataset(): QuestionDataset = mutex.withLock {
        cached ?: load().also { cached = it }
    }

    suspend fun getTopics(): List<Topic> {
        val entries = dataset().entries
        return entries.groupBy { it.section }
            .map { (name, list) -> Topic(name, list.size) }
            .sortedBy { it.name }
    }

    /** Счётчики виртуальных тем — через [filterByTopic], чтобы число в списке совпадало с выборкой. */
    suspend fun getVirtualTopics(): List<Topic> {
        val entries = dataset().entries
        return VIRTUAL_TOPICS.map { name -> Topic(name, filterByTopic(entries, name).size) }
    }

    suspend fun getQuestionsByTopic(topicName: String): List<QuestionEntry> =
        filterByTopic(dataset().entries, topicName)

    suspend fun getTotalQuestionsCount(): Int {
        return dataset().entries.size
    }

    private suspend fun load(): QuestionDataset {
        val entries = parseQuestions(readQuestions())
        val (dim, vectors) = parseEmbeddings(readEmbeddings())
        check(vectors.size == entries.size) {
            "Dataset mismatch: ${entries.size} questions vs ${vectors.size} embeddings"
        }
        // embeddings.bin записан в порядке id, questions.json — тоже
        val vectorsById = buildMap {
            entries.forEachIndexed { index, entry -> put(entry.id, vectors[index]) }
        }
        check(vectorsById.size == entries.size) { "Duplicate question ids in dataset" }
        return QuestionDataset(entries, dim, vectorsById)
    }

    companion object {
        /** Имена виртуальных тем — видны в UI, поэтому по-испански. */
        const val TOPIC_DATES = "Fechas"
        const val TOPIC_PEOPLE = "Personajes"
        const val TOPIC_EVENTS = "Eventos"

        /** Порядок виртуальных тем в UI. */
        private val VIRTUAL_TOPICS = listOf(TOPIC_DATES, TOPIC_PEOPLE, TOPIC_EVENTS)

        /** Единая фильтрация по теме (виртуальной или `section`) для репозитория и use case'ов. */
        fun filterByTopic(entries: List<QuestionEntry>, topicName: String): List<QuestionEntry> =
            when (topicName) {
                TOPIC_DATES -> entries.filter { QuestionEntry.isDate(it) }
                TOPIC_PEOPLE -> entries.filter { QuestionEntry.isPeople(it) }
                TOPIC_EVENTS -> entries.filter { QuestionEntry.isEvent(it) }
                else -> entries.filter { it.section == topicName }
            }

        private val json = Json { ignoreUnknownKeys = true }

        fun parseQuestions(bytes: ByteArray): List<QuestionEntry> =
            json.decodeFromString(bytes.decodeToString())

        /**
         * Формат: заголовок int32 LE count, int32 LE dim, затем count×dim float32 LE.
         */
        fun parseEmbeddings(bytes: ByteArray): Pair<Int, List<FloatArray>> {
            check(bytes.size >= HEADER_SIZE) { "embeddings.bin too short: ${bytes.size} bytes" }
            val count = bytes.intLeAt(0)
            val dim = bytes.intLeAt(4)
            val expected = HEADER_SIZE + count.toLong() * dim * Float.SIZE_BYTES
            check(bytes.size.toLong() == expected) {
                "embeddings.bin size mismatch: expected $expected bytes (count=$count, dim=$dim), got ${bytes.size}"
            }
            val vectors = List(count) { row ->
                val base = HEADER_SIZE + row * dim * Float.SIZE_BYTES
                FloatArray(dim) { col ->
                    Float.fromBits(bytes.intLeAt(base + col * Float.SIZE_BYTES))
                }
            }
            return dim to vectors
        }

        private const val HEADER_SIZE = 8

        private fun ByteArray.intLeAt(offset: Int): Int =
            (this[offset].toInt() and 0xFF) or
                ((this[offset + 1].toInt() and 0xFF) shl 8) or
                ((this[offset + 2].toInt() and 0xFF) shl 16) or
                ((this[offset + 3].toInt() and 0xFF) shl 24)
    }
}
