package org.quranscan.reader.core

import java.text.Normalizer

/** Finds contiguous Quran word matches for review while preserving canonical text and positions. */
object VerseCandidates {
    data class Suggestion(
        val id: VerseId,
        val canonicalArabic: String,
        val tokenCoverage: Float,
        val firstWord: Int = -1,
        val wordCount: Int = 0
    )

    private val marks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val nonArabic = Regex("[^\\p{IsArabic} \\t]")
    private val whitespace = Regex("\\s+")
    private val arabicWord = Regex("[\\p{IsArabic}&&\\p{L}]+")

    private fun normalize(text: String): String = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .replace(marks, "")
        .replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ی', 'ي').replace('ک', 'ك')

    fun normalizedWords(text: String): List<String> = arabicWord.findAll(normalize(text))
        .map { it.value }.filter { it.length >= 2 }.toList()

    fun suggest(rawOcr: String, corpus: Map<VerseId, String>, limit: Int = 3): List<Suggestion> {
        if (rawOcr.length !in 1..20001 || corpus.size != 6236) return emptyList()
        // Keep boundaries between scripts, punctuation and lines so separate snippets cannot join.
        val runs = normalize(rawOcr).replace(nonArabic, "|").split('|').asSequence()
            .map { piece -> whitespace.split(piece.trim()).filter { arabicWord.matches(it) } }
            .filter { it.isNotEmpty() }
            .take(128)
            .toList()
        val positions = mutableMapOf<String, MutableList<Pair<List<String>, Int>>>()
        for (run in runs) {
            for (i in 0 until minOf(run.size, 512)) {
                val hits = positions.getOrPut(run[i]) { mutableListOf() }
                if (hits.size < 32) hits.add(run to i)
            }
        }
        if (positions.isEmpty()) return emptyList()

        val maximum = limit.coerceIn(1, 5)
        val results = mutableListOf<Suggestion>()
        var longest = 0
        var occurrences = 0
        for ((id, arabic) in corpus) {
            // This is the same whitespace boundary used by word-timing files.
            val canonicalWords = whitespace.split(arabic).map { word ->
                val normalized = normalize(word)
                arabicWord.findAll(normalized).joinToString("") { it.value }
            }
            for ((start, word) in canonicalWords.withIndex()) {
                var best = 0
                for ((run, index) in positions[word].orEmpty()) {
                    var count = 1
                    while (start + count < canonicalWords.size && index + count < run.size &&
                        canonicalWords[start + count] == run[index + count]) count++
                    best = maxOf(best, count)
                }
                if (best == 0 || best < longest || (best == 1 && word.length < 4)) continue
                if (best > longest) {
                    longest = best
                    results.clear()
                    occurrences = 0
                }
                occurrences++
                if (results.size < maximum) {
                    results.add(Suggestion(id, arabic, best.toFloat() / canonicalWords.size, start, best))
                }
            }
        }
        // A common isolated word has too many possible locations to present as a useful result.
        return if (longest == 1 && occurrences > maximum) emptyList() else results
    }

    /** Recover a review-only full-verse candidate from a short noisy OCR line. */
    fun suggestNoisyLine(lines: List<String>, corpus: Map<VerseId, String>): Suggestion? {
        if (corpus.size != 6236) return null
        val readings = lines.asSequence().flatMap { it.lineSequence() }
            .map { normalizedWords(it) }.filter { it.size in 3..9 }
            .map { it.joinToString("") }.filter { it.length in 16..36 }.distinct().take(8).toList()
        if (readings.isEmpty()) return null
        val keys = corpus.mapNotNull { (id, arabic) ->
            val key = normalizedWords(arabic).joinToString("")
            if (key.length in 11..41) Suggestion(id, arabic, 0f) to key else null
        }
        var best: Suggestion? = null
        for (reading in readings) {
            // Keep a two-edit lead over any other verse. A near match is never eligible for autoplay.
            val maxEdits = minOf(4, reading.length / 5)
            var closest = maxEdits + 2
            var runnerUp = maxEdits + 2
            var candidate: Suggestion? = null
            for ((possible, key) in keys) {
                if (kotlin.math.abs(key.length - reading.length) > closest + 1) continue
                val edits = editDistance(reading, key)
                if (edits < closest) {
                    runnerUp = closest
                    closest = edits
                    candidate = possible
                } else if (edits < runnerUp) {
                    runnerUp = edits
                }
            }
            if (candidate != null && closest <= maxEdits && runnerUp - closest >= 2) {
                if (best != null && best.id != candidate.id) return null
                best = candidate
            }
        }
        return best
    }

    private fun editDistance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for ((i, letter) in a.withIndex()) {
            current[0] = i + 1
            for (j in b.indices) {
                val substitution = previous[j] + if (letter == b[j]) 0 else 1
                current[j + 1] = minOf(current[j] + 1, previous[j + 1] + 1, substitution)
            }
            val completed = current
            current = previous
            previous = completed
        }
        return previous.last()
    }
}
