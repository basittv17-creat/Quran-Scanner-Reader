package org.quranscan.reader.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/** Offline, bounded-memory preprocessing for OCR. Never interprets Quranic text. */
object ImagePreprocessor {
    /** Upsample a narrow crop and give Tesseract a light background and a clean margin. */
    fun prepareLine(source: Bitmap, invert: Boolean, binary: Boolean = false): Bitmap {
        val padding = 30
        val scale = minOf(3f, 1540f / maxOf(source.width, source.height))
        val width = maxOf(1, (source.width * scale).toInt())
        val height = maxOf(1, (source.height * scale).toInt())
        val input = if (scale < 1f) Bitmap.createScaledBitmap(source, width, height, true) else source
        try {
            val pixels = IntArray(input.width * input.height)
            input.getPixels(pixels, 0, input.width, 0, 0, input.width, input.height)
            for (index in pixels.indices) {
                val argb = pixels[index]
                val red = (argb ushr 16) and 255
                val green = (argb ushr 8) and 255
                val blue = argb and 255
                val brightness = (54 * red + 183 * green + 19 * blue) ushr 8
                pixels[index] = when {
                    binary -> if (brightness >= 145) 0 else 255
                    invert -> 255 - brightness
                    else -> brightness
                }
            }
            val resized = if (width == input.width && height == input.height) pixels else {
                // Interpolate each axis independently to preserve thin strokes and diacritics.
                val horizontal = resizeGrayRows(pixels, input.width, input.height, width)
                val transposed = IntArray(horizontal.size) { i ->
                    horizontal[(i % input.height) * width + i / input.height]
                }
                val vertical = resizeGrayRows(transposed, input.height, width, height)
                IntArray(width * height) { i -> vertical[(i % width) * height + i / width] }
            }
            for (i in resized.indices) {
                val gray = resized[i]
                resized[i] = 0xff000000.toInt() or (gray shl 16) or (gray shl 8) or gray
            }
            return Bitmap.createBitmap(width + 2 * padding, height + 2 * padding, Bitmap.Config.ARGB_8888).apply {
                eraseColor(0xffffffff.toInt())
                setPixels(resized, 0, width, padding, padding, width, height)
            }
        } finally {
            if (input !== source) input.recycle()
        }
    }

    private fun resizeGrayRows(pixels: IntArray, oldWidth: Int, rows: Int, newWidth: Int): IntArray {
        val result = IntArray(newWidth * rows)
        for (x in 0 until newWidth) {
            val sourceX = (x + .5f) * oldWidth / newWidth - .5f
            val center = kotlin.math.floor(sourceX).toInt()
            val t = sourceX - center
            val w0 = -.5f * t * (1f - t) * (1f - t)
            val w1 = 1f + 1.5f * t * t * t - 2.5f * t * t
            val w2 = .5f * t + 2f * t * t - 1.5f * t * t * t
            val w3 = -.5f * t * t * (1f - t)
            val a = (center - 1).coerceIn(0, oldWidth - 1)
            val b = center.coerceIn(0, oldWidth - 1)
            val c = (center + 1).coerceIn(0, oldWidth - 1)
            val d = (center + 2).coerceIn(0, oldWidth - 1)
            for (row in 0 until rows) {
                val offset = row * oldWidth
                val gray = pixels[offset + a] * w0 + pixels[offset + b] * w1 +
                    pixels[offset + c] * w2 + pixels[offset + d] * w3
                result[row * newWidth + x] = (gray + .5f).toInt().coerceIn(0, 255)
            }
        }
        return result
    }

    fun normalize(source: Bitmap, maxEdge: Int = 1600): Bitmap {
        require(maxEdge in 256..4096)
        val scale = minOf(1f, maxEdge.toFloat() / maxOf(source.width, source.height))
        val w = maxOf(1, (source.width * scale).toInt())
        val h = maxOf(1, (source.height * scale).toInt())
        val resized = Bitmap.createScaledBitmap(source, w, h, true)
        try {
            val matrix = ColorMatrix().apply { setSaturation(0f) }
            val contrast = 1.25f
            val offset = (1f - contrast) * 128f
            val adjustment = ColorMatrix(floatArrayOf(
                contrast, 0f, 0f, 0f, offset,
                0f, contrast, 0f, 0f, offset,
                0f, 0f, contrast, 0f, offset,
                0f, 0f, 0f, 1f, 0f
            ))
            matrix.postConcat(adjustment)
            val paint = Paint(Paint.FILTER_BITMAP_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
            val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            var drawn = false
            try {
                Canvas(output).drawBitmap(resized, 0f, 0f, paint)
                drawn = true
                return output
            } finally {
                if (!drawn) output.recycle()
            }
        } finally {
            if (resized !== source) resized.recycle()
        }
    }
}
