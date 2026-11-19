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
}
