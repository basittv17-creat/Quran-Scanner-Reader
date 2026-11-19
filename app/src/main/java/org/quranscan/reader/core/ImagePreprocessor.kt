package org.quranscan.reader.core

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

/** Offline, bounded-memory preprocessing for future OCR. Never interprets Quranic text. */
object ImagePreprocessor {
    fun normalize(source: Bitmap, maxEdge: Int = 1600): Bitmap {
        require(maxEdge in 256..4096)
        val scale = minOf(1f, maxEdge.toFloat() / maxOf(source.width, source.height))
        val w = maxOf(1, (source.width * scale).toInt())
        val h = maxOf(1, (source.height * scale).toInt())
        val resized = Bitmap.createScaledBitmap(source, w, h, true)
        val output = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
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
        Canvas(output).drawBitmap(resized, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG).apply {
            colorFilter = ColorMatrixColorFilter(matrix)
        })
        if (resized !== source && resized !== output) resized.recycle()
        return output
    }
}
