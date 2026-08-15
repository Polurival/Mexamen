package dev.polurival.mexamen.ui

import android.media.MediaPlayer
import dev.polurival.mexamen.AppContext

class AndroidAudioPlayer : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null

    override fun play(legendId: String) {
        stop()
        mediaPlayer = MediaPlayer().apply {
            try {
                // Путь к ресурсам в Compose Multiplatform для Android
                // Формат: composeResources/[библиотека/модуль]/files/[путь]
                val assetPath = "composeResources/mexamen.composeapp.generated.resources/files/audio/$legendId.mp3"
                val fd = AppContext.instance.assets.openFd(assetPath)
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                prepare()
                start()
            } catch (e: Exception) {
                println("Error playing audio: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    override fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            } else {
                it.start()
            }
        }
    }

    override fun stop() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    override fun release() {
        stop()
    }
}

actual fun provideAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
