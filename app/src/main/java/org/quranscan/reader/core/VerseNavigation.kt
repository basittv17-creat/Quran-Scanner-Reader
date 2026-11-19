package org.quranscan.reader.core

/** Canonical adjacent references; ends of the Quran do not wrap automatically. */
object VerseNavigation {
    fun adjacent(id: VerseId, direction: Int): VerseId? {
        require(direction == -1 || direction == 1)
        return if (direction > 0) {
            when {
                id.ayah < QuranVerseCounts.counts[id.surah - 1] -> VerseId(id.surah, id.ayah + 1)
                id.surah < 114 -> VerseId(id.surah + 1, 1)
                else -> null
            }
        } else {
            when {
                id.ayah > 1 -> VerseId(id.surah, id.ayah - 1)
                id.surah > 1 -> VerseId(id.surah - 1, QuranVerseCounts.counts[id.surah - 2])
                else -> null
            }
        }
    }
}
