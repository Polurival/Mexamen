package dev.polurival.mexamen.ui

import kotlinx.browser.document
import kotlinx.coroutines.*
import org.w3c.dom.HTMLAudioElement
import kotlin.js.ExperimentalWasmJsInterop
import kotlin.time.Duration.Companion.milliseconds

class WasmAudioPlayer : AudioPlayer {
    private var audio: HTMLAudioElement? = null
    private var progressJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var lastOnProgress: ((Long) -> Unit)? = null

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun play(
        legendId: String,
        onProgress: (Long) -> Unit,
        onCompletion: () -> Unit
    ) {
        stop()
        lastOnProgress = onProgress
        
        val newAudio = document.createElement("audio") as HTMLAudioElement
        // Following the resource path pattern used in Android
        val assetPath = "composeResources/mexamen.composeapp.generated.resources/files/audio/$legendId.mp3"
        newAudio.src = assetPath
        
        newAudio.addEventListener("ended", {
            progressJob?.cancel()
            lastOnProgress = null
            onCompletion()
        })
        
        newAudio.play()
        audio = newAudio
        startProgressPolling(onProgress)
    }

    private fun startProgressPolling(onProgress: (Long) -> Unit) {
        progressJob?.cancel()
        progressJob = scope.launch {
            while (isActive) {
                audio?.let {
                    if (!it.paused) {
                        onProgress((it.currentTime * 1000).toLong())
                    }
                }
                delay(50.milliseconds)
            }
        }
    }

    @OptIn(ExperimentalWasmJsInterop::class)
    override fun pause() {
        audio?.let {
            if (!it.paused) {
                it.pause()
                progressJob?.cancel()
            } else {
                it.play()
                lastOnProgress?.let { startProgressPolling(it) }
            }
        }
    }

    override fun stop() {
        progressJob?.cancel()
        progressJob = null
        lastOnProgress = null
        audio?.let {
            it.pause()
            it.currentTime = 0.0
        }
        audio = null
    }

    override fun isPlaying(): Boolean = audio?.let { !it.paused } ?: false

    override fun getCurrentPosition(): Long = audio?.let { (it.currentTime * 1000).toLong() } ?: 0L

    override fun release() {
        stop()
        scope.cancel()
    }
}

actual fun provideAudioPlayer(): AudioPlayer = WasmAudioPlayer()
