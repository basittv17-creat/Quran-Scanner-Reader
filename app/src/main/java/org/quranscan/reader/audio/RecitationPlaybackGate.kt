package org.quranscan.reader.audio

internal enum class RecitationMode {
    FULL_AYAH, FOLLOW_WORDS, SELECTED_WORD;

    companion object {
        fun restore(stored: String?): RecitationMode = entries.firstOrNull { it.name == stored } ?: FULL_AYAH
    }
}

/** Main-thread state. A new activity starts without playback authorization. */
internal class RecitationPlaybackGate {
    private var foreground = false
    private var confirmed = false
    private var generation = 0L

    val canPlay: Boolean get() = foreground && confirmed

    fun enterForeground() { foreground = true }

    fun leaveForeground() {
        foreground = false
        revokeConfirmation()
    }

    fun confirm() { confirmed = foreground }

    fun revokeConfirmation() {
        confirmed = false
        cancelRequest()
    }

    fun cancelRequest() { generation++ }

    fun beginRequest(): Long? {
        if (!canPlay) return null
        return ++generation
    }

    fun accepts(request: Long): Boolean = canPlay && request == generation
}
