package org.quranscan.reader.audio

import android.content.Context
import android.net.Uri
import org.quranscan.reader.core.OfflineCorpus
import org.quranscan.reader.core.VerseId
import org.quranscan.reader.core.QuranVerseCounts
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.zip.ZipInputStream

/** Import only from a user selected local ZIP; rejects traversal, oversized and mismatched data.
 * Word timings are *not* an authoritative Quran source. Installed data is validated against
 * canonical text and the *exact* installed audio pack, not just user claims in the timing ZIP.
 */
class OfflineWordTimingStore(private val context: Context) {
    private val root get() = File(context.filesDir, "offline_word_timings")
    private val active get() = PackActivation.recover(root)
    private val filename = Regex("[0-9]{3}_[0-9]{3}\\.tsv")

    fun manifest(): WordTimingManifest? = runCatching {
        val f = File(active, "manifest.tsv")
        if (!f.isFile) null else WordTimingFormat.parseManifest(f.readText(Charsets.UTF_8))
    }.getOrNull()

    /** Return null if the audio pack or the canonical verse changed, or if any timing bytes changed. */
    fun timeline(key: AudioVerseKey, canonicalVerse: String, audio: AudioPackManifest?): List<TimedWord>? {
        val m = manifest() ?: return null
        val record = m.entries[key] ?: return null
        if (audio == null || m.reciter != audio.reciter) return null
        if (record.audioSha256 != audio.entries[key]?.sha256) return null
        if (record.canonicalTextSha256 != WordTimingFormat.verseDigest(canonicalVerse)) return null
        val path = File(active, record.filename)
        if (!path.isFile || path.length() > 32 * 1024L) return null
        val bytes = path.readBytes()
        if (WordTimingFormat.sha256(bytes) != record.sha256) return null
        if (WordTimingFormat.words(canonicalVerse).size != record.wordCount) return null
        return runCatching {
            WordTimingFormat.parseTimeline(bytes.toString(Charsets.UTF_8), record.wordCount)
        }.getOrNull()
    }

    fun install(uri: Uri, corpus: OfflineCorpus, audio: AudioPackManifest?): WordTimingManifest {
        require(corpus.isComplete()) { "Import blocked: complete canonical Quran text is missing" }
        require(audio != null) { "Import a licensed ayah MP3 pack first" }
        require(root.isDirectory || root.mkdirs())
        val stage = File(root, "stage_${UUID.randomUUID()}")
        check(stage.mkdir())
        try {
            val names = hashSetOf<String>()
            var total = 0L
            val input = context.contentResolver.openInputStream(uri) ?: error("Cannot open timing ZIP")
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val entry = zip.nextEntry ?: break
                    require(!entry.isDirectory) { "Directories are forbidden" }
                    val name = entry.name
                    require(name == "manifest.tsv" || filename.matches(name)) { "Unsafe ZIP filename" }
                    require(names.add(name)) { "Duplicate ZIP filename" }
                    require(names.size <= QuranVerseCounts.totalVerses + 1) { "Too many timing files" }
                    val out = File(stage, name)
                    val limit = if (name == "manifest.tsv") 2 * 1024 * 1024L else 32 * 1024L
                    var count = 0L
                    FileOutputStream(out).use { dest ->
                        val buf = ByteArray(8192)
                        while (true) {
                            if (Thread.currentThread().isInterrupted) throw InterruptedException("Import cancelled")
                            val n = zip.read(buf)
                            if (n == -1) break
                            if (n == 0) continue
                            count += n
                            total += n
                            require(count <= limit && total <= 40 * 1024 * 1024L) { "Oversized timing ZIP" }
                            dest.write(buf, 0, n)
                        }
                        dest.fd.sync()
                    }
                    zip.closeEntry()
                }
            }
            require("manifest.tsv" in names)
            val manifest = WordTimingFormat.parseManifest(File(stage, "manifest.tsv").readText(Charsets.UTF_8))
            require(manifest.reciter == audio.reciter) { "Timing reciter is different from installed audio pack" }
            require(names == manifest.entries.values.mapTo(hashSetOf("manifest.tsv")) { it.filename }) {
                "Timing ZIP and manifest disagree"
            }
            manifest.entries.values.forEach { record ->
                val verse = corpus.find(VerseId(record.key.surah, record.key.ayah))
                    ?: error("Canonical verse missing")
                require(record.canonicalTextSha256 == WordTimingFormat.verseDigest(verse)) {
                    "Timing text does not match verbatim canonical verse"
                }
                require(record.audioSha256 == audio.entries[record.key]?.sha256) {
                    "Timings do not match the installed audio recording"
                }
                val bytes = File(stage, record.filename).readBytes()
                require(WordTimingFormat.sha256(bytes) == record.sha256) { "Timing file SHA-256 mismatch" }
                require(record.wordCount == WordTimingFormat.words(verse).size) { "Word count mismatch" }
                WordTimingFormat.parseTimeline(bytes.toString(Charsets.UTF_8), record.wordCount)
            }
            if (Thread.currentThread().isInterrupted) throw InterruptedException("Import cancelled")
            PackActivation.activate(root, stage)
            return manifest
        } finally {
            if (stage.exists()) stage.deleteRecursively()
        }
    }
}
