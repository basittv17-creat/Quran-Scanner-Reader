package org.quranscan.reader.audio

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.IOException

/** Imports user-provided licensed audio as local app-private files. No internet/network calls.
 * Data integrity, archive limits and path validation are enforced before activation.
 * IMPORTANT: checksums do not establish recording authenticity or redistribution rights.
 */
class OfflineAudioPackStore(private val context: Context) {
    private val root get() = File(context.filesDir.canonicalFile, "offline_ayah_audio")
    private val catalog get() = AudioPackCatalog(root)
    private val active get() = catalog.selectedDirectory()
    data class InstalledPack(val id: String, val manifest: AudioPackManifest?)

    fun installedPacks(): List<InstalledPack> {
        PackStaging.cleanAbandoned(root)
        PackStaging.cleanAbandoned(File(root, "selection"), selection = true)
        return catalog.directories().map { (id, folder) -> InstalledPack(id, manifestAt(folder)) }
    }
    // The pack chooser can repair a damaged record. Playback and removal still use the strict catalog.
    fun selectedPackId(): String = try {
        catalog.selectedId()
    } catch (_: InvalidPackSelection) {
        ""
    }
    fun selectPack(id: String) {
        catalog.select(id) { folder ->
            val manifest = manifestAt(folder) ?: error("Selected pack is corrupt or incomplete")
            AudioPackArchive.verify(folder, manifest)
        }
    }
    fun removeInactivePack(id: String) = catalog.removeInactive(id)

    fun installedManifest(): AudioPackManifest? = runCatching { manifestAt(active) }.getOrNull()

    private fun manifestAt(folder: File): AudioPackManifest? = runCatching {
        val manifestFile = File(folder, "manifest.tsv")
        if (!isPrivatePackPath(manifestFile) || !manifestFile.isFile || manifestFile.length() > 1024 * 1024L) null else {
            val manifest = AudioPackFormat.parse(manifestFile.readText(Charsets.UTF_8))
            if (manifest.entries.values.any {
                val file = File(folder, it.filename)
                !isPrivatePackPath(file) || !file.isFile || file.length() !in 1..8L * 1024 * 1024
            }) null else manifest
        }
    }.getOrNull()

    fun audioFile(key: AudioVerseKey): File? {
        val folder = runCatching { active }.getOrNull() ?: return null
        val record = manifestAt(folder)?.entries?.get(key) ?: return null
        val file = File(folder, record.filename)
        return file.takeIf { it.isFile }
    }

    fun installArchive(uri: Uri): AudioPackManifest {
        catalog.requireCapacity()
        val stage = PackStaging.create(root, exclusive = true)
        try {
            val manifest = try {
                val input = context.contentResolver.openInputStream(uri)
                    ?: error("Cannot open selected audio ZIP")
                input.use { AudioPackArchive.extract(it, stage) }
            } catch (failure: SecurityException) {
                throw IOException("Audio ZIP access denied. Choose the local ZIP again.", failure)
            }
            AudioPackArchive.checkInterrupted()
            catalog.publishValidated(stage)
            return manifest
        } finally {
            PackStaging.release(stage)
        }
    }
}
