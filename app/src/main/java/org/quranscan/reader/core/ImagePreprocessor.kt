package org.quranscan.reader.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/** Offline, bounded-memory preprocessing for OCR. Never interprets Quranic text. */
object ImagePreprocessor {
    /** Convert bright lettering on a dark background to ink on white with a quiet border. */
    fun prepareDarkLine(source: Bitmap): Bitmap {
        val padding = 20
        val scale = minOf(3f, 1560f / maxOf(source.width, source.height))
        val width = maxOf(1, (source.width * scale).toInt())
        val height = maxOf(1, (source.height * scale).toInt())
        val resized = Bitmap.createScaledBitmap(source, width, height, true)
        try {
            val pixels = IntArray(width * height)
            resized.getPixels(pixels, 0, width, 0, 0, width, height)
            for (index in pixels.indices) {
                val argb = pixels[index]
                val red = (argb ushr 16) and 255
                val green = (argb ushr 8) and 255
                val blue = argb and 255
                val brightness = (54 * red + 183 * green + 19 * blue) ushr 8
                pixels[index] = if (brightness >= 145) 0xff000000.toInt() else 0xffffffff.toInt()
            }
            return Bitmap.createBitmap(width + 2 * padding, height + 2 * padding, Bitmap.Config.ARGB_8888).apply {
                eraseColor(0xffffffff.toInt())
                setPixels(pixels, 0, width, padding, padding, width, height)
            }
        } finally {
            if (resized !== source) resized.recycle()
        }
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
