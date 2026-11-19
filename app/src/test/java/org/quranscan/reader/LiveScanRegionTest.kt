package org.quranscan.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.quranscan.reader.core.CropBounds
import org.quranscan.reader.core.LiveScanRegion

class LiveScanRegionTest {
    private val selection = LiveScanRegion(.125f, .25f, .5f, .625f)
    private val viewport = CropBounds(80, 40, 720, 520)

    @Test fun mapsAllQuarterTurnsWithinOffsetViewport() {
        val expected = mapOf(
            0 to CropBounds(160, 160, 400, 340),
            90 to CropBounds(240, 280, 480, 460),
            180 to CropBounds(400, 220, 640, 400),
            270 to CropBounds(320, 100, 560, 280)
        )
        expected.forEach { (rotation, bounds) ->
            assertEquals(bounds, selection.bitmapBounds(viewport, rotation, 800, 600, 800, 600))
        }
    }

    @Test fun mapsSampledJpegAndKeepsFullSelectionWithinViewport() {
        assertEquals(
            CropBounds(120, 140, 240, 230),
            selection.bitmapBounds(viewport, 90, 800, 600, 400, 300)
        )
        assertEquals(
            CropBounds(40, 20, 360, 260),
            LiveScanRegion(0f, 0f, 1f, 1f).bitmapBounds(viewport, 270, 800, 600, 400, 300)
        )
    }

    @Test fun rejectsInvalidSelectionsAndFrames() {
        assertEquals(selection, LiveScanRegion.selection(.5f, .625f, .125f, .25f))
        assertNull(LiveScanRegion.selection(0f, 0f, .01f, 1f))
        assertNull(LiveScanRegion.selection(0f, 0f, Float.NaN, 1f))
        assertNull(LiveScanRegion.selection(0f, 0f, 2f, 1f))
        assertNull(selection.bitmapBounds(viewport, 45, 800, 600, 400, 300))
        assertNull(selection.bitmapBounds(viewport, 0, 700, 600, 400, 300))
        assertNull(selection.bitmapBounds(viewport, 0, 800, 600, 16, 16))
    }
}
