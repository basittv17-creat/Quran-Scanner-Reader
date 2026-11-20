package org.quranscan.reader.core

import java.text.Normalizer

/** A text candidate must match exactly ONE approved corpus entry after OCR normalization.
 *  This is NOT an OCR engine. Never returns a verified result for an ambiguous candidate.
 */
class ConservativeVerseMatcher(private val corpus: Map<VerseId, String>) {
    private val marks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val separators = Regex("[^\\p{L}]+")
    private fun key(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKC)
        .replace(marks, "")
        .replace('ٱ', 'ا')
        .replace(separators, " ")
        .trim()

    fun match(ocrText: String): RecognitionResult {
        if (corpus.size != QuranVerseCounts.totalVerses) return RecognitionResult.Uncertain("Incomplete reference corpus")
        val candidate = key(ocrText)
        if (candidate.count { it.isLetter() } < 12 || candidate.count { it == ' ' } < 2) return RecognitionResult.Uncertain("Insufficient OCR text")
        var match: VerseMatch? = null
        for ((id, arabic) in corpus) {
            if (key(arabic) != candidate) continue
            if (match != null) return RecognitionResult.Uncertain("Ambiguous verse match")
            match = VerseMatch(id, 1f, arabic)
        }
        return match?.let { RecognitionResult.VerifiedCandidate(it, true) }
            ?: RecognitionResult.Uncertain("No unique verified verse match")
    }
}
