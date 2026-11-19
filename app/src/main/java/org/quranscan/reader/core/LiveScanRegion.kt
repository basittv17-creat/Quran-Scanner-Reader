package org.quranscan.reader.core

/** A selection in the upright CameraX viewport, normalized to its visible bounds. */
data class LiveScanRegion(
    val left: Float, val top: Float, val right: Float, val bottom: Float,
    val useFullImage: Boolean = false
) {
    fun bitmapBounds(
        viewport: CropBounds,
        rotation: Int,
        imageWidth: Int,
        imageHeight: Int,
        bitmapWidth: Int,
        bitmapHeight: Int
    ): CropBounds? {
        if (imageWidth <= 0 || imageHeight <= 0 || bitmapWidth <= 0 || bitmapHeight <= 0) return null
        if (useFullImage) return when (rotation) {
            0, 90, 180, 270 -> CropBounds(0, 0, bitmapWidth, bitmapHeight)
            else -> null
        }
        if (viewport.left < 0 || viewport.top < 0 || viewport.right > imageWidth ||
            viewport.bottom > imageHeight || viewport.width <= 0 || viewport.height <= 0) return null
        val raw = when (rotation) {
            0 -> this
            90 -> LiveScanRegion(top, 1f - right, bottom, 1f - left)
            180 -> LiveScanRegion(1f - right, 1f - bottom, 1f - left, 1f - top)
            270 -> LiveScanRegion(1f - bottom, left, 1f - top, right)
            else -> return null
        }
        return CropBounds.fromDrag(
            bitmapWidth, bitmapHeight,
            (viewport.left + raw.left * viewport.width) * bitmapWidth / imageWidth,
            (viewport.top + raw.top * viewport.height) * bitmapHeight / imageHeight,
            (viewport.left + raw.right * viewport.width) * bitmapWidth / imageWidth,
            (viewport.top + raw.bottom * viewport.height) * bitmapHeight / imageHeight
        )
    }

    fun coordinates(): FloatArray = floatArrayOf(left, top, right, bottom)

    companion object {
        val FULL_IMAGE = LiveScanRegion(0f, 0f, 1f, 1f, useFullImage = true)
        val DEFAULT = LiveScanRegion(.09f, .20f, .91f, .80f)

        fun selection(x1: Float, y1: Float, x2: Float, y2: Float): LiveScanRegion? {
            if (listOf(x1, y1, x2, y2).any { it !in 0f..1f }) return null
            val region = LiveScanRegion(minOf(x1, x2), minOf(y1, y2), maxOf(x1, x2), maxOf(y1, y2))
            return region.takeIf { it.right - it.left >= .05f && it.bottom - it.top >= .05f }
        }
    }
}
