package org.quranscan.reader

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test
import org.quranscan.reader.audio.PackActivation

/** Synthetic files only. Simulate process-death directory states and rename failures. */
class PackActivationTest {
    private fun withRoot(test: (File) -> Unit) {
        val root = Files.createTempDirectory("qsr-activation-test").toFile()
        try { test(root) } finally { root.deleteRecursively() }
    }
    private fun pack(root: File, name: String, value: String): File = File(root, name).apply {
        check(mkdir()); File(this, "fixture").writeText(value)
    }
    @Test fun deathBeforePublicationRestoresOldPackAndNeverPublishesStage() = withRoot { root ->
        pack(root, "previous", "old")
        pack(root, "stage_incomplete", "unvalidated")
        assertEquals("old", File(PackActivation.recover(root), "fixture").readText())
        assertTrue(File(root, "stage_incomplete").isDirectory)
    }
    @Test fun deathAfterPublicationKeepsNewPackAndNextImportWorks() = withRoot { root ->
        pack(root, "active", "new")
        pack(root, "previous", "old")
        assertEquals("new", File(PackActivation.recover(root), "fixture").readText())
        PackActivation.activate(root, pack(root, "stage_next", "next"))
        assertEquals("next", File(PackActivation.recover(root), "fixture").readText())
        assertFalse(File(root, "previous").exists())
    }
    @Test fun failedPublicationRollsBackAndFailedRollbackIsRecoverable() = withRoot { root ->
        pack(root, "active", "old")
        val stage = pack(root, "stage_new", "new")
        assertTrue(runCatching { PackActivation.activateWithMove(root, stage) { from, to ->
            if (from == stage) false else from.renameTo(to)
        } }.isFailure)
        assertEquals("old", File(PackActivation.recover(root), "fixture").readText())
        assertTrue(runCatching { PackActivation.activateWithMove(root, stage) { from, to ->
            if (from.name == "active") from.renameTo(to) else false
        } }.isFailure)
        assertFalse(File(root, "active").exists())
        assertEquals("old", File(PackActivation.recover(root), "fixture").readText())
    }
    @Test fun legacyBackupRestoresButAmbiguousBackupsRemainUntouched() = withRoot { root ->
        val first = pack(root, "previous_legacy", "old")
        val second = pack(root, "previous_other", "other")
        assertTrue(runCatching { PackActivation.recover(root) }.isFailure)
        assertTrue(first.exists() && second.exists())
        second.deleteRecursively()
        assertEquals("old", File(PackActivation.recover(root), "fixture").readText())
    }
    @Test fun invalidStagingDoesNotTouchActivePack() = withRoot { root ->
        val active = pack(root, "active", "old")
        assertTrue(runCatching { PackActivation.activate(root, active) }.isFailure)
        assertEquals("old", File(active, "fixture").readText())
    }
}
