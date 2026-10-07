# suspend-conversion

归属：[文] 1.4

要覆盖：`suspend` 函数引用与 `suspend` lambda 转换；这一档开始能理解 CPS 的入口。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.suspendconv.SuspendConversionKt`）

通过，输出：`suspend conversion(1.4) OK: out=describe x`。

- 转换方向是单向的（探针原文）：普通函数引用可以贴到 suspend 类型上（`val lifted: suspend () -> String = ::plainSide` 编译通过），反过来不行：
  `val bad: () -> String = ::upper` → `Initializer type mismatch: expected 'kotlin.Function0<kotlin.String>', actual 'kotlin.reflect.KSuspendFunction0<kotlin.String>'.`
  直接调用挂起引用则是 `Suspend function 'suspend fun invoke(): String' should be called only from a coroutine or another suspend function.`
- 高阶函数的形参写成 `suspend () -> T` 之后，调用点可以直接传挂起 lambda，不需要 kotlinx-coroutines：stdlib 的 `startCoroutine` + 一次性 `Continuation(EmptyCoroutineContext) { r -> ... }` 就能把结果落地（`kotlin.coroutines` 那几个符号 1.3 就有，见 version/1.3 的 coroutines-infra）。
- 手撸的 `runBlockingLike` 只在"这段挂起代码实际不会挂起"时成立：它靠 resumeWith 同步写回 result，没有调度器、没有事件循环。别把它当 `kotlinx.coroutines.runBlocking` 用，切片复核：`awk -F'\t' '$1=="1.3" && $2=="interface" && $3 ~ /^(Continuation|CoroutineContext)$/{print $3}' docs/_data/slices/kotlin_coroutines.tsv`。
- 观察：`suspend lambda` 在 JVM 上就是多带一个 `Continuation` 参数的方法，`runBlocking(ref)` 这种"同步执行到底"的写法能拿到 `KOTLIN`，说明没有真正的挂起点时状态机不切换线程。
