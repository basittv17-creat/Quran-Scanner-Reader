package org.quranscan.reader.ocr

import android.content.Context
import android.graphics.Bitmap
import android.os.SystemClock
import com.googlecode.tesseract.android.TessBaseAPI
import org.quranscan.reader.core.ImagePreprocessor
import org.quranscan.reader.core.ScanImageQuality
import java.io.File
import java.security.MessageDigest

/**
 * Offline Arabic OCR baseline. No network, never downloads runtime models.
 * IMPORTANT: This is generic printed-Arabic recognition, NOT a trained and
 * independently benchmarked Quranic OCR system, and NOT a diacritic verifier.
 * Must be invoked from a background thread. No instance may be shared concurrently.
 */
class OfflineArabicOcr(private val context: Context) {
    enum class Profile { AUTO, INDO_PAK, MADANI }
    data class Scan(
        val rawUnverifiedText: String,
        val profile: Profile,
        val variantsChecked: Int,
        // OCR pipeline wall time, including diagnostics, model preparation and per-variant cleanup.
        val elapsedMs: Long,
        val quality: ScanImageQuality?
    )

    fun read(bitmap: Bitmap, profile: Profile): Scan {
        val startedAt = SystemClock.elapsedRealtime()
        val quality = if (bitmap.width > 1 && bitmap.height > 1) {
            ScanImageQuality.measure(bitmap.width, bitmap.height, bitmap::getPixel)
        } else null
        val dataRoot = prepareModel()
        val engine = TessBaseAPI()
        try {
            check(engine.init(dataRoot.absolutePath, "ara")) { "Arabic OCR model failed to initialize" }
            // Baseline attempts only; script names are scanning profiles, NOT specialized models.
            val modes = when (profile) {
                Profile.INDO_PAK -> listOf(TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK, TessBaseAPI.PageSegMode.PSM_AUTO)
                Profile.MADANI -> listOf(TessBaseAPI.PageSegMode.PSM_AUTO, TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
                Profile.AUTO -> listOf(TessBaseAPI.PageSegMode.PSM_AUTO, TessBaseAPI.PageSegMode.PSM_SINGLE_BLOCK)
            }
            var bestText = ""
            var bestScore = -1
            var count = 0

            fun checkCancellation() {
                if (Thread.currentThread().isInterrupted) throw InterruptedException("OCR cancelled")
            }

            fun tryModes(input: Bitmap): Boolean {
                for (mode in modes) {
                    checkCancellation()
                    engine.setPageSegMode(mode)
                    engine.setImage(input)
                    val result = engine.getUTF8Text().orEmpty().trim()
                    val confidence = engine.meanConfidence().coerceIn(0, 100)
                    // Confidence is only OCR confidence, NOT Quran verification.
                    val arabicCount = result.count { it in '\u0621'..'\u064A' }
                    val score = if (arabicCount > 0) confidence * 100 + minOf(arabicCount, 100) else -1
                    if (score > bestScore) {
                        bestScore = score
                        bestText = result
                    }
                    count++
                    engine.clear()
                    // Fast path on clearly legible pages; still UNVERIFIED OCR.
                    if (confidence >= 83 && arabicCount >= 30) return true
                }
                return false
            }

            if (!tryModes(bitmap)) {
                checkCancellation()
                val prepared = ImagePreprocessor.normalize(bitmap)
                try {
                    tryModes(prepared)
                } finally {
                    prepared.recycle()
                }
            }
            return Scan(
                bestText.take(10000), profile, count,
                SystemClock.elapsedRealtime() - startedAt, quality
            )
        } finally {
            engine.recycle()
        }
    }

    private fun prepareModel(): File = synchronized(modelLock) {
        val folder = File(context.filesDir, "tesseract/tessdata")
        check(folder.isDirectory || folder.mkdirs()) { "Cannot make private OCR data folder" }
        val dest = File(folder, "ara.traineddata")
        val asset = "tessdata/ara.traineddata"
        // Verify packaged asset and extracted bytes, even if old model file is present.
        val expected = MODEL_SHA256
        if (!dest.exists() || sha256(dest) != expected) {
            val temp = File(folder, "ara.traineddata.partial")
            try {
                context.assets.open(asset).use { input -> temp.outputStream().use { input.copyTo(it) } }
                check(sha256(temp) == expected) { "Packaged Arabic OCR model checksum mismatch" }
                if (dest.exists() && !dest.delete()) error("Cannot replace previous OCR model")
                check(temp.renameTo(dest)) { "Cannot install private OCR model" }
            } finally {
                if (temp.exists()) temp.delete()
            }
        }
        folder.parentFile ?: error("Model path invalid")
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(64 * 1024)
            while (true) {
                val n = input.read(buffer)
                if (n < 0) break
                digest.update(buffer, 0, n)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        // Gallery and camera workers may overlap during navigation.
        private val modelLock = Any()
        // Debian tesseract-ocr-ara tessdata_fast model, Apache-2.0.
        const val MODEL_SHA256 = "e3206d3dc87fd50c24a0fb9f01838615911d25168f4e64415244b67d2bb3e729"
    }
}
