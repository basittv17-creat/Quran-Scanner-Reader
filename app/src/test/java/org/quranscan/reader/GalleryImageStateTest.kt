package org.quranscan.reader

import org.junit.Assert.*
import org.junit.Test
import org.quranscan.reader.core.CropBounds
import org.quranscan.reader.core.GalleryImageState

class GalleryImageStateTest {
    @Test fun rejectsInvalidSavedEdges() {
        assertNull(GalleryImageState.savedSelection(null))
        assertNull(GalleryImageState.savedSelection(intArrayOf(0, 1, 2)))
        assertNull(GalleryImageState.savedSelection(intArrayOf(-1, 0, 20, 20)))
        assertNull(GalleryImageState.savedSelection(intArrayOf(20, 0, 10, 20)))
        assertNull(GalleryImageState.savedSelection(intArrayOf(0, 10, 20, 10)))
        assertEquals(CropBounds(0, 4, 20, 24), GalleryImageState.savedSelection(intArrayOf(0, 4, 20, 24)))
    }

    @Test fun validatesSavedCropAgainstDecodedImage() {
        val full = CropBounds(0, 0, 80, 60)
        val atEdge = CropBounds(64, 44, 80, 60)
        assertEquals(atEdge, GalleryImageState.selectionFor(80, 60, atEdge))
        assertEquals(full, GalleryImageState.selectionFor(80, 60, atEdge.copy(right = 81)))
        assertEquals(full, GalleryImageState.selectionFor(80, 60, atEdge.copy(left = 65)))
        assertEquals(full, GalleryImageState.selectionFor(80, 60, atEdge.copy(left = Int.MIN_VALUE)))
        assertEquals(full, GalleryImageState.selectionFor(80, 60, atEdge.copy(bottom = Int.MIN_VALUE)))
        assertEquals(CropBounds(0, 0, 1, 10), GalleryImageState.selectionFor(1, 10, atEdge))
    }

    @Test fun boundsSampledAndActualDimensions() {
        assertNull(GalleryImageState.sampleSize(-1, 400))
        assertNull(GalleryImageState.sampleSize(400, 0))
        assertEquals(1, GalleryImageState.sampleSize(2048, 1))
        assertEquals(2, GalleryImageState.sampleSize(1, 2049))
        assertEquals(1 shl 20, GalleryImageState.sampleSize(Int.MAX_VALUE, 1))
        assertTrue(GalleryImageState.acceptsPreview(2048, 2048))
        assertFalse(GalleryImageState.acceptsPreview(2049, 1))
        assertFalse(GalleryImageState.acceptsPreview(1, 2049))
        assertFalse(GalleryImageState.acceptsPreview(0, 1))
    }
}
