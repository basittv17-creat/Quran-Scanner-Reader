package org.quranscan.reader.core

import java.text.Normalizer

/** A text candidate must match exactly ONE approved corpus entry after OCR normalization.
 *  This is NOT an OCR engine. Never returns a verified result for an ambiguous candidate.
 */
class ConservativeVerseMatcher(private val corpus: Map<VerseId, String>) {
    private fun key(s: String): String = Normalizer.normalize(s, Normalizer.Form.NFKC)
        .replace(Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]"), "")
        .replace(Regex("[\\s\\p{Punct}\\u060C\\u061B\\u061F\\u06DD]+"), "")
        .trim()

    fun match(ocrText: String): RecognitionResult {
        val candidate = key(ocrText)
        if (candidate.length < 12) return RecognitionResult.Uncertain("Insufficient OCR text")
        val matches = corpus.filterValues { key(it) == candidate }
        if (matches.size != 1) return RecognitionResult.Uncertain("No unique verified verse match")
        val (id, arabic) = matches.entries.single()
        return RecognitionResult.VerifiedCandidate(VerseMatch(id, 1f, arabic), true)
    }
}
