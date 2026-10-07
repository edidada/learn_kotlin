package learn.kotlin1416.dur

import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlin.time.toJavaDuration
import kotlin.time.toKotlinDuration

// Duration / DurationUnit / toDuration 全是 1.6 戳（@ExperimentalTime 在 1.6 就结束了）。
// 复核：awk -F'\t' '$1=="1.6" && $2=="class"{print $3}' docs/_data/slices/kotlin_time.tsv
fun main() {
    val d = 500.milliseconds
    check(d.inWholeMilliseconds == 500L)              // inWhole* 一律返回 Long
    check(d.toString() == "500ms")                    // 人类可读形式
    check(d.toIsoString() == "PT0.500S")              // ISO-8601：实测毫秒位补零到三位
    check(1.seconds.toIsoString() == "PT1S")           // 整秒不留小数
    check(90.seconds.toIsoString() == "PT1M30S")       // 自动进位到分
    check(2.seconds > d)
    check((2.seconds - d).inWholeMilliseconds == 1500L)
    check((1.minutes + 30.seconds).inWholeSeconds == 90L)

    // 单位换算两种写法
    check(3.toDuration(DurationUnit.MINUTES).inWholeSeconds == 180L)
    check(2.hours.inWholeMinutes == 120L)
    check(d.toDouble(DurationUnit.SECONDS) == 0.5)

    // 解析
    check(Duration.parse("PT1M30S").inWholeSeconds == 90L)
    check(Duration.parseOrNull("not-a-duration") == null)
    val bad = runCatching { Duration.parse("PT") }.exceptionOrNull()
    check(bad is IllegalArgumentException)

    // 排序与零值（Duration 是 Comparable）
    check(listOf(1.seconds, 500.milliseconds, 2.seconds).sorted().first() == 500.milliseconds)
    check(Duration.ZERO == 0.milliseconds)
    check((-1).seconds.isNegative() && 1.seconds.isPositive())
    check((2.seconds).coerceIn(1.seconds, 3.seconds) == 2.seconds)

    // 与 java.time 互转（1.6）
    val jd: java.time.Duration = d.toJavaDuration()
    check(jd == java.time.Duration.ofMillis(500))
    check(jd.toKotlinDuration() == d)
    // 实测互不模仿：同一个 500ms，Kotlin 出 PT0.500S，java.time 出 PT0.5S
    check(jd.toString() == "PT0.5S")

    // 观察：Duration 本身是 value class，底层是 Long（纳秒）
    println("duration(1.6) OK: iso=${d.toIsoString()} java=$jd class=${Duration::class.java.name}")
}
