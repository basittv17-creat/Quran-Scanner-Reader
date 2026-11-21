package org.quranscan.reader

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Color
import android.graphics.Matrix
import android.net.Uri
import android.provider.Settings
import android.os.Bundle
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.Camera
import androidx.camera.core.FocusMeteringAction
import androidx.camera.core.CameraSelector
import androidx.camera.core.CameraState
import androidx.camera.core.UseCase
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.UseCaseGroup
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import org.quranscan.reader.core.CropBounds
import org.quranscan.reader.core.LiveScanRegion
import org.quranscan.reader.core.ImageDecodeLimits
import org.quranscan.reader.core.CameraSessionGate
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.audio.WordHighlightText
import org.quranscan.reader.core.VerseCandidates
import org.quranscan.reader.core.ConservativeVerseMatcher
import org.quranscan.reader.core.RecognitionResult
import org.quranscan.reader.ocr.OfflineArabicOcr
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Phase 6: real CameraX live preview, periodically sampled offline Arabic OCR,
 * and native high-resolution capture (with analyzer-only fallback on older cameras).
 * Important: raw OCR is NOT certified Quran text, not diacritic verification,
 * and this screen does NOT automatically recite unverified text.
 */
class LiveScanActivity : ComponentActivity() {
    private val green = Color.rgb(0, 65, 48)
    private val gold = Color.rgb(216, 173, 86)
    private val analysisExecutor = Executors.newSingleThreadExecutor()
    // A late callback still owns its proxy after shutdown. Its invalid session makes it close-only.
    private val cameraCallbacks = Executor { task ->
        try {
            analysisExecutor.execute(task)
        } catch (_: RejectedExecutionException) {
            task.run()
        }
    }
    private val gate = CameraSessionGate()
    private val paused = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null
    private var boundUseCases = emptyArray<UseCase>()
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var openingSession: CameraSessionGate.Session? = null
    private var boundSession: CameraSessionGate.Session? = null
    private var targetRotation: Int? = null
    private var cameraFailed = false
    private var resumed = false
    private var permissionRequested = false
    private val displayManager by lazy { getSystemService(DisplayManager::class.java) }
    private val displayListener = object : DisplayManager.DisplayListener {
        override fun onDisplayAdded(displayId: Int) = Unit
        override fun onDisplayRemoved(displayId: Int) = Unit
        override fun onDisplayChanged(displayId: Int) {
            val display = previewView.display ?: return
            if (display.displayId == displayId && targetRotation != null && targetRotation != display.rotation) {
                releaseCamera()
                startCamera()
            }
        }
    }
    private var exposureSteps = 0
    @Volatile private var scanRegion = LiveScanRegion.DEFAULT
    private lateinit var reticle: ScanReticleView
    private var dragX = 0f
    private var dragY = 0f
    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private lateinit var rawOutput: TextView
    private lateinit var resultGuidance: TextView
    private lateinit var togglePause: Button
    private lateinit var reviewContainer: LinearLayout
    private var profile = OfflineArabicOcr.Profile.AUTO
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera() else showPermissionDenied()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.keepScreenOn = getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getBoolean("keep_screen_on", false)
        profile = runCatching {
            OfflineArabicOcr.Profile.valueOf(
                getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getString("profile", "AUTO") ?: "AUTO"
            )
        }.getOrDefault(OfflineArabicOcr.Profile.AUTO)
        // Recreated screens wait for a new scan or an explicit automatic-scan resume.
        paused.set(savedInstanceState != null || getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getBoolean("auto_scan_paused", false))
        savedInstanceState?.getFloatArray("live_scan_region")?.takeIf { it.size == 4 }?.let {
            scanRegion = LiveScanRegion.selection(it[0], it[1], it[2], it[3]) ?: LiveScanRegion.DEFAULT
        }
        if (savedInstanceState?.getBoolean("live_scan_full_image") == true) {
            scanRegion = LiveScanRegion.FULL_IMAGE
        }
        permissionRequested = savedInstanceState?.getBoolean("live_permission_requested") == true
        buildInterface()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        outState.putBoolean("live_permission_requested", permissionRequested)
        outState.putFloatArray("live_scan_region", scanRegion.coordinates())
        outState.putBoolean("live_scan_full_image", scanRegion.useFullImage)
        super.onSaveInstanceState(outState)
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(green)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        setContentView(ScrollView(this).apply {
            isFillViewport = true
            addView(root)
            applyReaderInsets()
        })
        val title = TextView(this).apply {
            text = getString(R.string.live_title)
            textSize = 19f
            setTextColor(gold)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(title)
        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FILL_CENTER
            contentDescription = getString(R.string.live_crop_hint)
            var tappedPoint: Pair<Float, Float>? = null
            setOnClickListener {
                val (x, y) = tappedPoint ?: (width / 2f to height / 2f)
                focusPreview(x, y)
            }
            setOnTouchListener { view, event ->
                if (width <= 0 || height <= 0) return@setOnTouchListener false
                val x = (event.x / width).coerceIn(0f, 1f)
                val y = (event.y / height).coerceIn(0f, 1f)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        view.parent?.requestDisallowInterceptTouchEvent(true)
                        dragX = x
                        dragY = y
                    }
                    MotionEvent.ACTION_UP -> {
                        view.parent?.requestDisallowInterceptTouchEvent(false)
                        val region = LiveScanRegion.selection(dragX, dragY, x, y)
                        if (region != null) {
                            setScanRegion(region)
                        } else if (kotlin.math.abs(x - dragX) * width < dp(12) &&
                            kotlin.math.abs(y - dragY) * height < dp(12)) {
                            tappedPoint = event.x to event.y
                            view.performClick()
                            tappedPoint = null
                        }
                    }
                    MotionEvent.ACTION_CANCEL -> view.parent?.requestDisallowInterceptTouchEvent(false)
                }
                true
            }
        }
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        frame.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        reticle = ScanReticleView(this).apply {
            region = scanRegion
            importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        }
        frame.addView(reticle, FrameLayout.LayoutParams(-1, -1))
        previewView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                if (boundSession != null || openingSession != null) releaseCamera()
                startCamera()
            }
        }
        root.addView(frame, LinearLayout.LayoutParams(-1, dp(260)))
        status = TextView(this).apply {
            textSize = 13f
            setTextColor(gold)
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(8), dp(5), dp(8))
            text = getString(R.string.live_preparing)
        }
        root.addView(status)
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun control(text: String, click: () -> Unit): Button = Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 12f
            minHeight = dp(54)
            setOnClickListener { click() }
            buttons.addView(this, LinearLayout.LayoutParams(0, -2, 1f))
        }
        control(getString(R.string.live_scan_photo)) { capturePhotoOrNextFrame() }
        togglePause = control(if (paused.get()) getString(R.string.live_resume_auto) else getString(R.string.live_pause_auto)) { toggleAutomaticScan() }
        control(getString(R.string.nav_home)) { finish() }
        root.addView(buttons)
        val exposure = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun exposureButton(title: String, delta: Int) {
            exposure.addView(Button(this).apply {
                text = title; isAllCaps = false
                minHeight = dp(48)
                setOnClickListener {
                    val current = camera?.takeIf { hasCameraPermission() } ?: return@setOnClickListener
                    val state = current.cameraInfo.exposureState
                    if (!state.isExposureCompensationSupported) {
                        showStatus(getString(R.string.live_exposure_unavailable))
                    } else {
                        exposureSteps = (state.exposureCompensationIndex + delta).coerceIn(
                            state.exposureCompensationRange.lower, state.exposureCompensationRange.upper)
                        current.cameraControl.setExposureCompensationIndex(exposureSteps)
                        showStatus(getString(R.string.live_exposure_step, exposureSteps))
                    }
                }
            }, LinearLayout.LayoutParams(0, -2, 1f))
        }
        exposureButton(getString(R.string.live_darker), -1)
        exposureButton(getString(R.string.live_brighter), 1)
        exposure.addView(Button(this).apply {
            setText(R.string.live_crop_reset)
            isAllCaps = false
            minHeight = dp(48)
            setOnClickListener { setScanRegion(LiveScanRegion.DEFAULT) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(exposure)
        root.addView(Button(this).apply {
            setText(R.string.live_scan_full_image)
            isAllCaps = false
            minHeight = dp(48)
            setOnClickListener { setScanRegion(LiveScanRegion.FULL_IMAGE) }
        }, LinearLayout.LayoutParams(-1, -2))
        rawOutput = TextView(this).apply {
            visibility = View.GONE
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.START
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(12, 83, 65))
        }
        reviewContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        resultGuidance = TextView(this).apply {
            setText(R.string.live_initial_guidance)
            setTextColor(Color.WHITE)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        root.addView(resultGuidance)
        root.addView(rawOutput)
        root.addView(reviewContainer)
    }

    private fun focusPreview(x: Float, y: Float) {
        val current = camera?.takeIf { hasCameraPermission() } ?: return
        val point = previewView.meteringPointFactory.createPoint(x, y)
        current.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
    }

    private fun setScanRegion(region: LiveScanRegion) {
        scanRegion = region.copy()
        reticle.region = region
        reticle.invalidate()
        reviewContainer.removeAllViews()
        val guidance = getString(if (region.useFullImage) R.string.live_scan_full_image_selected else R.string.live_crop_updated)
        rawOutput.visibility = View.GONE
        resultGuidance.text = guidance
        resultGuidance.announceForAccessibility(guidance)
        gate.resetInterval()
    }

    private fun hasCameraPermission(): Boolean =
        ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    private fun hasRearCamera(): Boolean = packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA)

    private fun showPermissionDenied() {
        releaseCamera()
        status.setText(R.string.live_permission_denied)
        rawOutput.visibility = View.GONE
        resultGuidance.setText(R.string.live_initial_guidance)
        reviewContainer.removeAllViews()
    }

    private fun startCamera() {
        if (!resumed || isFinishing || isDestroyed || openingSession != null || boundSession != null) return
        if (!hasRearCamera()) {
            status.setText(R.string.live_camera_missing)
            return
        }
        if (!hasCameraPermission()) {
            showPermissionDenied()
            return
        }
        if (previewView.viewPort == null) return
        targetRotation = previewView.display?.rotation ?: return
        val session = gate.open()
        openingSession = session
        status.setText(R.string.live_opening_camera)
        try {
            val future = ProcessCameraProvider.getInstance(this)
            future.addListener({
                if (!gate.isCurrent(session) || isFinishing || isDestroyed) return@addListener
                if (!hasCameraPermission()) {
                    showPermissionDenied()
                    return@addListener
                }
                try {
                    bindCamera(future.get(), session)
                } catch (_: Exception) {
                    showCameraFailure()
                }
            }, ContextCompat.getMainExecutor(this))
        } catch (_: Exception) {
            showCameraFailure()
        }
    }

    private fun bindCamera(provider: ProcessCameraProvider, session: CameraSessionGate.Session) {
        val viewport = checkNotNull(previewView.viewPort)
        val rotation = checkNotNull(targetRotation)
        val selector = CameraSelector.DEFAULT_BACK_CAMERA
        if (!provider.hasCamera(selector)) {
            releaseCamera()
            status.setText(R.string.live_camera_missing)
            return
        }
        cameraProvider = provider
        val preview = Preview.Builder().setTargetRotation(rotation).build().apply {
            setSurfaceProvider(previewView.surfaceProvider)
        }
        val analysis = ImageAnalysis.Builder()
            .setTargetRotation(rotation)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .setTargetResolution(android.util.Size(960, 720))
            .build()
        val still = ImageCapture.Builder()
            .setTargetRotation(rotation)
            .setTargetResolution(android.util.Size(1500, 1125))
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .build()
        fun bind(vararg useCases: UseCase): Camera {
            boundUseCases = arrayOf(*useCases)
            val group = UseCaseGroup.Builder().setViewPort(viewport)
            useCases.forEach { group.addUseCase(it) }
            return provider.bindToLifecycle(this, selector, group.build())
        }
        val current = try {
            bind(preview, analysis, still).also { imageCapture = still }
        } catch (_: IllegalArgumentException) {
            // Some older cameras cannot provide three streams at once.
            provider.unbind(*boundUseCases)
            bind(preview, analysis)
        }
        camera = current
        boundSession = session
        openingSession = null
        current.cameraInfo.cameraState.observe(this) { state ->
            if (!gate.isCurrent(session)) return@observe
            if (!hasCameraPermission()) {
                showPermissionDenied()
            } else if (state.error != null) {
                cameraFailed = true
                status.setText(R.string.live_camera_retry)
            } else if (state.type == CameraState.Type.OPEN) {
                cameraFailed = false
                status.setText(R.string.live_crop_hint)
            }
        }
        if (gate.isCurrent(session)) {
            analysis.setAnalyzer(cameraCallbacks) { image -> analyze(image, session) }
        }
    }

    private fun showCameraFailure() {
        if (!hasCameraPermission()) {
            showPermissionDenied()
        } else {
            releaseCamera()
            status.setText(R.string.live_camera_retry)
        }
    }

    private fun analyze(image: ImageProxy, session: CameraSessionGate.Session) {
        val region = scanRegion
        var scan: CameraSessionGate.Scan? = null
        val bitmap = try {
            if (!gate.isCurrent(session) || !hasCameraPermission()) return
            scan = gate.claimFrame(session, SystemClock.elapsedRealtime(), paused.get()) ?: return
            toUprightBitmap(image, region)
        } catch (e: Exception) {
            scan?.let { gate.finish(it) }
            postStatus(getString(R.string.live_frame_failed), session, region)
            return
        } finally {
            image.close()
        }
        scan?.let { recognizeOffline(bitmap, it, region) }
    }

    private fun capturePhotoOrNextFrame() {
        if (!resumed) return
        if (!hasRearCamera()) {
            status.setText(R.string.live_camera_missing)
            return
        }
        if (!hasCameraPermission()) {
            showPermissionDenied()
            if (permissionRequested && !shouldShowRequestPermissionRationale(Manifest.permission.CAMERA)) {
                startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", packageName, null)))
            } else {
                permissionRequested = true
                permissionLauncher.launch(Manifest.permission.CAMERA)
            }
            return
        }
        val session = boundSession
        if (session == null || cameraFailed) {
            if (cameraFailed) releaseCamera()
            startCamera()
            return
        }
        val region = scanRegion
        val still = imageCapture
        if (still == null) {
            status.setText(if (gate.requestNextFrame(session)) R.string.live_frame_fallback
                else R.string.live_previous_scan_running)
            return
        }
        val scan = gate.claimStill(session, SystemClock.elapsedRealtime())
        if (scan == null) {
            status.setText(R.string.live_previous_scan_running)
            return
        }
        status.setText(R.string.live_capturing)
        try {
            still.takePicture(cameraCallbacks, object : ImageCapture.OnImageCapturedCallback() {
                override fun onCaptureSuccess(image: ImageProxy) {
                    val upright = try {
                        if (!isCurrentScan(scan, region)) {
                            gate.finish(scan)
                            return
                        }
                        toUprightBitmap(image, region)
                    } catch (e: Exception) {
                        gate.finish(scan)
                        postStatus(getString(R.string.live_capture_failed), session, region)
                        return
                    } finally {
                        image.close()
                    }
                    recognizeOffline(upright, scan, region)
                }

                override fun onError(exception: ImageCaptureException) {
                    gate.finish(scan)
                    postStatus(getString(R.string.live_capture_error), session, region)
                }
            })
        } catch (_: Exception) {
            gate.finish(scan)
            showCameraFailure()
        }
    }

    private fun toggleAutomaticScan() {
        val nowPaused = !paused.get()
        paused.set(nowPaused)
        getSharedPreferences("quran_reader_settings", MODE_PRIVATE).edit().putBoolean("auto_scan_paused", nowPaused).apply()
        togglePause.text = if (nowPaused) getString(R.string.live_resume_auto) else getString(R.string.live_pause_auto)
        if (!nowPaused) gate.resetInterval()
        showStatus(if (nowPaused) getString(R.string.live_auto_paused_notice)
            else getString(R.string.live_auto_resumed_notice))
    }

    private fun toUprightBitmap(image: ImageProxy, region: LiveScanRegion): Bitmap {
        val original = if (image.format == ImageFormat.JPEG) {
            // Still-capture resolution requests are hints. Read JPEG bounds before pixel allocation.
            val buffer = image.planes[0].buffer.duplicate()
            require(buffer.remaining() in 1..32 * 1024 * 1024) { "Capture is too large" }
            val bytes = ByteArray(buffer.remaining()).also { buffer.get(it) }
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            val options = BitmapFactory.Options().apply {
                inSampleSize = ImageDecodeLimits.sampleSize(bounds.outWidth, bounds.outHeight, 1500)
            }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, options)
                ?: error("Cannot decode captured JPEG")
        } else {
            // Analysis buffers are requested at 960x720; reject oversized vendor output first.
            require(image.width.toLong() * image.height <= 4_000_000L) { "Analysis frame is too large" }
            image.toBitmap()
        }
        val upright = try {
            val angle = image.imageInfo.rotationDegrees
            val viewport = image.cropRect
            val bounds = region.bitmapBoundsOrFullImage(
                CropBounds(viewport.left, viewport.top, viewport.right, viewport.bottom),
                angle, image.width, image.height, original.width, original.height
            )
            Bitmap.createBitmap(
                original, bounds.left, bounds.top, bounds.width, bounds.height,
                Matrix().apply { postRotate(angle.toFloat()) }, true
            )
        } catch (error: Exception) {
            original.recycle()
            throw error
        }
        if (upright !== original) original.recycle()
        val max = maxOf(upright.width, upright.height)
        if (max <= 1500) return upright
        val factor = 1500f / max
        var resized: Bitmap? = null
        try {
            return Bitmap.createScaledBitmap(
                upright, (upright.width * factor).toInt().coerceAtLeast(1),
                (upright.height * factor).toInt().coerceAtLeast(1), true
            ).also { resized = it }
        } finally {
            if (resized !== upright) upright.recycle()
        }
    }

    private fun isCurrentScan(scan: CameraSessionGate.Scan, region: LiveScanRegion): Boolean =
        gate.isCurrent(scan.session) && scanRegion === region && hasCameraPermission()

    private fun recognizeOffline(bitmap: Bitmap, claim: CameraSessionGate.Scan, region: LiveScanRegion) {
        try {
            if (!isCurrentScan(claim, region)) return
            postStatus(getString(R.string.live_processing), claim.session, region)
            val scan = OfflineArabicOcr(applicationContext).read(bitmap, profile, selectedRegion = true)
            if (!isCurrentScan(claim, region)) return
            val corpus = OfflineCorpus(applicationContext)
            val complete = corpus.isComplete()
            val exact = if (complete) ConservativeVerseMatcher(corpus.asMap()).match(scan.rawUnverifiedText)
                as? RecognitionResult.VerifiedCandidate else null
            val candidates = if (exact != null) {
                listOf(VerseCandidates.Suggestion(exact.match.id, exact.match.canonicalArabic, 1f))
            } else if (complete) VerseCandidates.suggest(scan.rawUnverifiedText + "\n" + scan.sparseText, corpus.asMap())
                            .ifEmpty { listOfNotNull(VerseCandidates.suggestNoisyLine(scan.lineAlternatives, corpus.asMap())) } else emptyList()
            runOnUiThread {
                if (!isCurrentScan(claim, region) || isFinishing || isDestroyed) return@runOnUiThread
                if (exact != null && hasWindowFocus()) {
                    paused.set(true)
                    togglePause.setText(R.string.live_resume_auto)
                    startActivity(Intent(this, RecitationActivity::class.java).apply {
                        putExtra(RecitationActivity.EXTRA_SURAH, exact.match.id.surah)
                        putExtra(RecitationActivity.EXTRA_AYAH, exact.match.id.ayah)
                        putExtra(RecitationActivity.EXTRA_AUTO_PLAY, true)
                        putExtra(RecitationActivity.EXTRA_SCANNED_TEXT,
                            scan.rawUnverifiedText.take(RecitationActivity.MAX_COMPARISON_LENGTH))
                    })
                }
                reviewContainer.removeAllViews()
                if (complete && candidates.isNotEmpty()) {
                    candidates.take(3).forEach { candidate ->
                        val btn = Button(this).apply {
                            val reference = candidateReference(candidate)
                            text = getString(R.string.scan_review_candidate, reference)
                            isAllCaps = false
                            minHeight = dp(48)
                            setOnClickListener {
                                startActivity(Intent(this@LiveScanActivity, RecitationActivity::class.java).apply {
                                    putExtra(RecitationActivity.EXTRA_SURAH, candidate.id.surah)
                                    putExtra(RecitationActivity.EXTRA_AYAH, candidate.id.ayah)
                                    putExtra(RecitationActivity.EXTRA_FIRST_WORD, candidate.firstWord)
                                    putExtra(RecitationActivity.EXTRA_WORD_COUNT, candidate.wordCount)
                                    putExtra(RecitationActivity.EXTRA_SCANNED_TEXT,
                                        scan.rawUnverifiedText.ifBlank { scan.sparseText }
                                            .take(RecitationActivity.MAX_COMPARISON_LENGTH))
                                })
                            }
                        }
                        reviewContainer.addView(TextView(this).apply {
                            text = WordHighlightText.decorate(
                                candidate.canonicalArabic, candidate.firstWord, candidate.wordCount)
                            textDirection = View.TEXT_DIRECTION_RTL
                            textSize = 19f
                            setTextColor(Color.WHITE)
                        }, LinearLayout.LayoutParams(-1, -2))
                        reviewContainer.addView(btn, LinearLayout.LayoutParams(-1, -2))
                    }
                }
                val result = scan.rawUnverifiedText.ifBlank { scan.sparseText }.trim()
                rawOutput.text = result.take(1800)
                rawOutput.visibility = if (result.isEmpty()) View.GONE else View.VISIBLE
                resultGuidance.text = if (result.isEmpty()) getString(R.string.live_empty_result)
                else listOf(
                    getString(R.string.gallery_ocr_unverified),
                    getString(when {
                        !complete -> R.string.live_no_reference
                        candidates.isEmpty() -> R.string.live_no_candidate
                        else -> R.string.gallery_possible_matches
                    })
                ).joinToString("\n")
                val summary = resources.getQuantityString(R.plurals.scan_processing_summary, scan.variantsChecked,
                    scan.variantsChecked, scan.elapsedMs)
                showStatus(listOfNotNull(summary, scan.quality.guidance(this)).joinToString("\n"))
            }
        } catch (e: Exception) {
            postStatus(getString(R.string.live_ocr_failed), claim.session, region)
        } finally {
            bitmap.recycle()
            gate.finish(claim)
        }
    }

    private fun postStatus(msg: String, session: CameraSessionGate.Session, region: LiveScanRegion) = runOnUiThread {
        if (gate.isCurrent(session) && scanRegion === region && !isFinishing && !isDestroyed && hasCameraPermission()) {
            showStatus(msg)
        }
    }
    private fun showStatus(msg: String) { status.text = msg }
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun releaseCamera() {
        gate.invalidate()
        openingSession = null
        boundSession = null
        targetRotation = null
        cameraFailed = false
        camera?.cameraInfo?.cameraState?.removeObservers(this)
        camera = null
        imageCapture = null
        boundUseCases.filterIsInstance<ImageAnalysis>().forEach { it.clearAnalyzer() }
        if (boundUseCases.isNotEmpty()) cameraProvider?.unbind(*boundUseCases)
        boundUseCases = emptyArray()
        cameraProvider = null
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        displayManager?.registerDisplayListener(displayListener, Handler(Looper.getMainLooper()))
        // Allow lifecycle dispatch and layout to complete before binding.
        previewView.post {
            if (!resumed || isFinishing || isDestroyed) return@post
            if (hasRearCamera() && !hasCameraPermission() && !permissionRequested) {
                permissionRequested = true
                permissionLauncher.launch(Manifest.permission.CAMERA)
            } else {
                startCamera()
            }
        }
    }

    override fun onPause() {
        resumed = false
        displayManager?.unregisterDisplayListener(displayListener)
        releaseCamera()
        super.onPause()
    }

    override fun onDestroy() {
        releaseCamera()
        analysisExecutor.shutdown()
        super.onDestroy()
    }
}
