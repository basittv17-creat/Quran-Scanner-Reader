package org.quranscan.reader.audio

import android.graphics.Color
import android.text.SpannableString
import android.text.Spanned
import android.text.style.BackgroundColorSpan
import android.text.style.ForegroundColorSpan

/** Preserve the exact canonical Arabic bytes and diacritics in display. Only add spans. */
object WordHighlightText {
    fun decorate(canonicalArabic: String, activeIndex: Int): SpannableString {
        val spans = SpannableString(canonicalArabic)
        if (activeIndex < 0) return spans
        val regions = Regex("\\S+").findAll(canonicalArabic).toList()
        val target = regions.getOrNull(activeIndex) ?: return spans
        spans.setSpan(BackgroundColorSpan(Color.rgb(242, 212, 121)), target.range.first,
            target.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        spans.setSpan(ForegroundColorSpan(Color.rgb(0, 50, 35)), target.range.first,
            target.range.last + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        return spans
    }
}
