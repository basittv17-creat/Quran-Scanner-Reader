package org.quranscan.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.*
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.core.VerseCandidates
import org.quranscan.reader.ocr.OfflineArabicOcr
import java.util.concurrent.Executors

/** Phase 6 Android-native development app. CameraX live scanner is in LiveScanActivity.
 * Gallery OCR remains available. Recitation is intentionally blocked until trusted data exists.
 */
class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private val bg = Color.rgb(0, 65, 48)
    private val gold = Color.rgb(216, 173, 86)
    private val panel = Color.rgb(12, 91, 71)
    private val worker = Executors.newSingleThreadExecutor()
    private val pickImage = 101
    private var scanToken = 0
    private var profile = OfflineArabicOcr.Profile.AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        profile = runCatching {
            OfflineArabicOcr.Profile.valueOf(getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getString("profile", "AUTO")!!)
        }.getOrDefault(OfflineArabicOcr.Profile.AUTO)
        home()
    }

    override fun onDestroy() {
        scanToken++
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun screen(title: String) {
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(22, 26, 22, 22)
        }
        setContentView(root)
        label(title, 25, gold)
    }
    private fun label(value: String, size: Int = 17, color: Int = Color.WHITE, arabic: Boolean = false) {
        root.addView(TextView(this).apply {
            text = value
            textSize = size.toFloat()
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(6, 12, 6, 12)
            if (arabic) textDirection = View.TEXT_DIRECTION_RTL
        }, LinearLayout.LayoutParams(-1, -2))
    }
    private fun action(text: String, onClick: () -> Unit) {
        root.addView(Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            setTextColor(Color.WHITE)
            background = GradientDrawable().apply {
                setColor(panel)
                cornerRadius = 18f * resources.displayMetrics.density
                setStroke((1f * resources.displayMetrics.density).toInt(), gold)
            }
            setPadding(12, 16, 12, 16)
            setOnClickListener { onClick() }
        }, LinearLayout.LayoutParams(-1, -2).apply {
            setMargins(0, 7, 0, 7)
        })
    }

    private fun home() {
        scanToken++
        screen("☪ Quran Scan Reader")
        label("بِسْمِ اللَّهِ الرَّحْمَٰنِ الرَّحِيمِ", 23, gold, true)
        label("Native offline Quran scanner + local recitation prototype — Phase 9", 14)
        val status = runCatching {
            val db = OfflineCorpus(this)
            "Quran text: ${db.count()}/6236  |  ${if (db.isComplete()) "structurally complete (review pending)" else "incomplete; matching disabled"}"
        }.getOrElse { "Quran corpus checksum error; matching disabled" }
        label(status, 13)
        label("SCAN THE HOLY QURAN", 15, gold)
        action("📷 Live Scan — CameraX offline OCR") { camera() }
        action("▧ Select Image — offline OCR") { gallery() }
        action("▶ Offline Recitation & Word Highlight · Phase 8") { recitation() }
        action("☰ Menu / Settings") { menu() }
        action("WebView design preview") { preview() }
        label("🔒 Offline by design · No Internet permission · OCR results unverified", 12)
    }

    private fun recitation(surah: Int = 1, ayah: Int = 1) {
        startActivity(Intent(this, RecitationActivity::class.java).apply {
            putExtra(RecitationActivity.EXTRA_SURAH, surah)
            putExtra(RecitationActivity.EXTRA_AYAH, ayah)
        })
    }

    private fun camera() {
        startActivity(Intent(this, LiveScanActivity::class.java))
    }
    private fun gallery() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            type = "image/*"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(intent, pickImage)
    }
    @Deprecated("Temporary camera/gallery prototype")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode != RESULT_OK || requestCode != pickImage) return
        val bitmap = data?.data?.let { decodePreview(it) }
        if (bitmap == null) { message("Unable to decode image"); return }
        scan(bitmap)
    }
    private fun decodePreview(uri: Uri): Bitmap? {
        return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (maxOf(bounds.outWidth / sample, bounds.outHeight / sample) > 2048) sample *= 2
        contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        }
        } catch (_: Exception) { null }
    }

    private fun scan(bitmap: Bitmap) {
        val token = ++scanToken
        screen("Offline Arabic OCR")
        label("اسکین جاری ہے — شناخت شدہ متن ابھی غیر مصدقہ ہے", 17, gold, true)
        label("Profile: $profile   •   All processing happens on your phone", 13)
        root.addView(ImageView(this).apply {
            setImageBitmap(bitmap)
            adjustViewBounds = true
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(-1, 0, 1f))
        val progress = ProgressBar(this)
        root.addView(progress)
        action("Home") { home() }
        worker.execute {
            val response = runCatching {
                val ocr = OfflineArabicOcr(applicationContext).read(bitmap, profile)
                val db = OfflineCorpus(applicationContext)
                val candidates = if (db.isComplete()) VerseCandidates.suggest(ocr.rawUnverifiedText, db.asMap())
                    else emptyList()
                Triple(ocr, candidates, db.isComplete())
            }
            runOnUiThread {
                if (token != scanToken || isFinishing || isDestroyed) return@runOnUiThread
                root.removeView(progress)
                response.onSuccess { (ocr, candidates, complete) ->
                    if (ocr.rawUnverifiedText.isBlank()) {
                        label("No Arabic text detected. Try sharper lighting and a closer image.", 15)
                    } else {
                        label("⚠ UNVERIFIED OCR — may contain letter and diacritic errors", 15, gold)
                        val scroll = ScrollView(this)
                        val raw = TextView(this).apply {
                            text = ocr.rawUnverifiedText.take(2000)
                            textSize = 21f
                            setTextColor(Color.WHITE)
                            textDirection = View.TEXT_DIRECTION_RTL
                            gravity = Gravity.RIGHT
                            setPadding(12, 12, 12, 12)
                        }
                        scroll.addView(raw)
                        root.addView(scroll, LinearLayout.LayoutParams(-1, 0, 1f))
                    }
                    if (!complete) label("Quran reference data not installed — NO verse was verified. Recitation disabled.", 14, gold)
                    else if (candidates.isEmpty()) label("No sufficiently strong candidate. Do not use raw OCR as Quran text.", 14)
                    else {
                        label("Potential references ONLY (not verified). Compare visually:", 14, gold)
                        candidates.forEach { c ->
                            label("${c.id.surah}:${c.id.ayah} — ${c.canonicalArabic}", 17, gold, true)
                            action("Review ${c.id.surah}:${c.id.ayah} · do not auto-play") { recitation(c.id.surah, c.id.ayah) }
                        }
                    }
                    label("OCR variants examined: ${ocr.variantsChecked}. Word-level timing requires independently reviewed audio and timestamps.", 12)
                }.onFailure { label("OCR failed safely: ${it.message ?: "Unknown error"}", 14, gold) }
            }
        }
    }

    private fun menu() {
        screen("Menu")
        action("Home") { home() }
        action("Offline Recitation") { recitation() }
        action("Settings") { settings() }
        action("About") { about() }
        action("WebView preview") { preview() }
    }
    private fun settings() {
        screen("Settings")
        label("OCR style selection (same generic Arabic model; script training and print testing pending)", 14)
        action("OCR profile: $profile  — change") {
            val options = OfflineArabicOcr.Profile.entries
            profile = options[(options.indexOf(profile) + 1) % options.size]
            getSharedPreferences("quran_reader_settings", MODE_PRIVATE).edit()
                .putString("profile", profile.name).apply()
            settings()
        }
        label("Bundled Arabic model: tessdata_fast / ara.traineddata, Apache-2.0.  No INTERNET permission.", 14)
        action("Back") { menu() }
    }
    private fun about() {
        screen("About")
        label("Quran Scan Reader Phase 9 CameraX + Arabic OCR + local recitation engine baseline.\n\n" +
            "OCR: Tesseract4Android + Arabic tessdata_fast (Apache-2.0).\n" +
            "Canonical Quran source: Tanzil Uthmani v1.1 (CC BY 3.0), https://tanzil.net.\n" +
            "Tanzil Uthmani v1.1 publisher text is bundled verbatim with integrity checks; independent scholarly and rendering review remains. Arabic OCR has not been benchmarked on actual Pakistani or Madani mushafs.\n" +
            "Live CameraX preview and sampled OCR are implemented in source, but not yet tested on physical Android. " +
            "A local ayah-audio importer and player are implemented, but no licensed recordings have been bundled. Word-level timestamp import and highlighting code exist; reviewed timing data and precise diacritic checking remain missing.", 14)
        action("Back") { menu() }
    }
    private fun preview() {
        val web = android.webkit.WebView(this)
        web.settings.javaScriptEnabled = false
        web.loadUrl("file:///android_asset/preview.html")
        setContentView(web)
    }
    private fun message(text: String) { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    @Deprecated("Prototype navigation")
    override fun onBackPressed() { home() }
}
