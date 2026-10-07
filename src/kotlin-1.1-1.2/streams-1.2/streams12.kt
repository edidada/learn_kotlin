package learn.kotlin1112.streams

import java.util.stream.Collectors
import java.util.stream.IntStream
import kotlin.streams.asSequence
import kotlin.streams.asStream

// 1.2 新开 kotlin.streams 包：Sequence<T>.asStream()、Stream<T>.asSequence()、
// IntStream/LongStream/DoubleStream 的 asSequence()/toList()（切片 since 全是 1.2）。
// 复核：awk -F'\t' '$1=="1.2"{print $3"|"$4}' docs/_data/slices/kotlin_streams.tsv | sort -u

fun main() {
    // 1) 双向转换：Sequence -> java.util.stream -> Sequence
    val roundTrip = sequenceOf(1, 2, 3, 4).asStream().filter { it % 2 == 0 }.asSequence().toList()
    check(roundTrip == listOf(2, 4))

    // 2) IntStream -> List（kotlin.streams 的 1.2 扩展）
    val fromRange: List<Int> = IntStream.rangeClosed(1, 5).asSequence().toList()
    check(fromRange == listOf(1, 2, 3, 4, 5))

    // 3) 基本类型流的 asSequence 会把 int 自动装箱成 Int
    val even = IntStream.range(0, 10).filter { it % 2 == 0 }.asSequence().sum()
    check(even == 20)

    // 4) Java 收集器 + Kotlin lambda 混用
    val joined = listOf("a", "b", "c").asSequence().asStream().map { it.uppercase() }
        .collect(Collectors.joining("-"))
    check(joined == "A-B-C")

    // 5) 实测坑：JDK 16+ 给 Stream 加了同名成员 toList()，成员优先于 1.2 的扩展
    val viaMember = listOf(1, 2, 3).asSequence().asStream().toList()
    check(viaMember == listOf(1, 2, 3))

    println("streams(1.2) OK: $joined $viaMember")
}
