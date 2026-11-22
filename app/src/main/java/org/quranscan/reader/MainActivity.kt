package org.quranscan.reader

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.drawerlayout.widget.DrawerLayout
import androidx.core.view.GravityCompat
import org.quranscan.reader.core.CropBounds
import org.quranscan.reader.core.GalleryImageState
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.audio.WordHighlightText
import org.quranscan.reader.core.VerseCandidates
import org.quranscan.reader.core.QuranPageVerseMatcher
import org.quranscan.reader.core.ConservativeVerseMatcher
import org.quranscan.reader.core.RecognitionResult
import org.quranscan.reader.core.VerseId
import org.quranscan.reader.ocr.OfflineArabicOcr
import java.util.concurrent.Executors

/** Phase 6 Android-native development app. CameraX live scanner is in LiveScanActivity.
 * Gallery OCR remains available. Recitation is intentionally blocked until trusted data exists.
 */
class MainActivity : Activity() {
    private lateinit var root: LinearLayout
    private lateinit var drawer: DrawerLayout
    private val preferences by lazy { getSharedPreferences("quran_reader_settings", MODE_PRIVATE) }
    private enum class Destination { HOME, SETTINGS, HELP, ABOUT, PREVIEW }

    private var destination = Destination.HOME
    private var arabicSize = 25
    private val bg = Color.rgb(0, 65, 48)
    private val gold = Color.rgb(216, 173, 86)
    private val panel = Color.rgb(12, 91, 71)
    private val worker = Executors.newSingleThreadExecutor()
    private val pickImage = 101
    @Volatile private var scanToken = 0
    private var galleryBusy = false
    private var galleryScanScreen = false
    private var reopenGalleryOnStart = false
    private var galleryStarted = false
    private var galleryUri: Uri? = null
    private var galleryBitmap: Bitmap? = null
    private var galleryCrop: GalleryCropView? = null
    private var gallerySelection: CropBounds? = null
    private var designPreview: android.webkit.WebView? = null
    private var profile = OfflineArabicOcr.Profile.AUTO

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        arabicSize = preferences.getInt("arabic_size", 25).coerceIn(21, 37)
        profile = runCatching {
            OfflineArabicOcr.Profile.valueOf(getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getString("profile", "AUTO")!!)
        }.getOrDefault(OfflineArabicOcr.Profile.AUTO)
        val restoredUri = savedInstanceState?.getString("gallery_uri")?.takeIf { galleryUriScheme(it) }?.let(Uri::parse)
        val restoredCrop = GalleryImageState.savedSelection(savedInstanceState?.getIntArray("gallery_selection"))
        if (restoredUri != null) {
            galleryUri = restoredUri
            gallerySelection = restoredCrop
            reopenGalleryOnStart = true
            return
        }
        when (savedInstanceState?.getString("destination")) {
            Destination.SETTINGS.name -> settings()
            Destination.HELP.name -> help()
            Destination.ABOUT.name -> about()
            Destination.PREVIEW.name -> preview()
            else -> home()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putString("destination", destination.name)
        galleryUri?.let { outState.putString("gallery_uri", it.toString()) }
        (galleryCrop?.selectedBounds() ?: gallerySelection)?.let {
            outState.putIntArray("gallery_selection", intArrayOf(it.left, it.top, it.right, it.bottom))
        }
        super.onSaveInstanceState(outState)
    }

    override fun onStart() {
        super.onStart()
        galleryStarted = true
        if (reopenGalleryOnStart) {
            galleryUri?.let { openGalleryImage(it, gallerySelection, autoProcess = gallerySelection == null) }
        }
    }

    override fun onStop() {
        galleryStarted = false
        if (galleryBusy) {
            scanToken++
            galleryBusy = false
            reopenGalleryOnStart = true
        }
        super.onStop()
    }

    override fun onDestroy() {
        scanToken++
        releaseDesignPreview()
        releaseGalleryPreview()
        worker.shutdownNow()
        super.onDestroy()
    }

