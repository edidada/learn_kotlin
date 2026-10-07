# trailing-comma

归属：[文] 1.4

要覆盖：尾逗号语法，练一次记一次它在多行实参里的排版收益。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.trailing.TrailingCommaKt`）

通过，输出：`trailing comma(1.4) OK: [a, b]`。

允许尾逗号的位置（逐个探针编过）：形参列表（含局部函数）、实参列表、集合/数组字面量、`mapOf(...)`/`setOf(...)` 这类多行调用、枚举条目、**类型实参列表** `listOf<String,>()`、以及**解构声明** `val (a, b,) = Pair(1, 2)`。

唯一实测**不**允许的位置：继承/实现列表 —— `class SuperTrailing : Runnable, { ... }` 报 `Syntax error: Type expected.`（外加一条 `Syntax error: Incomplete code.`）。

复现限制（重要）：2.1.10 编译器已不接受 1.3/1.4/1.5 的语言版本，`-Plv=1.4 -Pav=1.4` 直接 `Language version 1.4 is no longer supported; please, use version 1.6 or greater.`。所以本档只能在 ≥1.6 上验证"语法存在"，无法用它对比 1.4 当年刚落地时的行为差异。
