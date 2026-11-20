package org.quranscan.reader.audio

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan

/** Preserve the exact canonical Arabic bytes and diacritics in display. Only add spans. */
object WordHighlightText {
    fun decorate(canonicalArabic: String, activeIndex: Int, wordCount: Int = 1): SpannableString {
        val spans = SpannableString(canonicalArabic)
        if (activeIndex < 0 || wordCount < 1) return spans
        val words = WordTimingFormat.wordRegions(canonicalArabic).drop(activeIndex).take(wordCount).toList()
        val start = words.firstOrNull()?.range?.first ?: return spans
        val end = words.last().range.last + 1
        spans.setSpan(BackgroundColorSpan(Color.rgb(242, 212, 121)), start,
            end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spans.setSpan(ForegroundColorSpan(Color.rgb(0, 50, 35)), start,
            end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spans
    }
}
