# type-alias

归属：[文] 1.1

要覆盖：`typealias`：给函数类型和嵌套泛型起别名，注意它是纯语法别名不做类型区分。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.aliases.TypeAliasKt`）

- 坑（编译器给的）：包名不能叫 `typealias`。`package learn.kotlin1112.typealias` 报 `Syntax error: Package name must be a '.'-separated identifier list`，因为它是硬关键字；本目录包名改成 `learn.kotlin1112.aliases`。
- 别名不创建新类型：`Registry` 与展开形式 `MutableMap<String, MutableList<(String) -> Unit>>` 双向可赋值，实测通过。
- 1.1 的内置 JVM 别名是真别名不是包装：`val l: IntList = arrayListOf(1, 2)` 赋给 `java.util.ArrayList<Int>` 后 `javaList === l`（同一个对象）。
- `kotlin.Comparator` 同理是 `java.util.Comparator` 的别名，所以 `Comparator { a, b -> a - b }` 的 SAM 构造可直接用，`sortedWith(cmp)` 结果正确。
