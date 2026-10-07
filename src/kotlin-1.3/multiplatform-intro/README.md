# multiplatform-intro

归属：[文] 1.3

要覆盖：commonMain/commonTest 源集、`expect`/`actual`、expect/actual stdlib。从 1.1–1.2 档挪来：能真正跑起来的 MPP 形态是 1.3 才成型的。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin13.mpp.PlatformShapeKt`）

- 探针原文：`'expect' and 'actual' declarations can be used only in multiplatform projects. Learn more about Kotlin Multiplatform: https://kotl.in/multiplatform-setup`。这一行就是"1.3 才成型"的最硬证据——expect/actual 是编译器按源集结构（commonMain + 各 target）启用的能力，单 JVM 模块连关键字都不给用。
- 探针位置有讲究：临时文件必须放进 build.gradle 已注册的 `src/kotlin-*` 目录（这次用的是 `src/kotlin-1.3/_probe/`）；放 `src/_probe/` 时 `compileKotlin` 一条错误都不出，因为压根没参与编译。
- 本档不引 MPP 插件，改用 JVM 等价物演示"形状"：`interface PlatformContract`（common 侧只声明）+ `class JvmPlatform`（平台侧给实现），实测 `name = jvm`、`lineSeparator = [\r, \n]`。
- 本质差别写进代码注释里：interface 的约定发生在类型系统/运行期，忘了实现照样能跑起来才发现；expect/actual 是编译期强制"每个 target 都必须有 actual"，缺一即报错。
