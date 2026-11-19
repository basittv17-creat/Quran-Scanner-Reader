package org.quranscan.reader

import org.junit.Assert.*
import org.junit.Test
import org.quranscan.reader.core.*
import org.quranscan.reader.audio.*

/** Synthetic fixtures only: these tests make no Quran/OCR accuracy claims. */
class CoreSafetyTest {
    @Test fun decoderSamplingBoundsLargeImagesAndRejectsInvalidDimensions() {
        for ((w, h) in listOf(1500 to 1125, 1501 to 1200, 3001 to 1,
            48000 to 36000, Int.MAX_VALUE to Int.MAX_VALUE)) {
            val sample = ImageDecodeLimits.sampleSize(w, h, 1500)
            assertTrue(sample > 0 && sample and (sample - 1) == 0)
            assertTrue((maxOf(w, h).toLong() + sample - 1) / sample <= 1500)
            if (sample > 1) assertTrue((maxOf(w, h).toLong() + sample / 2 - 1) / (sample / 2) > 1500)
        }
        for ((w, h) in listOf(0 to 10, -1 to 100, 20 to 0)) {
            assertTrue(runCatching { ImageDecodeLimits.sampleSize(w, h, 1500) }.isFailure)
        }
    }

    @Test fun samplingChangesAtExactLimitsAndRecoversAfterInvalidBounds() {
        assertNull(GalleryImageState.sampleSize(-1, -1))
        assertFalse(GalleryImageState.acceptsPreview(2049, 1))
        assertTrue(GalleryImageState.acceptsPreview(2048, 2048))
        assertEquals(1, ImageDecodeLimits.sampleSize(1500, 1, 1500))
        assertEquals(2, ImageDecodeLimits.sampleSize(1501, 1, 1500))
        assertEquals(2, ImageDecodeLimits.sampleSize(3000, 1, 1500))
        assertEquals(4, ImageDecodeLimits.sampleSize(3001, 1, 1500))
        assertEquals(1 shl 23, ImageDecodeLimits.sampleSize(1, Int.MAX_VALUE, 256))
    }

    @Test fun cropDragClampsEdgesSupportsReverseAndRejectsTinyOrNonFiniteAreas() {
        assertEquals(CropBounds(0, 0, 100, 80), CropBounds.fromDrag(100, 80, 110f, 90f, -10f, -5f))
        assertEquals(CropBounds(10, 20, 41, 61), CropBounds.fromDrag(100, 80, 40.2f, 60.1f, 10.5f, 20.8f))
        assertNull(CropBounds.fromDrag(100, 80, 0f, 0f, 15f, 40f))
        assertNull(CropBounds.fromDrag(100, 80, Float.NaN, 0f, 40f, 40f))
        assertNull(CropBounds.fromDrag(100, 80, 0f, 0f, Float.POSITIVE_INFINITY, 40f))
    }

    @Test fun adjacentReferencesCrossSurahsAndStopAtQuranBoundaries() {
        assertNull(VerseNavigation.adjacent(VerseId(1, 1), -1))
        assertNull(VerseNavigation.adjacent(VerseId(114, 6), 1))
        assertEquals(VerseId(2, 1), VerseNavigation.adjacent(VerseId(1, 7), 1))
        assertEquals(VerseId(1, 7), VerseNavigation.adjacent(VerseId(2, 1), -1))
        for (s in 1..114) for (a in 1..QuranVerseCounts.counts[s - 1]) {
            val id = VerseId(s, a)
            VerseNavigation.adjacent(id, 1)?.let { assertEquals(id, VerseNavigation.adjacent(it, -1)) }
        }
        assertTrue(runCatching { VerseNavigation.adjacent(VerseId(1, 1), 0) }.isFailure)
    }
    @Test fun rejectsOutOfRangeReferences() {
        for ((s, a) in listOf(0 to 1, 115 to 1, 1 to 8, 114 to 7, 2 to 287)) {
            assertTrue(runCatching { VerseId(s, a) }.isFailure)
        }
        assertEquals(6236, QuranVerseCounts.counts.sum())
        assertEquals(114, QuranVerseCounts.counts.size)
    }
    @Test fun incompleteCorpusCannotVerifyExactOcr() {
        val r = ConservativeVerseMatcher(mapOf(VerseId(1, 1) to "مثال اصطناعي لا يمثل نص القرآن")).match("مثال اصطناعي لا يمثل نص القرآن")
        assertTrue(r is RecognitionResult.Uncertain)
    }
    @Test fun repeatedTextRequiresReviewOfEveryCandidate() {
        val corpus = linkedMapOf<VerseId, String>()
        QuranVerseCounts.counts.forEachIndexed { s, n ->
            (1..n).forEach { a -> corpus[VerseId(s + 1, a)] = "اختبار بيانات تجريبية فقط" }
        }
        assertTrue(ConservativeVerseMatcher(corpus).match("اختبار بيانات تجريبية فقط") is RecognitionResult.Uncertain)
        assertEquals(3, VerseCandidates.suggest("اختبار بيانات تجريبية فقط", corpus).size)
        assertTrue(VerseCandidates.suggest("اختبار", corpus).isEmpty())
        assertTrue(VerseCandidates.suggest("اختبار بيانات تجريبية فقط", corpus - VerseId(1,1)).isEmpty())
    }
    @Test fun scanGateHandlesManualScanAndClockReset() {
        val gate = CameraScanGate(100)
        assertTrue(gate.shouldScan(1000))
        assertFalse(gate.shouldScan(1099))
        assertTrue(gate.shouldScan(1099, manual = true))
        assertFalse(gate.shouldScan(1100))
        assertTrue(gate.shouldScan(1))
        gate.reset()
        assertTrue(gate.shouldScan(1))
    }
    @Test fun wordTimelineGapsAndBoundariesDoNotHighlightOtherWords() {
        val words = WordTimingFormat.parseTimeline("1\t0\t100\n2\t150\t300", 2)
        assertEquals(0, WordTimingFormat.activeIndex(words, 99))
        assertEquals(-1, WordTimingFormat.activeIndex(words, 100))
        assertEquals(-1, WordTimingFormat.activeIndex(words, 149))
        assertEquals(1, WordTimingFormat.activeIndex(words, 150))
        assertEquals(-1, WordTimingFormat.activeIndex(words, 300))
        assertTrue(runCatching { WordTimingFormat.parseTimeline("1\t0\t200\n2\t150\t300", 2) }.isFailure)
    }
    @Test fun timingDigestBindsExactDiacritics() {
        assertNotEquals(WordTimingFormat.verseDigest("كَلِمَةٌ"), WordTimingFormat.verseDigest("كلمة"))
    }
    @Test fun audioManifestRejectsTraversalDuplicateAndBadDigest() {
        val sha = "ab".repeat(32)
        val fixture = "# QSR-AUDIO-1\n# reciter=Test\n# source=Synthetic\n# rights=Tests only\n1\t1\t001_001.mp3\t$sha\n"
        assertEquals(1, AudioPackFormat.parse(fixture).entries.size)
        for (bad in listOf(fixture.replace("001_001.mp3", "../001_001.mp3"), fixture.replace(sha,"invalid"), fixture + "1\t1\t001_001.mp3\t$sha\n")) {
            assertTrue(runCatching { AudioPackFormat.parse(bad) }.isFailure)
        }
    }
}
