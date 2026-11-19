package org.quranscan.reader.audio

import java.io.File
import java.io.FileOutputStream
import java.util.UUID

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
        val selected = File(PackActivation.recover(selectionRoot), "id")
        if (!selected.exists()) "legacy" else {
            require(selected.isFile && selected.length() <= 64) { "Invalid pack selection record" }
            selected.readText(Charsets.UTF_8).also { require(validId(it)) { "Invalid selected pack ID" } }
        }
    }
    private fun validId(id: String) = id == "legacy" || idPattern.matches(id)

    fun directory(id: String): File = synchronized(lock) {
        require(validId(id)) { "Unsafe pack ID" }
        if (id == "legacy") PackActivation.recover(root) else File(packs, id)
    }
    fun selectedDirectory(): File = synchronized(lock) { directory(selectedId()) }

    fun directories(): List<Pair<String, File>> = synchronized(lock) {
        val result = mutableListOf<Pair<String, File>>()
        directory("legacy").takeIf { it.isDirectory }?.let { result += "legacy" to it }
        packs.listFiles()?.filter { it.isDirectory && idPattern.matches(it.name) }
            ?.sortedBy { it.name }?.forEach { result += it.name to it }
        result
    }
    fun requireCapacity() = synchronized(lock) {
        require(directories().size < MAX_PACKS) { "Eight audio packs already stored. Remove an inactive pack first." }
    }
    fun publishValidated(stage: File): String = synchronized(lock) {
        requireCapacity()
        require(stage.isDirectory && stage.name.startsWith("stage_") &&
            stage.canonicalFile.parentFile == root.canonicalFile) { "Invalid staging directory" }
        check(packs.isDirectory || packs.mkdirs()) { "Cannot create pack catalog" }
        val id = UUID.randomUUID().toString()
        check(stage.renameTo(File(packs, id))) { "Cannot preserve validated audio pack" }
        select(id)
        id
    }
    fun select(id: String) = synchronized(lock) {
        require(directory(id).isDirectory) { "Selected audio pack is unavailable" }
        check(selectionRoot.isDirectory || selectionRoot.mkdirs())
        val stage = File(selectionRoot, "stage_${UUID.randomUUID()}")
        check(stage.mkdir())
        try {
            FileOutputStream(File(stage, "id")).use { it.write(id.toByteArray(Charsets.UTF_8)); it.fd.sync() }
            PackActivation.activate(selectionRoot, stage)
        } finally { if (stage.exists()) stage.deleteRecursively() }
    }
    fun removeInactive(id: String) = synchronized(lock) {
        require(id != selectedId()) { "Select another audio pack before removing the active pack" }
        val path = directory(id)
        check(!path.exists() || path.deleteRecursively()) { "Cannot remove stored audio pack" }
    }
}
