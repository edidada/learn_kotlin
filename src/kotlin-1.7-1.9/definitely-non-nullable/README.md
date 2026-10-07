# definitely-non-nullable

归属：[文] 1.7

要覆盖：`T!!`（definitely non-nullable 类型）：`@JvmField val x: String!!`；与 `lateinit` 的分工。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.dnn.DefinitelyNonNullableKt`）

输出：`definitely-non-nullable(1.7, 语法 T & Any) OK:   c lateinitGuard=UninitializedPropertyAccessException`

- **先更正归属语法**：这个特性写的是 `T & Any`，**不是 `T!!`**。在 2.1.10 上 `String!!`、`lateinit var v: String!!`、`external var f: String!!` 一律报
  `Syntax error: Property getter or setter expected.`（`-Plv=1.9` 走 K1 也是同一句）。`!!` 从来只是后置运算符，不是类型语法。
- 交集类型的限制（探针原话）：`val x: String & Any = "a"` →
  `Intersection types are supported only for definitely non-nullable types: left part should be a type parameter with nullable bounds.`
  也就是说左侧必须是**可空上界的类型参数**；`fun <T : Number> f(y: T & Any)` 同样被拒（上界已经非空）。
- 语言版本门槛：`-Plv=1.6` 下直接
  `The feature "definitely non nullable types" is only available since language version 1.7`。
- 可用的三处真实写法（本样本都编过并跑过）：`fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y`、`Comparator<T & Any>` 形参、`fun <T> Bag<T & Any>.lengthOf()` 扩展接收者。
- 行为核对：`elvisLike("", "b") == ""`（左值非空就直接返回左值，空串也算非空）、`elvisLike("k","b")=="k"`、`elvisLike<String?>(null, "c")` 返回值可直接当 `String` 用（`length == 1`），不需要 `!!`。
- 传可空实参会在**编译期**被拦：`Argument type mismatch: actual type is 'kotlin.String?', but 'T & Any' was expected.` —— 所以这是"写不出来的代码"，不是运行期异常，样本里只留注释。
- 与 `lateinit` 的分工（本档关注点）：`lateinit` 是"值在别处赋"，失败在运行期，实测 `UninitializedPropertyAccessException` + message `lateinit property name has not been initialized`；`T & Any` 是类型层面的非空承诺，失败在编译期。
