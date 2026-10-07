# fun-interface

归属：[文] 1.4

要覆盖：`fun interface` 单一抽象方法接口 + lambda 当作实现。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.funif.FunInterfaceKt`）

通过，输出：`fun interface(1.4) OK: same impl class = false, class = FunInterfaceKt$$Lambda$4/0x000002a584002e58`。

- "只有一个抽象方法"不再自动等于可 SAM：对照探针 `interface PlainOne2 { fun a(): Int }` + `val p = PlainOne2 { 1 }`，编译器原话 `Interface 'interface PlainOne2 : Any' does not have constructors.`。加了 `fun` 关键字才有那个合成的 SAM 构造器。
- SAM 构造器带泛型实参可用：`Converter<Int, String> { it.toString(2) }`；`in`/`out` 型变声明不影响转换。
- 函数引用的转换发生在**实参位置**：`consumeRef(::strLen)` 通过；写进声明位置就变成 sam-conversion 那篇记的 Initializer type mismatch。
- 对象表达式照旧合法，`object : Converter<String, Int>` 与 lambda 版本可混用。
- 运行期形态：`ClickHandler { _, _ -> }` 写两次、放在两个不同调用点，`a.javaClass == b.javaClass` 实测 **false**，类名是 `...$$Lambda$N/0x...`，即默认走 invokedynamic 生成 lambda proxy（跟 sam-conversion 那篇的 A/B 一致），所以别按"同一个内部类"去设计身份判断。
