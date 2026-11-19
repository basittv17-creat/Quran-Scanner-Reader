package org.quranscan.reader.audio

import org.quranscan.reader.core.QuranVerseCounts
import java.security.MessageDigest
import java.util.Locale

/** QSR-WORD-TIMING-1 is a user-supplied *unverified* timing format.
 * Each row is bound cryptographically to the exact installed audio file and unmodified verse text.
 * A hash proves equality to input bytes, never correct tajweed/alignment or permission to publish.
 */
data class TimedWord(val index: Int, val startMs: Int, val endMs: Int)
data class TimedVerseRecord(
    val key: AudioVerseKey, val filename: String, val sha256: String,
    val audioSha256: String, val canonicalTextSha256: String, val wordCount: Int
)
data class WordTimingManifest(
    val reciter: String, val source: String, val rights: String,
    val entries: Map<AudioVerseKey, TimedVerseRecord>
)

object WordTimingFormat {
    private val shaRegex = Regex("[a-fA-F0-9]{64}")
    private val filenameRegex = Regex("[0-9]{3}_[0-9]{3}\\.tsv")
    fun fileName(key: AudioVerseKey): String = "%03d_%03d.tsv".format(Locale.US, key.surah, key.ayah)
    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256")
        .digest(bytes).joinToString("") { "%02x".format(it) }
    fun verseDigest(canonicalArabic: String): String = sha256(canonicalArabic.toByteArray(Charsets.UTF_8))

    /** Never reformat, strip harakat, or substitute OCR for the canonical display text. */
    fun words(canonicalArabic: String): List<String> = canonicalArabic
        .split(Regex("\\s+")).filter { it.isNotEmpty() }

    fun parseManifest(text: String): WordTimingManifest {
        val lines = text.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotEmpty() }.toList()
        require(lines.firstOrNull() == "# QSR-WORD-TIMING-1") { "Unsupported word timing manifest" }
        val headers = linkedMapOf<String, String>()
        val records = linkedMapOf<AudioVerseKey, TimedVerseRecord>()
        var inRecords = false
        for (line in lines.drop(1)) {
            if (line.startsWith("# ")) {
                require(!inRecords) { "Headers must precede records" }
                val parts = line.removePrefix("# ").split("=", limit = 2)
                require(parts.size == 2 && parts[0] in setOf("reciter", "source", "rights") &&
                    parts[0] !in headers) { "Invalid timing pack header" }
                val value = parts[1].trim()
                require(value.isNotEmpty() && value.length <= 240 && value.none { it.isISOControl() })
                headers[parts[0]] = value
            } else {
                inRecords = true
                val p = line.split('\t')
                require(p.size == 7) { "Expected 7 timing manifest columns" }
                val s = p[0].toIntOrNull() ?: error("Invalid surah")
                val a = p[1].toIntOrNull() ?: error("Invalid ayah")
                require(QuranVerseCounts.valid(s, a)) { "Invalid Quran verse key" }
                val key = AudioVerseKey(s, a)
                require(filenameRegex.matches(p[2]) && p[2] == fileName(key)) { "Invalid timing file name" }
                require(p[3].matches(shaRegex) && p[4].matches(shaRegex) && p[5].matches(shaRegex)) {
                    "Invalid timing/audio/text digest"
                }
                val count = p[6].toIntOrNull() ?: error("Invalid timing word count")
                require(count in 1..800) { "Invalid word count" }
                val record = TimedVerseRecord(key, p[2], p[3].lowercase(Locale.US),
                    p[4].lowercase(Locale.US), p[5].lowercase(Locale.US), count)
                require(records.put(key, record) == null) { "Duplicate timing verse" }
                require(records.size <= QuranVerseCounts.totalVerses)
            }
        }
        require(headers.keys == setOf("reciter", "source", "rights")) { "Timing attribution missing" }
        require(records.isNotEmpty()) { "Timing pack has no verses" }
        return WordTimingManifest(headers.getValue("reciter"), headers.getValue("source"),
            headers.getValue("rights"), records)
    }

    fun parseTimeline(text: String, expectedWords: Int): List<TimedWord> {
        require(expectedWords in 1..800) { "Invalid number of Quran words" }
        val lines = text.lineSequence().map { it.trimEnd('\r') }.filter { it.isNotBlank() }.toList()
        require(lines.size == expectedWords) { "Timeline does not match canonical word count" }
        var previousEnd = -1
        return lines.mapIndexed { index, line ->
            val p = line.split('\t')
            require(p.size == 3) { "Expected index/start/end" }
            val n = p[0].toIntOrNull() ?: error("Invalid word index")
            val from = p[1].toIntOrNull() ?: error("Invalid start time")
            val to = p[2].toIntOrNull() ?: error("Invalid end time")
            require(n == index + 1) { "Word positions are not consecutive" }
            require(from >= 0 && to > from && to <= 900_000) { "Invalid word window" }
            require(from >= previousEnd) { "Overlapping or unsorted word timings" }
            previousEnd = to
            TimedWord(n, from, to)
        }
    }

    /** -1 in silence, -1 for invalid or absent timing. Boundaries: [start,end). */
    fun activeIndex(words: List<TimedWord>, positionMs: Int): Int {
        if (positionMs < 0) return -1
        var lo = 0
        var hi = words.lastIndex
        while (lo <= hi) {
            val mid = (lo + hi).ushr(1)
            val word = words[mid]
            when {
                positionMs < word.startMs -> hi = mid - 1
                positionMs >= word.endMs -> lo = mid + 1
                else -> return mid
            }
        }
        return -1
    }
}
