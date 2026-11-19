package org.quranscan.reader

import java.io.ByteArrayOutputStream
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.quranscan.reader.audio.AudioPackArchive
import org.quranscan.reader.audio.AudioPackCatalog
import org.quranscan.reader.audio.AudioVerseKey
import org.quranscan.reader.audio.PackStaging

/** Synthetic byte fixtures exercise the importer; they contain no recordings. */
class AudioPackArchiveTest {
    @get:Rule val temporary = TemporaryFolder()
    private val clip = "001_001.mp3"
    private val bytes = "abc".toByteArray()
    private val digest = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
    private val manifest = "# QSR-AUDIO-1\n# reciter=Fixture\n# source=Test\n# rights=Test\n1\t1\t$clip\t$digest\n"

    private fun zip(vararg entries: Pair<String, ByteArray>): ByteArray {
        val buffer = ByteArrayOutputStream()
        ZipOutputStream(buffer).use { output ->
            for ((name, content) in entries) {
                output.putNextEntry(ZipEntry(name))
                output.write(content)
                output.closeEntry()
            }
        }
        return buffer.toByteArray()
    }

    private fun validZip() = zip("manifest.tsv" to manifest.toByteArray(), clip to bytes)

    private fun <T> withStage(action: (File) -> T): T {
        val stage = PackStaging.create(temporary.root, exclusive = true)
        return try { action(stage) } finally { PackStaging.release(stage) }
    }

    @Test fun validPartialPackPublishesAndExactBytesAreRevalidated(): Unit = withStage { stage ->
        val catalog = AudioPackCatalog(temporary.root)
        val parsed = AudioPackArchive.extract(validZip().inputStream(), stage)
        assertEquals(digest, parsed.entries.getValue(AudioVerseKey(1, 1)).sha256)
        assertFalse(parsed.isStructurallyComplete)
        assertFalse(File(stage, PackStaging.AUDIO_ARCHIVE).exists())
        val id = catalog.publishValidated(stage)
        val reopened = AudioPackCatalog(temporary.root)
        val stored = reopened.selectedDirectory()
        assertEquals(id, reopened.selectedId())
        AudioPackArchive.verify(stored, parsed)
        File(stored, clip).writeText("abd")
        assertThrows(IllegalArgumentException::class.java) { AudioPackArchive.verify(stored, parsed) }
        File(stored, clip).delete()
        assertThrows(IllegalArgumentException::class.java) { AudioPackArchive.verify(stored, parsed) }
    }

    @Test fun invalidAndTruncatedArchivesAreRejected() {
        val valid = validZip()
        for (bad in listOf(byteArrayOf(1, 2, 3), valid.copyOf(valid.size - 22), valid.copyOf(valid.size - 1))) {
            withStage { stage ->
                assertThrows(IOException::class.java) { AudioPackArchive.extract(bad.inputStream(), stage) }
            }
        }
    }

    @Test fun unexpectedTraversalAndUnlistedEntriesAreRejected() {
        for (name in listOf("../$clip", "/$clip", "folder/$clip", "folder\\$clip", "folder/", "notes.txt", "001_002.mp3")) {
            val bad = zip("manifest.tsv" to manifest.toByteArray(), clip to bytes, name to bytes)
            withStage { stage ->
                assertThrows(IllegalArgumentException::class.java) { AudioPackArchive.extract(bad.inputStream(), stage) }
            }
        }
        assertFalse(File(temporary.root, clip).exists())
    }

    @Test fun smallCompressedArchiveCannotExpandPastClipLimit() = withStage { stage ->
        val bad = zip("manifest.tsv" to manifest.toByteArray(), clip to ByteArray(8 * 1024 * 1024 + 1))
        assertTrue(bad.size < 16 * 1024)
        assertThrows(IllegalArgumentException::class.java) { AudioPackArchive.extract(bad.inputStream(), stage) }
        assertFalse(File(stage, clip).exists())
    }

    @Test fun malformedManifestMissingClipAndWrongDigestAreRejected() {
        for (bad in listOf(
            zip("manifest.tsv" to manifest.replace("# rights=Test\n", "").toByteArray(), clip to bytes),
            zip("manifest.tsv" to manifest.toByteArray()),
            zip("manifest.tsv" to manifest.toByteArray(), clip to "abd".toByteArray())
        )) {
            withStage { stage ->
                assertThrows(IllegalArgumentException::class.java) { AudioPackArchive.extract(bad.inputStream(), stage) }
            }
        }
    }

    @Test fun preexistingStagingLinkIsNeverOverwritten() = withStage { stage ->
        val outside = File(temporary.root, "outside").apply { writeText("unchanged") }
        val link = File(stage, clip)
        java.nio.file.Files.createSymbolicLink(link.toPath(), outside.toPath())
        try {
            assertThrows(IllegalStateException::class.java) {
                AudioPackArchive.extract(validZip().inputStream(), stage)
            }
            assertEquals("unchanged", outside.readText())
        } finally { link.delete() }
    }

    @Test fun readIoFailureProvidesStorageOrAccessFeedbackAndCleansStage() {
        withStage { stage ->
            val source = object : InputStream() {
                override fun read(): Int = throw java.nio.file.AccessDeniedException("Fixture")
            }
            val error = assertThrows(IOException::class.java) { AudioPackArchive.extract(source, stage) }
            assertTrue(error.message!!.contains("file access"))
            assertTrue(error.cause is java.nio.file.AccessDeniedException)
        }
        assertTrue(temporary.root.list()!!.isEmpty())
    }

    @Test fun lowSpaceAndInterruptedReadReleaseStageWithoutReplacingLegacy() {
        val legacy = File(temporary.root, "active").apply { mkdir() }
        val recording = File(legacy, clip).apply { writeBytes(bytes) }
        withStage { stage ->
            assertThrows(IllegalArgumentException::class.java) {
                AudioPackArchive.extract(validZip().inputStream(), stage) { 0L }
            }
        }
        withStage { stage ->
            var delivered = false
            val source = object : InputStream() {
                override fun read(): Int = error("Expected buffered reads")
                override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
                    if (delivered) throw InterruptedException("Simulated cancellation")
                    delivered = true
                    buffer[offset] = 1
                    return 1
                }
            }
            assertThrows(InterruptedException::class.java) { AudioPackArchive.extract(source, stage) }
        }
        assertArrayEquals(bytes, recording.readBytes())
        assertEquals(listOf("active"), temporary.root.list()!!.toList())
    }
}
