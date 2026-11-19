package org.quranscan.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quranscan.reader.core.ScanImageQuality

class ScanImageQualityTest {
    @Test fun flatDarkAndFlatBrightHaveDifferentLighting() {
        val dark = ScanImageQuality.measure(800, 600) { _, _ -> 0xff202020.toInt() }
        val bright = ScanImageQuality.measure(800, 600) { _, _ -> 0xffeeeeee.toInt() }
        assertEquals(32.0, dark.meanLuma, 0.0)
        assertTrue(dark.lowLight)
        assertFalse(bright.lowLight)
        assertTrue(bright.lowDetail)
    }

    @Test fun sharpSyntheticEdgesAreMeasuredWithoutReadingAllPixels() {
        var pixelReads = 0
        val quality = ScanImageQuality.measure(1000, 1000) { x, y ->
            pixelReads++
            if ((x + y) % 2 == 0) 0xff202020.toInt() else 0xffeeeeee.toInt()
        }
        assertEquals(56 * 56 * 3, pixelReads)
        assertEquals(100.0, quality.strongEdgePercent, 0.0)
        assertFalse(quality.lowDetail)
        assertFalse(quality.lowLight)
    }

    @Test fun transparentPixelsAreEvaluatedAgainstPaperWhite() {
        val quality = ScanImageQuality.measure(2, 2) { _, _ -> 0 }
        assertEquals(255.0, quality.meanLuma, 0.0)
        assertFalse(quality.lowLight)
    }
    @Test fun partialAlphaAndEdgeThresholdHaveExactResults() {
        val quality = ScanImageQuality.measure(2, 2) { x, y ->
            when {
                x == 1 -> 0xff696969.toInt() // Luma 105, exactly 22 below the center.
                y == 1 -> 0xff6a6a6a.toInt() // Luma 106, only 21 below the center.
                else -> 0x80000000.toInt() // Half-transparent black on paper white has luma 127.
            }
        }
        assertEquals(127.0, quality.meanLuma, 0.0)
        assertEquals(50.0, quality.strongEdgePercent, 0.0)
        assertFalse(ScanImageQuality(65.0, 0.6).lowLight)
        assertFalse(ScanImageQuality(65.0, 0.6).lowDetail)
    }

    @Test fun extremeDimensionsKeepEveryReadInRange() {
        val quality = ScanImageQuality.measure(Int.MAX_VALUE, 2) { x, y ->
            assertTrue(x in 0 until Int.MAX_VALUE)
            assertTrue(y in 0..1)
            0xff404040.toInt()
        }
        assertEquals(64.0, quality.meanLuma, 0.0)
        assertEquals(0.0, quality.strongEdgePercent, 0.0)
    }
}
