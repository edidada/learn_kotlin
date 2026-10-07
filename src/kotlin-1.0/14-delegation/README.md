# 14-delegation

归属：[文] 1.0

要覆盖：by 表达式、`lazy`/`observable`/`vetoable` 三个标准属性委托。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin10.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin10.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10，跑 `delegation.kt` 得到）

- `by lazy` 的初始化 lambda 只执行一次：两次读 `lazyValue` 只打印一行"真的被构造了"，且 `===` 为 true。
- `LazyThreadSafetyMode.NONE` 下局部委托属性同样只算一次（实测 `creations=1`）——别把 NONE 理解成"每次都算"。
- `observable` 是**先写入再回调**（`0 -> 5` 打印后读到的就是 5）；`vetoable` 返回 false 时写入被丢弃（`positive` 仍是 1）。
- 自定义委托可以偷偷变换值：写入 21，读回 42。
- map 委托把属性直接落到 map：`{name=ada, age=36}`。
