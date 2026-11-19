package org.quranscan.reader.audio

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

internal class InvalidPackSelection(message: String) : IllegalStateException(message)

/** Immutable validated pack directories plus a recoverable, tiny selection record.
 * The store must validate manifest/audio bytes before publish and before selecting a pack.
 * Interrupted publication can leave a complete unselected pack, never an incomplete active pack.
 */
class AudioPackCatalog(private val root: File) {
    companion object {
        private val lock = Any()
        private val idPattern = Regex("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}")
        const val MAX_PACKS = 8
    }
    private val packs get() = File(root, "packs")
    private val selectionRoot get() = File(root, "selection")

    fun selectedId(): String = synchronized(lock) {
        val record = PackActivation.recover(selectionRoot)
        val selected = File(record, "id")
        if (!record.exists()) "legacy" else {
            if (!isPrivatePackPath(selected) || !selected.isFile || selected.length() !in 1..64) {
                throw InvalidPackSelection("Invalid pack selection. Choose a stored audio pack.")
            }
            val id = selected.readText(Charsets.UTF_8)
            if (!validId(id) || (id != "legacy" && !directory(id).isDirectory)) {
                throw InvalidPackSelection("Saved pack is unavailable. Choose a stored audio pack.")
            }
            id
        }
    }
    private fun validId(id: String) = id == "legacy" || idPattern.matches(id)

    fun directory(id: String): File = synchronized(lock) {
        require(validId(id)) { "Unsafe pack ID" }
        if (id == "legacy") PackActivation.recover(root) else File(packs, id).also {
            require(isPrivatePackPath(it)) { "Unsafe stored audio pack path" }
        }
    }
    fun selectedDirectory(): File = synchronized(lock) { directory(selectedId()) }

    fun directories(): List<Pair<String, File>> = synchronized(lock) {
        check(isPrivatePackPath(packs)) { "Unsafe audio catalog directory" }
        val result = mutableListOf<Pair<String, File>>()
        directory("legacy").takeIf { it.isDirectory }?.let { result += "legacy" to it }
        val folders = if (packs.exists()) {
            checkNotNull(packs.listFiles()) { "Cannot read the private audio catalog" }
        } else emptyArray()
        folders.filter { it.isDirectory && idPattern.matches(it.name) && isPrivatePackPath(it) }
            .sortedBy { it.name }.forEach { result += it.name to it }
        result
    }
    fun requireCapacity() = synchronized(lock) {
        require(directories().size < MAX_PACKS) { "Eight audio packs already stored. Remove an inactive pack first." }
    }
    fun publishValidated(stage: File): String = synchronized(lock) {
        requireCapacity()
        require(isPrivatePackPath(stage) && stage.isDirectory && stage.name.startsWith("stage_") &&
            stage.canonicalFile.parentFile == root.canonicalFile) { "Invalid staging directory" }
        check(packs.isDirectory || packs.mkdirs()) { "Cannot create pack catalog" }
        val id = UUID.randomUUID().toString()
        check(stage.renameTo(File(packs, id))) { "Cannot preserve validated audio pack" }
        try {
            select(id)
        } catch (failure: Exception) {
            throw IllegalStateException(
                "Audio pack saved but could not be selected. Select the saved pack after checking private storage.",
                failure
            )
        }
        id
    }
    fun select(id: String, validate: (File) -> Unit = {}) = synchronized(lock) {
        val folder = directory(id)
        require(folder.isDirectory) { "Selected audio pack is unavailable" }
        validate(folder)
        val stage = PackStaging.create(selectionRoot, selection = true)
        try {
            FileOutputStream(File(stage, "id")).use { it.write(id.toByteArray(Charsets.UTF_8)); it.fd.sync() }
            PackActivation.activate(selectionRoot, stage)
        } finally { PackStaging.release(stage) }
    }
    fun removeInactive(id: String) = synchronized(lock) {
        require(id != selectedId()) { "Select another audio pack before removing the active pack" }
        check(File(selectionRoot, "active/id").isFile) {
            "Choose a stored audio pack before removing any pack."
        }
        val path = directory(id)
        check(!path.exists() || deleteFlatPack(path)) { "Cannot remove stored audio pack" }
    }
}
