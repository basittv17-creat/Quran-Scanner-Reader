package org.quranscan.reader

import android.content.Context
import org.quranscan.reader.core.ScanImageQuality

internal fun ScanImageQuality?.guidance(context: Context): String? = when {
    this == null -> null
    lowLight -> context.getString(R.string.scan_quality_low_light)
    lowDetail -> context.getString(R.string.scan_quality_low_detail)
    else -> null
}
