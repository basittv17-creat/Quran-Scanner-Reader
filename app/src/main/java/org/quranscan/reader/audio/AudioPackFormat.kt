package org.quranscan.reader.audio

import org.quranscan.reader.core.QuranVerseCounts

/** Pure Kotlin: a strict format for user-provided, locally stored ayah MP3s.
 * This verifies structure and bytes. It does NOT certify the reciter, recording or license.
 */

data class AudioVerseKey(val surah: Int, val ayah: Int)
data class AudioPackEntry(val key: AudioVerseKey, val filename: String, val sha256: String)
data class AudioPackManifest(
    val reciter: String,
    val source: String,
    val rights: String,
    val entries: Map<AudioVerseKey, AudioPackEntry>
) {
    val isStructurallyComplete: Boolean get() = entries.size == QuranVerseCounts.totalVerses &&
        (1..114).all { s -> (1..QuranVerseCounts.counts[s - 1]).all { a ->
            entries.containsKey(AudioVerseKey(s, a))
        } }
}

object AudioPackFormat {
    private val shaRegex = Regex("[a-fA-F0-9]{64}")
    private val clipRegex = Regex("[0-9]{3}_[0-9]{3}\\.mp3")

    fun fileName(key: AudioVerseKey): String = "%03d_%03d.mp3".format(java.util.Locale.US, key.surah, key.ayah)

    fun parse(text: String): AudioPackManifest {
        val lines = text.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotBlank() }.toList()
        require(lines.firstOrNull() == "# QSR-AUDIO-1") { "Unsupported audio pack format" }
        val headers = linkedMapOf<String, String>()
        val clips = linkedMapOf<AudioVerseKey, AudioPackEntry>()
        var dataStarted = false
        for (line in lines.drop(1)) {
            if (line.startsWith("# ")) {
                require(!dataStarted) { "Pack headers must precede entries" }
                val pair = line.removePrefix("# ").split("=", limit = 2)
                require(pair.size == 2 && pair[0] in setOf("reciter", "source", "rights") &&
                    !headers.containsKey(pair[0])) { "Invalid or duplicate pack header" }
                val value = pair[1].trim()
                require(value.isNotEmpty() && value.length <= 240 && value.none { it.isISOControl() }) {
                    "Invalid pack header value"
                }
                headers[pair[0]] = value
            } else {
                dataStarted = true
                val parts = line.split('\t')
                require(parts.size == 4) { "Each entry must have 4 tab-separated columns" }
                val surah = parts[0].toIntOrNull() ?: error("Invalid surah number")
                val ayah = parts[1].toIntOrNull() ?: error("Invalid ayah number")
                require(QuranVerseCounts.valid(surah, ayah)) { "Out-of-range Quran reference" }
                val key = AudioVerseKey(surah, ayah)
                val path = parts[2]
                require(clipRegex.matches(path) && path == fileName(key)) { "Invalid audio file path" }
                val digest = parts[3]
                require(shaRegex.matches(digest)) { "Invalid SHA-256 checksum" }
                require(clips.put(key, AudioPackEntry(key, path, digest.lowercase(java.util.Locale.US))) == null) {
                    "Duplicate verse in pack"
                }
                require(clips.size <= QuranVerseCounts.totalVerses) { "Too many audio entries" }
            }
        }
        require(headers.keys == setOf("reciter", "source", "rights")) { "Missing pack attribution fields" }
        require(clips.isNotEmpty()) { "Audio pack is empty" }
        return AudioPackManifest(
            headers.getValue("reciter"), headers.getValue("source"),
            headers.getValue("rights"), clips
        )
    }
}
