package dev.polurival.mexamen.testing

import dev.polurival.mexamen.ui.AudioPlayer

/** No-op плеер для тестов LegendViewModel без платформенного аудио. */
internal class FakeAudioPlayer : AudioPlayer {
    override fun play(legendId: String, onProgress: (Long) -> Unit, onCompletion: () -> Unit) = Unit
    override fun pause() = Unit
    override fun stop() = Unit
    override fun isPlaying(): Boolean = false
    override fun getCurrentPosition(): Long = 0L
    override fun release() = Unit
}
