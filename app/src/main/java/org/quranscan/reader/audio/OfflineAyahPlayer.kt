package org.quranscan.reader.audio

import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import java.io.File

/** Always local audio. Segment seeking is approximate (device MP3 seek precision varies).
 * Stop at/after a selected word end; never synthesize Quran recitation.
 */
class OfflineAyahPlayer(
    private val onFinished: () -> Unit,
    private val onError: (String) -> Unit
) {
    private var player: MediaPlayer? = null
    private var segmentEnd: Int? = null
    private val handler = Handler(Looper.getMainLooper())
    private val checkpoint = object : Runnable {
        override fun run() {
            val mp = player ?: return
            val end = segmentEnd ?: return
            if (runCatching { mp.isPlaying && mp.currentPosition >= end }.getOrDefault(false)) {
                stop()
                onFinished()
            } else {
                handler.postDelayed(this, 40)
            }
        }
    }

    fun play(file: File, startMs: Int = 0, endMs: Int? = null) {
        require(file.isFile && file.name.matches(Regex("[0-9]{3}_[0-9]{3}\\.mp3"))) {
            "Missing permitted local verse recording"
        }
        require(startMs >= 0 && (endMs == null || endMs > startMs)) { "Invalid audio segment" }
        stop()
        val mp = MediaPlayer()
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnCompletionListener {
                stop()
                onFinished()
            }
            mp.setOnErrorListener { _, _, _ ->
                stop()
                onError("Local audio playback failed")
                true
            }
            mp.prepare()
            require(startMs < mp.duration && (endMs == null || endMs <= mp.duration + 350)) {
                "Word timing is outside audio duration"
            }
            segmentEnd = endMs
            if (startMs > 0) mp.seekTo(startMs)
            player = mp
            mp.start()
            if (endMs != null) handler.post(checkpoint)
        } catch (e: Exception) {
            runCatching { mp.release() }
            player = null
            segmentEnd = null
            onError("Unable to play local audio: ${e.message ?: "unknown error"}")
        }
    }

    fun currentPositionMs(): Int? = runCatching { player?.currentPosition }.getOrNull()
    fun isPlaying(): Boolean = runCatching { player?.isPlaying == true }.getOrDefault(false)
    fun pause() { player?.takeIf { it.isPlaying }?.pause() }
    fun resume() { player?.takeIf { !it.isPlaying }?.start() }
    fun stop() {
        handler.removeCallbacks(checkpoint)
        segmentEnd = null
        player?.run { runCatching { stop() }; release() }
        player = null
    }
    fun release() = stop()
}
