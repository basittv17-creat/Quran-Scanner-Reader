package org.quranscan.reader.audio

import android.content.Context
import android.net.Uri
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipInputStream

/** Imports user-provided licensed audio as local app-private files. No internet/network calls.
 * Data integrity, archive limits and path validation are enforced before activation.
 * IMPORTANT: checksums do not establish recording authenticity or redistribution rights.
 */
class OfflineAudioPackStore(private val context: Context) {
    private val root get() = File(context.filesDir, "offline_ayah_audio")
    private val active get() = File(root, "active")
    private val allowedClip = Regex("[0-9]{3}_[0-9]{3}\\.mp3")

    fun installedManifest(): AudioPackManifest? = runCatching {
        val manifestFile = File(active, "manifest.tsv")
        if (!manifestFile.isFile) null else {
            val manifest = AudioPackFormat.parse(manifestFile.readText(Charsets.UTF_8))
            // Check files exist here; hashes were checked at import, not on each launch.
            if (manifest.entries.values.any { !File(active, it.filename).isFile }) null else manifest
        }
    }.getOrNull()

    fun audioFile(key: AudioVerseKey): File? {
        val record = installedManifest()?.entries?.get(key) ?: return null
        val file = File(active, record.filename)
        return file.takeIf { it.isFile }
    }

    fun installArchive(uri: Uri): AudioPackManifest {
        check(root.isDirectory || root.mkdirs()) { "Unable to create app-private audio folder" }
        val stage = File(root, "stage_${UUID.randomUUID()}")
        check(stage.mkdir()) { "Unable to create staging directory" }
        try {
            val names = hashSetOf<String>()
            var totalBytes = 0L
            var itemCount = 0
            val input = context.contentResolver.openInputStream(uri)
                ?: error("Cannot open selected audio ZIP")
            ZipInputStream(BufferedInputStream(input)).use { zip ->
                while (true) {
                    val item = zip.nextEntry ?: break
                    require(!item.isDirectory) { "Folder entries not allowed" }
                    val name = item.name
                    require(name == "manifest.tsv" || allowedClip.matches(name)) {
                        "Unrecognized or unsafe archive path"
                    }
                    require(names.add(name)) { "Duplicate ZIP entry" }
                    require(++itemCount <= QuranVerseCounts.totalVerses + 1) { "Too many ZIP entries" }
                    val out = File(stage, name)
                    // Enforce byte limits on *decompressed* files to prevent ZIP bombs.
                    val maxFile = if (name == "manifest.tsv") 1024L * 1024L else 8L * 1024L * 1024L
                    var fileBytes = 0L
                    FileOutputStream(out).use { output ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val count = zip.read(buf)
                            if (count == -1) break
                            if (count == 0) continue
                            fileBytes += count
                            totalBytes += count
                            require(fileBytes <= maxFile && totalBytes <= 1500L * 1024L * 1024L) {
                                "Audio pack exceeds safe size limit"
                            }
                            output.write(buf, 0, count)
                        }
                        output.fd.sync()
                    }
                    zip.closeEntry()
                }
            }
            require("manifest.tsv" in names) { "Missing manifest.tsv" }
            val manifest = AudioPackFormat.parse(File(stage, "manifest.tsv").readText(Charsets.UTF_8))
            require(names == manifest.entries.values.mapTo(hashSetOf("manifest.tsv")) { it.filename }) {
                "ZIP files must exactly match the manifest"
            }
            manifest.entries.values.forEach { item ->
                val digest = MessageDigest.getInstance("SHA-256")
                File(stage, item.filename).inputStream().buffered().use { input ->
                    val buf = ByteArray(64 * 1024)
                    while (true) {
                        val read = input.read(buf)
                        if (read == -1) break
                        digest.update(buf, 0, read)
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                require(actual == item.sha256) { "Audio file hash mismatch: ${item.filename}" }
            }
            // Swap only after EVERY file was validated. Restore older pack if activation fails.
            val previous = File(root, "previous_${UUID.randomUUID()}")
            if (active.exists()) check(active.renameTo(previous)) { "Could not stage current pack for replacement" }
            if (!stage.renameTo(active)) {
                if (previous.exists()) previous.renameTo(active)
                error("Could not activate audio pack")
            }
            if (previous.exists()) previous.deleteRecursively()
            return manifest
        } finally {
            if (stage.exists()) stage.deleteRecursively()
        }
    }
}
