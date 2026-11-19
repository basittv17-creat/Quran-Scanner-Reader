package org.quranscan.reader.audio

import org.quranscan.reader.core.QuranVerseCounts
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.zip.CRC32
import java.util.zip.ZipEntry
import java.util.zip.ZipException
import java.util.zip.ZipFile

/** Validates the ZIP directory and bounds actual bytes written to private storage. */
internal object AudioPackArchive {
    private const val MANIFEST_LIMIT = 1024L * 1024
    private const val CLIP_LIMIT = 8L * 1024 * 1024
    private const val TOTAL_LIMIT = 1500L * 1024 * 1024
    private const val ARCHIVE_LIMIT = TOTAL_LIMIT + 32L * 1024 * 1024
    private val clipName = Regex("[0-9]{3}_[0-9]{3}\\.mp3")

    fun extract(input: InputStream, stage: File, usableSpace: () -> Long = { stage.usableSpace }): AudioPackManifest {
        val archive = File(stage, PackStaging.AUDIO_ARCHIVE)
        try {
            write(input, archive, ARCHIVE_LIMIT, usableSpace)
            checkDirectoryBounds(archive)
            val validated = ZipFile(archive).use { zip ->
                require(zip.size() in 1..QuranVerseCounts.totalVerses + 1) { "Invalid number of ZIP entries" }
                val entries = zip.entries().asSequence().toList()
                val names = hashSetOf<String>()
                var declaredBytes = 0L
                for (entry in entries) {
                    require(!entry.isDirectory && (entry.name == "manifest.tsv" || clipName.matches(entry.name))) {
                        "Unrecognized or unsafe archive path"
                    }
                    require(names.add(entry.name)) { "Duplicate ZIP entry" }
                    require(entry.size in 1..limit(entry.name)) { "Invalid audio ZIP entry size" }
                    declaredBytes += entry.size
                    require(declaredBytes <= TOTAL_LIMIT) { "Audio pack exceeds safe size limit" }
                }
                val manifestEntry = entries.firstOrNull { it.name == "manifest.tsv" }
                    ?: error("Missing manifest.tsv")
                require(usableSpace() >= declaredBytes) {
                    "Not enough private storage to extract the audio ZIP. Free space and try again."
                }
                val manifestFile = File(stage, manifestEntry.name)
                fun extractEntry(entry: ZipEntry) {
                    val crc = CRC32()
                    val count = zip.getInputStream(entry).use { member ->
                        write(member, File(stage, entry.name), entry.size, usableSpace, crc)
                    }
                    require(count == entry.size && crc.value == entry.crc) { "Damaged ZIP entry: ${entry.name}" }
                }
                extractEntry(manifestEntry)
                val manifest = parse(manifestFile)
                require(names == manifest.entries.values.mapTo(hashSetOf("manifest.tsv")) { it.filename }) {
                    "ZIP files must exactly match the manifest"
                }
                entries.filter { it != manifestEntry }.forEach { extractEntry(it) }
                manifest.also { verify(stage, it) }
            }
            check(archive.delete()) { "Cannot remove the temporary audio ZIP. The pack was not installed." }
            return validated
        } catch (failure: ZipException) {
            throw IOException("Invalid or damaged audio ZIP", failure)
        } catch (failure: IOException) {
            if (usableSpace() <= 0) throw IOException("Private audio storage is full. Free space and try again.", failure)
            throw IOException("Cannot read or write the audio ZIP. Check free space and file access.", failure)
        }
    }

    // This format fits in classic ZIP. Bound the central directory before ZipFile loads it.
    private fun checkDirectoryBounds(archive: File) {
        RandomAccessFile(archive, "r").use { file ->
            val tail = ByteArray(minOf(file.length(), 22L + 65535).toInt())
            file.seek(file.length() - tail.size)
            file.readFully(tail)
            val fields = ByteBuffer.wrap(tail).order(ByteOrder.LITTLE_ENDIAN)
            for (end in tail.size - 22 downTo 0) {
                if (fields.getInt(end) != 0x06054b50 ||
                    (fields.getShort(end + 20).toInt() and 0xffff) != tail.size - end - 22) continue
                val count = fields.getShort(end + 10).toInt() and 0xffff
                val size = fields.getInt(end + 12).toLong()
                val offset = fields.getInt(end + 16).toLong()
                if (fields.getInt(end + 4) == 0 &&
                    fields.getShort(end + 8) == fields.getShort(end + 10) &&
                    count in 1..QuranVerseCounts.totalVerses + 1 &&
                    size in (46L * count)..(4L * 1024 * 1024) &&
                    offset > 0 && offset + size == file.length() - tail.size + end) return
                throw ZipException("Invalid or oversized ZIP directory")
            }
            throw ZipException("Missing ZIP directory")
        }
    }

    private fun parse(file: File) = AudioPackFormat.parse(
        Charsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(file.readBytes())).toString()
    )

    private fun limit(name: String) = if (name == "manifest.tsv") MANIFEST_LIMIT else CLIP_LIMIT

    fun verify(folder: File, manifest: AudioPackManifest) {
        for (entry in manifest.entries.values) {
            val file = File(folder, entry.filename)
            require(isPrivatePackPath(file) && file.isFile) { "Missing or unsafe audio file: ${entry.filename}" }
            VerifiedRecording.open(file, entry.sha256).close()
        }
    }

    fun checkInterrupted() {
        if (Thread.currentThread().isInterrupted) throw InterruptedException("Audio import cancelled")
    }

    private fun write(
        input: InputStream,
        file: File,
        limit: Long,
        usableSpace: () -> Long,
        crc: CRC32? = null
    ): Long {
        check(file.createNewFile()) { "Audio staging file already exists" }
        var bytes = 0L
        val buffer = ByteArray(64 * 1024)
        FileOutputStream(file).use { output ->
            while (true) {
                checkInterrupted()
                val count = input.read(buffer)
                if (count == -1) break
                bytes += count
                require(bytes <= limit) { "Audio pack exceeds safe size limit" }
                require(usableSpace() >= count.toLong()) { "Not enough private storage for the audio ZIP" }
                output.write(buffer, 0, count)
                crc?.update(buffer, 0, count)
            }
            output.fd.sync()
        }
        return bytes
    }
}
