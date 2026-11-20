package org.quranscan.reader

import android.content.Context
import org.quranscan.reader.core.VerseCandidates

internal fun Context.candidateReference(candidate: VerseCandidates.Suggestion): String {
    val id = candidate.id
    return when {
        candidate.wordCount == 1 -> getString(R.string.scan_word_reference, id.surah, id.ayah, candidate.firstWord + 1)
        candidate.wordCount > 1 -> getString(R.string.scan_words_reference, id.surah, id.ayah,
            candidate.firstWord + 1, candidate.firstWord + candidate.wordCount)
        else -> getString(R.string.canonical_ayah_reference, id.surah, id.ayah)
    }
}
