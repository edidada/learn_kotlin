# time-source

归属：[实] 1.9

要覆盖：时间测量在 1.9 被整体重写：`TimeSource`/`TimeMark`/`ComparableTimeMark`/`measureTime`/`TestTimeSource` 全打 1.9 戳，替代 1.3 的 `ExperimentalTime`/`MonotonicTimeSource`。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="TimeSource" || $3=="TimeMark" || $3=="measureTime" || $3=="TestTimeSource" || $3=="MonotonicTimeSource"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_time.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
