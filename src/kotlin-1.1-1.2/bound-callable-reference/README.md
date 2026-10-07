# bound-callable-reference

归属：[文] 1.1

要覆盖：绑定接收者的函数引用 `str::trim`、`::` 运算符、可引用属性与构造器。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.refs.CallableRefsKt`）

- 这是语言特性，stdlib 切片里查不到声明，所以证据只能来自编译器：下面 7 条都是 2.1.10 实跑通过的。
- 绑定引用把接收者固定掉，参数表少一个：`val add: (Int) -> Int = c::add`，且 `add(2) == 3` 同时改掉了 `c.count`（引用共享实例状态）。
- 同一成员的非绑定引用是 `KFunction2<Counter, Int, Int> = Counter::add`——接收者降级成第一个参数。
- 绑定引用还能当 `KFunction1<Int, Int>` 用（`fn(1) == 4`），说明 `(Int) -> Int` 与 `KFunction1` 在这条链上可互换。
- 属性引用同样能绑定：`val prop: KProperty0<Int> = c::count`，`prop.get() == 3`、`prop.name == "count"`；任意接收者都行，`"kotlin"::length` 得到 6。
- 构造器引用可以当函数类型：`val mk: (Int) -> Counter = ::Counter`，`mk(7).count == 7`。
- 顶层函数引用喂高阶函数：`listOf(1, 2, 3).map(::twice) == listOf(2, 4, 6)`。
