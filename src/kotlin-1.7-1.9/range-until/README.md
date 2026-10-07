# range-until

归属：[实] 1.9

要覆盖：`until`（`rangeUntil`）与 `OpenEndRange`；对比 `..<` 与 `downTo` 的边界行为。实测：`println(0..<5)` 输出的是 `0..4`——常量区间被折成 `IntRange`，别拿 `toString` 判断开闭；`5 in 0..<5` 为 false、`4 in 0..<5` 为 true（见本目录 `rangeUntil.kt`，`./gradlew run -PmainClass=learn.kotlin1719.range.RangeUntilKt` 可直接跑）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="rangeUntil" || $3=="OpenEndRange" || $3=="endExclusive"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_ranges.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
