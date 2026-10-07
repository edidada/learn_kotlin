# duration

归属：[实] 1.6

要覆盖：`Duration`/`DurationUnit`/`toDuration` 全是 1.6 戳（原方案标 1.3 有误）；练 `parse`/`toIsoString`/单位换算/超时相加。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="Duration" || $3=="DurationUnit" || $3=="toDuration"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_time.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.dur.DurationKt`）

通过，输出：`duration(1.6) OK: iso=PT0.500S java=PT0.5S class=kotlin.time.Duration`。

- 切片复核：`awk -F'\t' '$1=="1.6" && $2=="class" && $3=="Duration"{print $6}' docs/_data/slices/kotlin_time.tsv` —— 声明行是 `public value class Duration internal constructor(private val rawValue: Long)`，所以 Duration 本身就是 value class，底层 Long（纳秒），`Duration::class.java.name == "kotlin.time.Duration"`。
- `inWhole*` 一律返回 `Long`（`inWholeMilliseconds == 500L`），别写成 Int 比较；要小数就用 `toDouble(DurationUnit.SECONDS) == 0.5`。
- ISO 输出实测三条一起记：`500.milliseconds -> PT0.500S`、`1.seconds -> PT1S`、`90.seconds -> PT1M30S`（毫秒位补齐三位，整秒不留小数，会进位到分）。同一个 500ms，`toJavaDuration().toString()` 是 `PT0.5S` —— 两边格式互不模仿，跨库断言要各自写。
- `@ExperimentalTime` 到 1.6 就结束了（`Duration`/`DurationUnit`/`toDuration` 全是 1.6 戳），本档不需要 `@OptIn`。
- 解析：`Duration.parse("PT1M30S").inWholeSeconds == 90L`；`parseOrNull("not-a-duration") == null`；`parse("PT")` 抛的是 `IllegalArgumentException`（不是自定义异常）。
- Duration 是 `Comparable`，`listOf(1.seconds, 500.milliseconds, 2.seconds).sorted().first() == 500.milliseconds`、`coerceIn(1.seconds, 3.seconds)` 都可用；`Duration.ZERO == 0.milliseconds`；负值走 `isNegative()`。
- 与 java.time 互转（`toJavaDuration`/`toKotlinDuration`，切片 `1.6 fun toJavaDuration receiver=Duration`）往返无损。
