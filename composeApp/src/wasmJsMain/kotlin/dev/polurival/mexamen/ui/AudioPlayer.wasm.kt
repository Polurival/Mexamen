package dev.polurival.mexamen.ui

class WasmAudioPlayer : AudioPlayer {
    override fun play(legendId: String, onProgress: (Long) -> Unit, onCompletion: () -> Unit) {}
    override fun pause() {}
    override fun stop() {}
    override fun isPlaying(): Boolean = false
    override fun getCurrentPosition(): Long = 0L
    override fun release() {}
}

actual fun provideAudioPlayer(): AudioPlayer = WasmAudioPlayer()
