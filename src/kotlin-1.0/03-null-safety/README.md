# 03-null-safety

归属：[文] 1.0

要覆盖：可空类型、`?.`/`?:`/`!!`、`lateinit`、平台类型的边界。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin10.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin10.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10，跑 `nullSafety.kt` 得到）

- `a ?: run{...}` 的右侧确实惰性：只有 `a` 为 null 时 `elseBranchRan` 才 +1，实测值 1。
- `null!!` 抛的是普通 `NullPointerException`，**message 为 null**。1.0 时代是 `KotlinNullPointerException`，1.4 起换掉——这条只能实测，背下来的都是旧知识。
- 未赋值的 `lateinit var` 读出来是 `UninitializedPropertyAccessException: lateinit property name has not been initialized`。
