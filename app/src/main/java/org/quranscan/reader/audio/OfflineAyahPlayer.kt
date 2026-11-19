package org.quranscan.reader.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import java.io.FileDescriptor

/** Foreground, local-only playback. MP3 segment seeking remains approximate.
 * All controls and callbacks run on the main thread; preparation is asynchronous.
 * Focus loss and headphone disconnect pause; only an explicit Resume restarts audio.
 */
class OfflineAyahPlayer(
    context: Context,
    private val onFinished: () -> Unit,
    private val onError: (String) -> Unit
) {
    private val app = context.applicationContext
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var player: MediaPlayer? = null
    private var prepared = false
    private var seeking = false
    private var playWhenReady = false
    private var segmentEnd: Int? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false
    private var released = false
    private val handler = Handler(Looper.getMainLooper())
    private val focusListener = AudioManager.OnAudioFocusChangeListener { change ->
        if (change == AudioManager.AUDIOFOCUS_LOSS ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT ||
            change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK) {
            pause()
        }
    }
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) pause()
        }
    }
    init {
        ContextCompat.registerReceiver(app, noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    private val checkpoint = object : Runnable {
        override fun run() {
            val mp = player ?: return
            val end = segmentEnd ?: return
            if (!isPlaying()) return
            if (runCatching { mp.currentPosition >= end }.getOrDefault(false)) {
                stop()
                onFinished()
            } else handler.postDelayed(this, 30)
        }
    }

    fun play(recording: FileDescriptor, startMs: Int = 0, endMs: Int? = null, requiredDurationMs: Int = 0) {
        stop()
        if (released) return
        val mp = MediaPlayer()
        player = mp
        try {
            require(requiredDurationMs >= 0) { "Invalid timing duration" }
            require(startMs >= 0 && (endMs == null || endMs > startMs)) { "Invalid audio segment" }
            segmentEnd = endMs
            playWhenReady = true
            mp.setAudioAttributes(attributes)
            mp.setDataSource(recording)
            mp.setOnCompletionListener {
                if (player === mp) { stop(); onFinished() }
            }
            mp.setOnErrorListener { _, _, _ ->
                if (player === mp) fail("Local audio playback failed")
                true
            }
            mp.setOnSeekCompleteListener {
                if (player === mp) { seeking = false; startIfReady() }
            }
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                try {
                    require(requiredDurationMs <= mp.duration) { "Word timeline extends beyond this recording" }
                    require(startMs < mp.duration && (endMs == null || endMs <= mp.duration)) {
                        "Word timing is outside audio duration"
                    }
                    prepared = true
                    if (startMs > 0) {
                        seeking = true
                        mp.seekTo(startMs)
                    } else startIfReady()
                } catch (e: Exception) { fail(e.message ?: "Invalid local recording") }
            }
            mp.prepareAsync()
        } catch (e: Exception) { fail("Unable to play local audio: ${e.message ?: "unknown error"}") }
    }

    private fun startIfReady() {
        val mp = player ?: return
        if (!prepared || seeking || !playWhenReady) return
        try {
            if (!requestFocus()) { fail("Audio focus unavailable. Return to this screen and try again."); return }
            mp.start()
            handler.removeCallbacks(checkpoint)
            if (segmentEnd != null) handler.post(checkpoint)
        } catch (e: Exception) { fail("Unable to resume local audio: ${e.message ?: "unknown error"}") }
    }

    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(focusListener, handler).build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            audioManager.requestAudioFocus(focusListener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }
    private fun abandonFocus() {
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        else if (hasFocus) audioManager.abandonAudioFocus(focusListener)
        focusRequest = null
        hasFocus = false
    }
    private fun fail(message: String) { stop(); onError(message) }
    fun currentPositionMs(): Int? = if (prepared && !seeking) runCatching { player?.currentPosition }.getOrNull() else null
    fun isPlaying(): Boolean = prepared && !seeking && runCatching { player?.isPlaying == true }.getOrDefault(false)
    fun pause() {
        playWhenReady = false // Also cancels start if prepare/seek completes after onStop.
        handler.removeCallbacks(checkpoint)
        if (prepared) runCatching { player?.takeIf { it.isPlaying }?.pause() }
        abandonFocus()
    }
    fun resume() { if (!released && player != null) { playWhenReady = true; startIfReady() } }
    fun stop() {
        handler.removeCallbacks(checkpoint)
        playWhenReady = false
        prepared = false
        seeking = false
        segmentEnd = null
        val old = player
        player = null // Late callbacks from replaced/released players cannot start audio.
        old?.let { runCatching { it.release() } }
        abandonFocus()
    }
    fun release() {
        stop()
        if (!released) { released = true; app.unregisterReceiver(noisyReceiver) }
    }
}
