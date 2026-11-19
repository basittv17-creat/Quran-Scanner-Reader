package org.quranscan.reader

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Test
import org.quranscan.reader.audio.AudioPackCatalog

/** Files are synthetic placeholders, not recordings or Quran data. */
class AudioPackCatalogTest {
    private fun withCatalog(test: (File, AudioPackCatalog) -> Unit) {
        val root = Files.createTempDirectory("qsr-catalog").toFile()
        try { test(root, AudioPackCatalog(root)) } finally { root.deleteRecursively() }
    }
    private fun stage(root: File, name: String): File = File(root, "stage_$name").apply {
        check(mkdir()); File(this, "fixture").writeText(name)
    }
    @Test fun importRetainsPreviousPacksAndLegacySelectionSurvivesRestart() = withCatalog { root, catalog ->
        File(root, "active").mkdir()
        File(root, "active/fixture").writeText("legacy")
        val a = catalog.publishValidated(stage(root, "a"))
        val b = catalog.publishValidated(stage(root, "b"))
        assertEquals(3, catalog.directories().size)
        assertEquals(b, AudioPackCatalog(root).selectedId())
        catalog.select(a)
        assertEquals("a", File(catalog.selectedDirectory(), "fixture").readText())
        catalog.select("legacy")
        assertEquals("legacy", File(catalog.selectedDirectory(), "fixture").readText())
        assertEquals("b", File(catalog.directory(b), "fixture").readText())
    }
    @Test fun activePackCannotBeDeletedButInactivePackCan() = withCatalog { root, catalog ->
        val a = catalog.publishValidated(stage(root, "a"))
        val b = catalog.publishValidated(stage(root, "b"))
        assertTrue(runCatching { catalog.removeInactive(b) }.isFailure)
        catalog.removeInactive(a)
        assertFalse(catalog.directory(a).exists())
        assertEquals(b, catalog.selectedId())
    }
    @Test fun interruptedSelectionRecoversOldIdAndNeverSelectsStagedId() = withCatalog { root, catalog ->
        val a = catalog.publishValidated(stage(root, "a"))
        val selected = File(root, "selection/active")
        assertTrue(selected.renameTo(File(root, "selection/previous")))
        val pending = File(root, "selection/stage_interrupted").apply { mkdir() }
        File(pending, "id").writeText("legacy")
        assertEquals(a, AudioPackCatalog(root).selectedId())
        assertTrue(pending.exists())
    }
    @Test fun traversalAndCorruptSelectionFailClosed() = withCatalog { root, catalog ->
        for (id in listOf("../active", "/tmp", "packs", "", "legacy/../")) {
            assertTrue(runCatching { catalog.directory(id) }.isFailure)
        }
        catalog.publishValidated(stage(root, "a"))
        File(root, "selection/active/id").writeText("../active")
        assertTrue(runCatching { catalog.selectedDirectory() }.isFailure)
    }
    @Test fun capacityRejectionPreservesSelectionAndStaging() = withCatalog { root, catalog ->
        var last = ""
        repeat(AudioPackCatalog.MAX_PACKS) { last = catalog.publishValidated(stage(root, it.toString())) }
        val extra = stage(root, "extra")
        assertTrue(runCatching { catalog.publishValidated(extra) }.isFailure)
        assertEquals(last, catalog.selectedId())
        assertTrue(extra.exists())
        assertEquals(AudioPackCatalog.MAX_PACKS, catalog.directories().size)
    }
}
