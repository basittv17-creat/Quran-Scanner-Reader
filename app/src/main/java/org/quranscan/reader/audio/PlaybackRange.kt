package org.quranscan.reader.audio

/** Segment endpoints and the complete timeline must fit the decoded recording. */
internal class PlaybackRange(
    private val startMs: Int,
    private val endMs: Int?,
    private val requiredDurationMs: Int
) {
    init {
        require(requiredDurationMs >= 0) { "Invalid timing duration" }
        require(startMs >= 0 && (endMs == null || endMs > startMs)) { "Invalid audio segment" }
    }

    val isSegment: Boolean get() = endMs != null

    fun hasReachedEnd(positionMs: Int): Boolean = endMs != null && positionMs >= endMs

    fun validateDuration(durationMs: Int) {
        require(requiredDurationMs <= durationMs) { "Word timeline extends beyond this recording" }
        require(startMs < durationMs && (endMs == null || endMs <= durationMs)) {
            "Word timing is outside audio duration"
        }
    }
}
