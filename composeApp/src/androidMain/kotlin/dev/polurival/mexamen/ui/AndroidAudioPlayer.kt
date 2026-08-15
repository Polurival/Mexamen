package dev.polurival.mexamen.ui

import android.media.MediaPlayer
import dev.polurival.mexamen.AppContext
import kotlinx.coroutines.*

class AndroidAudioPlayer : AudioPlayer {
    private var mediaPlayer: MediaPlayer? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastOnProgress: ((Long) -> Unit)? = null

    override fun play(
        legendId: String,
        onProgress: (Long) -> Unit,
        onCompletion: () -> Unit
    ) {
        stop()
        lastOnProgress = onProgress
        mediaPlayer = MediaPlayer().apply {
            try {
                val assetPath = "composeResources/mexamen.composeapp.generated.resources/files/audio/$legendId.mp3"
                val fd = AppContext.instance.assets.openFd(assetPath)
                setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
                fd.close()
                
                setOnCompletionListener {
                    progressJob?.cancel()
                    lastOnProgress = null
                    onCompletion()
                }
                
                prepare()
                start()
                
                startProgressPolling(onProgress)
            } catch (e: Exception) {
                println("Error playing audio: ${e.message}")
                e.printStackTrace()
            }
        }
    }

    private fun startProgressPolling(onProgress: (Long) -> Unit) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                mediaPlayer?.let {
                    if (it.isPlaying) {
                        onProgress(it.currentPosition.toLong())
                    }
                }
                delay(50)
            }
        }
    }

    override fun pause() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
                progressJob?.cancel()
            } else {
                it.start()
                lastOnProgress?.let { startProgressPolling(it) }
            }
        }
    }

    override fun stop() {
        progressJob?.cancel()
        progressJob = null
        lastOnProgress = null
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.stop()
            }
            it.release()
        }
        mediaPlayer = null
    }

    override fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    override fun getCurrentPosition(): Long = mediaPlayer?.currentPosition?.toLong() ?: 0L

    override fun release() {
        stop()
        scope.cancel()
    }
}

actual fun provideAudioPlayer(): AudioPlayer = AndroidAudioPlayer()
