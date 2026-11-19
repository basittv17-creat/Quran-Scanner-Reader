import org.quranscan.reader.audio.*

fun reject(name: String, block: () -> Unit) {
    val rejected = runCatching { block() }.isFailure
    check(rejected) { "Must reject $name" }
}
fun main() {
    val verse = "كَلِمَةٌ ثَانِيَةٌ" // synthetic test fixture, NOT Quran text
    val textHash = WordTimingFormat.verseDigest(verse)
    val timing = "1\t0\t240\n2\t270\t610\n"
    val sha = WordTimingFormat.sha256(timing.toByteArray())
    val audioSha = "cd".repeat(32)
    val m = """# QSR-WORD-TIMING-1
# reciter=Test only
# source=Unreleased synthetic data
# rights=Test only
1	1	001_001.tsv	$sha	$audioSha	$textHash	2
""".trimIndent().replace("\\t", "\t")
    val parsed = WordTimingFormat.parseManifest(m)
    check(parsed.entries.size == 1)
    check(parsed.entries[AudioVerseKey(1, 1)]!!.wordCount == 2)
    check(WordTimingFormat.words(verse) == listOf("كَلِمَةٌ", "ثَانِيَةٌ"))
    check(WordTimingFormat.verseDigest(verse) != WordTimingFormat.verseDigest("كلمة ثانية"))
    val words = WordTimingFormat.parseTimeline(timing, 2)
    check(WordTimingFormat.activeIndex(words, -1) == -1)
    check(WordTimingFormat.activeIndex(words, 0) == 0)
    check(WordTimingFormat.activeIndex(words, 239) == 0)
    check(WordTimingFormat.activeIndex(words, 240) == -1)
    check(WordTimingFormat.activeIndex(words, 269) == -1)
    check(WordTimingFormat.activeIndex(words, 270) == 1)
    check(WordTimingFormat.activeIndex(words, 610) == -1)
    reject("empty") { WordTimingFormat.parseManifest("") }
    reject("path traversal") { WordTimingFormat.parseManifest(m.replace("001_001.tsv", "../evil.tsv")) }
    reject("wrong verse filename") { WordTimingFormat.parseManifest(m.replace("001_001.tsv", "001_002.tsv")) }
    reject("duplicate word entry") { WordTimingFormat.parseManifest(m + m.lines().last()) }
    reject("bad checksum") { WordTimingFormat.parseManifest(m.replace(audioSha, "abcd")) }
    reject("missing rights") { WordTimingFormat.parseManifest(m.replace("# rights=Test only\n", "")) }
    reject("extra tab") { WordTimingFormat.parseManifest(m.replace("\t2", "\t2\textra")) }
    reject("overlap") { WordTimingFormat.parseTimeline("1\t0\t250\n2\t240\t600", 2) }
    reject("out of order") { WordTimingFormat.parseTimeline("2\t0\t250\n1\t260\t600", 2) }
    reject("zero duration") { WordTimingFormat.parseTimeline("1\t0\t0\n2\t260\t600", 2) }
    reject("extra row") { WordTimingFormat.parseTimeline(timing + "3\t620\t700", 2) }
    reject("missing row") { WordTimingFormat.parseTimeline("1\t0\t240", 2) }
    reject("beyond time") { WordTimingFormat.parseTimeline("1\t0\t240\n2\t270\t900001", 2) }
    println("PASS: Phase 8 pure Kotlin word timing and fail-closed checks")
}
