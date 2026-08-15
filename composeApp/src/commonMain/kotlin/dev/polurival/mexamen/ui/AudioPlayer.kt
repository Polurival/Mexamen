package dev.polurival.mexamen.ui

interface AudioPlayer {
    fun play(legendId: String)
    fun pause()
    fun stop()
    fun isPlaying(): Boolean
    fun release()
}

expect fun provideAudioPlayer(): AudioPlayer
