package org.quranscan.reader.audio

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** Opens and verifies the same file descriptor handed to the media decoder. */
internal object VerifiedRecording {
    fun open(file: File, expectedSha256: String): FileInputStream {
        val input = FileInputStream(file)
        try {
            val limit = 8L * 1024 * 1024
            require(input.channel.size() in 1..limit) { "Invalid local recording size" }
            val digest = MessageDigest.getInstance("SHA-256")
            val buffer = ByteArray(64 * 1024)
            var bytes = 0L
            while (true) {
                if (Thread.currentThread().isInterrupted) throw InterruptedException("Playback check cancelled")
                val read = input.read(buffer)
                if (read == -1) break
                bytes += read
                require(bytes <= limit) { "Local recording exceeds the size limit" }
                digest.update(buffer, 0, read)
            }
            val actual = digest.digest().joinToString("") { "%02x".format(it) }
            require(actual == expectedSha256) { "Local recording checksum changed. Import the audio pack again." }
            input.channel.position(0)
            return input
        } catch (error: Exception) {
            input.close()
            throw error
        }
    }
}
