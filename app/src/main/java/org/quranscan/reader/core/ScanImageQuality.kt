package org.quranscan.reader.core

import kotlin.math.abs

/** Bounded diagnostics for capture guidance. Thresholds need real-page calibration. */
data class ScanImageQuality(val meanLuma: Double, val strongEdgePercent: Double) {
    val lowLight get() = meanLuma < 65
    val lowDetail get() = strongEdgePercent < 0.6

    companion object {
        fun measure(width: Int, height: Int, pixelAt: (Int, Int) -> Int): ScanImageQuality {
            require(width > 1 && height > 1)
            val columns = minOf(56, width - 1)
            val rows = minOf(56, height - 1)
            var totalLuma = 0
            var edgeCount = 0
            for (row in 0 until rows) {
                val y = ((row + .5) * (height - 1) / rows).toInt()
                for (column in 0 until columns) {
                    val x = ((column + .5) * (width - 1) / columns).toInt()
                    val center = luma(pixelAt(x, y))
                    totalLuma += center
                    if (abs(center - luma(pixelAt(x + 1, y))) >= 22) edgeCount++
                    if (abs(center - luma(pixelAt(x, y + 1))) >= 22) edgeCount++
                }
            }
            val samples = rows * columns
            return ScanImageQuality(totalLuma.toDouble() / samples, edgeCount * 50.0 / samples)
        }

        private fun luma(argb: Int): Int {
            val alpha = argb ushr 24
            val red = (argb ushr 16) and 255
            val green = (argb ushr 8) and 255
            val blue = argb and 255
            val gray = (54 * red + 183 * green + 19 * blue) ushr 8
            return (gray * alpha + 255 * (255 - alpha)) / 255
        }
    }
}
