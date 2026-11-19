package org.quranscan.reader.core

/** Validation for the sampled gallery preview and its saved image-space selection. */
internal object GalleryImageState {
    const val MAX_EDGE = 2048

    fun sampleSize(width: Int, height: Int): Int? =
        if (width > 0 && height > 0) ImageDecodeLimits.sampleSize(width, height, MAX_EDGE) else null

    fun acceptsPreview(width: Int, height: Int): Boolean =
        width in 1..MAX_EDGE && height in 1..MAX_EDGE

    fun savedSelection(edges: IntArray?): CropBounds? {
        if (edges?.size != 4) return null
        val (left, top, right, bottom) = edges
        if (left < 0 || top < 0 || right <= left || bottom <= top) return null
        return CropBounds(left, top, right, bottom)
    }

    fun selectionFor(width: Int, height: Int, restored: CropBounds?): CropBounds {
        val fullImage = CropBounds(0, 0, width, height)
        return restored?.takeIf {
            it == fullImage || (it.left >= 0 && it.top >= 0 && it.right <= width &&
                it.bottom <= height && it.right.toLong() - it.left >= 16 &&
                it.bottom.toLong() - it.top >= 16)
        } ?: fullImage
    }
}
