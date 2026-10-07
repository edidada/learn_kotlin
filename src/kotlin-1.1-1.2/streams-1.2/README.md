# streams-1.2

归属：[实] 1.2

要覆盖：`kotlin.streams` 包（`asStream`/`asSequence`/`toList`）是 Java Stream 互操作的最早一层。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="asStream" || $3=="asSequence"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_streams.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.streams.Streams12Kt`）

- `asStream()` 的接收者只有 `Sequence<T>`，不含 `Iterable`：`listOf("a","b","c").asStream()` 实测报 `Unresolved reference. None of the following candidates is applicable because of a receiver type mismatch: fun <T> Sequence<T>.asStream(): Stream<T>`，必须先 `.asSequence()`。
- 双向转换可链式：`sequenceOf(1,2,3,4).asStream().filter { it % 2 == 0 }.asSequence().toList() == [2,4]`。
- `IntStream.range(0,10).filter { ... }.asSequence().sum() == 20`——基本类型流的 `asSequence` 帮你把 int 装箱成 `Int`，省掉 Java 的 `boxed()`。
- `kotlin.streams` 的扩展不自动导入：要写 `import kotlin.streams.asSequence` / `import kotlin.streams.asStream`。
- 命名撞车实测：JDK 16+ 给 `Stream` 加了成员 `toList()`，成员优先于 1.2 的扩展，所以 `listOf(1,2,3).asSequence().asStream().toList()` 走的是 Java 成员版（结果仍是 `[1,2,3]`）。这就是 2.x 里 `kotlin.streams.toList()` 看起来没生效的原因。
