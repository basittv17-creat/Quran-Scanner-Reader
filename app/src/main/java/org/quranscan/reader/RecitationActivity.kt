package org.quranscan.reader

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import org.quranscan.reader.audio.AudioVerseKey
import org.quranscan.reader.audio.OfflineAudioPackStore
import org.quranscan.reader.audio.OfflineAyahPlayer
import org.quranscan.reader.audio.OfflineWordTimingStore
import org.quranscan.reader.audio.TimedWord
import org.quranscan.reader.audio.WordHighlightText
import org.quranscan.reader.audio.WordTimingFormat
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.core.QuranVerseCounts
import org.quranscan.reader.core.VerseId
import org.quranscan.reader.core.VerseNavigation
import java.util.concurrent.Executors

/** Native verse + word-following playback. Nothing is verified from OCR alone.
 * Import requires independently reviewed corpus, audio and timing source before activation.
 */
class RecitationActivity : ComponentActivity() {
    companion object {
        const val EXTRA_SURAH = "selected_surah"
        const val EXTRA_AYAH = "selected_ayah"
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
    private lateinit var surahInput: EditText
    private lateinit var ayahInput: EditText
    private lateinit var player: OfflineAyahPlayer
    private var activeKey = AudioVerseKey(1, 1)
    private var canonicalArabic: String? = null
    private var timing: List<TimedWord>? = null
    private var chosenWord = 0
    private var followWords = false
    private var lastHighlighted = -2

    private val highlightLoop = object : Runnable {
        override fun run() {
            if (followWords && player.isPlaying()) {
                val ms = player.currentPositionMs()
                val word = if (ms == null) -1 else WordTimingFormat.activeIndex(timing.orEmpty(), ms)
                if (word >= 0) chosenWord = word
                if (word != lastHighlighted) displayWord(word)
            }
            timer.postDelayed(this, 80)
        }
    }

    private val importAudio = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        player.stop()
        status.text = "آف لائن آڈیو پیک کی جانچ جاری ہے…"
        executor.execute {
            val result = runCatching { audioStore.installArchive(uri) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                status.text = result.fold(
                    onSuccess = { "Audio clips: ${it.entries.size}. Source not independently audited. Existing word timings may no longer apply." },
                    onFailure = { "Audio import rejected: ${it.message ?: "unknown error"}" }
                )
                loadSelection()
            }
        }
    }

