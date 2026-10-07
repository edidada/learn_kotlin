# stdlib/auto-closeable

归属：[实] 2.0

要覆盖：`AutoCloseable` 进 common + `use` 扩展（2.0 戳），JVM 的 `Closeable.use` 是 1.0 老前辈。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="AutoCloseable" || $3=="use"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin20.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin20.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin20.autoclose.AutoCloseableKt`）

输出：`AutoCloseable/use(2.0) OK: jvm=java.lang.AutoCloseable order=[o, i, close:inner, close:outer] suppressed=1`

- 归属复核（切片）：`2.0 expect interface AutoCloseable` / `2.0 expect inline fun AutoCloseable(crossinline closeAction: () -> Unit)` / `2.0 expect inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R)`；
  JVM 侧 `2.0 actual typealias AutoCloseable = java.lang.AutoCloseable` —— 实测 `AutoCloseable::class.java.name == "java.lang.AutoCloseable"`。
  同一条查询还有个容易混的行：`1.2` 那条是 `Closeable.use` 的 actual，**不是** 2.0 这份 common `use`。
- `use` 的四条语义全部量过：
  1. 正常路径：块先执行、`close` 后执行，返回值就是块的返回值（`[body:a, close:a]`）。
  2. 可空接收者：`null.use { ... }` 不抛不关，块照跑（`[body:null]`，返回值 7）。
  3. 块抛异常：`close` 仍然执行，异常原样抛出（`[close:b]` + `IllegalArgumentException`）。
  4. 块和 `close` 都抛：**块里的异常是主异常**，`close` 的异常进 `suppressed`（实测 `primary.suppressed.map{it.message} == ["close failed"]`）；只有 `close` 抛时，异常直接来自 `close` 且 `suppressed` 为空。
- 嵌套 `use` 的关闭顺序是后开先关：`[o, i, close:inner, close:outer]`。
- 写样本时踩到的坑：`use` 有 `<T, R>` 两个类型参数，只写 `<Int>` 会被当成 **T**，于是报
  `Cannot infer type for this parameter. Please specify it explicitly.` 和
  `Argument type mismatch: actual type is 'java.io.Closeable?', but 'java.lang.AutoCloseable' was expected.`
  块体只抛异常时推断出 `Nothing` 也别扭，正确写法是把期望类型给外层：`runCatching<Int> { res.use { throw IllegalArgumentException("body") } }`。
