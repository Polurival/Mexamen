package dev.polurival.mexamen.testing

import dev.polurival.mexamen.data.ThemeModeStore
import kotlinx.coroutines.CompletableDeferred

/**
 * In-memory store темы. `gatedRead`/`gatedWrite` держат чтение/запись «в полёте» до [openRead]/[openWrite],
 * чтобы тест мог попасть в окно между нажатием и завершением IO.
 */
internal class FakeThemeModeStore(
    var value: String? = null,
    gatedRead: Boolean = false,
    gatedWrite: Boolean = false,
) : ThemeModeStore {
    private val readGate = CompletableDeferred<Unit>().apply { if (!gatedRead) complete(Unit) }
    private val writeGate = CompletableDeferred<Unit>().apply { if (!gatedWrite) complete(Unit) }

    /** Значения в порядке фактической записи. */
    val writes = mutableListOf<String>()

    fun openRead() {
        readGate.complete(Unit)
    }

    fun openWrite() {
        writeGate.complete(Unit)
    }

    override suspend fun read(): String? {
        readGate.await()
        return value
    }

    override suspend fun write(value: String) {
        writeGate.await()
        this.value = value
        writes += value
    }
}