    private val importTiming = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri == null) return@registerForActivityResult
        player.stop()
        status.text = "لفظ بہ لفظ ٹائمنگ پیک چیک کیا جا رہا ہے…"
        executor.execute {
            val result = runCatching { timingStore.install(uri, corpus, audioStore.installedManifest()) }
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                status.text = result.fold(
                    onSuccess = { "Timing files accepted: ${it.entries.size}. Alignment accuracy is NOT independently authenticated." },
                    onFailure = { "Timing import rejected: ${it.message ?: "unknown error"}" }
                )
                loadSelection()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val arabicSize = getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getInt("arabic_size", 25).coerceIn(21, 37)
        player = OfflineAyahPlayer(
            applicationContext,
            onFinished = { status.text = "آڈیو ختم ہوگئی۔"; displayWord(-1) },
            onError = { status.text = it; displayWord(-1) }
        )
        buildScreen()
        verseView.textSize = arabicSize.toFloat()
        val s = intent.getIntExtra(EXTRA_SURAH, 1)
        val a = intent.getIntExtra(EXTRA_AYAH, 1)
        if (QuranVerseCounts.valid(s, a)) {
            surahInput.setText(s.toString()); ayahInput.setText(a.toString())
        }
        loadSelection()
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
                text = value; isAllCaps = false; setOnClickListener { callback() }
            }, LinearLayout.LayoutParams(-1, dp(51)))
        }
        heading("Quran Scan Reader · Offline Recitation", 22f, gold)
        heading("قرآنی متن: Tanzil Uthmani v1.1۔ آڈیو پیک الگ درکار ہے؛ OCR سے خودکار تلاوت نہیں ہوتی۔", 15f, Color.WHITE, true)
        val inputs = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        surahInput = EditText(this).apply {
            hint = "Surah"; setText("1"); inputType = 2; setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY); gravity = Gravity.CENTER
        }
        ayahInput = EditText(this).apply {
            hint = "Ayah"; setText("1"); inputType = 2; setTextColor(Color.WHITE)
            setHintTextColor(Color.LTGRAY); gravity = Gravity.CENTER
        }
        inputs.addView(surahInput, LinearLayout.LayoutParams(0, dp(58), 1f))
        inputs.addView(ayahInput, LinearLayout.LayoutParams(0, dp(58), 1f))
        column.addView(inputs)
        button("آیت منتخب کریں") { loadSelection() }
        button("◀ پچھلی آیت") { moveAyah(-1) }
        button("اگلی آیت ▶") { moveAyah(1) }
        verseView = heading("", 25f, gold, true)
        button("▶ مکمل آیت چلائیں") { followWords = false; displayWord(-1); playFull() }
        button("▶ مکمل آیت + الفاظ ہائی لائٹ") { followWords = true; playFull() }
        button("◀ پچھلا لفظ") { selectWord(chosenWord - 1) }
        button("اگلا لفظ ▶") { selectWord(chosenWord + 1) }
        button("▶ منتخب لفظ سنیں") { playWord() }
        button("⏸ Pause") { player.pause() }
        button("▶ Resume") { player.resume() }
        button("■ Stop") { player.stop(); displayWord(-1) }
        button("▣ آڈیو MP3 ZIP امپورٹ کریں") {
            player.stop()
            importAudio.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        }
        button("▣ لفظ بہ لفظ Timing ZIP امپورٹ کریں") {
            player.stop()
            importTiming.launch(arrayOf("application/zip", "application/x-zip-compressed", "application/octet-stream"))
        }
        status = heading("Checking local resources…", 14f, gold)
        heading("SHA صرف فائل کی سالمیت چیک کرتا ہے؛ قاری، تلفظ، اعراب اور لفظی اوقات کی انسانی تصدیق باقی رہتی ہے۔", 13f)
        button("← Home") { finish() }
    }

    private fun moveAyah(direction: Int) {
        val next = VerseNavigation.adjacent(VerseId(activeKey.surah, activeKey.ayah), direction)
        if (next == null) { status.text = "No adjacent verse at this boundary."; return }
        surahInput.setText(next.surah.toString())
        ayahInput.setText(next.ayah.toString())
        loadSelection() // Selection never starts playback.
    }

    private fun displayWord(index: Int) {
        val fullText = canonicalArabic ?: return
        lastHighlighted = index
        verseView.text = WordHighlightText.decorate(fullText, index)
    }

    private fun loadSelection() {
        player.stop()
        followWords = false
        lastHighlighted = -2
        val s = surahInput.text.toString().toIntOrNull()
        val a = ayahInput.text.toString().toIntOrNull()
        if (s == null || a == null || !QuranVerseCounts.valid(s, a)) {
            canonicalArabic = null
            timing = null
            verseView.text = "Invalid Surah / Ayah"
            status.text = "Select a valid Quran verse reference."
            return
        }
        activeKey = AudioVerseKey(s, a)
        val complete = runCatching { corpus.isComplete() }.getOrDefault(false)
        canonicalArabic = if (complete) runCatching { corpus.find(VerseId(s, a)) }.getOrNull() else null
        verseView.text = canonicalArabic ?: "مستند قرآنی متن دستیاب نہیں ہے۔"
        val audioManifest = audioStore.installedManifest()
        timing = canonicalArabic?.let { verse -> timingStore.timeline(activeKey, verse, audioManifest) }
        chosenWord = 0
        val audio = audioManifest?.entries?.get(activeKey)
        status.text = when {
            !complete -> "Verified complete Quran corpus missing. Audio and timing disabled."
            audio == null -> "No licensed local ayah recording installed for $s:$a."
            timing == null -> "Ayah audio exists, but no validated local word timings for this exact audio/text."
            else -> "${timing!!.size} timing windows loaded. Timings are user-supplied and require human listening review."
        }
    }

    private fun playFull() {
        val file = approvedAudio() ?: return
        if (followWords && timing == null) {
            status.text = "Word highlighting unavailable: import timings for the SAME audio + canonical text."
            return
        }
        status.text = if (followWords) "Following audio timing; approximate MP3 seek and unverified timing quality." else "Preparing local full ayah audio…"
        player.play(file)
    }

    private fun selectWord(index: Int) {
        val words = timing ?: run { status.text = "Word timings are not installed for this verse."; return }
        player.stop()
        chosenWord = index.coerceIn(0, words.lastIndex)
        displayWord(chosenWord)
        status.text = "Selected word ${chosenWord + 1}/${words.size} — timings may require manual review."
    }

    private fun playWord() {
        val file = approvedAudio() ?: return
        val window = timing?.getOrNull(chosenWord)
        if (window == null) {
            status.text = "Cannot isolate word: valid word timings are missing."
            return
        }
        followWords = false
        displayWord(chosenWord)
        status.text = "Preparing word ${chosenWord + 1}; MP3 seek may be approximate."
        player.play(file, window.startMs, window.endMs)
    }

    private fun approvedAudio(): java.io.File? {
        if (canonicalArabic == null) { status.text = "Playback blocked: verified corpus is incomplete."; return null }
        val file = audioStore.audioFile(activeKey)
        if (file == null) status.text = "No local audio file: import a licensed, checked MP3 pack."
        return file
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density + 0.5f).toInt()
    override fun onStart() {
        super.onStart()
        timer.post(highlightLoop)
    }

    override fun onStop() {
        timer.removeCallbacks(highlightLoop)
        player.pause()
        super.onStop()
    }

    override fun onDestroy() {
        timer.removeCallbacks(highlightLoop)
        player.release()
        executor.shutdownNow()
        super.onDestroy()
    }
}
