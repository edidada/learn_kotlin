# math-batch

归属：[实] 1.2

要覆盖：`kotlin.math` 一次补了 40 多个函数：`nextUp`/`nextDown`/`nextTowards`/`pow`/`withSign`/`ln1p`/`IEEErem`，正好拿来理解浮点 ULP。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="nextUp" || $3=="nextDown" || $3=="nextTowards" || $3=="pow" || $3=="withSign" || $3=="ln1p"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_math.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.mathbatch.MathBatchKt`）

- 不 import 时的真实报错共 12 处 Unresolved reference，而且不一致：`sign`/`expm1`/`ln1p` 三行不需要 import 就能解析（kotlin 包里 1.2 的 `MathH.kt` 版本还活着），`log2`/`hypot`/`acosh`/`atanh`/`round`/`roundToInt` 与 `Double.pow`/`nextUp`/`nextDown`/`withSign` 必须从 `kotlin.math` 显式导入。结论：一律 `import kotlin.math.*`，别赌 kotlin 包里的老同名函数。
- 舍入是两套语义，实测有差：`round(2.5) == 2.0`（走 `Math.rint`，银行家舍入，.5 取偶）但 `2.5.roundToInt() == 3`（走 `Math.round`，.5 向上）；`round(3.5) == 4.0` 同时成立。
- `2.0.pow(10) == 1024.0`、`1.0.nextUp() > 1.0`、`1.0.nextDown() < 1.0`、`0.0.nextUp() > 0.0`（nextUp 在 0 处给最小正数，不是 0.0）。
- `(-1.0).toRawBits()` 与 `Double.fromBits(bits)` 往返无损。
- `withSign` 只改符号位：`3.0.withSign(-1.0) == -3.0`、`(-3.0).withSign(2.0) == 3.0`。
- 纠正了自己写的断言：`0.1.toBigDecimal() == "0.1".toBigDecimal()`（扩展实现是 `BigDecimal(this.toString())`，`scale == 1`），原以为能看出二进制误差是错的；要暴露误差得用 Java 构造器 `BigDecimal(0.1)`（实测 `scale > 1` 且 `compareTo("0.1".toBigDecimal()) > 0`）。
- 十进制算术才是它存在的理由：`"0.1".toBigDecimal() + "0.2".toBigDecimal() == BigDecimal("0.3")` 成立，而 Double 上 `0.1 + 0.2 == 0.3` 不成立。
- `BigInteger("99").inc() == BigInteger("100")`（1.2 给大数补的 `inc`/`dec`）。
