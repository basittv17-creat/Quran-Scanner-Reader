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
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import org.quranscan.reader.core.ImageDecodeLimits
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.core.VerseCandidates
import org.quranscan.reader.ocr.OfflineArabicOcr
import java.util.concurrent.Executors

/** Phase 6 Android-native development app. CameraX live scanner is in LiveScanActivity.
 * Gallery OCR remains available. Recitation is intentionally blocked until trusted data exists.
 */
class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var drawer: DrawerLayout
    private val preferences by lazy { getSharedPreferences("quran_reader_settings", MODE_PRIVATE) }
    private var arabicSize = 25
    private val bg = Color.rgb(0, 65, 48)
    private val gold = Color.rgb(216, 173, 86)
    private val panel = Color.rgb(12, 91, 71)
    private val worker = Executors.newSingleThreadExecutor()
    private val pickImage = 101
    private var scanToken = 0
    private var profile = OfflineArabicOcr.Profile.AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        arabicSize = preferences.getInt("arabic_size", 25).coerceIn(21, 37)
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

    private fun screen(title: String, scrollable: Boolean = true) {
        scanToken++ // Navigation invalidates pending gallery results.
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(22, 26, 22, 22)
        }
        drawer = DrawerLayout(this)
        val content: View = if (scrollable) ScrollView(this).apply { isFillViewport = true; addView(root) } else root
        drawer.addView(content, DrawerLayout.LayoutParams(-1, -1))
        val side = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(16, 32, 16, 24)
            setBackgroundColor(bg)
        }
        fun item(title: String, click: () -> Unit) {
            side.addView(Button(this).apply {
                text = title; isAllCaps = false; setTextColor(Color.WHITE)
                setOnClickListener { drawer.closeDrawers(); click() }
            }, LinearLayout.LayoutParams(-1, -2))
        }
        side.addView(TextView(this).apply { text = "Quran Scan Reader"; textSize = 23f; setTextColor(gold) })
        item("Home") { home() }
        item("Live scanner") { camera() }
        item("Gallery scanner") { gallery() }
        item("Read Quran / recitation") { recitation() }
        item("Settings") { settings() }
        item("Help") { help() }
        item("About / text source") { about() }
        item("Exit") { finishAndRemoveTask() }
        item("Offline design preview") { preview() }
        drawer.addView(ScrollView(this).apply { addView(side) }, DrawerLayout.LayoutParams(
            (280 * resources.displayMetrics.density).toInt(), -1, Gravity.START))
        setContentView(drawer)
        drawer.applyReaderInsets()
        root.addView(Button(this).apply {
            text = "☰ Menu"; contentDescription = "Open navigation menu"
            setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        }, LinearLayout.LayoutParams(-2, -2))
        label(title, 25, gold)
    }
    private fun label(value: String, size: Int = 17, color: Int = Color.WHITE, arabic: Boolean = false) {
        root.addView(TextView(this).apply {
            text = value
            textSize = if (arabic) maxOf(size, arabicSize).toFloat() else size.toFloat()
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
        runCatching { OfflineCorpus(this).find(org.quranscan.reader.core.VerseId(1, 1)) }.getOrNull()?.let { label(it, 23, gold, true) }
        label("Offline Quran text • Camera and gallery scanning", 14)
        val status = runCatching {
            val db = OfflineCorpus(this)
            "Quran text: ${db.count()}/6236  |  ${if (db.isComplete()) "structurally complete (review pending)" else "incomplete; matching disabled"}"
        }.getOrElse { "Quran corpus checksum error; matching disabled" }
        label(status, 13)
        label("SCAN THE HOLY QURAN", 15, gold)
        action("📷 Live Quran Scan") { camera() }
        action("▧ Upload Quran Picture") { gallery() }
        action("▶ Offline Recitation & Word Highlight · Phase 8") { recitation() }
        action("Settings") { settings() }
        action("Help") { help() }
        action("About") { about() }
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
        val uri = data?.data ?: return
        screen("Opening selected image")
        val token = scanToken
        label("Preparing image offline…", 16)
        worker.execute {
            val bitmap = decodePreview(uri)
            runOnUiThread {
                if (token != scanToken || isFinishing || isDestroyed) { bitmap?.recycle(); return@runOnUiThread }
                if (bitmap == null) { message("Unable to decode image"); home() } else previewGalleryCrop(bitmap)
            }
        }
    }
    private fun decodePreview(uri: Uri): Bitmap? {
        return try {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = ImageDecodeLimits.sampleSize(bounds.outWidth, bounds.outHeight, 2048)
        val decoded = contentResolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        val orientation = runCatching {
            contentResolver.openInputStream(uri)?.use { androidx.exifinterface.media.ExifInterface(it).getAttributeInt(
                androidx.exifinterface.media.ExifInterface.TAG_ORIENTATION, 1) }
        }.getOrNull() ?: 1
        val matrix = android.graphics.Matrix()
        when (orientation) {
            2 -> matrix.setScale(-1f, 1f)
            3 -> matrix.setRotate(180f)
            4 -> matrix.setScale(1f, -1f)
            5 -> { matrix.setRotate(90f); matrix.postScale(-1f, 1f) }
            6 -> matrix.setRotate(90f)
            7 -> { matrix.setRotate(270f); matrix.postScale(-1f, 1f) }
            8 -> matrix.setRotate(270f)
        }
        if (matrix.isIdentity) decoded else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)
            .also { if (it !== decoded) decoded.recycle() }
        } catch (_: Exception) { null }
    }

    private fun previewGalleryCrop(bitmap: Bitmap) {
        screen("Select Quran text", scrollable = false)
        label("Drag a rectangle around the text, then scan the selection. Full image scanning is also available.", 15)
        val crop = GalleryCropView(this).apply { setImage(bitmap) }
        root.addView(crop, LinearLayout.LayoutParams(-1, 0, 1f))
        action("Scan selected area") {
            val selected = runCatching { crop.croppedBitmap() }.getOrNull()
            if (selected == null) message("Unable to crop image. Try scanning the full image.")
            else {
                root.removeView(crop)
                if (selected !== bitmap) bitmap.recycle()
                scan(selected)
            }
        }
        action("Scan full image") { root.removeView(crop); scan(bitmap) }
        action("Cancel") { root.removeView(crop); bitmap.recycle(); home() }
    }

    private fun scan(bitmap: Bitmap) {
        screen("Offline Arabic OCR", scrollable = false)
        val token = scanToken
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
                            gravity = Gravity.START
                            layoutDirection = View.LAYOUT_DIRECTION_RTL
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

    private fun menu() { drawer.openDrawer(GravityCompat.START) }
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
        action("Arabic text size: $arabicSize — change") {
            arabicSize = if (arabicSize >= 37) 21 else arabicSize + 4
            preferences.edit().putInt("arabic_size", arabicSize).apply()
            settings()
        }
        val keepAwake = preferences.getBoolean("keep_screen_on", false)
        action("Keep scanner screen awake: ${if (keepAwake) "On" else "Off"}") {
            preferences.edit().putBoolean("keep_screen_on", !keepAwake).apply()
            settings()
        }
        val autoPaused = preferences.getBoolean("auto_scan_paused", false)
        action("Automatic camera scanning: ${if (autoPaused) "Paused" else "Enabled"}") {
            preferences.edit().putBoolean("auto_scan_paused", !autoPaused).apply()
            settings()
        }
        action("Recitation mode and audio pack / reciter settings") { recitation() }
        val availableMb = android.os.StatFs(filesDir.absolutePath).availableBytes / (1024 * 1024)
        label("Internal storage available: $availableMb MB. Audio packs are limited to 1500 MB; timing packs to 40 MB. Up to eight audio packs can be retained; select a reciter or remove an inactive pack in Recitation. No recordings are bundled.", 14)
        label("Privacy: local processing only; selected images are not uploaded. Audio/timings are stored privately. Cloud backup is disabled. Manage or remove stored data in Android app settings.", 14)
        label("Bundled Arabic model: tessdata_fast / ara.traineddata, Apache-2.0.  No INTERNET permission.", 14)
        action("Back") { menu() }
    }
    private fun help() {
        screen("Help")
        label("Live scan: allow camera permission, keep the page steady in good light, and tap the preview to focus. Darker/Brighter adjusts supported cameras. Pause auto still allows manual Scan photo.", 16)
        label("Upload picture: choose a local, sharp image. Rotation is read from EXIF. The gold frame is a guide; OCR currently examines the full image.", 16)
        label("Every OCR result is unverified. Open a candidate, compare the complete canonical verse with your printed page and check the Surah/Ayah before pressing Play. No OCR result starts recitation automatically.", 16)
        label("Recitation: import your legally obtained ayah MP3 ZIP. Word playback additionally needs reviewed timings for those exact recordings and this text. Missing or mismatched files disable playback/highlighting.", 16)
        label("Privacy: images are processed locally in memory. No Internet permission, image uploads or analytics. Imported recordings and timing packs stay in app-private storage; Android app storage controls can remove them.", 16)
        label("This development source has no validated Pakistani/Madani OCR accuracy or device certification. Arabic text follows Tanzil Uthmani v1.1; it does not reproduce a Pakistani 15-line page layout.", 15, gold)
        action("Home") { home() }
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
        web.settings.allowContentAccess = false
        web.settings.allowFileAccess = false
        web.settings.blockNetworkLoads = true
        web.webViewClient = object : android.webkit.WebViewClient() {
            @Deprecated("API 23 compatibility")
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean = true
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, request: android.webkit.WebResourceRequest?): Boolean = true
        }
        web.loadUrl("file:///android_asset/preview.html")
        setContentView(web)
        web.applyReaderInsets()
    }
    private fun message(text: String) { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    @Deprecated("Prototype navigation")
    override fun onBackPressed() { if (::drawer.isInitialized && drawer.isDrawerOpen(GravityCompat.START)) drawer.closeDrawers() else home() }
}
