package org.quranscan.reader.audio

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat
import org.quranscan.reader.R
import java.io.FileDescriptor

/** Foreground, local-only playback. MP3 segment seeking remains approximate.
 * All controls and callbacks run on the main thread; preparation is asynchronous.
 * Focus loss and headphone disconnect revoke the activity’s playback confirmation.
 */
class OfflineAyahPlayer(
    context: Context,
    private val onFinished: () -> Unit,
    private val onError: (Int) -> Unit,
    private val onInterrupted: () -> Unit
) {
    private val app = context.applicationContext
    private val audioManager = app.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private var player: MediaPlayer? = null
    private var speed = 1f
    private var prepared = false
    private var seeking = false
    private var playWhenReady = false
    private var playbackRange: PlaybackRange? = null
    private var focusRequest: AudioFocusRequest? = null
    private var hasFocus = false
    private var released = false
    private val handler = Handler(Looper.getMainLooper())
    private var focusListener: AudioManager.OnAudioFocusChangeListener? = null
    private val noisyReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == AudioManager.ACTION_AUDIO_BECOMING_NOISY) onInterrupted()
        }
    }
    init {
        ContextCompat.registerReceiver(app, noisyReceiver,
            IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY), ContextCompat.RECEIVER_NOT_EXPORTED)
    }
    private val checkpoint = object : Runnable {
        override fun run() {
            val mp = player ?: return
            val range = playbackRange ?: return
            if (!playWhenReady) return
            try {
                if (range.hasReachedEnd(mp.currentPosition)) {
                    stop()
                    onFinished()
                } else handler.postDelayed(this, 30)
            } catch (e: IllegalStateException) {
                fail(R.string.audio_position_failed)
            }
        }
    }

    fun play(
        recording: FileDescriptor,
        startMs: Int = 0,
        endMs: Int? = null,
        requiredDurationMs: Int = 0,
        slowPlayback: Boolean = false
    ) {
        stop()
        if (released) return
        try {
            val range = PlaybackRange(startMs, endMs, requiredDurationMs)
            if (!requestFocus()) {
                fail(R.string.audio_focus_failed)
                return
            }
            val mp = MediaPlayer()
            player = mp
            playbackRange = range
            speed = if (slowPlayback) 0.75f else 1f
            playWhenReady = true
            mp.setAudioAttributes(attributes)
            mp.setDataSource(recording)
            mp.setOnCompletionListener {
                if (player === mp) { stop(); onFinished() }
            }
            mp.setOnErrorListener { _, _, _ ->
                if (player === mp) fail(R.string.audio_playback_failed)
                true
            }
            mp.setOnSeekCompleteListener {
                if (player === mp) { seeking = false; startIfReady() }
            }
            mp.setOnPreparedListener {
                if (player !== mp) return@setOnPreparedListener
                try {
                    range.validateDuration(mp.duration)
                    prepared = true
                    if (startMs > 0) {
                        seeking = true
                        if (Build.VERSION.SDK_INT >= 26) {
                            mp.seekTo(startMs.toLong(), MediaPlayer.SEEK_CLOSEST)
                        } else {
                            mp.seekTo(startMs)
                        }
                    } else startIfReady()
                } catch (e: Exception) { fail(R.string.audio_preparation_failed) }
            }
            mp.prepareAsync()
        } catch (e: Exception) { fail(R.string.audio_playback_failed) }
    }

    private fun startIfReady() {
        val mp = player ?: return
        if (!prepared || seeking || !playWhenReady) return
        try {
            if (!requestFocus()) { fail(R.string.audio_focus_failed); return }
            val range = checkNotNull(playbackRange)
            if (range.isSegment && range.hasReachedEnd(mp.currentPosition)) {
                fail(R.string.audio_word_end_reached)
                return
            }
            if (speed == 1f) {
                mp.start()
            } else {
                // Setting a nonzero speed starts a prepared player. Do this only after seek and focus.
                mp.playbackParams = PlaybackParams().setSpeed(speed).setPitch(1f)
            }
            handler.removeCallbacks(checkpoint)
            if (range.isSegment) handler.post(checkpoint)
        } catch (e: Exception) { fail(R.string.audio_playback_failed) }
    }

    private fun requestFocus(): Boolean {
        if (hasFocus) return true
        // A call can already be active before this player requests focus.
        when (audioManager.mode) {
            AudioManager.MODE_RINGTONE, AudioManager.MODE_IN_CALL, AudioManager.MODE_IN_COMMUNICATION -> return false
        }
        val listener = object : AudioManager.OnAudioFocusChangeListener {
            override fun onAudioFocusChange(change: Int) {
                // An abandoned request can still have a callback queued.
                if (focusListener !== this) return
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS, AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> onInterrupted()
                }
            }
        }
        focusListener = listener
        val result = if (Build.VERSION.SDK_INT >= 26) {
            val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                .setAudioAttributes(attributes).setWillPauseWhenDucked(true)
                .setOnAudioFocusChangeListener(listener, handler).build()
            focusRequest = request
            audioManager.requestAudioFocus(request)
        } else {
            audioManager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)
        }
        hasFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
        return hasFocus
    }

    private fun abandonFocus() {
        val listener = focusListener
        focusListener = null
        if (Build.VERSION.SDK_INT >= 26) focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        else if (listener != null && hasFocus) audioManager.abandonAudioFocus(listener)
        focusRequest = null
        hasFocus = false
    }

    private fun fail(message: Int) { stop(); onError(message) }
    fun currentPositionMs(): Int? = if (prepared && !seeking) runCatching { player?.currentPosition }.getOrNull() else null
    fun isPlaying(): Boolean = prepared && !seeking && runCatching { player?.isPlaying == true }.getOrDefault(false)
    fun pause() {
        if (!prepared || seeking) {
            stop() // A pending prepare or seek must be replayed explicitly.
            return
        }
        playWhenReady = false
        handler.removeCallbacks(checkpoint)
        try {
            if (prepared) player?.takeIf { it.isPlaying }?.pause()
            abandonFocus()
        } catch (e: IllegalStateException) {
            fail(R.string.audio_pause_failed)
        }
    }
    fun resume() { if (!released && player != null) { playWhenReady = true; startIfReady() } }
    fun stop() {
        handler.removeCallbacks(checkpoint)
        playWhenReady = false
        prepared = false
        seeking = false
        playbackRange = null
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
