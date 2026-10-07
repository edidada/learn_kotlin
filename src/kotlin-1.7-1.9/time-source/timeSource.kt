package learn.kotlin1719.timesrc

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TestTimeSource
import kotlin.time.TimeSource
import kotlin.time.measureTime

// 1.9 重写的时间测量：TimeSource / TimeMark / ComparableTimeMark / measureTime / TestTimeSource
// 全打 1.9 戳（切片复核：awk -F'\t' '$3 ~ /^(TimeSource|TimeMark|ComparableTimeMark|measureTime|TestTimeSource)$/{print $1"|"$2"|"$3"|"$4}' docs/_data/slices/kotlin_time.tsv）
// 1.3 的 kotlin.time.MonotonicTimeSource 现在已经是 internal：
// 源码里 import 它会报 Cannot access 'object MonotonicTimeSource : TimeSource.WithComparableMarks': it is internal in file.
// （和 ReadAfterEOFException 一样：字节码 public、元数据 internal —— javap 看得见，Kotlin 用不了）

fun main() {
    // 1) 单调源 + markNow/elapsedNow
    val source = TimeSource.Monotonic
    val mark = source.markNow()
    Thread.sleep(15)
    val elapsed: Duration = mark.elapsedNow()
    check(elapsed.inWholeMilliseconds >= 10)
    check(mark.hasPassedNow())
    check(!mark.hasNotPassedNow())

    // 2) 顶层 measureTime 用的就是 TimeSource.Monotonic
    val spent = measureTime { Thread.sleep(10) }
    check(spent.inWholeMilliseconds >= 5)

    // 3) TimeSource.measureTime 也是返回 Duration（不是 Pair，切片签名 `fun TimeSource.measureTime(block): Duration`）
    val viaReceiver = source.measureTime { Thread.sleep(3) }
    check(viaReceiver >= Duration.ZERO)

    // 4) ComparableTimeMark：Monotonic 的 mark 可比较、可减出 Duration
    val earlier = source.markNow()
    val later = source.markNow()
    check(later > earlier)
    check((later - earlier) >= Duration.ZERO)
    check((earlier - later).isNegative())

    // 5) 虚拟时钟：TestTimeSource 用 += 前进（没有 advance() 这个方法，实测 Unresolved reference 'advance'）
    val ts = TestTimeSource()
    val t0 = ts.markNow()
    ts += 250.milliseconds
    check(t0.elapsedNow() == 250.milliseconds)
    check(ts.markNow() > t0)
    check((ts.markNow() - t0) == 250.milliseconds)

    // 6) 与 java.time 的关系：这里不引 Clock，1.9 的 TimeSource 是独立抽象（Instant/Clock 是 2.1 的档）
    check(1.seconds > 500.milliseconds)

    println("time source(1.9) OK: elapsed=${elapsed.inWholeMilliseconds}ms spent=${spent.inWholeMilliseconds}ms virtual=${t0.elapsedNow()}")
}
