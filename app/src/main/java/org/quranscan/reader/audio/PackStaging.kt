package org.quranscan.reader.audio

import java.io.File
import java.util.UUID

/** Tracks imports across store instances in the application's single process. */
internal object PackStaging {
    private val importDirectory = Regex("stage_[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val stagedEntry = Regex("manifest\\.tsv|[0-9]{3}_[0-9]{3}\\.(mp3|tsv)")
    const val AUDIO_ARCHIVE = "import.zip.part"
    private val running = mutableMapOf<File, Boolean>()

    @Synchronized fun create(root: File, exclusive: Boolean = false, selection: Boolean = false): File {
        check(isPrivatePackPath(root)) { "Unsafe private pack directory" }
        val folder = root.canonicalFile
        check(!exclusive || running.keys.none { it.parentFile == folder }) { "An audio import is already running" }
        check(folder.isDirectory || folder.mkdirs()) { "Unable to create private pack folder" }
        cleanAbandoned(folder, selection)
        val stage = File(folder, "stage_${UUID.randomUUID()}")
        check(stage.mkdir()) { "Unable to create staging directory" }
        running[stage] = selection
        return stage
    }

    @Synchronized fun cleanAbandoned(folder: File, selection: Boolean = false) {
        if (!isPrivatePackPath(folder)) return
        folder.listFiles()?.forEach { candidate ->
            if (candidate !in running && importDirectory.matches(candidate.name)) {
                discard(candidate, selection)
            }
        }
    }

    @Synchronized fun release(stage: File) {
        running.remove(stage)?.let { discard(stage, it) }
    }

    /** Generated import directories are flat. Leave unexpected entries and links untouched. */
    private fun discard(folder: File, selection: Boolean) {
        runCatching {
            if (!folder.isDirectory || folder.canonicalFile != folder) return
            val files = folder.listFiles() ?: return
            if (files.any {
                val known = if (selection) it.name == "id" else stagedEntry.matches(it.name) || it.name == AUDIO_ARCHIVE
                !known || !it.isFile || !isPrivatePackPath(it)
            }) return
            files.forEach { it.delete() }
            folder.delete()
        }
    }
}