    private fun screen(title: String, page: Destination = Destination.HOME, keepGallery: Boolean = false) {
        destination = page
        scanToken++ // Navigation invalidates pending gallery results.
        releaseDesignPreview()
        releaseGalleryPreview()
        galleryBusy = false
        galleryScanScreen = false
        reopenGalleryOnStart = false
        if (!keepGallery) clearGalleryState()
        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(bg)
            setPadding(22, 26, 22, 22)
        }
        drawer = DrawerLayout(this)
        val content = ScrollView(this).apply {
            isFillViewport = true
            addView(root)
            applyReaderInsets()
        }
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
        side.addView(TextView(this).apply { text = getString(R.string.reader_title); textSize = 23f; setTextColor(gold) })
        item(getString(R.string.nav_home)) { home() }
        item(getString(R.string.nav_camera)) { camera() }
        item(getString(R.string.nav_gallery)) { gallery() }
        item(getString(R.string.nav_recitation)) { recitation() }
        item(getString(R.string.nav_settings)) { settings() }
        item(getString(R.string.nav_help)) { help() }
        item(getString(R.string.nav_about)) { about() }
        item(getString(R.string.nav_exit)) { finishAndRemoveTask() }
        item(getString(R.string.nav_preview)) { preview() }
        drawer.addView(ScrollView(this).apply {
            addView(side)
            applyReaderInsets()
        }, DrawerLayout.LayoutParams(
            (280 * resources.displayMetrics.density).toInt(), -1, Gravity.START))
        setContentView(drawer)
        root.addView(Button(this).apply {
            text = getString(R.string.nav_menu); contentDescription = getString(R.string.nav_menu_description)
            isAllCaps = false
            minHeight = (48 * resources.displayMetrics.density).toInt()
            setOnClickListener { drawer.openDrawer(GravityCompat.START) }
        }, LinearLayout.LayoutParams(-2, -2))
        label(title, 25, gold)
    }
    private fun label(value: String, size: Int = 17, color: Int = Color.WHITE, arabic: Boolean = false): TextView {
        val view = TextView(this).apply {
            text = value
            textSize = if (arabic) maxOf(size, arabicSize).toFloat() else size.toFloat()
            setTextColor(color)
            gravity = Gravity.CENTER
            setPadding(6, 12, 6, 12)
            if (arabic) textDirection = View.TEXT_DIRECTION_RTL
        }
        root.addView(view, LinearLayout.LayoutParams(-1, -2))
        return view
    }
    private fun action(text: String, onClick: () -> Unit) {
        root.addView(Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 16f
            minHeight = (48 * resources.displayMetrics.density).toInt()
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
        screen(getString(R.string.reader_title))
        val token = scanToken
        val openingVerse = TextView(this).apply {
            visibility = View.GONE
            textDirection = View.TEXT_DIRECTION_RTL
            textSize = arabicSize.toFloat()
            setTextColor(gold)
            gravity = Gravity.CENTER
        }
        root.addView(openingVerse)
        val corpusStatus = TextView(this).apply {
            text = getString(R.string.home_corpus_loading)
            setTextColor(gold)
            gravity = Gravity.CENTER
        }
        root.addView(corpusStatus)
        worker.execute {
            val db = OfflineCorpus(applicationContext)
            val complete = runCatching { db.isComplete() }.getOrNull()
            val firstAyah = if (complete == true) db.find(VerseId(1, 1)) else null
            val status = when (complete) {
                true -> getString(R.string.home_corpus_available, db.count())
                false -> getString(R.string.home_corpus_incomplete)
                null -> getString(R.string.home_corpus_invalid)
            }
            runOnUiThread {
                if (token != scanToken || isFinishing || isDestroyed) return@runOnUiThread
                corpusStatus.text = status
                if (firstAyah != null) {
                    openingVerse.text = firstAyah
                    openingVerse.visibility = View.VISIBLE
                }
            }
        }
        action(getString(R.string.nav_camera)) { camera() }
        action(getString(R.string.nav_gallery)) { gallery() }
        action(getString(R.string.nav_recitation)) { recitation() }
        action(getString(R.string.nav_settings)) { settings() }
        action(getString(R.string.nav_help)) { help() }
        action(getString(R.string.nav_about)) { about() }
        action(getString(R.string.nav_preview)) { preview() }
        label(getString(R.string.home_privacy), 12)
    }

    private fun recitation(surah: Int = 1, ayah: Int = 1, scannedText: String? = null, autoPlay: Boolean = false, firstWord: Int = -1, wordCount: Int = 0) {
        startActivity(Intent(this, RecitationActivity::class.java).apply {
            putExtra(RecitationActivity.EXTRA_SURAH, surah)
            putExtra(RecitationActivity.EXTRA_AYAH, ayah)
            putExtra(RecitationActivity.EXTRA_AUTO_PLAY, autoPlay)
            putExtra(RecitationActivity.EXTRA_FIRST_WORD, firstWord)
            putExtra(RecitationActivity.EXTRA_WORD_COUNT, wordCount)
            scannedText?.let {
                putExtra(RecitationActivity.EXTRA_SCANNED_TEXT, it.take(RecitationActivity.MAX_COMPARISON_LENGTH))
            }
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
        val uri = data?.data
        if (uri == null || !galleryUriScheme(uri.toString())) {
            message(getString(R.string.gallery_open_error))
            return
        }
        retainGalleryPermission(uri, data.flags)
        if (galleryStarted) {
            openGalleryImage(uri, autoProcess = true)
        } else {
            galleryUri = uri
            gallerySelection = null
            reopenGalleryOnStart = true
        }
    }

    private fun galleryUriScheme(value: String): Boolean =
        Uri.parse(value).let { it.scheme == "content" && !it.authority.isNullOrEmpty() }

    private fun retainGalleryPermission(uri: Uri, flags: Int) {
        val previous = preferences.getString(GALLERY_GRANT, null)
        if (previous != uri.toString()) releaseGalleryPermission()
        val read = Intent.FLAG_GRANT_READ_URI_PERMISSION
        if (flags and read == 0 || flags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == 0) return
        try {
            contentResolver.takePersistableUriPermission(uri, read)
            preferences.edit().putString(GALLERY_GRANT, uri.toString()).apply()
        } catch (_: SecurityException) {
            // The temporary grant may still allow previewing this image.
        }
    }

    private fun releaseGalleryPermission() {
        val uri = preferences.getString(GALLERY_GRANT, null) ?: return
        try {
            contentResolver.releasePersistableUriPermission(Uri.parse(uri), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: SecurityException) {
            // The provider may already have withdrawn the grant.
        }
        preferences.edit().remove(GALLERY_GRANT).apply()
    }

    private fun clearGalleryState() {
        galleryUri = null
        gallerySelection = null
        releaseGalleryPermission()
    }

    private fun openGalleryImage(uri: Uri, selection: CropBounds? = null, autoProcess: Boolean = false) {
        screen(getString(R.string.gallery_opening), keepGallery = true)
        galleryUri = uri
        gallerySelection = selection
        galleryBusy = true
        val token = scanToken
        label(getString(R.string.gallery_preparing), 16)
        action(getString(R.string.gallery_cancel)) { home() }
        galleryWorker.execute {
            if (token != scanToken) return@execute
            val bitmap = decodeGalleryPreview(contentResolver, uri) { token == scanToken }
            runOnUiThread {
                if (token != scanToken || isFinishing || isDestroyed) {
                    bitmap?.recycle()
                    return@runOnUiThread
                }
                if (bitmap == null) {
                    message(getString(R.string.gallery_open_error))
                    home()
                } else if (autoProcess) {
                    // Gallery uploads and screenshots scan immediately. Keep Crop & retry.
                    galleryBitmap = bitmap
                    startGalleryScan(uri, bitmap, CropBounds(0, 0, bitmap.width, bitmap.height), false)
                } else previewGalleryCrop(uri, bitmap, selection)
            }
        }
    }

    private fun previewGalleryCrop(uri: Uri, bitmap: Bitmap, selection: CropBounds?) {
        screen(getString(R.string.gallery_select_text), keepGallery = true)
        galleryUri = uri
        galleryBitmap = bitmap
        label(getString(R.string.gallery_crop_help), 15)
        val crop = GalleryCropView(this).apply { setImage(bitmap, selection) }
        galleryCrop = crop
        root.addView(crop, LinearLayout.LayoutParams(-1, (240 * resources.displayMetrics.density).toInt()))
        action(getString(R.string.gallery_scan_crop)) { startGalleryScan(uri, bitmap, crop.selectedBounds(), true) }
        action(getString(R.string.gallery_scan_full)) { startGalleryScan(uri, bitmap, crop.selectedBounds(), false) }
        action(getString(R.string.gallery_cancel)) { home() }
    }

    private fun releaseGalleryPreview() {
        galleryCrop?.let { root.removeView(it) }
        galleryCrop = null
        galleryBitmap?.recycle()
        galleryBitmap = null
    }

    private fun startGalleryScan(uri: Uri, bitmap: Bitmap, selection: CropBounds, cropToSelection: Boolean) {
        if (galleryBitmap !== bitmap) return
        galleryBitmap = null // The gallery worker owns the detached preview.
        screen(getString(R.string.gallery_offline_ocr), keepGallery = true)
        galleryScanScreen = true
        galleryUri = uri
        gallerySelection = selection
        galleryBusy = true
        val token = scanToken
        val scanProfile = profile
        val scanning = label(getString(R.string.gallery_ocr_in_progress), 17, gold)
        label(getString(R.string.gallery_local_profile, getString(profileLabel(scanProfile))), 13)
        val progress = ProgressBar(this)
        root.addView(progress)
        action(getString(R.string.nav_home)) { home() }
        galleryWorker.execute {
            var input = bitmap
            val response = try {
                if (token != scanToken) return@execute
                runCatching {
                    if (cropToSelection) {
                        input = Bitmap.createBitmap(bitmap, selection.left, selection.top,
                            selection.width, selection.height)
                    }
                    if (token != scanToken) return@execute
                    val ocr = OfflineArabicOcr(applicationContext).read(input, scanProfile, selectedRegion = cropToSelection, tryRotations = true)
                    if (token != scanToken) return@execute
                    val db = OfflineCorpus(applicationContext)
                    val complete = db.isComplete()
                    val exact = if (complete) ConservativeVerseMatcher(db.asMap())
                        .match(ocr.rawUnverifiedText) as? RecognitionResult.VerifiedCandidate else null
                    val page = if (complete && exact == null)
                        QuranPageVerseMatcher.find(ocr.rawUnverifiedText, db.asMap()) else null
                    val candidates = when {
                        exact != null -> listOf(VerseCandidates.Suggestion(exact.match.id, exact.match.canonicalArabic, 1f))
                        page != null && page.matches.isNotEmpty() -> page.matches
                        complete -> VerseCandidates.suggest(ocr.rawUnverifiedText + "\n" + ocr.sparseText, db.asMap())
                            .ifEmpty { listOfNotNull(VerseCandidates.suggestNoisyLine(ocr.lineAlternatives, db.asMap())) }
                        else -> emptyList()
                    }
                    Triple(ocr, candidates, (exact?.match?.id ?: page?.autoPlayable?.id) to complete)
                }
            } finally {
                if (input !== bitmap) input.recycle()
                bitmap.recycle()
            }
            runOnUiThread {
                if (token != scanToken || isFinishing || isDestroyed) return@runOnUiThread
                galleryBusy = false
                root.removeView(progress)
                root.removeView(scanning)
                response.onSuccess { (ocr, candidates, recognition) ->
                    val (autoVerse, complete) = recognition
                    if (autoVerse != null && hasWindowFocus()) {
                        recitation(autoVerse.surah, autoVerse.ayah, ocr.rawUnverifiedText, autoPlay = true)
                    }
                    val scannedText = ocr.rawUnverifiedText.ifBlank { ocr.sparseText }
                    if (scannedText.isBlank()) {
                        label(getString(R.string.gallery_no_arabic), 15)
                    } else {
                        label(getString(R.string.gallery_ocr_unverified), 15, gold)
                        val raw = TextView(this).apply {
                            text = scannedText.take(2000)
                            textSize = 21f
                            setTextColor(Color.WHITE)
                            textDirection = View.TEXT_DIRECTION_RTL
                            gravity = Gravity.START
                            layoutDirection = View.LAYOUT_DIRECTION_RTL
                            setPadding(12, 12, 12, 12)
                        }
                        root.addView(raw, LinearLayout.LayoutParams(-1, -2))
                    }
                    if (!complete) label(getString(R.string.gallery_no_corpus), 14, gold)
                    else if (candidates.isEmpty()) {
                        label(getString(R.string.gallery_no_candidate), 14)
                        action(getString(R.string.nav_recitation)) { recitation() }
                    } else {
                        label(getString(R.string.gallery_possible_matches), 14, gold)
                        candidates.forEach { c ->
                            val reference = candidateReference(c)
                            label(reference, 15, gold)
                            label(c.canonicalArabic, 17, gold, true).apply {
                                text = WordHighlightText.decorate(c.canonicalArabic, c.firstWord, c.wordCount)
                            }
                            action(getString(R.string.scan_review_candidate, reference)) {
                                recitation(c.id.surah, c.id.ayah, scannedText,
                                    firstWord = c.firstWord, wordCount = c.wordCount)
                            }
                        }
                    }
                    label(resources.getQuantityString(R.plurals.scan_processing_summary, ocr.variantsChecked,
                        ocr.variantsChecked, ocr.elapsedMs), 12)
                    ocr.quality.guidance(this)?.let { label(it, 13) }
                }.onFailure {
                    label(getString(R.string.gallery_scan_error), 14, gold)
                }
                action(getString(R.string.nav_back)) { openGalleryImage(uri, selection) }
            }
        }
    }

    private fun profileLabel(value: OfflineArabicOcr.Profile): Int = when (value) {
        OfflineArabicOcr.Profile.AUTO -> R.string.profile_auto
        OfflineArabicOcr.Profile.INDO_PAK -> R.string.profile_indo_pak
        OfflineArabicOcr.Profile.MADANI -> R.string.profile_madani
    }

    private fun menu() { drawer.openDrawer(GravityCompat.START) }
    private fun settings() {
        screen(getString(R.string.nav_settings), page = Destination.SETTINGS)
        label(getString(R.string.settings_ocr_info), 14)
        action(getString(R.string.settings_profile, getString(profileLabel(profile)))) {
            val options = OfflineArabicOcr.Profile.entries
            profile = options[(options.indexOf(profile) + 1) % options.size]
            getSharedPreferences("quran_reader_settings", MODE_PRIVATE).edit()
                .putString("profile", profile.name).apply()
            settings()
        }
        action(getString(R.string.settings_text_size, arabicSize)) {
            arabicSize = if (arabicSize >= 37) 21 else arabicSize + 4
            preferences.edit().putInt("arabic_size", arabicSize).apply()
            settings()
        }
        val keepAwake = preferences.getBoolean("keep_screen_on", false)
        action(getString(R.string.settings_keep_awake, getString(if (keepAwake) R.string.settings_on else R.string.settings_off))) {
            preferences.edit().putBoolean("keep_screen_on", !keepAwake).apply()
            settings()
        }
        val autoPaused = preferences.getBoolean("auto_scan_paused", false)
        action(getString(R.string.settings_auto_scan, getString(if (autoPaused) R.string.settings_paused else R.string.settings_enabled))) {
            preferences.edit().putBoolean("auto_scan_paused", !autoPaused).apply()
            settings()
        }
        action(getString(R.string.settings_recitation)) { recitation() }
        val availableMb = android.os.StatFs(filesDir.absolutePath).availableBytes / (1024 * 1024)
        label(getString(R.string.settings_storage, availableMb), 14)
        label(getString(R.string.reader_privacy), 14)
        label(getString(R.string.settings_model), 14)
        action(getString(R.string.nav_back)) { menu() }
    }
    private fun help() {
        screen(getString(R.string.nav_help), page = Destination.HELP)
        label(getString(R.string.help_camera), 16)
        label(getString(R.string.help_gallery), 16)
        label(getString(R.string.help_review), 16)
        label(getString(R.string.help_recitation), 16)
        label(getString(R.string.reader_privacy), 16)
        label(getString(R.string.help_validation), 15, gold)
        action(getString(R.string.nav_home)) { home() }
    }

    private fun about() {
        screen(getString(R.string.nav_about), page = Destination.ABOUT)
        label(getString(R.string.about_sources), 14)
        action(getString(R.string.nav_back)) { menu() }
    }
    private fun preview() {
        destination = Destination.PREVIEW
        scanToken++
        releaseGalleryPreview()
        galleryBusy = false
        galleryScanScreen = false
        reopenGalleryOnStart = false
        clearGalleryState()
        releaseDesignPreview()
        val web = android.webkit.WebView(this)
        designPreview = web
        web.settings.javaScriptEnabled = false
        web.settings.allowContentAccess = false
        web.settings.allowFileAccess = false
        web.settings.blockNetworkLoads = true
        val previewAsset = "file:///android_asset/preview.html"
        web.webViewClient = object : android.webkit.WebViewClient() {
            private fun isPreview(url: String?) = url?.substringBefore('#') == previewAsset

            @Deprecated("API 23 compatibility")
            override fun shouldOverrideUrlLoading(view: android.webkit.WebView?, url: String?): Boolean =
                !isPreview(url)

            override fun shouldOverrideUrlLoading(
                view: android.webkit.WebView?,
                request: android.webkit.WebResourceRequest?
            ): Boolean = request?.isForMainFrame != true || !isPreview(request.url.toString())
        }
        web.loadUrl(previewAsset)
        setContentView(web)
        web.applyReaderInsets()
    }
    private fun releaseDesignPreview() {
        designPreview?.let {
            (it.parent as? ViewGroup)?.removeView(it)
            it.destroy()
        }
        designPreview = null
    }

    private fun message(text: String) { Toast.makeText(this, text, Toast.LENGTH_LONG).show() }
    @Deprecated("Prototype navigation")
    override fun onBackPressed() {
        if (designPreview == null && ::drawer.isInitialized && drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawers()
        } else if (galleryScanScreen && galleryUri != null) {
            openGalleryImage(requireNotNull(galleryUri), gallerySelection)
        } else {
            home()
        }
    }

    private companion object {
        const val GALLERY_GRANT = "gallery_read_grant"
        val galleryWorker = Executors.newSingleThreadExecutor()
    }
}
