package org.quranscan.reader.core

import java.text.Normalizer

/**
 * Indexless, conservative Quran verse suggestion logic for a phase-5 prototype.
 * Raw OCR can be wrong: these suggestions are NEVER verification, endorsements,
 * or a license to automatically read aloud. Candidate Arabic must come only
 * from the trusted, nonempty canonical corpus, NOT from this matcher.
 */
object VerseCandidates {
    data class Suggestion(val id: VerseId, val canonicalArabic: String, val tokenCoverage: Float)

    private val marks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val punctuation = Regex("[^\\p{IsArabic} ]")

    /** Normalization is used ONLY to compare OCR with unchanged Quran reference text. */
    fun normalizedWords(text: String): List<String> = Normalizer.normalize(text, Normalizer.Form.NFKC)
        .replace(marks, "")
        .replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
        .replace('ى', 'ي').replace('ی', 'ي').replace('ک', 'ك')
        .replace(punctuation, " ")
        .trim().split(Regex("\\s+"))
        .filter { it.length >= 2 }

    /**
     * Only show suggestions where the OCR contains a run of consecutive words
     * from ONE verse. Require >= 4 words and >= 75% of the verse, at most 3
     * possibilities, and always require user confirmation. Short verses excluded
     * rather than guessed. This is deliberately conservative, not an accuracy guarantee.
     */
    fun suggest(rawOcr: String, corpus: Map<VerseId, String>, limit: Int = 3): List<Suggestion> {
        if (rawOcr.length !in 10..20000 || corpus.size != 6236) return emptyList()
        val words = normalizedWords(rawOcr)
        if (words.size < 4) return emptyList()
        val windows = words.windowed(4).map { it.joinToString("|") }.toSet()
        return corpus.mapNotNull { (id, arabic) ->
            val canon = normalizedWords(arabic)
            if (canon.size < 4) return@mapNotNull null
            val cwindows = canon.windowed(4).map { it.joinToString("|") }
            if (cwindows.none { it in windows }) return@mapNotNull null
            // Measure how many consecutive canonical words appear in OCR.
            val maxRun = longestContiguousRun(words, canon)
            val coverage = maxRun.toFloat() / canon.size
            if (maxRun >= 4 && coverage >= 0.75f) Suggestion(id, arabic, coverage) else null
        }.sortedWith(compareByDescending<Suggestion> { it.tokenCoverage }.thenByDescending { it.canonicalArabic.length })
            .take(limit.coerceIn(1, 5))
    }

    private fun longestContiguousRun(a: List<String>, b: List<String>): Int {
        var longest = 0
        var previous = IntArray(b.size + 1)
        for (word in a) {
            val current = IntArray(b.size + 1)
            for (j in b.indices) if (word == b[j]) {
                current[j+1] = previous[j] + 1
                if (current[j+1] > longest) longest = current[j+1]
            }
            previous = current
        }
        return longest
    }
}
