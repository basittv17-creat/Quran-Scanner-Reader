import org.quranscan.reader.core.CameraScanGate

fun main() {
    val g = CameraScanGate(4_500)
    check(g.shouldScan(0)) { "first frame must be allowed" }
    check(!g.shouldScan(1_000)) { "must throttle fast frames" }
    check(!g.shouldScan(4_499)) { "interval boundary - 1" }
    check(g.shouldScan(4_500)) { "interval boundary" }
    check(!g.shouldScan(6_000)) { "next interval throttled" }
    check(g.shouldScan(6_001, manual = true)) { "manual request must bypass" }
    check(!g.shouldScan(10_000)) { "manual request restarts interval" }
    check(g.shouldScan(10_501)) { "manual interval elapsed" }
    g.reset()
    check(g.shouldScan(10_502)) { "reset permits immediate frame" }
    check(g.shouldScan(10, manual = false)) { "monotonic clock rollback safety" }
    var rejected = false
    try { CameraScanGate(0) } catch (_: IllegalArgumentException) { rejected = true }
    check(rejected) { "invalid interval must fail" }
    println("PASS: CameraScanGate 11 Kotlin assertions")
}
