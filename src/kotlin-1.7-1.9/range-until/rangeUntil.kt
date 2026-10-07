package learn.kotlin1719.range

// Kotlin 1.9：until / rangeUntil（OpenEndRange）。
// 归属：[实] 1.9 —— `rangeUntil` 与 `OpenEndRange` 在 stdlib 里都带 @SinceKotlin("1.9")，
// 复核：awk -F'\t' '$3=="rangeUntil"{print $1"|"$6}' docs/_data/slices/kotlin_ranges.tsv
fun main() {
    val halfOpen = 0..<5                    // 1.9 语法糖，等价于 rangeUntil
    val closed = 0..4                       // 对照：闭区间一直都有
    val down = 5 downTo 1 step 2

    println("0..<5   = $halfOpen, size=${halfOpen.count()}")
    println("0..4    = $closed, size=${closed.count()}")
    println("5 downTo 1 step 2 = ${down.toList()}")
    println("5 in 0..<5 ? ${5 in 0..<5}")   // false：右端开区间
    println("4 in 0..<5 ? ${4 in 0..<5}")   // true
}
