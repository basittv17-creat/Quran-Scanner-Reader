package org.quranscan.reader

import android.Manifest
import android.content.pm.PackageManager
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Matrix
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.Gravity
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
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
            showStatus("Camera permission declined. Allow camera permission to use Live Scan.")
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
        buildInterface()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED) startCamera()
        else permissionLauncher.launch(Manifest.permission.CAMERA)
    }

    private fun buildInterface() {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setBackgroundColor(green)
            setPadding(dp(12), dp(12), dp(12), dp(12))
        }
        setContentView(root)
        val title = TextView(this).apply {
            text = "Quran Scan Reader  •  Live Scan"
            textSize = 19f
            setTextColor(gold)
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, dp(8))
        }
        root.addView(title)
        previewView = PreviewView(this).apply {
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
            scaleType = PreviewView.ScaleType.FIT_CENTER
        }
        val frame = FrameLayout(this).apply { setBackgroundColor(Color.BLACK) }
        frame.addView(previewView, FrameLayout.LayoutParams(-1, -1))
        // Noninteractive decorative frame; no coordinates are treated as OCR certainty.
        frame.addView(ScanReticleView(this), FrameLayout.LayoutParams(-1, -1))
        root.addView(frame, LinearLayout.LayoutParams(-1, 0, 1f))
        status = TextView(this).apply {
            textSize = 13f
            setTextColor(gold)
            gravity = Gravity.CENTER
            setPadding(dp(5), dp(8), dp(5), dp(8))
            text = "Preparing camera…"
        }
        root.addView(status)
        val buttons = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        fun control(text: String, click: () -> Unit): Button = Button(this).apply {
            this.text = text
            isAllCaps = false
            textSize = 12f
            setOnClickListener { click() }
            buttons.addView(this, LinearLayout.LayoutParams(0, dp(54), 1f))
        }
        control("Scan photo") { capturePhotoOrNextFrame() }
        togglePause = control("Pause auto") { toggleAutomaticScan() }
        control("Home") { finish() }
        root.addView(buttons)
        rawOutput = TextView(this).apply {
            text = "Point the camera at a well-lit Quran page. Text below is unverified OCR. " +
                "No recitation will play until a trusted Quran corpus and verified audio are installed."
            textDirection = View.TEXT_DIRECTION_RTL
            gravity = Gravity.RIGHT
            setTextColor(Color.WHITE)
            textSize = 17f
            setPadding(dp(12), dp(12), dp(12), dp(12))
            setBackgroundColor(Color.rgb(12, 83, 65))
        }
        val scroller = ScrollView(this).apply {
            addView(rawOutput)
            isFillViewport = true
        }
        root.addView(scroller, LinearLayout.LayoutParams(-1, dp(155)))
        reviewContainer = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        root.addView(reviewContainer)
    }

    private fun startCamera() {
        if (isFinishing || isDestroyed) return
        showStatus("Opening CameraX… No internet access is required.")
        val future = ProcessCameraProvider.getInstance(this)
        future.addListener({
            if (!lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.STARTED)) return@addListener
            try {
                val provider = future.get()
                cameraProvider = provider
                val preview = Preview.Builder().build().apply {
                    setSurfaceProvider(previewView.surfaceProvider)
                }
                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setTargetResolution(android.util.Size(960, 720))
                    .build().apply {
                        setAnalyzer(analysisExecutor) { image -> analyze(image) }
                    }
                val still = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                // Some older cameras cannot provide three streams at once.
                try {
                    provider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis, still
                    )
                    imageCapture = still
                } catch (_: Exception) {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        this, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis
                    )
                    imageCapture = null
                }
                active = true
                showStatus("Live camera ready. Auto OCR samples one frame about every 4.5 seconds. " +
                    "Profile: $profile (generic Arabic model).")
            } catch (e: Exception) {
                active = false
                showStatus("CameraX unavailable on this device: ${e.message ?: "unknown error"}")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    /** CameraX calls this on one background executor. ImageProxy is ALWAYS closed. */
    private fun analyze(image: ImageProxy) {
        var bitmap: Bitmap? = null
        try {
            if (!active || processing.get()) return
            val manual = manuallyRequested.getAndSet(false)
            if (paused.get() && !manual) return
            if (!gate.shouldScan(SystemClock.elapsedRealtime(), manual)) return
            if (!processing.compareAndSet(false, true)) return
            bitmap = toUprightBitmap(image)
        } catch (e: Exception) {
            processing.set(false)
            postStatus("Frame conversion failed: ${e.message ?: "unknown"}")
        } finally {
            // Important: return camera buffer before running slow Tesseract OCR.
            image.close()
        }
        if (bitmap != null) recognizeOffline(bitmap)
    }

    /** Capture a real CameraX still, or request the next live frame as a fallback. */
    private fun capturePhotoOrNextFrame() {
        if (!active) { showStatus("The camera is not ready."); return }
        if (processing.get()) { showStatus("Previous scan is still running. Try again."); return }
        val still = imageCapture
        if (still == null) {
            manuallyRequested.set(true)
            showStatus("This camera supports frame scanning; capturing the next live frame…")
            return
        }
        // Pause automatic attempts briefly while a still is captured.
        processing.set(true)
        showStatus("Capturing a Quran page with CameraX…")
        still.takePicture(analysisExecutor, object : ImageCapture.OnImageCapturedCallback() {
            override fun onCaptureSuccess(image: ImageProxy) {
                try {
                    val upright = toUprightBitmap(image)
                    recognizeOffline(upright)
                } catch (e: Exception) {
                    postStatus("Capture failed safely: ${e.message ?: "unknown"}")
                    processing.set(false)
                } finally { image.close() }
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
        togglePause.text = if (nowPaused) "Resume auto" else "Pause auto"
        if (!nowPaused) gate.reset()
        showStatus(if (nowPaused) "Automatic scanning paused. 'Scan photo' still works."
            else "Automatic scanning resumed. Text remains unverified.")
    }

    private fun toUprightBitmap(image: ImageProxy): Bitmap {
        val original = image.toBitmap()
        val angle = image.imageInfo.rotationDegrees
        val upright = if (angle == 0) original else {
            val rotated = Bitmap.createBitmap(
                original, 0, 0, original.width, original.height,
                Matrix().apply { postRotate(angle.toFloat()) }, true
            )
            if (rotated !== original) original.recycle()
            rotated
        }
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

    private fun recognizeOffline(bitmap: Bitmap) {
        try {
            postStatus("Processing Arabic OCR completely offline…")
            val scan = OfflineArabicOcr(applicationContext).read(bitmap, profile)
            val corpus = OfflineCorpus(applicationContext)
            val complete = corpus.isComplete()
            val candidates = if (complete)
                VerseCandidates.suggest(scan.rawUnverifiedText, corpus.asMap()) else emptyList()
            runOnUiThread {
                if (isFinishing || isDestroyed) return@runOnUiThread
                reviewContainer.removeAllViews()
                if (complete && candidates.isNotEmpty()) {
                    candidates.take(3).forEach { candidate ->
                        val btn = Button(this).apply {
                            text = "Review ${candidate.id.surah}:${candidate.id.ayah} (manual verification)"
                            isAllCaps = false
                            setOnClickListener {
                                startActivity(Intent(this@LiveScanActivity, RecitationActivity::class.java).apply {
                                    putExtra(RecitationActivity.EXTRA_SURAH, candidate.id.surah)
                                    putExtra(RecitationActivity.EXTRA_AYAH, candidate.id.ayah)
                                })
                            }
                        }
                        reviewContainer.addView(btn)
                    }
                }
                val result = scan.rawUnverifiedText.trim()
                rawOutput.text = if (result.isEmpty())
                    "No readable Arabic found. Hold steady, increase lighting, and try again."
                else buildString {
                    append("⚠ UNVERIFIED OCR (may contain errors in words or harakat)\n\n")
                    append(result.take(1800))
                    append("\n\n")
                    if (!complete) append("Reference Quran text is not yet bundled. No verse is verified. Audio disabled.")
                    else if (candidates.isEmpty()) append("No reliable verse candidate. Audio disabled.")
                    else {
                        append("Possible references only; user confirmation required:\n")
                        candidates.forEach { append("${it.id.surah}:${it.id.ayah} — ${it.canonicalArabic}\n") }
                    }
                }
                showStatus("OCR finished · ${scan.variantsChecked} variants · No automatic recitation")
            }
        } catch (e: Exception) {
            postStatus("OCR failed safely: ${e.message ?: "unknown"}")
        } finally {
            bitmap.recycle()
            processing.set(false)
        }
    }

    private fun postStatus(msg: String) = runOnUiThread {
        if (!isFinishing && !isDestroyed) showStatus(msg)
    }
    private fun showStatus(msg: String) { status.text = msg }
    private fun dp(v: Int) = (v * resources.displayMetrics.density + 0.5f).toInt()

    override fun onStop() {
        active = false
        super.onStop()
    }

    override fun onStart() {
        super.onStart()
        if (cameraProvider != null) { active = true; gate.reset() }
    }

    override fun onDestroy() {
        active = false
        cameraProvider?.unbindAll()
        analysisExecutor.shutdownNow()
        super.onDestroy()
    }
}
