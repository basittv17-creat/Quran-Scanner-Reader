package org.quranscan.reader

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.quranscan.reader.audio.PackStaging

class PackStagingTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun cleansAbandonedImportsWithoutDeletingActiveAndConcurrentPacks() {
        val root = temporary.root
        val abandoned = File(root, "stage_00000000-0000-0000-0000-000000000000").apply { mkdir() }
        File(abandoned, "001_001.mp3").writeText("temporary data fixture")
        val active = File(root, "active").apply { mkdir() }
        val uncertain = File(root, "stage_11111111-1111-1111-1111-111111111111").apply { mkdir() }
        File(uncertain, "unknown-directory").mkdir()
        val first = PackStaging.create(root)
        try {
            val second = PackStaging.create(root)
            try {
                assertFalse(abandoned.exists())
                assertTrue(active.isDirectory)
                assertTrue(uncertain.isDirectory)
                assertTrue(first.isDirectory)
                assertTrue(second.isDirectory)
            } finally {
                PackStaging.release(second)
            }
            assertFalse(second.exists())
        } finally {
            PackStaging.release(first)
        }
        assertFalse(first.exists())
    }
}
