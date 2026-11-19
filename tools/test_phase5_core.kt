// JVM-only check of actual VerseCandidates.kt in a non-Android environment.
// Only syntactically synthetic tokens are used; no fabricated Quran text.
package org.quranscan.reader.core

data class VerseId(val surah: Int, val ayah: Int)

fun main() {
    fun checkIt(value: Boolean, why: String) { check(value) { why } }
    val normalized = VerseCandidates.normalizedWords("أَبْجَدَ   کتب  آسمان")
    checkIt(normalized == listOf("ابجد", "كتب", "اسمان"), "normalization")
    val synthetic = linkedMapOf<VerseId, String>()
    // A full-size synthetic lookup tests the fail-closed structural guard, not Quran data.
    for (i in 1..6236) synthetic[VerseId(1, i)] = "تمثيل فقط ملف رقم خمسة خمسة $i"
    synthetic[VerseId(1, 6236)] = "أَبْجَدَ بَسِيْطَ مِثَالَ كَلِمَةً مُمْتَازَةً"
    val ocr = "ابجد بسيط مثال كلمة ممتازة"
    val matches = VerseCandidates.suggest(ocr, synthetic)
    checkIt(matches.size == 1 && matches.single().id == VerseId(1, 6236), "unique match")
    checkIt(VerseCandidates.suggest(ocr, synthetic - VerseId(1, 20)).isEmpty(), "incomplete corpus must fail closed")
    checkIt(VerseCandidates.suggest("نص", synthetic).isEmpty(), "short candidate must fail closed")
    synthetic[VerseId(1, 1)] = synthetic[VerseId(1, 6236)]!!
    checkIt(VerseCandidates.suggest(ocr, synthetic).size == 2, "ambiguity must remain visible")
    println("Phase 5 Kotlin matching tests: PASS (5 assertions). No Quran OCR accuracy evaluated.")
}
