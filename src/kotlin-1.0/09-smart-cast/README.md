# 09-smart-cast

归属：[文] 1.0

要覆盖：`is` 后的自动收窄、`&&`/`||` 里的智能转换、可变属性为何转不了。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin10.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin10.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10，跑 `smartCast.kt` 得到）

- 类默认 final：`class Num : Expr()` 在 `Expr` 没写 `open`/`sealed` 时直接编译失败（`This type is final, so it cannot be extended`），这条是 1.0 的默认封闭性。
- 同模块 `val x: Any?` 属性 `is String` 之后可直接 `.uppercase()`，无需强转。
- 带自定义 getter 的 `val x: Any? get() = ...` **不能**智能转换，`.length` 报 Unresolved reference；把值先拷进局部 `val` 就恢复可转换——这是标准解法。
- 局部 `var` 在同一流程段内可以智能转换，函数后面才重新赋值不影响这段判断。
