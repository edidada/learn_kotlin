# range-until

归属：[实] 1.9

要覆盖：`until`（`rangeUntil`）与 `OpenEndRange`；对比 `..<` 与 `downTo` 的边界行为。实测：`println(0..<5)` 输出的是 `0..4`——常量区间被折成 `IntRange`，别拿 `toString` 判断开闭；`5 in 0..<5` 为 false、`4 in 0..<5` 为 true（见本目录 `rangeUntil.kt`，`./gradlew run -PmainClass=learn.kotlin1719.range.RangeUntilKt` 可直接跑）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="rangeUntil" || $3=="OpenEndRange" || $3=="endExclusive"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_ranges.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.range.RangeUntilKt`）

输出：

```
0..<5   = 0..4, size=5
0..4    = 0..4, size=5
5 downTo 1 step 2 = [5, 3, 1]
5 in 0..<5 ? false
4 in 0..<5 ? true
```

- `0..<5` 的 `toString()` 打印成 **`0..4`**：它立刻折成了 `IntRange`（`halfOpen == closed` 为 true，`first=0 last=4`），开区间的"形状"只存在于类型检查期，运行期没有独立的 OpenIntRange 对象。
- 归属复核：`rangeUntil` 与 `OpenEndRange` 在 stdlib 都带 `@SinceKotlin("1.9")`；中缀 `until` 是 1.0 就在的**闭**区间构造器，两者不是一回事，切片里 `$3=="until"` 和 `$3=="rangeUntil"` 分开查。
- **坑**：`rangeUntil` **没有** `@Infix`，中缀写法 `0 rangeUntil 5` 报
  `'infix' modifier is required on 'FirNamedFunctionSymbol kotlin/Int.rangeUntil'`。
  能用的是点式 `0.rangeUntil(5)` 或运算符 `..<`，所以文档里"中缀 rangeUntil"那种写法是错的。
- `OpenEndRange` 的端点属性名是 `endExclusive`（`openRange.endExclusive == 5`），没有 `end`。
- 其它标量：`0L..<5L` 也折成 `LongRange`（`last == 4L`）；`0.0..<5.0` 是 `OpenEndRange<Double>`，没有 `last`，只能用 `in` 判定（`4.9999 in` true、`5.0 in` false）；Char 版 `'a'..<'f'` 同样开区间（`'e' in` true、`'f' in` false）。
- 空区间与步进：`(5..<5).isEmpty()` 为 true，`(0..<1)` 非空；`(0..<10 step 3).toList() == [0,3,6,9]`，`(0..<5).reversed()` 得到 `[4,3,2,1,0]`。
