package org.quranscan.reader

import org.junit.Assert.*
import org.junit.Test
import org.quranscan.reader.core.QuranPageVerseMatcher
import org.quranscan.reader.core.QuranVerseCounts
import org.quranscan.reader.core.VerseId

/** Synthetic fixtures only; not a real-page Quran OCR validation. */
class QuranPageVerseMatcherTest {
    private val first = VerseId(1, 5)
    private val second = VerseId(1, 6)
    private val markedFirst = "أَبْجَدَ بِسِيطَ مِثَالَ كَلِمَةً مُمْتَازَةً رَائِعَةً"
    private val markedSecond = "مَعْلُومَاتٌ جَدِيدَةٌ طَوِيلَةٌ كَثِيرَةٌ مَقْبُولَةٌ أَخِيرَةٌ"
    private fun corpus(): MutableMap<VerseId, String> = linkedMapOf<VerseId, String>().apply {
        QuranVerseCounts.counts.forEachIndexed { surah, count ->
            for (ayah in 1..count) put(VerseId(surah + 1, ayah), "نص اصطناعي للاختبار فقط")
        }
        put(first, markedFirst)
        put(second, markedSecond)
    }

    @Test fun embeddedFullVerseRetainsCanonicalMarksAndCanAutoplay() {
        val result = QuranPageVerseMatcher.find("عنوان ابجد بسيط مثال كلمة ممتازة رائعة آخر الصفحة", corpus())
        assertEquals(listOf(first), result.matches.map { it.id })
        assertEquals(markedFirst, result.matches.single().canonicalArabic)
        assertEquals(first, result.autoPlayable?.id)
    }

    @Test fun multiVersePageKeepsReadingOrderButDoesNotAutoplay() {
        val result = QuranPageVerseMatcher.find(
            "معلومات جديدة طويلة كثيرة مقبولة أخيرة ابجد بسيط مثال كلمة ممتازة رائعة", corpus())
        assertEquals(listOf(second, first), result.matches.map { it.id })
        assertNull(result.autoPlayable)
    }

    @Test fun incompleteCorpusAndPartialVerseFailClosed() {
        val corpus = corpus()
        assertTrue(QuranPageVerseMatcher.find("ابجد بسيط مثال كلمة ممتازة", corpus).matches.isEmpty())
        assertTrue(QuranPageVerseMatcher.find("ابجد بسيط مثال كلمة ممتازة رائعة", corpus - first).matches.isEmpty())
    }

    @Test fun duplicateCanonicalVerseAlwaysNeedsSelection() {
        val corpus = corpus()
        corpus[VerseId(1, 7)] = markedFirst
        val result = QuranPageVerseMatcher.find("ابجد بسيط مثال كلمة ممتازة رائعة", corpus)
        assertEquals(2, result.matches.size)
        assertNull(result.autoPlayable)
    }

    @Test fun mixedScriptInterruptionCannotBecomeExact() {
        val result = QuranPageVerseMatcher.find("ابجد بسيط English مثال كلمة ممتازة رائعة", corpus())
        assertNull(result.autoPlayable)
        assertTrue(result.matches.isEmpty())
    }
}
