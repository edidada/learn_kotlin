package learn.kotlin1719.range

// Kotlin 1.9：until / rangeUntil（OpenEndRange）。
// 归属：[实] 1.9 —— `rangeUntil` 与 `OpenEndRange` 在 stdlib 里都带 @SinceKotlin("1.9")，
// 复核：awk -F'\t' '$3=="rangeUntil"{print $1"|"$6}' docs/_data/slices/kotlin_ranges.tsv
// 注意区分：中缀 `until`（Int/Long/Char/Short/Byte，切片里 since 为空即 1.0 就在）造的是**闭**区间；
// 1.9 的 `..<` / rangeUntil 造的才是**开**区间。
fun main() {
    val halfOpen = 0..<5                    // 1.9 语法糖，等价于 rangeUntil
    val closed = 0..4                       // 对照：闭区间一直都有
    val down = 5 downTo 1 step 2

    println("0..<5   = $halfOpen, size=${halfOpen.count()}")
    println("0..4    = $closed, size=${closed.count()}")
    println("5 downTo 1 step 2 = ${down.toList()}")
    println("5 in 0..<5 ? ${5 in 0..<5}")   // false：右端开区间
    println("4 in 0..<5 ? ${4 in 0..<5}")   // true

    check(5 !in halfOpen && 4 in halfOpen)
    check(halfOpen.count() == 5 && closed.count() == 5)
    check(halfOpen == closed)               // 值域相同，两都折成 IntRange
    check(halfOpen.first == 0 && halfOpen.last == 4)
    check(down.toList() == listOf(5, 3, 1))

    // rangeUntil 显式调用与 `..<` 等价。实测坑：`rangeUntil` **没有** @Infix，
    // 中缀写法 `0 rangeUntil 5` 报 'infix' modifier is required on 'FirNamedFunctionSymbol kotlin/Int.rangeUntil'，
    // 只有运算符形式 `..<` 能用。
    check(0.rangeUntil(5) == halfOpen)
    check(0.rangeUntil(5).toList() == listOf(0, 1, 2, 3, 4))

    // OpenEndRange：端点属性叫 endExclusive，不是 end
    val openRange: OpenEndRange<Int> = 0..<5
    check(openRange.start == 0 && openRange.endExclusive == 5)

    // 其它标量类型（1.9 的 rangeUntil 覆盖 Long/Double/Char，切片里 $3=="rangeUntil" 能看到 receiver 种类）
    val longOpen = 0L..<5L
    check(longOpen.last == 4L && 5L !in longOpen)          // Long 也折成 LongRange
    val dblOpen = 0.0..<5.0
    check(4.9999 in dblOpen && 5.0 !in dblOpen)            // Double 没有 last，只能按开区间判定
    val charOpen = 'a'..<'f'
    check('e' in charOpen && 'f' !in charOpen)             // Char 版同样开区间；判定只能用 `in`
    check('d' in 'a'..'f' && 'a' !in 'b'..'f')             // 对照：闭区间 `..`

    // 反向：空开区间
    check((5..<5).isEmpty())
    check(!(0..<1).isEmpty())

    // 步进与切片
    check((0..<10 step 3).toList() == listOf(0, 3, 6, 9))
    check((0..<5).reversed().toList() == listOf(4, 3, 2, 1, 0))
}
