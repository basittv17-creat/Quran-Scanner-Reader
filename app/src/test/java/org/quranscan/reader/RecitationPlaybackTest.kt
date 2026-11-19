package org.quranscan.reader

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.quranscan.reader.audio.PlaybackRange
import org.quranscan.reader.audio.RecitationMode
import org.quranscan.reader.audio.RecitationPlaybackGate
import org.quranscan.reader.audio.WordTimingFormat

class RecitationPlaybackTest {
    @Test fun foregroundAndRecreatedScreensRequireExplicitConfirmation() {
        val gate = RecitationPlaybackGate()
        gate.confirm()
        gate.enterForeground()
        assertNull(gate.beginRequest())
        gate.confirm()
        val pending = checkNotNull(gate.beginRequest())
        gate.leaveForeground()
        gate.enterForeground()
        assertFalse(gate.accepts(pending))
        assertNull(gate.beginRequest())
        assertFalse(RecitationPlaybackGate().apply { enterForeground() }.canPlay)
    }

    @Test fun reConfirmingAfterInterruptionCannotRevivePendingVerification() {
        val gate = RecitationPlaybackGate()
        gate.enterForeground()
        gate.confirm()
        val interrupted = checkNotNull(gate.beginRequest())
        gate.revokeConfirmation()
        assertNull(gate.beginRequest())
        gate.confirm()
        assertFalse(gate.accepts(interrupted))
        val current = checkNotNull(gate.beginRequest())
        assertTrue(gate.accepts(current))
    }

    @Test fun pauseAndReplayInvalidateOlderRequestsInSameConfirmedSession() {
        val gate = RecitationPlaybackGate()
        gate.enterForeground()
        gate.confirm()
        val paused = checkNotNull(gate.beginRequest())
        gate.cancelRequest()
        assertFalse(gate.accepts(paused))
        assertTrue(gate.canPlay)
        val firstPlay = checkNotNull(gate.beginRequest())
        val replay = checkNotNull(gate.beginRequest())
        assertFalse(gate.accepts(firstPlay))
        assertTrue(gate.accepts(replay))
    }

    @Test fun persistedModeRoundTripsAndUnknownValuesDefaultToFullAyah() {
        RecitationMode.entries.forEach { assertEquals(it, RecitationMode.restore(it.name)) }
        assertEquals(RecitationMode.FULL_AYAH, RecitationMode.restore(null))
        assertEquals(RecitationMode.FULL_AYAH, RecitationMode.restore("unknown"))
    }

    @Test fun selectedWordRequiresEntireTimelineToFitRecording() {
        PlaybackRange(100, 150, 400).validateDuration(400)
        assertTrue(runCatching { PlaybackRange(100, 150, 400).validateDuration(399) }.isFailure)
        assertTrue(runCatching { PlaybackRange(100, 401, 0).validateDuration(400) }.isFailure)
        assertTrue(runCatching { PlaybackRange(100, 100, 0) }.isFailure)
        assertTrue(runCatching { PlaybackRange(0, null, 0).validateDuration(0) }.isFailure)
    }

    @Test fun selectedWordCutoffIncludesExactEndAndSeekOvershoot() {
        val segment = PlaybackRange(100, 150, 150)
        segment.validateDuration(150)
        assertTrue(segment.isSegment)
        assertFalse(segment.hasReachedEnd(100))
        assertFalse(segment.hasReachedEnd(149))
        assertTrue(segment.hasReachedEnd(150))
        assertTrue(segment.hasReachedEnd(151))
        val full = PlaybackRange(0, null, 150)
        assertFalse(full.isSegment)
        assertFalse(full.hasReachedEnd(150))
    }

    @Test fun rejectsStartAtDurationAndOversizedOrNegativeTimingWindows() {
        assertTrue(runCatching { PlaybackRange(150, null, 0).validateDuration(150) }.isFailure)
        assertTrue(runCatching { PlaybackRange(-1, 150, 0) }.isFailure)
        assertTrue(runCatching { PlaybackRange(0, null, -1) }.isFailure)
        assertTrue(runCatching { WordTimingFormat.parseTimeline("1\t0\t900001", 1) }.isFailure)
        assertTrue(runCatching { WordTimingFormat.parseTimeline("1\t0\t900000", 1) }.isSuccess)
    }

    @Test fun highlightsRespectGapsAndApproximateSeekIntoAnotherWord() {
        val words = WordTimingFormat.parseTimeline("1\t10\t20\n2\t30\t50", 2)
        assertEquals(-1, WordTimingFormat.highlightIndex(words, null, null))
        assertEquals(0, WordTimingFormat.highlightIndex(words, 10, null))
        assertEquals(-1, WordTimingFormat.highlightIndex(words, 20, null))
        assertEquals(1, WordTimingFormat.highlightIndex(words, 30, null))
        assertEquals(-1, WordTimingFormat.highlightIndex(words, 15, 1))
        assertEquals(1, WordTimingFormat.highlightIndex(words, 30, 1))
        assertEquals(-1, WordTimingFormat.highlightIndex(words, 50, 1))
        assertTrue(runCatching { WordTimingFormat.parseTimeline("1\t10\t20\n2\t19\t50", 2) }.isFailure)
    }
}
