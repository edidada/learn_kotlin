# sam-conversion

归属：[文] 1.4

要覆盖：SAM 转换：Kotlin 函数式接口与 Java 接口的互转规则，`-Xsam-conversions` 的 class/invoke 两态。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.sam.SamConversionKt`）

通过，输出：`sam OK: Runnable impl = learn.kotlin1416.sam.SamConversionKt$$Lambda$9/0x0000025ae6005950`。

- 位置决定一切：Java 函数式接口在**实参位置**能直接收 lambda（`runLaterRunnable { check(true) }`、`sortedWith { x, y -> ... }`），在**声明/赋值位置**必须写 SAM 构造器。两条探针原文：
  - `val asAssignment: Runnable = { println("hi") }` → `Initializer type mismatch: expected 'java.lang.Runnable', actual 'kotlin.Function0<kotlin.Unit>'.`
  - `val callableAssign: Callable<Int> = { 1 }` → `Initializer type mismatch: expected 'java.util.concurrent.Callable<kotlin.Int>', actual 'kotlin.Function0<kotlin.Int>'.`
- `-Xsam-conversions` A/B（本机实测，推翻"class 是默认"这个常见说法）：
  - 默认编译：`Runnable { }` 的 javaClass = `SamConversionKt$$Lambda$9/0x...` → indy（invokedynamic + lambda proxy）。
  - 加 `-Xsam-conversions=class`（临时 init script 注入 freeCompilerArgs）后同一行 = `SamConversionKt$main$probe$1` → 匿名内部类，且 `javaClass.simpleName` 变成空串。
  - 结论：形态可切，但**别把 SAM 实例的类名写进断言**，换编译器开关就变。
- Kotlin 侧接口要参与 SAM 必须显式 `fun interface`（见隔壁 fun-interface 篇的 `does not have constructors` 原文）。
- 形参位置上的 `() -> Unit` → `Runnable` 也是转换，`Runnable(block).run()` 实测可用。
