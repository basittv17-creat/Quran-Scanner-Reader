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
        val record = File(root, "selection/active/id")
        record.delete()
        assertTrue(runCatching { catalog.selectedDirectory() }.isFailure)
        record.writeText("../active")
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
    @Test fun publicationFailureRetainsCompleteNewPackAndLegacy() = withCatalog { root, catalog ->
        val legacy = File(root, "active").apply { mkdir() }
        File(legacy, "fixture").writeText("legacy")
        val blocked = File(root, "selection").apply { writeText("unavailable") }
        assertTrue(runCatching { catalog.publishValidated(stage(root, "new")) }.isFailure)
        assertEquals("legacy", File(legacy, "fixture").readText())
        assertTrue(blocked.delete())
        val reopened = AudioPackCatalog(root)
        val saved = reopened.directories().single { it.first != "legacy" }
        assertEquals("new", File(saved.second, "fixture").readText())
        assertEquals("legacy", reopened.selectedId())
        assertTrue(runCatching { reopened.removeInactive(saved.first) }.isFailure)
        reopened.select(saved.first)
        assertEquals(saved.first, reopened.selectedId())
    }

    @Test fun missingTargetFailsClosedUntilAnotherPackIsSelected() = withCatalog { root, catalog ->
        val original = catalog.publishValidated(stage(root, "original"))
        val missing = catalog.publishValidated(stage(root, "missing"))
        assertTrue(catalog.directory(missing).deleteRecursively())
        assertTrue(runCatching { catalog.selectedDirectory() }.isFailure)
        assertTrue(runCatching { catalog.removeInactive(original) }.isFailure)
        catalog.select(original)
        assertEquals(original, catalog.selectedId())
    }

    @Test fun symlinkInInactivePackCannotDeleteExternalFiles() = withCatalog { root, catalog ->
        val target = stage(root, "outside")
        val a = catalog.publishValidated(stage(root, "a"))
        val active = catalog.publishValidated(stage(root, "b"))
        File(catalog.directory(a), "link").let { Files.createSymbolicLink(it.toPath(), target.toPath()) }
        assertTrue(runCatching { catalog.removeInactive(a) }.isFailure)
        assertEquals("outside", File(target, "fixture").readText())
        assertEquals("a", File(catalog.directory(a), "fixture").readText())
        assertEquals(active, catalog.selectedId())
    }
}
