package org.quranscan.reader.core

import kotlin.math.ceil
import kotlin.math.floor

/** Image-space selection only. Never transforms or interprets Arabic text. */
data class CropBounds(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    val width get() = right - left
    val height get() = bottom - top
    companion object {
        fun fromDrag(width: Int, height: Int, x1: Float, y1: Float, x2: Float, y2: Float): CropBounds? {
            require(width > 0 && height > 0)
            if (listOf(x1, y1, x2, y2).any { !it.isFinite() }) return null
            val l = floor(minOf(x1, x2).coerceIn(0f, width.toFloat())).toInt()
            val t = floor(minOf(y1, y2).coerceIn(0f, height.toFloat())).toInt()
            val r = ceil(maxOf(x1, x2).coerceIn(0f, width.toFloat())).toInt()
            val b = ceil(maxOf(y1, y2).coerceIn(0f, height.toFloat())).toInt()
            return if (r - l >= 16 && b - t >= 16) CropBounds(l, t, r, b) else null
        }
    }
}
