package org.quranscan.reader

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.quranscan.reader.audio.VerifiedRecording

class VerifiedRecordingTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun rewindsVerifiedDescriptorAndRejectsChangedBytes() {
        val file = temporary.newFile()
        val original = "abc".toByteArray()
        val hash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        file.writeBytes(original)
        VerifiedRecording.open(file, hash).use { assertArrayEquals(original, it.readBytes()) }
        file.writeText("abd")
        assertTrue(runCatching { VerifiedRecording.open(file, hash).close() }.isFailure)
    }

    @Test fun rejectsTruncatedEmptyAndMissingFiles() {
        val file = temporary.newFile()
        val hash = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
        file.writeText("ab")
        assertTrue(runCatching { VerifiedRecording.open(file, hash).close() }.isFailure)
        file.writeText("")
        assertTrue(runCatching { VerifiedRecording.open(file, hash).close() }.isFailure)
        assertTrue(file.delete())
        assertTrue(runCatching { VerifiedRecording.open(file, hash).close() }.isFailure)
    }
}
