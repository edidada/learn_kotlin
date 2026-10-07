package learn.kotlin1719.minmax

// 1.7 把 min/max 一族补齐成"非空返回 + 空集合抛异常"的形态（切片复核：
// awk -F'\t' '($3=="max"||$3=="min") && $4=="-"{print $1"|"$6}' docs/_data/slices/kotlin_collections.tsv | grep 'Iterable<T>'
//   -> 无 since 的是 `Iterable<T>.max(): T?`（老的），1.7 起多了 `Iterable<T>.max(): T`
// awk -F'\t' '$1=="1.7" && $3 ~ /^(max|min|maxBy|minBy|maxWith|minWith)$/{print $3"|"$4}' docs/_data/slices/kotlin_collections.tsv | sort -u
//   -> 数组版（IntArray/LongArray/.../UIntArray/UShortArray）也全在 1.7）
// 对照：可空版 maxOrNull/minOrNull/maxByOrNull 是 1.4 就有的（$3 ~ /OrNull$/ 查得到 1.4）。

fun main() {
    val ints = listOf(3, 1, 2)
    val strs = listOf("a", "bb", "ccc")

    // 1) 非空版：正常集合直接给值，不再需要 !! 或 ?.
    check(ints.max() == 3)
    check(ints.min() == 1)
    check(strs.maxBy { it.length } == "ccc")
    check(strs.minWith(compareBy { it.length }) == "a")

    // 2) 空集合：非空版抛 NoSuchElementException，可空版给 null —— 这是 1.7 的分工
    //    实测异常**没有 message**（`message == null`），别照着老文档去断言 "Empty collection can't be reduced."
    val boom = runCatching { emptyList<Int>().max() }.exceptionOrNull()
    check(boom is NoSuchElementException)
    check(boom?.message == null)
    check(runCatching { emptyList<Int>().min() }.exceptionOrNull() is NoSuchElementException)
    check(runCatching { emptyList<Int>().maxBy { it } }.exceptionOrNull() is NoSuchElementException)
    check(runCatching { intArrayOf().max() }.exceptionOrNull() is NoSuchElementException)
    check(runCatching { sequenceOf<Int>().max() }.exceptionOrNull() is NoSuchElementException)
    check(emptyList<Int>().maxOrNull() == null)
    check(emptyList<Int>().minOrNull() == null)
    check(emptyList<String>().maxByOrNull { it.length } == null)

    // 3) 数组版同样是 1.7（含无符号数组）
    check(intArrayOf(5, 9, 2).max() == 9)
    check(longArrayOf(1L, 7L).min() == 1L)
    check(uintArrayOf(1u, 8u).max() == 8u)
    check(charArrayOf('z', 'a').max() == 'z')

    // 4) 序列也有；Map 的 maxBy 选择器作用在 Entry 上
    check(sequenceOf(1, 5, 3).max() == 5)
    val map = mapOf("a" to 3, "b" to 9)
    check(map.maxBy { it.value }.key == "b")
    check(map.maxBy { it.value }.value == 9)

    // 5) 双元素/多元素的 maxOf/minOf 早就有（1.1），不属于本档新增，放一起做对照
    check(maxOf(2, 7) == 7 && maxOf(2, 7, 5) == 7 && minOf(2, 7) == 2)

    // 6) NaN 的行为差异（实测）：max() 遇到 NaN 直接返回 NaN，maxOf 也返回 NaN
    check(listOf(1.0, Double.NaN, 3.0).max().isNaN())
    check(listOf(1.0, Double.NaN, 3.0).maxOrNull()?.isNaN() == true)

    println("min/max batch(1.7) OK: max=${ints.max()} maxBy=${strs.maxBy { it.length }} emptyGuard=${boom?.javaClass?.simpleName}")
}
