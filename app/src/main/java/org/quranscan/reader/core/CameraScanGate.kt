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
