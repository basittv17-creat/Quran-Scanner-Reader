package org.quranscan.reader.core

/** No OCR output is considered canonical Quran text without corpus verification. */
data class VerseId(val surah: Int, val ayah: Int) {
    init { require(surah in 1..114 && ayah > 0) }
}
data class VerseMatch(val id: VerseId, val confidence: Float, val canonicalArabic: String) {
    init { require(confidence in 0f..1f && canonicalArabic.isNotBlank()) }
}
sealed class RecognitionResult {
    data class VerifiedCandidate(val match: VerseMatch, val needsUserConfirmation: Boolean) : RecognitionResult()
    data class Uncertain(val reason: String) : RecognitionResult()
}
interface OfflineVerseRecognizer {
    suspend fun recognize(image: android.graphics.Bitmap): RecognitionResult
}
interface CanonicalQuranRepository {
    fun find(id: VerseId): String?
}
interface OfflineRecitationRepository {
    fun ayahAudio(id: VerseId): android.net.Uri?
    fun wordAudio(id: VerseId, wordIndex: Int): android.net.Uri?
}
