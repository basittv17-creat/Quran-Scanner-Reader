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

    @Test fun wideAnalysisAndTallSampledCaptureMapTheSameViewportSelection() {
        val wideAnalysisViewport = CropBounds(160, 60, 800, 420)
        val tallCaptureViewport = CropBounds(64, 384, 1344, 1104)
        val expected = mapOf(
            0 to CropBounds(80, 90, 320, 225),
            90 to CropBounds(160, 180, 400, 315),
            180 to CropBounds(320, 135, 560, 270),
            270 to CropBounds(240, 45, 480, 180)
        )
        expected.forEach { (rotation, localBounds) ->
            assertEquals(
                CropBounds(localBounds.left + 160, localBounds.top + 60,
                    localBounds.right + 160, localBounds.bottom + 60),
                selection.bitmapBounds(wideAnalysisViewport, rotation, 960, 480, 960, 480)
            )
            assertEquals(
                CropBounds(localBounds.left + 32, localBounds.top + 192,
                    localBounds.right + 32, localBounds.bottom + 192),
                selection.bitmapBounds(tallCaptureViewport, rotation, 1440, 1920, 720, 960)
            )
        }
    }

    @Test fun roundsOutwardWithUnevenDecodeDimensions() {
        assertEquals(
            CropBounds(80, 80, 201, 171),
            selection.bitmapBounds(viewport, 0, 800, 600, 401, 301)
        )
    }

    @Test fun invalidViewportOrTooSmallSelectionFallsBackToDecodedFullImage() {
        val fullImage = CropBounds(0, 0, 400, 300)
        for (invalid in listOf(
            CropBounds(720, 40, 80, 520),
            CropBounds(80, 520, 720, 40),
            CropBounds(-1, 40, 720, 520),
            CropBounds(80, 40, 720, 601),
            CropBounds(80, 40, 80, 520)
        )) {
            assertEquals(fullImage, selection.bitmapBoundsOrFullImage(invalid, 90, 800, 600, 400, 300))
        }
        assertEquals(
            CropBounds(0, 0, 16, 16),
            selection.bitmapBoundsOrFullImage(viewport, 270, 800, 600, 16, 16)
        )
        assertEquals(
            CropBounds(120, 140, 240, 230),
            selection.bitmapBoundsOrFullImage(viewport, 90, 800, 600, 400, 300)
        )
    }

    @Test fun malformedDirectRegionsDoNotSilentlyClampOrReverse() {
        for (invalid in listOf(
            LiveScanRegion(.5f, .25f, .125f, .625f),
            LiveScanRegion(.125f, .625f, .5f, .25f),
            LiveScanRegion(-.125f, .25f, .5f, .625f),
            LiveScanRegion(.125f, .25f, 1.125f, .625f),
            LiveScanRegion(.125f, .25f, .5f, Float.NaN)
        )) {
            assertNull(invalid.bitmapBounds(viewport, 90, 800, 600, 400, 300))
        }
    }

    @Test fun fullCameraImageBypassesViewportCropAtEveryRotation() {
        for (rotation in listOf(0, 90, 180, 270)) {
            assertEquals(
                CropBounds(0, 0, 400, 300),
                LiveScanRegion.FULL_IMAGE.bitmapBounds(viewport, rotation, 800, 600, 400, 300)
            )
        }
        assertNull(LiveScanRegion.FULL_IMAGE.bitmapBounds(viewport, 45, 800, 600, 400, 300))
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
