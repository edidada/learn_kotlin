package learn.kotlin1112.chunkwin

// 实测：chunked / windowed / zipWithNext 在 Iterable、Array、CharSequence、Sequence 上的
// 重载全都带 @SinceKotlin("1.2")，是 1.2 成批补齐集合 API 的一部分。
// 复核：awk -F'\t' '$1=="1.2" && $3 ~ /^(chunked|windowed|zipWithNext)/{print $3}' docs/_data/slices/kotlin_collections.tsv | sort -u

fun main() {
    val xs = (1..7).toList()

    // 1) chunked：分块，最后一块允许不足
    check(xs.chunked(3) == listOf(listOf(1, 2, 3), listOf(4, 5, 6), listOf(7)))
    // 带 transform 的重载直接产出结果，省掉中间 List
    check(xs.chunked(3) { it.sum() } == listOf(6, 15, 7))

    // 2) windowed：滑动窗口，默认只保留完整窗口
    check(xs.windowed(3).size == 5)
    check(xs.windowed(3).first() == listOf(1, 2, 3))
    check(xs.windowed(3, step = 2) == listOf(listOf(1, 2, 3), listOf(3, 4, 5), listOf(5, 6, 7)))
    check(xs.windowed(4, partialWindows = true).last() == listOf(7))
    check(xs.windowed(3) { (a, b, c) -> a * 100 + b * 10 + c } == listOf(123, 234, 345, 456, 567))

    // 3) CharSequence 版（同样 1.2）
    check("abcdefgh".chunked(3) == listOf("abc", "def", "gh"))
    check("abcde".windowed(2) == listOf("ab", "bc", "cd", "de"))

    // 4) zipWithNext：相邻配对，长度 n-1
    check(xs.zipWithNext { p, q -> q - p } == listOf(1, 1, 1, 1, 1, 1))
    check("abcd".zipWithNext().map { (a, b) -> "$a$b" } == listOf("ab", "bc", "cd"))

    // 5) Sequence 版实测返回 Sequence<List<T>>，所以能惰性分块后再 map
    val lazySums = xs.asSequence().chunked(2).map { it.sum() }.toList()
    check(lazySums == listOf(3, 7, 11, 7))

    // 6) 边界：size 必须 >= 1，否则抛 IllegalArgumentException（实测异常类型）
    val bad = runCatching { xs.chunked(0) }.exceptionOrNull()
    check(bad is IllegalArgumentException)

    println("chunked/windowed(1.2) OK: ${xs.chunked(3)}")
}
