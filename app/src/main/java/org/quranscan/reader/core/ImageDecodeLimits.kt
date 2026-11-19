package org.quranscan.reader.core

/** Power-of-two decoder sampling before allocating pixels; no Android dependencies. */
object ImageDecodeLimits {
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        require(width > 0 && height > 0 && maxEdge in 256..4096) { "Invalid image dimensions" }
        var sample = 1
        // Ceiling division also bounds images just above a sample boundary.
        while ((maxOf(width, height).toLong() + sample - 1) / sample > maxEdge) {
            sample *= 2
        }
        return sample
    }
}
