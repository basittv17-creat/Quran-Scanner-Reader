package org.quranscan.reader.core

/**
 * Single-threaded interval gate for a continuously streaming image analyzer.
 * nowMs must come from a monotonic clock (e.g. SystemClock.elapsedRealtime()).
 * This class has no Android dependencies so its behavior can be unit tested.
 */
class CameraScanGate(private val intervalMs: Long = 4_500L) {
    init { require(intervalMs > 0) }
    private var lastMs: Long? = null

    /** Manual requests bypass throttling but restart the automatic interval. */
    @Synchronized fun shouldScan(nowMs: Long, manual: Boolean = false): Boolean {
        val previous = lastMs
        if (manual || previous == null || nowMs < previous || nowMs - previous >= intervalMs) {
            lastMs = nowMs
            return true
        }
        return false
    }

    @Synchronized fun reset() { lastMs = null }
}

/** Owns pending manual requests and frame claims for one foreground camera binding. */
internal class CameraSessionGate(intervalMs: Long = 4_500L) {
    class Session
    class Scan(val session: Session)

    private val interval = CameraScanGate(intervalMs)
    private var session: Session? = null
    private var busy: Scan? = null
    private var nextFrameRequested = false

    @Synchronized fun open(): Session = Session().also {
        invalidate()
        session = it
    }

    @Synchronized fun invalidate() {
        session = null
        busy = null
        nextFrameRequested = false
        interval.reset()
    }

    @Synchronized fun isCurrent(candidate: Session): Boolean = session === candidate

    @Synchronized fun resetInterval() = interval.reset()

    @Synchronized fun requestNextFrame(candidate: Session): Boolean {
        if (session !== candidate || busy != null || nextFrameRequested) return false
        nextFrameRequested = true
        return true
    }

    @Synchronized fun claimStill(candidate: Session, nowMs: Long): Scan? {
        if (session !== candidate || busy != null || nextFrameRequested) return null
        interval.shouldScan(nowMs, manual = true)
        return Scan(candidate).also { busy = it }
    }

    @Synchronized fun claimFrame(candidate: Session, nowMs: Long, paused: Boolean): Scan? {
        if (session !== candidate || busy != null || (paused && !nextFrameRequested)) return null
        if (!interval.shouldScan(nowMs, manual = nextFrameRequested)) return null
        nextFrameRequested = false
        return Scan(candidate).also { busy = it }
    }

    @Synchronized fun finish(scan: Scan) {
        if (busy === scan) busy = null
    }
}
