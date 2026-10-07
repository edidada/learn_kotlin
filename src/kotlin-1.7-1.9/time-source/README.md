# time-source

归属：[实] 1.9

要覆盖：时间测量在 1.9 被整体重写：`TimeSource`/`TimeMark`/`ComparableTimeMark`/`measureTime`/`TestTimeSource` 全打 1.9 戳，替代 1.3 的 `ExperimentalTime`/`MonotonicTimeSource`。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="TimeSource" || $3=="TimeMark" || $3=="measureTime" || $3=="TestTimeSource" || $3=="MonotonicTimeSource"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_time.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.timesrc.TimeSourceKt`）

输出（耗时随机器波动）：`time source(1.9) OK: elapsed=16ms spent=11ms virtual=250ms`

- `TimeSource.Monotonic` + `markNow()/elapsedNow()/hasPassedNow()/hasNotPassedNow()` 是主用法；`elapsedNow()` 返回 `Duration`，比较用 `inWholeMilliseconds`。
- 顶层 `measureTime { }` 用的就是 `TimeSource.Monotonic`；`source.measureTime { }`（receiver 版）**返回 `Duration`，不是 Pair** —— 切片签名 `fun TimeSource.measureTime(block: () -> R): Duration` 里那个 `R` 是被丢弃的，想拿返回值只能自己用变量接。
- `ComparableTimeMark` 可比较、可减：`later > earlier`、`(later - earlier) >= Duration.ZERO`，反过来减得到**负** Duration（`isNegative()` true），不是抛异常。
- **坑**：`TestTimeSource` 前进时钟用 `+=`（`ts += 250.milliseconds`），**没有 `advance()` 方法** —— 实测 `Unresolved reference 'advance'`。虚拟时间完全确定：`t0.elapsedNow() == 250.milliseconds` 精确相等，所以适合写测试断言。
- **坑**：1.3 时代的 `kotlin.time.MonotonicTimeSource` 现在源码里不可用：
  `Cannot access 'object MonotonicTimeSource : TimeSource.WithComparableMarks': it is internal in file.`
  而 `javap` 在 jar 上看得见这个 public 类 —— 和 `ReadAfterEOFException` 一样是"字节码 public / 元数据 internal"的同一类现象。
- `Instant`/`Clock` 不在本档（那是 2.1 的 `kotlin.time` 扩展，见 `src/kotlin-2.1/`），1.9 的 `TimeSource` 是独立抽象。
