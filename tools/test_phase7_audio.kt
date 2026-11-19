import org.quranscan.reader.audio.*
import org.quranscan.reader.core.QuranVerseCounts

fun expectReject(name: String, block: () -> Unit) {
    var rejected = false
    try { block() } catch (_: IllegalArgumentException) { rejected = true }
    catch (_: IllegalStateException) { rejected = true }
    check(rejected) { "Must reject $name" }
}

fun main() {
    val digest = "ab".repeat(32)
    val minimal = """
# QSR-AUDIO-1
# reciter=Example recording, unchecked
# source=Local recording by user
# rights=User asserts permission, not authenticated
1	1	001_001.mp3	$digest
""".trimIndent().replace("\\t", "\t")
    val m = AudioPackFormat.parse(minimal)
    check(m.entries.size == 1)
    check(m.reciter.startsWith("Example"))
    check(m.entries.getValue(AudioVerseKey(1,1)).filename == "001_001.mp3")
    check(!m.isStructurallyComplete)
    check(QuranVerseCounts.counts.size == 114)
    check(QuranVerseCounts.counts.sum() == 6236)
    check(QuranVerseCounts.valid(114,6) && !QuranVerseCounts.valid(114,7))
    check(QuranVerseCounts.valid(2,286) && !QuranVerseCounts.valid(2,287))
    check(AudioPackFormat.fileName(AudioVerseKey(2,255)) == "002_255.mp3")
    expectReject("unknown header") { AudioPackFormat.parse(minimal.replace("# rights=", "# licensewrong=")) }
    expectReject("directory traversal") { AudioPackFormat.parse(minimal.replace("001_001.mp3", "../001_001.mp3")) }
    expectReject("wrong ayah filename") { AudioPackFormat.parse(minimal.replace("001_001.mp3", "001_002.mp3")) }
    expectReject("invalid digest") { AudioPackFormat.parse(minimal.replace(digest,"1234")) }
    expectReject("zero ayah") { AudioPackFormat.parse(minimal.replace("1\t1", "1\t0")) }
    expectReject("duplicate") { AudioPackFormat.parse(minimal + "\n1\t1\t001_001.mp3\t" + digest) }
    expectReject("missing rights") { AudioPackFormat.parse(minimal.replace("# rights=User asserts permission, not authenticated\n", "")) }
    expectReject("header after entries") { AudioPackFormat.parse(minimal + "\n# source=extra") }
    expectReject("extra column") { AudioPackFormat.parse(minimal + "\tmore") }
    println("PASS: 14 pure Kotlin audio format/verse-count checks")
}
