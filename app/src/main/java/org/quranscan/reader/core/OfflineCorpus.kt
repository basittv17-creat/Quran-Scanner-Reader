package org.quranscan.reader.core

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/** Loads ONLY locally bundled, independently reviewed verses. Empty corpus fails closed. */
class OfflineCorpus(private val context: Context) : CanonicalQuranRepository {
    private val verses: Map<VerseId, String> by lazy { load() }
    override fun find(id: VerseId): String? = verses[id]
    fun count(): Int = verses.size
    /** Never mutate canonical text. This snapshot is for candidate comparisons only. */
    fun asMap(): Map<VerseId, String> = verses.toMap()
    /** A partial or tampered text source must never enable automatic recitation. */
    fun isComplete(): Boolean = verses.size == 6236 && QuranVerseCounts.counts.indices.all { s ->
        (1..QuranVerseCounts.counts[s]).all { a -> verses.containsKey(VerseId(s+1,a)) }
    }

    private fun load(): Map<VerseId, String> {
        val bytes = context.assets.open("quran/verses.jsonl").use { it.readBytes() }
        val expected = context.assets.open("quran/verses.sha256").bufferedReader().use { it.readText().trim() }
        val actual = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
        require(actual.equals(expected, ignoreCase = true)) { "Bundled Quran corpus checksum mismatch" }
        val result = linkedMapOf<VerseId, String>()
        bytes.toString(Charsets.UTF_8).lineSequence().filter { it.isNotBlank() }.forEach { line ->
            val entry = JSONObject(line)
            val id = VerseId(entry.getInt("surah"), entry.getInt("ayah"))
            val arabic = entry.getString("arabic")
            require(arabic.isNotBlank() && arabic.any { it in '\u0621'..'\u064A' })
            require(result.put(id, arabic) == null) { "Duplicate Quran verse: $id" }
        }
        return result
    }
}
