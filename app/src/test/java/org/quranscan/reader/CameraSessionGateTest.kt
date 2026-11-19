package org.quranscan.reader

import org.junit.Assert.*
import org.junit.Test
import org.quranscan.reader.core.CameraSessionGate

class CameraSessionGateTest {
    @Test fun pendingManualFrameCoalescesTapsAndBypassesPauseAndThrottle() {
        val gate = CameraSessionGate(100)
        val session = gate.open()
        gate.finish(checkNotNull(gate.claimFrame(session, 0, paused = false)))
        assertTrue(gate.requestNextFrame(session))
        assertFalse(gate.requestNextFrame(session))
        assertNull(gate.claimStill(session, 1))
        val manual = checkNotNull(gate.claimFrame(session, 1, paused = true))
        assertNull(gate.claimFrame(session, 2, paused = false))
        gate.finish(manual)
        assertNull(gate.claimFrame(session, 100, paused = false))
        assertNotNull(gate.claimFrame(session, 101, paused = false))
    }

    @Test fun stillClaimExcludesAnalyzerAndRepeatedTaps() {
        val gate = CameraSessionGate()
        val session = gate.open()
        val still = checkNotNull(gate.claimStill(session, 0))
        assertNull(gate.claimStill(session, 1))
        assertNull(gate.claimFrame(session, 5_000, paused = false))
        assertFalse(gate.requestNextFrame(session))
        gate.finish(still)
        assertNotNull(gate.claimFrame(session, 5_000, paused = false))
    }

    @Test fun pauseInvalidatesOpeningAndLateCompletionCannotReleaseNewWork() {
        val gate = CameraSessionGate()
        val old = gate.open()
        val staleCapture = checkNotNull(gate.claimStill(old, 0))
        gate.invalidate()
        assertFalse(gate.isCurrent(old))
        assertNull(gate.claimFrame(old, 1, paused = false))
        val reopened = gate.open()
        assertTrue(gate.isCurrent(reopened))
        val currentCapture = checkNotNull(gate.claimStill(reopened, 1))
        gate.finish(staleCapture)
        assertNull(gate.claimStill(reopened, 2))
        gate.finish(currentCapture)
        assertNotNull(gate.claimStill(reopened, 3))
    }

    @Test fun pendingManualFrameDoesNotSurviveRecreationOfBinding() {
        val gate = CameraSessionGate()
        val old = gate.open()
        assertTrue(gate.requestNextFrame(old))
        val reopened = gate.open()
        assertFalse(gate.requestNextFrame(old))
        assertNull(gate.claimFrame(reopened, 0, paused = true))
        assertNotNull(gate.claimFrame(reopened, 0, paused = false))
    }
}
