package org.quranscan.reader.core

import java.text.Normalizer

/** Finds full canonical verses within a page, not just an isolated ayah.
 * Lexical matches do not prove print/OCR accuracy or certified Quran interpretation.
 */
object QuranPageVerseMatcher {
    data class Result(
        val matches: List<VerseCandidates.Suggestion>,
        val autoPlayable: VerseCandidates.Suggestion?
    )

    private data class IndexedVerse(val id: VerseId, val arabic: String, val words: List<String>)
    private data class CorpusIndex(val source: Map<VerseId, String>, val records: List<IndexedVerse>)
    private data class Hit(val at: Int, val verse: IndexedVerse)
    @Volatile private var cached: CorpusIndex? = null
    private val marks = Regex("[\\u064B-\\u065F\\u0670\\u06D6-\\u06ED\\u0640]")
    private val letters = Regex("\\p{L}+")

    private fun tokens(value: String): List<String> {
        val clean = Normalizer.normalize(value, Normalizer.Form.NFKC)
            .replace(marks, "")
            .replace('ٱ', 'ا').replace('أ', 'ا').replace('إ', 'ا').replace('آ', 'ا')
            .replace('ى', 'ي').replace('ی', 'ي').replace('ک', 'ك')
        return letters.findAll(clean).map { it.value }.take(1500).toList()
    }

    private fun indexFor(corpus: Map<VerseId, String>): List<IndexedVerse> {
        cached?.takeIf { it.source === corpus }?.let { return it.records }
        return synchronized(this) {
            cached?.takeIf { it.source === corpus }?.records ?: corpus.mapNotNull { (id, arabic) ->
                val parts = tokens(arabic)
                if (parts.size >= 5 && parts.sumOf { it.length } >= 25) IndexedVerse(id, arabic, parts)
                else null
            }.also { cached = CorpusIndex(corpus, it) }
        }
    }

    fun find(rawOcr: String, corpus: Map<VerseId, String>, limit: Int = 8): Result {
        val empty = Result(emptyList(), null)
        if (rawOcr.length !in 1..20000 || corpus.size != QuranVerseCounts.totalVerses) return empty
        val input = tokens(rawOcr)
        if (input.size < 5) return empty
        // Whole-token boundaries prevent accidental partial-word matches.
        val page = " " + input.joinToString(" ") + " "
        val hits = indexFor(corpus).mapNotNull { verse ->
            val needle = " " + verse.words.joinToString(" ") + " "
            val offset = page.indexOf(needle)
            if (offset < 0) null else Hit(offset, verse)
        }.sortedWith(compareBy<Hit> { it.at }.thenBy { it.verse.id.surah }.thenBy { it.verse.id.ayah })
        if (hits.isEmpty()) return empty
        val matches = hits.take(limit.coerceIn(1, 12)).map { hit ->
            VerseCandidates.Suggestion(hit.verse.id, hit.verse.arabic, 1f)
        }
        // Multiple or repeated canonical references are review-only. A single very
        // short match inside a long noisy page must never trigger a recitation.
        val single = hits.singleOrNull()
        val dominant = single != null && single.verse.words.size >= 6 &&
            single.verse.words.sumOf { it.length } >= 25 &&
            single.verse.words.size.toFloat() / input.size >= 0.60f
        return Result(matches, if (dominant) matches.singleOrNull() else null)
    }
}
