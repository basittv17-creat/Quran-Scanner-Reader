package org.quranscan.reader

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Color
import android.graphics.Matrix
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
import org.quranscan.reader.core.CameraScanGate
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.core.VerseCandidates
import org.quranscan.reader.ocr.OfflineArabicOcr
import java.util.concurrent.ExecutorService
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
    private val analysisExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private val gate = CameraScanGate(4_500L)
    private val manuallyRequested = AtomicBoolean(false)
    private val paused = AtomicBoolean(false)
    private val processing = AtomicBoolean(false)
    private var cameraProvider: ProcessCameraProvider? = null
    private var imageCapture: ImageCapture? = null
    private var camera: Camera? = null
    private var cameraStarting = false
    @Volatile private var session = 0
    private var exposureSteps = 0
    @Volatile private var scanRegion = LiveScanRegion.DEFAULT
    private lateinit var reticle: ScanReticleView
    private var dragX = 0f
    private var dragY = 0f
    private lateinit var previewView: PreviewView
    private lateinit var status: TextView
    private lateinit var rawOutput: TextView
    private lateinit var togglePause: Button
    private lateinit var reviewContainer: LinearLayout
    private var profile = OfflineArabicOcr.Profile.AUTO
    @Volatile private var active = false

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) startCamera()
        else {
            showStatus(getString(R.string.live_permission_denied))
            rawOutput.text = "Camera not opened. Gallery Scan is still available on Home."
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.keepScreenOn = getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getBoolean("keep_screen_on", false)
        profile = runCatching {
            OfflineArabicOcr.Profile.valueOf(
                getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getString("profile", "AUTO") ?: "AUTO"
            )
        }.getOrDefault(OfflineArabicOcr.Profile.AUTO)
        paused.set(getSharedPreferences("quran_reader_settings", MODE_PRIVATE).getBoolean("auto_scan_paused", false))
        savedInstanceState?.getFloatArray("live_scan_region")?.takeIf { it.size == 4 }?.let {
            scanRegion = LiveScanRegion.selection(it[0], it[1], it[2], it[3]) ?: LiveScanRegion.DEFAULT
        }
        if (savedInstanceState?.getBoolean("live_scan_full_image") == true) {
            scanRegion = LiveScanRegion.FULL_IMAGE
        }
        buildInterface()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) !=
            PackageManager.PERMISSION_GRANTED) permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    override fun onSaveInstanceState(outState: Bundle) {
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
        setContentView(root)
        root.applyReaderInsets()
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
            setOnTouchListener { view, event ->
                if (width <= 0 || height <= 0) return@setOnTouchListener false
                val x = (event.x / width).coerceIn(0f, 1f)
                val y = (event.y / height).coerceIn(0f, 1f)
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        dragX = x
                        dragY = y
                    }
                    MotionEvent.ACTION_UP -> {
                        val region = LiveScanRegion.selection(dragX, dragY, x, y)
                        if (region != null) {
                            setScanRegion(region)
                        } else if (kotlin.math.abs(x - dragX) * width < dp(12) &&
                            kotlin.math.abs(y - dragY) * height < dp(12)) {
                            view.performClick()
                            camera?.let { current ->
                                val point = meteringPointFactory.createPoint(event.x, event.y)
                                current.cameraControl.startFocusAndMetering(FocusMeteringAction.Builder(point).build())
                            }
                        }
                    }
                }
                true
            }
        }
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        frame.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        reticle = ScanReticleView(this).apply { region = scanRegion }
        frame.addView(reticle, FrameLayout.LayoutParams(-1, -1))
        previewView.addOnLayoutChangeListener { _, left, top, right, bottom, oldLeft, oldTop, oldRight, oldBottom ->
            if (right - left != oldRight - oldLeft || bottom - top != oldBottom - oldTop) {
                if (active || cameraStarting) releaseCamera()
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                    startCamera()
                }
            }
        }
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
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
                setOnClickListener {
                    val current = camera ?: return@setOnClickListener
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
            setOnClickListener { setScanRegion(LiveScanRegion.DEFAULT) }
        }, LinearLayout.LayoutParams(0, -2, 1f))
        root.addView(exposure)
        root.addView(Button(this).apply {
            setText(R.string.live_scan_full_image)
            isAllCaps = false
            setOnClickListener { setScanRegion(LiveScanRegion.FULL_IMAGE) }
        }, LinearLayout.LayoutParams(-1, -2))
        rawOutput = TextView(this).apply {
            text = getString(R.string.live_initial_guidance)
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.START
                            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(12, 83, 65))
        }
        val scroller = ScrollView(this).apply {
            addView(rawOutput)
            isFillViewport = true
        }
        reviewContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        scroller.removeView(rawOutput)
        val results = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            addView(rawOutput)
            addView(reviewContainer)
        }
        scroller.addView(results)
        // Candidates share the scroll area so all review controls remain reachable.
        root.addView(scroller, LinearLayout.LayoutParams(-1, 0, 1f))
    }

    private fun setScanRegion(region: LiveScanRegion) {
        scanRegion = region.copy()
        reticle.region = region
        reticle.invalidate()
        reviewContainer.removeAllViews()
        rawOutput.setText(if (region.useFullImage) R.string.live_scan_full_image_selected else R.string.live_crop_updated)
        gate.reset()
    }

    private fun startCamera() {
        if (isFinishing || isDestroyed || cameraStarting || active ||
            !lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return
        if (previewView.viewPort == null) return
        cameraStarting = true
        val openingSession = session
        showStatus(getString(R.string.live_opening_camera))
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (openingSession != session || isDestroyed) return@addListener
            cameraStarting = false
            if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return@addListener
            try {
                val viewport = previewView.viewPort ?: return@addListener
                val rotation = previewView.display?.rotation ?: return@addListener
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().setTargetRotation(rotation).build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setTargetRotation(rotation)
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(960, 720))
                    .build().apply {
                        setAnalyzer(analysisExecutor) { image -> analyze(image, openingSession) }
                    }
                val still = ImageCapture.Builder()
                    .setTargetRotation(rotation)
                    .setTargetResolution(android.util.Size(1500, 1125))
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                // Some older cameras cannot provide three streams at once.
                try {
                    camera = provider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA,
                        UseCaseGroup.Builder().setViewPort(viewport)
                            .addUseCase(preview).addUseCase(analysis).addUseCase(still).build()
                    )
                    imageCapture = still
                } catch (_: Exception) {
                    provider.unbindAll()
                    camera = provider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA,
                        UseCaseGroup.Builder().setViewPort(viewport)
                            .addUseCase(preview).addUseCase(analysis).build()
                    )
                    imageCapture = null
                }
                active = true
                showStatus(getString(R.string.live_crop_hint))
            } catch (e: Exception) {
                active = false
                showStatus("CameraX unavailable on this device: ${e.message ?: "unknown error"}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /** CameraX calls this on one background executor. ImageProxy is ALWAYS closed. */
    private fun analyze(image: ImageProxy, scanSession: Int) {
        var bitmap: Bitmap? = null
        val region = scanRegion
        try {
            if (!active || scanSession != session || processing.get()) return
            val manual = manuallyRequested.getAndSet(false)
            if (paused.get() && !manual) return
            if (!gate.shouldScan(SystemClock.elapsedRealtime(), manual)) return
            if (!processing.compareAndSet(false, true)) return
            bitmap = toUprightBitmap(image, region)
        } catch (e: Exception) {
            processing.set(false)
            postStatus("Frame conversion failed: ${e.message ?: "unknown"}")
        } finally {
            // Important: return camera buffer before running slow Tesseract OCR.
            image.close()
        }
        if (bitmap != null) recognizeOffline(bitmap, scanSession, region)
    }

    /** Capture a real CameraX still, or request the next live frame as a fallback. */
    private fun capturePhotoOrNextFrame() {
        if (!active) { showStatus(getString(R.string.live_not_ready)); return }
        if (!processing.compareAndSet(false, true)) { showStatus(getString(R.string.live_previous_scan_running)); return }
        val captureSession = session
        val region = scanRegion
        val still = imageCapture
        if (still == null) {
            manuallyRequested.set(true)
            processing.set(false)
            showStatus(getString(R.string.live_frame_fallback))
            return
        }
        // The atomic claim above excludes the analyzer while capturing.
        showStatus(getString(R.string.live_capturing))
        still.takePicture(analysisExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                var upright: Bitmap? = null
                try {
                    if (!active || captureSession != session) { processing.set(false); return }
                    upright = toUprightBitmap(image, region)
                } catch (e: Exception) {
                    postStatus("Capture failed safely: ${e.message ?: "unknown"}")
                    processing.set(false)
                } finally { image.close() }
                upright?.let { recognizeOffline(it, captureSession, region) }
            }

            override fun onError(exception: ImageCaptureException) {
                processing.set(false)
                postStatus("Camera capture error: ${exception.message ?: "unknown"}")
            }
        })
    }

    private fun toggleAutomaticScan() {
        val nowPaused = !paused.get()
        paused.set(nowPaused)
        getSharedPreferences("quran_reader_settings", MODE_PRIVATE).edit().putBoolean("auto_scan_paused", nowPaused).apply()
        togglePause.text = if (nowPaused) getString(R.string.live_resume_auto) else getString(R.string.live_pause_auto)
        if (!nowPaused) gate.reset()
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
        val angle = image.imageInfo.rotationDegrees
        val viewport = image.cropRect
        val upright = try {
            val bounds = requireNotNull(region.bitmapBounds(
                CropBounds(viewport.left, viewport.top, viewport.right, viewport.bottom),
                angle, image.width, image.height, original.width, original.height
            )) { "The selected camera area is too small or unavailable" }
            Bitmap.createBitmap(
                original, bounds.left, bounds.top, bounds.width, bounds.height,
                Matrix().apply { postRotate(angle.toFloat()) }, true
            )
        } catch (error: Exception) {
            original.recycle()
            throw error
        }
        if (upright !== original) original.recycle()
        // Limit OCR memory/CPU, especially on low-end phones and high-res captures.
        val max = maxOf(upright.width, upright.height)
        if (max <= 1500) return upright
        val factor = 1500f / max
        val resized = Bitmap.createScaledBitmap(
            upright, (upright.width * factor).toInt().coerceAtLeast(1),
            (upright.height * factor).toInt().coerceAtLeast(1), true
        )
        if (resized !== upright) upright.recycle()
        return resized
    }

    private fun recognizeOffline(bitmap: Bitmap, scanSession: Int, region: LiveScanRegion) {
        try {
            if (!active || scanSession != session || scanRegion !== region) return
            postStatus(getString(R.string.live_processing))
            val scan = OfflineArabicOcr(applicationContext).read(bitmap, profile)
            val corpus = OfflineCorpus(applicationContext)
            val complete = corpus.isComplete()
            val candidates = if (complete)
                VerseCandidates.suggest(scan.rawUnverifiedText, corpus.asMap()) else emptyList()
            runOnUiThread {
                if (!active || scanSession != session || scanRegion !== region || isFinishing || isDestroyed) return@runOnUiThread
                reviewContainer.removeAllViews()
                if (complete && candidates.isNotEmpty()) {
                    candidates.take(3).forEach { candidate ->
                        val btn = Button(this).apply {
                            text = getString(R.string.scan_review_candidate, candidate.id.surah, candidate.id.ayah)
                            isAllCaps = false
                            setOnClickListener {
                                startActivity(Intent(this@LiveScanActivity, RecitationActivity::class.java).apply {
                                    putExtra(RecitationActivity.EXTRA_SURAH, candidate.id.surah)
                                    putExtra(RecitationActivity.EXTRA_AYAH, candidate.id.ayah)
                                    putExtra(RecitationActivity.EXTRA_SCANNED_TEXT,
                                        scan.rawUnverifiedText.take(RecitationActivity.MAX_COMPARISON_LENGTH))
                                })
                            }
                        }
                        reviewContainer.addView(btn)
                    }
                }
                val result = scan.rawUnverifiedText.trim()
                rawOutput.text = if (result.isEmpty())
                    getString(R.string.live_empty_result)
                else buildString {
                    appendLine(getString(R.string.gallery_ocr_unverified))
                    appendLine()
                    append(result.take(1800))
                    append("\n\n")
                    if (!complete) append(getString(R.string.live_no_reference))
                    else if (candidates.isEmpty()) append(getString(R.string.live_no_candidate))
                    else {
                        appendLine(getString(R.string.gallery_possible_matches))
                        candidates.forEach { append("${it.id.surah}:${it.id.ayah} — ${it.canonicalArabic}\n") }
                    }
                }
                val summary = getString(R.string.scan_processing_summary, scan.variantsChecked, scan.elapsedMs)
                showStatus(listOfNotNull(summary, scan.quality.guidance(this)).joinToString("\n"))
            }
        } catch (e: Exception) {
            postStatus("OCR failed safely: ${e.message ?: "unknown"}")
        } finally {
            bitmap.recycle()
            processing.set(false)
        }
    }

    private fun postStatus(msg: String) = runOnUiThread {
        if (active && !isFinishing && !isDestroyed) showStatus(msg)
    }
    private fun showStatus(msg: String) { status.text = msg }
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    private fun releaseCamera() {
        active = false
        session++
        cameraStarting = false
        manuallyRequested.set(false)
        imageCapture = null
        camera = null
        cameraProvider?.unbindAll()
    }

    override fun onStop() {
        releaseCamera()
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        gate.reset()
        // Lifecycle ON_START can be dispatched after onStart returns.
        previewView.post {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) startCamera()
        }
    }

    override fun onDestroy() {
        active = false
        cameraProvider?.unbindAll()
        analysisExecutor.shutdownNow()
        super.onDestroy()
    }
}
