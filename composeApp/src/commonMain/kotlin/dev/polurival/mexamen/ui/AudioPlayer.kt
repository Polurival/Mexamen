package dev.polurival.mexamen.ui

interface AudioPlayer {
    fun play(
        legendId: String,
        onProgress: (Long) -> Unit = {},
        onCompletion: () -> Unit = {}
    )
    fun pause()
    fun stop()
    fun isPlaying(): Boolean
    fun getCurrentPosition(): Long
    fun release()
}

expect fun provideAudioPlayer(): AudioPlayer
