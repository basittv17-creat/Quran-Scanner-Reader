package org.quranscan.reader.audio

import java.io.File

/** Recoverable same-filesystem directory publication after the caller validates every staged byte.
 * Serializes audio/timing swaps across store instances. Does not certify pack contents or rights.
 * Process death before publication restores the old directory; after publication keeps the new one.
 * This is process-interruption recovery, not a guarantee against power loss/filesystem corruption.
 */
object PackActivation {
    @Synchronized fun recover(root: File): File {
        val active = File(root, "active")
        if (active.exists()) {
            check(active.isDirectory) { "Invalid active pack directory" }
            return active
        }
        val previous = File(root, "previous")
        val backups = if (previous.exists()) listOf(previous) else
            root.listFiles()?.filter { it.isDirectory && it.name.startsWith("previous_") }.orEmpty()
        check(backups.size <= 1) { "Ambiguous previous packs; stored files retained" }
        backups.singleOrNull()?.let {
            check(it.isDirectory && it.renameTo(active)) { "Cannot restore previous pack; stored files retained" }
        }
        return active
    }

    @Synchronized fun activate(root: File, stage: File) = activateWithMove(root, stage) { from, to -> from.renameTo(to) }

    /** Testable rename failure path; production always uses filesystem renameTo. */
    @Synchronized internal fun activateWithMove(root: File, stage: File, move: (File, File) -> Boolean) {
        require(stage.isDirectory && stage.name.startsWith("stage_") &&
            stage.canonicalFile.parentFile == root.canonicalFile) { "Invalid staging directory" }
        val active = recover(root)
        val previous = File(root, "previous")
        // A previous published transaction may have been killed during cleanup.
        check(!previous.exists() || previous.deleteRecursively()) { "Cannot clean previous pack" }
        if (active.exists()) check(move(active, previous)) { "Cannot preserve current pack" }
        if (!move(stage, active)) {
            check(!previous.exists() || move(previous, active)) { "Activation failed; previous pack retained for recovery" }
            error("Could not activate pack; previous pack restored")
        }
        // Cleanup is best effort; recovery prefers the completely validated active directory.
        if (previous.exists()) previous.deleteRecursively()
    }
}
