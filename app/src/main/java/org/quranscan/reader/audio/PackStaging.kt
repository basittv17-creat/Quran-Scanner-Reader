package org.quranscan.reader.audio

import java.io.File
import java.util.UUID

/** Tracks imports across store instances in the application's single process. */
internal object PackStaging {
    private val importDirectory = Regex("stage_[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}")
    private val stagedEntry = Regex("manifest\\.tsv|[0-9]{3}_[0-9]{3}\\.(mp3|tsv)")
    private val running = mutableSetOf<File>()

    @Synchronized fun create(root: File): File {
        val folder = root.canonicalFile
        check(folder.isDirectory || folder.mkdirs()) { "Unable to create private pack folder" }
        folder.listFiles()?.forEach { candidate ->
            if (candidate !in running && importDirectory.matches(candidate.name)) {
                discard(candidate)
            }
        }
        val stage = File(folder, "stage_${UUID.randomUUID()}")
        check(stage.mkdir()) { "Unable to create staging directory" }
        running.add(stage)
        return stage
    }

    @Synchronized fun release(stage: File) {
        if (running.remove(stage)) discard(stage)
    }

    /** Generated import directories are flat. Leave unexpected entries and links untouched. */
    private fun discard(folder: File) {
        runCatching {
            if (!folder.isDirectory || folder.canonicalFile != folder) return
            val files = folder.listFiles() ?: return
            if (files.any { !stagedEntry.matches(it.name) || !it.isFile || it.canonicalFile != it }) return
            files.forEach { it.delete() }
            folder.delete()
        }
    }
}
