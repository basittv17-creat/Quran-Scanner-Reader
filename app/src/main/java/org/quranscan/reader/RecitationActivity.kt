package org.quranscan.reader

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.core.os.ConfigurationCompat
import androidx.core.text.BidiFormatter
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.doAfterTextChanged
import org.quranscan.reader.audio.AudioVerseKey
import org.quranscan.reader.audio.OfflineAudioPackStore
import org.quranscan.reader.audio.OfflineAyahPlayer
import org.quranscan.reader.audio.OfflineWordTimingStore
import org.quranscan.reader.audio.RecitationMode
import org.quranscan.reader.audio.RecitationPlaybackGate
import org.quranscan.reader.audio.TimedWord
import org.quranscan.reader.audio.VerifiedRecording
import org.quranscan.reader.audio.WordHighlightText
import org.quranscan.reader.audio.WordTimingFormat
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.core.QuranVerseCounts
import org.quranscan.reader.core.VerseId
import org.quranscan.reader.core.VerseNavigation
import java.util.concurrent.Executors

/** Native verse + word-following playback. Nothing is verified from OCR alone.
 * Imported audio and timings require independent listening review.
 */
class RecitationActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SURAH = "selected_surah"
        const val EXTRA_AYAH = "selected_ayah"
        const val EXTRA_AUTO_PLAY = "auto_play_exact_scan"
        const val EXTRA_SCANNED_TEXT = "scanned_text_unverified"
        const val MAX_COMPARISON_LENGTH = 2000
    }
    private val dark = Color.rgb(0, 65, 48)
    private val gold = Color.rgb(216, 173, 86)
    private val audioStore by lazy { OfflineAudioPackStore(applicationContext) }
    private val timingStore by lazy { OfflineWordTimingStore(applicationContext) }
    private val corpus by lazy { OfflineCorpus(applicationContext) }
    private val executor = Executors.newSingleThreadExecutor()
    private val timer = Handler(Looper.getMainLooper())
    private lateinit var status: TextView
    private lateinit var verseView: TextView
    private lateinit var verseReference: TextView
    private lateinit var scanHeading: TextView
    private lateinit var scannedText: TextView
    private lateinit var surahInput: EditText
    private lateinit var ayahInput: EditText
    private lateinit var player: OfflineAyahPlayer
    private val bidi by lazy { BidiFormatter.getInstance(ConfigurationCompat.getLocales(resources.configuration)[0] ?: java.util.Locale.getDefault()) }
    private val preferences by lazy { getSharedPreferences("quran_reader_settings", MODE_PRIVATE) }
    private var mode = RecitationMode.FULL_AYAH
    private lateinit var modeLabel: TextView
    private lateinit var confirmation: CheckBox
    private var slowPlayback = false
    private var selectionToken = 0
    private val playbackGate = RecitationPlaybackGate()
    private var recordingSelection: Pair<String, String>? = null
    private var activeKey = AudioVerseKey(1, 1)
    private var canonicalArabic: String? = null
    private var timing: List<TimedWord>? = null
    private var importBusy = false
    private var chosenWord = 0
    private var highlightMode: RecitationMode? = null
    private var lastHighlighted = -2

    private val highlightLoop = object : Runnable {
        override fun run() {
            if (highlightMode != null && player.isPlaying()) {
                val word = WordTimingFormat.highlightIndex(
                    timing.orEmpty(), player.currentPositionMs(),
                    selectedIndex = chosenWord.takeIf { highlightMode == RecitationMode.SELECTED_WORD }
                )
                if (word >= 0 && highlightMode == RecitationMode.FOLLOW_WORDS) chosenWord = word
                if (word != lastHighlighted) displayWord(word)
            }
            timer.postDelayed(this, 80)
        }
    }

    private val importAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        clearPlaybackConfirmation()
        importBusy = true
        status.text = getString(R.string.recitation_check_audio)
        executor.execute {
            val result = runCatching { audioStore.installArchive(uri) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                importBusy = false
                val notice = result.fold(
                    onSuccess = { resources.getQuantityString(R.plurals.audio_imported, it.entries.size, it.entries.size) },
                    onFailure = { getString(R.string.audio_import_rejected) }
                )
                loadSelection(notice = notice)
            }
        }
    }

    private val importTiming = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        clearPlaybackConfirmation()
        importBusy = true
        status.text = getString(R.string.recitation_check_timing)
        executor.execute {
            val result = runCatching { timingStore.install(uri, corpus, audioStore.installedManifest()) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                importBusy = false
                val notice = result.fold(
                    onSuccess = { resources.getQuantityString(R.plurals.timing_imported, it.entries.size, it.entries.size) },
                    onFailure = { getString(R.string.timing_import_rejected) }
                )
                loadSelection(notice = notice)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arabicSize = preferences.getInt("arabic_size", 25).coerceIn(21, 37)
        mode = RecitationMode.restore(preferences.getString("recitation_mode", null))
        window.decorView.keepScreenOn = preferences.getBoolean("keep_screen_on", false)
        player = OfflineAyahPlayer(
            applicationContext,
            onFinished = { status.text = getString(R.string.recitation_finished); displayWord(-1) },
            onError = { status.setText(it); clearPlaybackConfirmation() },
            onInterrupted = {
                val wasConfirmed = playbackGate.canPlay
                clearPlaybackConfirmation()
                if (wasConfirmed) status.setText(R.string.confirm_before_play)
            }
        )
        buildScreen()
        verseView.textSize = arabicSize.toFloat()
        val s = savedInstanceState?.getInt("surah") ?: intent.getIntExtra(EXTRA_SURAH, 1)
        val a = savedInstanceState?.getInt("ayah") ?: intent.getIntExtra(EXTRA_AYAH, 1)
        val initial = if (QuranVerseCounts.valid(s, a)) AudioVerseKey(s, a) else activeKey
        showReference(initial.surah, initial.ayah)
        loadSelection(
            savedInstanceState?.getInt("chosen_word") ?: 0,
            autoPlay = savedInstanceState == null && intent.getBooleanExtra(EXTRA_AUTO_PLAY, false)
        )
    }

    private fun buildScreen() {
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(18), dp(20), dp(18), dp(30))
            setBackgroundColor(dark)
        }
        setContentView(ScrollView(this).apply { isFillViewport = true; addView(column); applyReaderInsets() })
        fun heading(value: String, size: Float, color: Int = Color.WHITE, rtl: Boolean = false): TextView {
            return TextView(this).apply {
                text = value; textSize = size; setTextColor(color); gravity = Gravity.CENTER
                setPadding(dp(8), dp(14), dp(8), dp(14))
                if (rtl) textDirection = View.TEXT_DIRECTION_RTL
                column.addView(this)
            }
        }
        fun button(value: String, callback: () -> Unit) {
            column.addView(Button(this).apply {
                text = value
                isAllCaps = false
                minHeight = dp(51)
                setOnClickListener {
                    if (importBusy) status.setText(R.string.import_busy)
                    else callback()
                }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        heading(getString(R.string.recitation_title), 22f, gold)
        heading(getString(R.string.recitation_intro), 15f)
        fun numberInput(label: Int): EditText {
            val field = EditText(this).apply {
                id = View.generateViewId()
                inputType = InputType.TYPE_CLASS_NUMBER
                minHeight = dp(48)
                setTextColor(Color.WHITE)
                gravity = Gravity.START
            }
            column.addView(TextView(this).apply {
                setText(label)
                setTextColor(Color.WHITE)
                labelFor = field.id
            })
            column.addView(field, LinearLayout.LayoutParams(-1, -2))
            return field
        }
        surahInput = numberInput(R.string.recitation_surah)
        ayahInput = numberInput(R.string.recitation_ayah)
        button(getString(R.string.recitation_choose_ayah)) { loadSelection() }
        button(getString(R.string.recitation_previous_ayah)) { moveAyah(-1) }
        button(getString(R.string.recitation_next_ayah)) { moveAyah(1) }
        scanHeading = heading(getString(R.string.scan_comparison_unverified), 15f, gold)
        scannedText = heading("", 18f, Color.WHITE, true)
        verseReference = heading("", 16f, gold)
        verseView = heading("", 25f, gold, true).apply {
            setBackgroundColor(Color.rgb(12, 83, 65))
            layoutDirection = View.LAYOUT_DIRECTION_RTL
        }
        status = heading(getString(R.string.recitation_check_resources), 14f, gold).apply {
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        updateScanComparison(null)
        confirmation = CheckBox(this).apply {
            text = getString(R.string.confirm_verse)
            setTextColor(Color.WHITE)
            minHeight = dp(48)
            setOnCheckedChangeListener { _, checked ->
                if (checked) playbackGate.confirm() else revokePlayback()
            }
        }
        column.addView(confirmation)
        modeLabel = heading("", 16f, gold)
        showMode()
        button(getString(R.string.change_recitation_mode)) {
            setMode(RecitationMode.entries[(mode.ordinal + 1) % RecitationMode.entries.size])
        }
        slowPlayback = preferences.getBoolean("recitation_slow_playback", false)
        column.addView(CheckBox(this).apply {
            setText(R.string.recitation_slow_playback)
            setTextColor(Color.WHITE)
            minHeight = dp(48)
            isChecked = slowPlayback
            setOnCheckedChangeListener { _, checked ->
                cancelPlayback()
                slowPlayback = checked
                preferences.edit().putBoolean("recitation_slow_playback", checked).apply()
            }
        })
        button(getString(R.string.play_selected_mode)) { playPreferred() }
        button(getString(R.string.replay_selected_mode)) { playPreferred() }
        button(getString(R.string.recitation_play_ayah)) { setMode(RecitationMode.FULL_AYAH); playPreferred() }
        button(getString(R.string.recitation_play_follow)) { setMode(RecitationMode.FOLLOW_WORDS); playPreferred() }
        button(getString(R.string.recitation_previous_word)) { selectWord(chosenWord - 1) }
        button(getString(R.string.recitation_next_word)) { selectWord(chosenWord + 1) }
        button(getString(R.string.recitation_play_word)) { setMode(RecitationMode.SELECTED_WORD); playPreferred() }
        button(getString(R.string.recitation_pause)) {
            playbackGate.cancelRequest()
            player.pause()
            displayWord(-1)
        }
        button(getString(R.string.recitation_resume)) { if (playbackGate.canPlay) player.resume() else status.setText(R.string.confirm_before_play) }
        button(getString(R.string.recitation_stop)) { cancelPlayback() }
        button(getString(R.string.recitation_import_audio)) {
            cancelPlayback()
            importAudio.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        }
        button(getString(R.string.choose_audio_pack)) { managePacks(false) }
        button(getString(R.string.remove_audio_pack)) { managePacks(true) }
        button(getString(R.string.recitation_import_timing)) {
            cancelPlayback()
            importTiming.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        }
        heading(getString(R.string.recitation_checks_notice), 13f)
        button(getString(R.string.nav_home)) { finish() }
        surahInput.doAfterTextChanged { invalidateEditedReference() }
        ayahInput.doAfterTextChanged { invalidateEditedReference() }
    }

    private fun invalidateEditedReference() {
        selectionToken++
        confirmation.isEnabled = false
        clearPlaybackConfirmation()
        updateScanComparison(null)
    }

    private fun managePacks(remove: Boolean) {
        clearPlaybackConfirmation()
        importBusy = true
        executor.execute {
            val result = runCatching { audioStore.installedPacks() to audioStore.selectedPackId() }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                importBusy = false
                result.onSuccess { (all, selected) ->
                    val options = if (remove) all.filter { it.id != selected } else all.filter { it.manifest != null }
                    if (options.isEmpty()) { status.setText(R.string.no_selectable_pack); return@onSuccess }
                    val labels = options.map { pack ->
                        val data = pack.manifest
                        if (data == null) getString(R.string.corrupt_audio_pack)
                        else getString(R.string.audio_pack_label, bidi.unicodeWrap(data.reciter),
                            resources.getQuantityString(R.plurals.audio_clip_count, data.entries.size, data.entries.size),
                            bidi.unicodeWrap(data.source))
                    }.toTypedArray()
                    val dialog = android.app.AlertDialog.Builder(this)
                        .setTitle(if (remove) R.string.remove_audio_pack else R.string.choose_audio_pack)
                        .setNegativeButton(android.R.string.cancel, null)
                    if (remove) {
                        dialog.setItems(labels) { _, index ->
                            android.app.AlertDialog.Builder(this)
                                .setMessage(getString(R.string.confirm_remove_pack, labels[index]))
                                .setNegativeButton(android.R.string.cancel, null)
                                .setPositiveButton(R.string.remove_pack) { _, _ -> changePack(options[index].id, true) }.show()
                        }
                    } else {
                        dialog.setSingleChoiceItems(labels, options.indexOfFirst { it.id == selected }) { choice, index ->
                            choice.dismiss()
                            changePack(options[index].id, false)
                        }
                    }
                    dialog.show()
                }.onFailure { status.setText(R.string.audio_pack_list_failed) }
            }
        }
    }

    private fun changePack(id: String, remove: Boolean) {
        clearPlaybackConfirmation()
        selectionToken++
        importBusy = true
        executor.execute {
            val result = runCatching {
                if (remove) audioStore.removeInactivePack(id) else audioStore.selectPack(id)
            }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                importBusy = false
                result.onSuccess {
                    loadSelection(notice = getString(if (remove) R.string.audio_pack_removed else R.string.audio_pack_selected))
                }.onFailure { status.setText(R.string.audio_pack_change_failed) }
            }
        }
    }

    private fun moveAyah(direction: Int) {
        val next = VerseNavigation.adjacent(VerseId(activeKey.surah, activeKey.ayah), direction)
        if (next == null) { status.text = getString(R.string.recitation_boundary); return }
        showReference(next.surah, next.ayah)
        loadSelection() // Selection never starts playback.
    }

    private fun showReference(surah: Int, ayah: Int) {
        surahInput.setText(getString(R.string.reference_number, surah))
        ayahInput.setText(getString(R.string.reference_number, ayah))
    }

    private fun displayWord(index: Int) {
        val fullText = canonicalArabic ?: return
        if (lastHighlighted == index) return
        lastHighlighted = index
        verseView.text = WordHighlightText.decorate(fullText, index)
    }

    private data class LoadedVerse(
        val arabic: String,
        val reciter: String?,
        val recording: Pair<String, String>?,
        val windows: List<TimedWord>?
    )

    private fun loadSelection(restoredWord: Int = 0, notice: String? = null, autoPlay: Boolean = false) {
        clearPlaybackConfirmation()
        confirmation.isEnabled = false
        lastHighlighted = -2
        canonicalArabic = null
        timing = null
        verseReference.text = ""
        updateScanComparison(null)
        recordingSelection = null
        val token = ++selectionToken
        val s = surahInput.text.toString().toIntOrNull()
        val a = ayahInput.text.toString().toIntOrNull()
        if (s == null || a == null || !QuranVerseCounts.valid(s, a)) {
            verseView.text = ""
            status.text = getString(R.string.recitation_invalid_reference)
            return
        }
        val key = AudioVerseKey(s, a)
        activeKey = key
        chosenWord = restoredWord
        verseView.text = ""
        status.setText(R.string.loading_local_verse)
        // Corpus parsing, manifest reads and timing hashes must not block Android's UI thread.
        executor.execute {
            val loaded = runCatching {
                check(corpus.isComplete()) { "Verified complete Quran corpus missing" }
                val verse = corpus.find(VerseId(s, a)) ?: error("Canonical verse missing")
                val packId = runCatching { audioStore.selectedPackId() }.getOrNull()
                val audio = audioStore.installedManifest()
                val windows = timingStore.timeline(key, verse, audio)
                val selection = audio?.entries?.get(key)?.sha256?.let { sha ->
                    if (packId != null && audioStore.selectedPackId() == packId) packId to sha else null
                }
                LoadedVerse(verse, audio?.reciter, selection, windows.takeIf { selection != null })
            }
            runOnUiThread {
                if (token != selectionToken || isFinishing || isDestroyed) return@runOnUiThread
                loaded.onSuccess { (verse, reciter, selection, windows) ->
                    canonicalArabic = verse
                    confirmation.isEnabled = true
                    timing = windows
                    recordingSelection = selection
                    chosenWord = restoredWord.coerceIn(0, (windows?.size ?: 1) - 1)
                    verseReference.text = getString(R.string.canonical_ayah_reference, key.surah, key.ayah)
                    displayWord(if (mode == RecitationMode.SELECTED_WORD && windows != null) chosenWord else -1)
                    updateScanComparison(key)
                    val details = when {
                        selection == null -> getString(R.string.recitation_no_recording_for_verse, s, a)
                        windows == null -> getString(R.string.recitation_no_matching_timings, bidi.unicodeWrap(reciter.orEmpty()))
                        else -> resources.getQuantityString(R.plurals.recitation_timing_count,
                            windows.size, bidi.unicodeWrap(reciter.orEmpty()), windows.size)
                    }
                    status.text = if (notice == null) details else getString(R.string.status_with_details, notice, details)
                    if (autoPlay && selection != null && lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                        mode = if (windows == null) RecitationMode.FULL_AYAH else RecitationMode.FOLLOW_WORDS
                        showMode()
                        confirmation.isChecked = true
                        playPreferred()
                    }
                }.onFailure {
                    status.setText(R.string.recitation_load_failed)
                }
            }
        }
    }

    private fun updateScanComparison(key: AudioVerseKey?) {
        val raw = intent.getStringExtra(EXTRA_SCANNED_TEXT)?.take(MAX_COMPARISON_LENGTH)?.trim()
        val matches = key != null && key.surah == intent.getIntExtra(EXTRA_SURAH, -1) &&
            key.ayah == intent.getIntExtra(EXTRA_AYAH, -1) && !raw.isNullOrEmpty()
        scanHeading.visibility = if (matches) View.VISIBLE else View.GONE
        scannedText.visibility = scanHeading.visibility
        scannedText.text = if (matches) raw else ""
    }

    private fun showMode() {
        val label = when (mode) {
            RecitationMode.FULL_AYAH -> R.string.mode_full_ayah
            RecitationMode.FOLLOW_WORDS -> R.string.mode_follow_words
            RecitationMode.SELECTED_WORD -> R.string.mode_selected_word
        }
        modeLabel.text = getString(R.string.current_recitation_mode, getString(label))
    }

    private fun setMode(value: RecitationMode) {
        cancelPlayback()
        mode = value
        preferences.edit().putString("recitation_mode", mode.name).apply()
        showMode()
    }

    private fun playPreferred() {
        if (!playbackGate.canPlay) { status.setText(R.string.confirm_before_play); return }
        if (mode == RecitationMode.SELECTED_WORD) playWord() else playFull()
    }

    private fun playFull() {
        val followWords = mode == RecitationMode.FOLLOW_WORDS
        if (followWords && timing == null) {
            status.setText(R.string.recitation_no_exact_timings)
            return
        }
        val notice = getString(if (followWords) R.string.recitation_preparing_follow
            else R.string.recitation_preparing_full)
        requestPlayback(notice, requiredDurationMs = if (followWords) timing?.lastOrNull()?.endMs ?: 0 else 0)
    }

    private fun selectWord(index: Int) {
        val words = timing ?: run { status.text = getString(R.string.recitation_no_timings); return }
        cancelPlayback()
        chosenWord = index.coerceIn(0, words.lastIndex)
        displayWord(chosenWord)
        status.text = getString(R.string.recitation_word_selected, chosenWord + 1, words.size)
    }

    private fun playWord() {
        val windows = timing
        val window = windows?.getOrNull(chosenWord)
        if (windows == null || window == null) {
            status.setText(R.string.recitation_no_exact_timings)
            return
        }
        requestPlayback(
            getString(R.string.recitation_preparing_word, chosenWord + 1),
            window.startMs, window.endMs, windows.last().endMs
        )
    }

    private fun cancelPlayback() {
        playbackGate.cancelRequest()
        highlightMode = null
        player.stop()
        displayWord(-1)
    }

    private fun clearPlaybackConfirmation() {
        if (confirmation.isChecked) confirmation.isChecked = false
        else revokePlayback()
    }

    private fun revokePlayback() {
        playbackGate.revokeConfirmation()
        cancelPlayback()
    }

    private fun requestPlayback(notice: String, startMs: Int = 0, endMs: Int? = null, requiredDurationMs: Int = 0) {
        val selection = recordingSelection
        if (selection == null) {
            status.setText(R.string.recitation_no_audio)
            return
        }
        cancelPlayback()
        val request = playbackGate.beginRequest() ?: return
        val (packId, expectedSha) = selection
        val key = activeKey
        val requestedMode = mode
        status.setText(R.string.recitation_verifying_audio)
        executor.execute {
            val verified = runCatching {
                check(audioStore.selectedPackId() == packId &&
                    audioStore.installedManifest()?.entries?.get(key)?.sha256 == expectedSha) {
                    "The local recording selection changed. Choose the verse again."
                }
                val file = audioStore.audioFile(key) ?: error("Local audio file is missing")
                check(audioStore.selectedPackId() == packId) { "The local audio pack changed. Choose the verse again." }
                val input = VerifiedRecording.open(file, expectedSha)
                try {
                    check(audioStore.selectedPackId() == packId) { "The local audio pack changed. Choose the verse again." }
                    input
                } catch (error: Exception) {
                    input.close()
                    throw error
                }
            }
            runOnUiThread {
                val recording = verified.getOrNull()
                try {
                    if (!playbackGate.accepts(request) || isFinishing || isDestroyed || importBusy) return@runOnUiThread
                    if (recording == null) {
                        clearPlaybackConfirmation()
                        status.setText(R.string.recitation_recording_check_failed)
                        return@runOnUiThread
                    }
                    highlightMode = requestedMode.takeUnless { it == RecitationMode.FULL_AYAH }
                    status.text = notice
                    player.play(recording.fd, startMs, endMs, requiredDurationMs, slowPlayback)
                } finally {
                    // MediaPlayer duplicates the descriptor synchronously in setDataSource.
                    runCatching { recording?.close() }
                }
            }
        }
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    override fun onResume() {
        super.onResume()
        playbackGate.enterForeground()
        timer.post(highlightLoop)
    }

    override fun onPause() {
        timer.removeCallbacks(highlightLoop)
        playbackGate.leaveForeground()
        clearPlaybackConfirmation()
        super.onPause()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("surah", activeKey.surah)
        outState.putInt("ayah", activeKey.ayah)
        outState.putInt("chosen_word", chosenWord)
        // Confirmation and playback are deliberately not restored after rotation/process recreation.
        super.onSaveInstanceState(outState)
    }

    override fun onDestroy() {
        selectionToken++
        timer.removeCallbacks(highlightLoop)
        player.release()
        executor.shutdownNow()
        super.onDestroy()
    }
}
