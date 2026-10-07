# Kotlin 标准库：kotlin.sequences / kotlin.ranges / kotlin.comparisons

> 本文所有 API 名与签名均来自本地 `kotlin-stdlib-2.2.10-sources.jar` 抽取结果（TSV 切片）与对应源文件，未凭记忆补充。
> 标注 `[JVM]` 的条目位于 `src/jvmMain/`，其余为 `src/commonMain/`（common）。`@SinceKotlin` 列为 `-` 表示源码中没有该注解。

---

## 一、kotlin.sequences：惰性序列

### 1.1 核心类型与创建入口

`Sequence<out T>` 本身只有一个成员：`public operator fun iterator(): Iterator<T>`（`commonMain/kotlin/collections/Sequence.kt:27`，包名 `kotlin.sequences`）。所有中间操作都只是"再包一层"，直到调用终端操作才真正驱动 `iterator()`。

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public interface Sequence<out T>` | `-` | 惰性元素序列，唯一抽象成员为 `iterator()` |
| `public fun <T> sequenceOf(vararg elements: T): Sequence<T>` | `-` | 由可变参数创建，实现为 `elements.asSequence()` |
| `public fun <T> sequenceOf(element: T): Sequence<T>` | `2.2` | 单元素序列重载 |
| `public inline fun <T> sequenceOf(): Sequence<T>` | `2.2` | 无参重载，实现为 `emptySequence()` |
| `public fun <T> emptySequence(): Sequence<T>` | `-` | 返回空序列对象（`EmptySequence`） |
| `public fun <T : Any> generateSequence(nextFunction: () -> T?): Sequence<T>` | `-` | 无种子，靠 `nextFunction` 返回 `null` 结束 |
| `public fun <T : Any> generateSequence(seed: T?, nextFunction: (T) -> T?): Sequence<T>` | `-` | 以 `seed` 为首个元素 |
| `public fun <T : Any> generateSequence(seedFunction: () -> T?, nextFunction: (T) -> T?): Sequence<T>` | `-` | 惰性求种子 |
| `public fun <T> sequence(@BuilderInference block: suspend SequenceScope<T>.() -> Unit): Sequence<T>` | `1.3` | 生成器构建序列（源码中的名字是 `sequence`，**不是** `buildSequence`） |
| `public fun <T> iterator(@BuilderInference block: suspend SequenceScope<T>.() -> Unit): Iterator<T>` | `1.3` | 同上的 `Iterator` 版本 |
| `public abstract class SequenceScope<in T> internal constructor()` | `1.3` | 生成器接收者，成员见下 |
| `public abstract suspend fun yield(value: T)` | `-` | 产出一个元素 |
| `public abstract suspend fun yieldAll(iterator: Iterator<T>)` | `-` | 产出一个迭代器的全部元素 |
| `public suspend fun yieldAll(elements: Iterable<T>)` | `-` | 产出集合元素 |
| `public suspend fun yieldAll(sequence: Sequence<T>)` | `-` | 产出另一个序列 |
| `public inline fun <T> java.util.Enumeration<T>.asSequence(): Sequence<T>` | `-` `[JVM]` | `Enumeration` 转序列 |
| `internal expect class ConstrainedOnceSequence<T> : Sequence<T>` | `-` | JVM 的 `actual` 实现用 `AtomicReference` 保证"只能消费一次"，第二次取元素抛 `IllegalStateException("This sequence can be consumed only once.")` |

```kotlin
// 示例 1：三种创建方式（sequenceOf / generateSequence / sequence 生成器）
val direct = sequenceOf(1, 2, 3)                       // sequenceOf(vararg)
val empty = sequenceOf<Int>()                          // 2.2 无参重载
val nothing = emptySequence<String>()

// generateSequence：nextFunction 返回 null 即停止
val powers = generateSequence(1) { if (it > 1000) null else it * 2 }
println(powers.toList())                               // [1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024]

// sequence { } 构建器（1.3），在 SequenceScope 上 yield / yieldAll
val fib = sequence {
    var a = 0L
    var b = 1L
    while (a < 50) {
        yield(a)
        val next = a + b
        a = b
        b = next
    }
}
val nested = sequence { yieldAll(listOf("a", "b")); yieldAll(sequenceOf("c")) }
println(fib.toList() to nested.toList())
```

### 1.2 中间操作（返回 `Sequence`，惰性）

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public fun <T, R> Sequence<T>.map(transform: (T) -> R): Sequence<R>` | `-` | 逐元素变换 |
| `public fun <T, R> Sequence<T>.mapIndexed(transform: (index: Int, T) -> R): Sequence<R>` | `-` | 带索引变换 |
| `public fun <T, R : Any> Sequence<T>.mapNotNull(transform: (T) -> R?): Sequence<R>` | `-` | 丢弃 `null` 结果 |
| `public fun <T, R> Sequence<T>.flatMap(transform: (T) -> Sequence<R>): Sequence<R>` | `-` | 展平嵌套序列 |
| `public fun <T, R> Sequence<T>.flatMap(transform: (T) -> Iterable<R>): Sequence<R>` | `1.4` | 展平嵌套可迭代对象 |
| `public fun <T, R> Sequence<T>.flatMapIndexed(transform: (index: Int, T) -> Sequence<R>): Sequence<R>` | `1.4` | 带索引的 flatMap |
| `public fun <T> Sequence<Sequence<T>>.flatten(): Sequence<T>` | `-` | 展平一层序列 |
| `public fun <T> Sequence<Iterable<T>>.flatten(): Sequence<T>` | `-` | 展平一层集合 |
| `public fun <T> Sequence<T>.filter(predicate: (T) -> Boolean): Sequence<T>` | `-` | 保留满足条件的元素 |
| `public fun <T> Sequence<T>.filterNot(predicate: (T) -> Boolean): Sequence<T>` | `-` | 取反过滤 |
| `public fun <T> Sequence<T>.filterIndexed(predicate: (index: Int, T) -> Boolean): Sequence<T>` | `-` | 带索引过滤 |
| `public inline fun <reified R> Sequence<*>.filterIsInstance(): Sequence<R>` | `-` | 按 reified 类型过滤 |
| `public fun <T> Sequence<*>.filterIsInstance(klass: Class<R>): Sequence<R>` | `-` `[JVM]` | 按 `Class` 过滤 |
| `public fun <T : Any> Sequence<T?>.filterNotNull(): Sequence<T>` | `-` | 去除可空元素 |
| `public fun <T> Sequence<T>.take(n: Int): Sequence<T>` | `-` | 取前 n 个 |
| `public fun <T> Sequence<T>.takeWhile(predicate: (T) -> Boolean): Sequence<T>` | `-` | 取满足条件的前缀 |
| `public fun <T> Sequence<T>.drop(n: Int): Sequence<T>` | `-` | 跳过前 n 个 |
| `public fun <T> Sequence<T>.dropWhile(predicate: (T) -> Boolean): Sequence<T>` | `-` | 跳过后缀前的连续段 |
| `public fun <T> Sequence<T>.distinct(): Sequence<T>` | `-` | 按 equals 去重 |
| `public fun <T, K> Sequence<T>.distinctBy(selector: (T) -> K): Sequence<T>` | `-` | 按 key 去重 |
| `public fun <T> Sequence<T>.sortedWith(comparator: Comparator<in T>): Sequence<T>` | `-` | 终端前排序（`toMutableList()` 后排序） |
| `public fun <T : Comparable<T>> Sequence<T>.sorted(): Sequence<T>` | `-` ※ | 自然排序 |
| `public inline fun <T, R : Comparable<R>> Sequence<T>.sortedBy(crossinline selector: (T) -> R?): Sequence<T>` | `-` ※ | 按选择器排序 |
| `public inline fun <T, R : Comparable<R>> Sequence<T>.sortedByDescending(crossinline selector: (T) -> R?): Sequence<T>` | `-` ※ | 降序 |
| `public fun <T : Comparable<T>> Sequence<T>.sortedDescending(): Sequence<T>` | `-` ※ | 自然降序 |
| `public fun <T> Sequence<T>.chunked(size: Int): Sequence<List<T>>` | `1.2` | 按 size 分块 |
| `public fun <T, R> Sequence<T>.chunked(size: Int, transform: (List<T>) -> R): Sequence<R>` | `1.2` | 分块并变换 |
| `public fun <T> Sequence<T>.windowed(size: Int, step: Int = 1, partialWindows: Boolean = false): Sequence<List<T>>` | `1.2` | 滑动窗口 |
| `public fun <T, R> Sequence<T>.windowed(size: Int, step: Int = 1, partialWindows: Boolean = false, transform: (List<T>) -> R): Sequence<R>` | `1.2` | 窗口并变换 |
| `public infix fun <T, R> Sequence<T>.zip(other: Sequence<R>): Sequence<Pair<T, R>>` | `-` | 配对合并 |
| `public fun <T, R, V> Sequence<T>.zip(other: Sequence<R>, transform: (a: T, b: R) -> V): Sequence<V>` | `-` | 自定义 zip 变换 |
| `public fun <T> Sequence<T>.zipWithNext(): Sequence<Pair<T, T>>` | `1.2` | 相邻元素配对 |
| `public fun <T, R> Sequence<T>.zipWithNext(transform: (a: T, b: T) -> R): Sequence<R>` | `1.2` | 相邻元素自定义变换 |
| `public fun <T> Sequence<T>.withIndex(): Sequence<IndexedValue<T>>` | `-` | 附加索引 |
| `public fun <T> Sequence<T>.onEach(action: (T) -> Unit): Sequence<T>` | `1.1` | 遍历时执行副作用 |
| `public fun <T> Sequence<T>.onEachIndexed(action: (index: Int, T) -> Unit): Sequence<T>` | `1.4` | 带索引的副作用 |
| `public fun <T> Sequence<T>.plus(element: T / elements: Iterable<T> / elements: Sequence<T>): Sequence<T>` | `-` | 多个 `plus` 重载（含 `Array<out T>`） |
| `public operator fun <T> Sequence<T>.minus(element: T): Sequence<T>` | `-` | 也有 `Array`/`Iterable`/`Sequence` 重载 |
| `public inline fun <T> Sequence<T>.plusElement(element: T): Sequence<T>` / `minusElement(element: T)` | `-` | 单元素增删 |
| `public fun <T> Sequence<T>.shuffled(): Sequence<T>` / `shuffled(random: Random)` | `1.4` | 洗牌 |
| `public fun <T> Sequence<T>.asIterable(): Iterable<T>` | `-` | 转回 `Iterable` |
| `public inline fun <T> Sequence<T>.asSequence(): Sequence<T>` | `-` | 自身转序列 |
| `public fun <T> Sequence<T>.ifEmpty(defaultValue: () -> Sequence<T>): Sequence<T>` | `1.3` | 空时改用备用序列 |
| `public inline fun <T> Sequence<T>?.orEmpty(): Sequence<T>` | `1.3` | `null` 转空序列 |

※ 标记行在 TSV 切片中缺失（抽取器漏掉了接收者带 `Comparable<...>` 约束的扩展函数），签名取自 `src/commonMain/generated/_Sequences.kt:587/607/621/632`。

```kotlin
// 示例 2：惰性 —— 中间操作不消耗元素，终端操作才驱动
fun main() {
    // 用 generateSequence 造一个"无限"源：集合版会立刻撑爆内存，序列版不会
    val source = generateSequence(1) { it + 1 }
    val pipeline = source
        .map { println("map $it"); it * 2 }
        .filter { it % 3 != 0 }
        .take(3)                                      // take 仍是中间操作，不触发计算
    println("流水线构造完毕，上面没有打印任何 map 日志")
    println(pipeline.toList())                         // 终端操作：只驱动到凑满 3 个元素

    val words = sequenceOf("kotlin", "is", "lazy", "and", "fast")
    println(words.chunked(2).toList())                 // [[kotlin, is], [lazy, and], [fast]]
    println(words.windowed(3, step = 2).toList())      // [[kotlin, is, lazy], [lazy, and, fast]]
    println(words.distinctBy { it.length }.toList())    // [kotlin, is, lazy]
    println(words.zip(words.shuffled()).count())        // 4（zip 以较短者为准）
    words.onEachIndexed { i, w -> if (i == 0) println("first=$w") }.toList()
    println(words.takeWhile { it.length != 3 }.count() to words.withIndex().count())
}
```

> 说明：`Iterable<T>.asSequence()`、`Iterator<T>.asSequence()` 的声明不在本切片（它们属 `kotlin.collections`，见 `kotlin_collections.tsv`），示例中仅作为已存在的语言惯用法使用。

### 1.3 终端操作（立即求值并返回非序列结果）

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public fun <T> Sequence<T>.toList(): List<T>` / `toMutableList(): MutableList<T>` | `-` | 物化为列表 |
| `public fun <T> Sequence<T>.toSet(): Set<T>` / `toMutableSet(): MutableSet<T>` / `toHashSet(): HashSet<T>` | `-` | 物化为集合 |
| `public fun <T> Sequence<T>.toSortedSet(comparator: Comparator<in T>): java.util.SortedSet<T>` | `-` `[JVM]` | 转为有序集 |
| `public inline fun <T, R> Sequence<T>.fold(initial: R, operation: (acc: R, T) -> R): R` | `-` | 左折叠 |
| `public inline fun <T, R> Sequence<T>.foldIndexed(initial: R, operation: (index: Int, acc: R, T) -> R): R` | `-` | 带索引折叠 |
| `public inline fun <S, T : S> Sequence<T>.reduce(operation: (acc: S, T) -> S): S` | `-` | 无初值折叠，空序列抛异常 |
| `public inline fun <S, T : S> Sequence<T>.reduceOrNull(operation: (acc: S, T) -> S): S?` | `1.4` | 空序列返回 `null` |
| `public inline fun <S, T : S> Sequence<T>.reduceIndexed / reduceIndexedOrNull` | `-` / `1.4` | 带索引版本 |
| `public fun <T, R> Sequence<T>.scan(initial: R, operation: (acc: R, T) -> R): Sequence<R>` | `1.4` | 返回中间累加结果序列 |
| `public fun <T, R> Sequence<T>.runningFold(initial: R, operation: (acc: R, T) -> R): Sequence<R>` | `1.4` | 同 scan 语义 |
| `public fun <S, T : S> Sequence<T>.runningReduce(operation: (acc: S, T) -> S): Sequence<S>` | `1.4` | 无初值的累加序列 |
| `public inline fun <T> Sequence<T>.forEach(action: (T) -> Unit): Unit` | `-` | 遍历（唯一"消费"式终端） |
| `public inline fun <T> Sequence<T>.forEachIndexed(action: (index: Int, T) -> Unit): Unit` | `-` | 带索引遍历 |
| `public inline fun <T> Sequence<T>.all(predicate: (T) -> Boolean): Boolean` | `-` | 全称判断 |
| `public fun <T> Sequence<T>.any(): Boolean` / `any(predicate)` | `-` | 存在判断 |
| `public fun <T> Sequence<T>.none(): Boolean` / `none(predicate)` | `-` | 全不满足 |
| `public fun <T> Sequence<T>.count(): Int` / `count(predicate)` | `-` | 计数 |
| `public fun <T> Sequence<T>.average(): Double` | `-` | `Byte/Short/Int/Long/Float/Double` 各一个重载 |
| `public inline fun <T> Sequence<T>.sum(): Int` / `sumBy(selector)` / `sumByDouble(selector)` | `-` | 旧式求和；`sumBy`/`sumByDouble` 已被 `sumOf` 取代 |
| `public inline fun <T> Sequence<T>.sumOf(selector: (T) -> Int/Long/Double): R` | `1.4` | 另有 `UInt/ULong`（1.5）与 `BigDecimal/BigInteger`（1.4，`[JVM]`）重载 |
| `public fun <T> Sequence<T>.maxWith(comparator: Comparator<in T>): T` / `minWith` | `1.7` | 按比较器取极值 |
| `public fun <T> Sequence<T>.maxWithOrNull / minWithOrNull` | `1.4` | 空序列返回 `null` |
| `public fun <T : Comparable<T>> Sequence<T>.max(): T` / `maxOrNull(): T?` / `min()` / `minOrNull()` | `-` ※ | 自然序极值；`[JVM]` 另有 `Sequence<Double>.max(): Double?`（1.1，1.7 起返回 `Double`） |
| `public inline fun <T, R> Sequence<T>.maxOf(selector: (T) -> R): R` / `maxOfWith` / `minOf` / `minOfWith` 及 `*OrNull` | `1.4` | `*OrNull` 系列；`Double/Float` 版在 common，`UInt/ULong` 版 1.5 |
| `public inline fun <T, R : Comparable<R>> Sequence<T>.maxBy(selector: (T) -> R): T` / `maxByOrNull` / `minBy` / `minByOrNull` | `-` ※ | 按选择器取元素 |
| `public inline fun <T, K, V> Sequence<T>.associate(transform: (T) -> Pair<K, V>): Map<K, V>` | `-` | 构建映射 |
| `public inline fun <T, K> Sequence<T>.associateBy(keySelector: (T) -> K): Map<K, T>`（另有 `valueTransform` 重载） | `-` | 按键建立映射 |
| `public inline fun <K, V> Sequence<K>.associateWith(valueSelector: (K) -> V): Map<K, V>` | `1.3` | 按值建立映射 |
| `public inline fun <T, K> Sequence<T>.groupBy(keySelector: (T) -> K): Map<K, List<T>>`（另有 `valueTransform` 重载） | `-` | 分组 |
| `public inline fun <T, K> Sequence<T>.groupingBy(crossinline keySelector: (T) -> K): Grouping<T, K>` | `1.1` | 返回 `Grouping`，配合 `eachCount` 等 |
| `public inline fun <T> Sequence<T>.partition(predicate: (T) -> Boolean): Pair<List<T>, List<T>>` | `-` | 二分 |
| `public fun <T, R> Sequence<Pair<T, R>>.unzip(): Pair<List<T>, List<R>>` | `-` | 解 zip |
| `public fun <T> Sequence<T>.joinToString(separator = ", ", prefix = "", postfix = "", limit = -1, truncated = "...", transform: ((T) -> CharSequence)? = null): String` | `-` | 拼接为字符串 |
| `public fun <T> Sequence<T>.first(): T` / `firstOrNull()` / `first(predicate)` / `firstOrNull(predicate)` | `-` | 取首元素 |
| `public fun <T> Sequence<T>.last(): T` / `lastOrNull(...)` | `-` | 取尾元素（需遍历到底） |
| `public fun <T> Sequence<T>.single(): T` / `singleOrNull(...)` / `single(predicate)` | `-` | 唯一元素判断 |
| `public fun <T> Sequence<T>.elementAt(index: Int): T` / `elementAtOrElse` / `elementAtOrNull` | `-` | 按下标取 |
| `public inline fun <T, R : Any> Sequence<T>.firstNotNullOf(transform: (T) -> R?): R` / `firstNotNullOfOrNull` | `1.5` | 首个非空变换结果 |
| `public inline fun <T> Sequence<T>.find(predicate: (T) -> Boolean): T?` / `findLast` | `-` | 条件查找 |
| `public fun <@OnlyInputTypes T> Sequence<T>.contains(element: T): Boolean` | `-` | `in` 运算符；`indexOf(element)` / `indexOfFirst` / `indexOfLast` / `lastIndexOf` 同批存在 |
| `public fun <T : Any> Sequence<T?>.requireNoNulls(): Sequence<T>` | `-` | 含 `null` 时抛异常 |

※ 该行的 `Comparable<...>` 约束版本未进入 TSV（源文件 `commonMain/generated/_Sequences.kt:1400/1430/1462/1549/1626/1747/1860/1890/1922/2009/2086/2207`）。

```kotlin
// 示例 3：终端操作与 fold/scan/partition/groupBy
data class Order(val id: Int, val owner: String, val amount: Int)

fun main() {
    val orders = sequenceOf(
        Order(1, "ann", 200), Order(2, "bob", 150), Order(3, "ann", 50)
    )
    val totals = orders.fold(0) { acc, o -> acc + o.amount }             // 400
    val running = orders.scan(0) { acc, o -> acc + o.amount }.toList()    // [0, 200, 350, 400]
    val byOwner = orders.groupBy { it.owner }                             // Map<String, List<Order>>
    val (keep, drop) = orders.partition { it.amount >= 150 }
    println(totals to running)
    println(byOwner.keys to keep.count() to drop.count())
    println(orders.joinToString(prefix = "[", postfix = "]", limit = 2, truncated = "…"))
    println(orders.map { it.amount }.average() to orders.sumOf { it.amount })
    println(orders.maxWithOrNull(compareBy { it.amount }) to orders.minOf { it.amount })
}
```

### 1.4 与 Iterable 的差异（仅依据本切片可见事实）

- 返回值类型：切片中序列版中间操作的返回类型一律是 `Sequence<...>`（如 `Sequence<T>.chunked(size: Int): Sequence<List<T>>`），而 `toList/toSet/groupBy/associate/partition/joinToString` 才返回具体集合或字符串——即"中间惰性、终端物化"。
- 无限源：`generateSequence` / `sequence { }` 没有任何长度限制参数，配合 `take`/`takeWhile`/`dropWhile` 即可在无限源上工作而不会预先物化。
  **不存在**的名字（源码 jar + 字节码双重实测，见下方"反幻觉清单"）：`whileTake`、`whileDrop`、`consume`。
- 一次性约束：`ConstrainedOnceSequence` 的 JVM 实现用 `AtomicReference` + `getAndSet(null)`，第二次迭代抛 `IllegalStateException`，说明平台侧对"只能消费一次"有序列做了显式保护。
- 互转：`Sequence<T>.asIterable()` / `Sequence<T>.asSequence()` 在切片中存在；`Iterable`→`Sequence` 的 `asSequence` 属 `kotlin.collections`，本切片不覆盖。

---

## 二、kotlin.ranges：区间与等差序列

### 2.1 三个接口

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public interface ClosedRange<T : Comparable<T>>` | `-` | 闭区间抽象：`val start: T`、`val endInclusive: T`、`operator fun contains(value: T): Boolean = value >= start && value <= endInclusive`、`fun isEmpty(): Boolean = start > endInclusive` |
| `public interface OpenEndRange<T : Comparable<T>>` | `1.9`（`@WasExperimental(ExperimentalStdlibApi::class)`，现为稳定 API） | 上界开区间：`val start: T`、`val endExclusive: T`、`contains = value >= start && value < endExclusive`、`isEmpty = start >= endExclusive` |
| `public interface ClosedFloatingPointRange<T : Comparable<T>> : ClosedRange<T>` | `1.1` | 额外提供 `fun lessThanOrEquals(a: T, b: T): Boolean`，按 IEEE-754 比较浮点 |

### 2.2 基本类型区间类

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public class IntRange(start: Int, endInclusive: Int) : IntProgression(start, endInclusive, 1), ClosedRange<Int>, OpenEndRange<Int>` | `-` | `override val start get() = first`、`override val endInclusive get() = last` |
| `public val EMPTY: IntRange = IntRange(1, 0)` | `-` | 伴生对象中的空区间（`LongRange`/`CharRange` 同名同形） |
| `public class LongRange(start: Long, endInclusive: Long) : LongProgression(...), ClosedRange<Long>, OpenEndRange<Long>` | `-` | 同上 |
| `public class CharRange(start: Char, endInclusive: Char) : CharProgression(...), ClosedRange<Char>, OpenEndRange<Char>` | `-` | 同上 |
| `override val endExclusive: Char/Int` | `1.9`，同时被 `@Deprecated` | 源码注释：区间含 `MAX_VALUE` 时会 `error(...)`，推荐改用 `endInclusive` |
| `public class UIntRange(start: UInt, endInclusive: UInt) : UIntProgression(...), ClosedRange<UInt>, OpenEndRange<UInt>` | `1.5` | `EMPTY = UIntRange(UInt.MAX_VALUE, UInt.MIN_VALUE)` |
| `public class ULongRange(start: ULong, endInclusive: ULong) : ULongProgression(...)` | `1.5` | `EMPTY = ULongRange(ULong.MAX_VALUE, ULong.MIN_VALUE)` |

### 2.3 Progression：等差序列（区间可迭代的底层）

`CharProgression` / `IntProgression` / `LongProgression` 是 `public open class`（`commonMain/kotlin/ranges/Progressions.kt`），`UIntProgression` / `ULongProgression` 位于 `commonMain/kotlin/UIntRange.kt`、`ULongRange.kt`（`1.5`）。共同成员：

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public val first: Int`（及 `Long`/`Char`/`UInt`/`ULong` 版本） | `-` | 首项 |
| `public val last: Int` = `getProgressionLastElement(start, endInclusive, step)` | `-` | 末项（按 step 调整，非直接等于上界） |
| `public val step: Int` / `Long` | `-` | 步长；构造时经 `checkStepIsPositive` 校验（internal） |
| `public open fun isEmpty(): Boolean = if (step > 0) first > last else first < last` | `-` | 空判定 |
| `public fun fromClosedRange(rangeStart: Int, rangeEnd: Int, step: Int): IntProgression` | `-` | 伴生工厂（各类型对应） |
| `public infix fun IntProgression.step(step: Int): IntProgression`（`Long`/`Char`/`UInt`/`ULong` 同名） | `-` / `1.5`(unsigned) | 重设步长 |
| `public fun IntProgression.reversed(): IntProgression`（`Long`/`Char` 同名；`UInt`/`ULong` 版 `1.5`） | `-` | 反向遍历 |
| `public fun IntProgression.first(): Int` / `last()` / `firstOrNull()` / `lastOrNull()` | `1.7` | `Int`/`Long`/`Char`（`UInt`/`ULong` 版亦为 1.7）——O(1) 取首末，无需迭代 |
| `internal class IntProgressionIterator(first: Int, last: Int, val step: Int) : IntIterator()` | `-` | 迭代器实现（`ProgressionIterators.kt`，其余类型同文件） |

> **实测结论（2.2.10）**：`kotlin.ranges` 里**没有** `CollectionProgression`，也**没有**通用的 `Progression` 基类/接口。字节码是硬证据：
> ```
> $ javap -classpath . kotlin.ranges.IntProgression
> public class kotlin.ranges.IntProgression implements java.lang.Iterable<java.lang.Integer>, kotlin.jvm.internal.markers.KMappedMarker
> ```
> 即 `IntProgression` 只实现 `Iterable`（`first/last/step` 是各自类里重复声明的普通成员，不是来自公共接口）；`IntRange` 则 `extends IntProgression implements ClosedRange<Integer>, OpenEndRange<Integer>`。
> jar 里 `kotlin/ranges/` 的全部公开类：`Char/Int/Long/UInt/ULong` 的 `*Progression`、`*ProgressionIterator`、`*Range`，加 `ClosedRange`、`ClosedDoubleRange`、`ClosedFloatRange`、`ClosedFloatingPointRange`、`ComparableOpenEndRange`、`ComparableRange`、`OpenEndRange`、`OpenEndDoubleRange`、`OpenEndFloatRange`（共 21 个，`docs/_data/bin_files.txt` 可复核）。
> **后果**：你不能写 `fun <T> p(a: Progression<T>)`，只能针对具体类型或用 `Iterable<T>`。

### 2.4 构造区间的运算符

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public infix fun Int.until(to: Int): IntRange` | `-` | 半开区间；同族覆盖 `Int/Long/Byte/Short/Char` 与混合数值参数（返回 `IntRange`/`LongRange`/`CharRange`） |
| `public infix fun UInt.until(to: UInt): UIntRange`（`ULong`/`UByte`/`UShort` 同名） | `1.5` | 无符号半开区间 |
| `public infix fun Int.downTo(to: Int): IntProgression` | `-` | 降序等差（返回 Progression，非 Range）；`Char.downTo(to: Char): CharProgression` 同名存在 |
| `public infix fun UByte.downTo(to: UByte): UIntProgression` / `UShort` / `UInt` / `ULong` | `1.5` | 无符号降序 |
| `public operator fun Double.rangeTo(that: Double): ClosedFloatingPointRange<Double>` | `1.1` | 实现为 `ClosedDoubleRange(this, that)`；`Float` 同名 |
| `public operator fun Double.rangeUntil(that: Double): OpenEndRange<Double>` | `1.9`（wasExperimental） | 实现为 `OpenEndDoubleRange`；`Float` 同名 |
| `public operator fun <T : Comparable<T>> T.rangeTo(that: T): ClosedRange<T>` | `-`（切片缺失） | 取自源文件 `commonMain/kotlin/ranges/Ranges.kt:37`，通用 Comparable 区间 |
| `public operator fun <T : Comparable<T>> T.rangeUntil(that: T): OpenEndRange<T>` | `1.9`（切片缺失） | 源文件 `commonMain/kotlin/ranges/Ranges.kt:67` |

### 2.5 成员判断（`in`）与数值收窄

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public inline operator fun IntRange.contains(element: Int?): Boolean`（`LongRange`/`CharRange`/`UIntRange 1.5`/`ULongRange 1.5`） | `1.3` | 装箱可空元素版本，`null` 恒为 `false` |
| `public operator fun ClosedRange<Int>.contains(value: Byte/Short/Long/Float/Double): Boolean` | `-` | 跨数值类型 `in` 检查，`ClosedRange<Long/Float/Double/Short/Byte>` 亦有对应重载 |
| `public operator fun OpenEndRange<Int>.contains(value: Byte/Short/Long): Boolean` 等 | `1.9` | 开区间侧的跨类型检查 |
| `public inline operator fun <T, R> R.contains(element: T?): Boolean where T : Any, R : ClosedRange<T>, R : Iterable<T>` | `1.3` | 可迭代区间的 `in`；另有 `R : OpenEndRange<T>` 版本（`1.9`） |
| `public fun Int.coerceIn(minimumValue: Int, maximumValue: Int): Int` | `-` | `Byte/Short/Long/Float/Double` 同名各一个 |
| `public fun Int.coerceIn(range: ClosedRange<Int>): Int` | `-` | `Long` 同名；`UInt`/`ULong` 版本为 `1.5` |
| `public fun Int.coerceAtLeast(minimumValue: Int): Int` / `coerceAtMost(maximumValue: Int)` | `-` | 六个数值类型各一份；无符号版 `1.5` |
| `public fun UInt.coerceIn(minimumValue: UInt, maximumValue: UInt): UInt`（`UByte/UShort/ULong` 同名） | `1.5` | 无符号收窄 |

### 2.6 区间上的随机取值

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public inline fun IntRange.random(): Int` / `LongRange.random()` / `CharRange.random()` | `1.3` | 区间内等概率取值 |
| `public fun IntRange.random(random: Random): Int`（`Long`/`Char` 同名） | `1.3` | 指定随机源 |
| `public inline fun IntRange.randomOrNull(): Int?`（`Long`/`Char` 同名） | `1.4` | 空区间返回 `null` |
| `public fun IntRange.randomOrNull(random: Random): Int?` | `1.4` | 指定随机源 |
| `public inline fun UIntRange.random(): UInt` / `ULongRange.random()` / `randomOrNull()` 及 `random(random)` | `1.5` | 无符号区间版本 |

```kotlin
// 示例 4：区间构造、成员判断与步长
val closed = 1..10                       // IntRange（同时实现 ClosedRange<Int> 与 OpenEndRange<Int>）
val openEnd = 1 until 10                 // IntRange
val chars = 'a'..'z'                     // CharRange
val descending = 10 downTo 1             // IntProgression（不是 Range）
val stepped: IntProgression = (1..20 step 3)
val halfOpen: OpenEndRange<Double> = 0.0.rangeUntil(1.0)   // 1.9

fun main() {
    println(closed.contains(5) to (7 in openEnd) to ('m' in chars))   // true true true
    println(stepped.toList())                                        // [1, 4, 7, 10, 13, 16, 19]
    println(descending.reversed().take(3).toList())                  // 见 IntProgression.reversed()
    println(halfOpen.contains(1.0))                                  // false：上界不含
    println(emptyRange())
}

fun emptyRange(): Boolean = IntRange.EMPTY.isEmpty()
```

```kotlin
// 示例 5：coerceIn 与 Progression 的 O(1) 首末取值
fun clampScore(v: Int): Int = v.coerceIn(0..100)                 // ClosedRange<Int> 版本
fun clamp(v: Double, lo: Double, hi: Double): Double = v.coerceIn(lo, hi)

fun main() {
    println(clampScore(250) to clampScore(-3))                    // 100 to 0
    println(clamp(0.5, 0.0, 0.25))                                // 0.25

    val big = 1..Int.MAX_VALUE
    println(big.first to big.last)                                // 1 to 2147483647，未迭代
    println(big.step)                                             // 1

    val p = IntProgression.fromClosedRange(0, 20, 5)
    println(p.toList() to p.lastOrNull() to p.firstOrNull())      // [0, 5, 10, 15, 20]

    println((1..100).random(random = kotlin.random.Random(0)) in 1..100)
}
```

---

## 三、kotlin.comparisons：比较器组合与极值

源码：`commonMain/kotlin/comparisons/Comparisons.kt`（无 `@SinceKotlin`，故版本列为 `-`）、`commonMain/generated/_Comparisons.kt`（`maxOf`/`minOf`）、`commonMain/generated/_UComparisons.kt`（无符号）、`jvmMain/generated/_ComparisonsJvm.kt`（`actual`）。

| API（真实签名） | @SinceKotlin | 说明 |
| --- | --- | --- |
| `public fun <T> compareValuesBy(a: T, b: T, vararg selectors: (T) -> Comparable<*>?): Int` | `-` | 按多个选择器依次比较 |
| `public inline fun <T> compareValuesBy(a: T, b: T, selector: (T) -> Comparable<*>?): Int` | `-` | 单选择器（`@kotlin.internal.InlineOnly`） |
| `public inline fun <T, K> compareValuesBy(a: T, b: T, comparator: Comparator<in K>, selector: (T) -> K): Int` | `-` | 选择器结果再用比较器比较 |
| `public fun <T> compareBy(vararg selectors: (T) -> Comparable<*>?): Comparator<T>` | `-` | 由选择器构造比较器 |
| `public inline fun <T> compareBy(crossinline selector: (T) -> Comparable<*>?): Comparator<T>` | `-` | 单选择器版本 |
| `public inline fun <T, K> compareBy(comparator: Comparator<in K>, crossinline selector: (T) -> K): Comparator<T>` | `-` | 带比较器版本 |
| `public inline fun <T> compareByDescending(crossinline selector: (T) -> Comparable<*>?): Comparator<T>` | `-` | 降序比较器；另有 `comparator` 重载 |
| `public inline fun <T> Comparator<T>.thenBy(crossinline selector: (T) -> Comparable<*>?): Comparator<T>` | `-` | 追加次级排序键；另有 `comparator` 重载 |
| `public inline fun <T> Comparator<T>.thenByDescending(crossinline selector: (T) -> Comparable<*>?): Comparator<T>` | `-` | 次级降序键；另有 `comparator` 重载 |
| `public inline fun <T> Comparator<T>.thenComparator(crossinline comparison: (a: T, b: T) -> Int): Comparator<T>` | `-` | 追加自定义比较逻辑 |
| `public infix fun <T> Comparator<T>.then(comparator: Comparator<in T>): Comparator<T>` | `-` | 串接另一个比较器 |
| `public infix fun <T> Comparator<T>.thenDescending(comparator: Comparator<in T>): Comparator<T>` | `-` | 串接并取反 |
| `public fun <T> Comparator<T>.reversed(): Comparator<T>` | `-` | 取反（内部用 `ReversedComparator`，对 `NaturalOrderComparator`/`ReverseOrderComparator` 直接互换） |
| `public fun <T : Any> nullsFirst(comparator: Comparator<in T>): Comparator<T?>` | `-` | `null` 排最前 |
| `public inline fun <T : Comparable<T>> nullsFirst(): Comparator<T?>` | `-` | 基于自然序 |
| `public fun <T : Any> nullsLast(comparator: Comparator<in T>): Comparator<T?>` | `-` | `null` 排最后 |
| `public inline fun <T : Comparable<T>> nullsLast(): Comparator<T?>` | `-` | 基于自然序 |
| `public fun <T : Comparable<T>> naturalOrder(): Comparator<T>` | `-`（切片缺失） | 源文件 `Comparisons.kt:286`，实现返回内部 `NaturalOrderComparator` 单例 |
| `public fun <T : Comparable<T>> reverseOrder(): Comparator<T>` | `-`（切片缺失） | 源文件 `Comparisons.kt:295`，实现返回 `ReverseOrderComparator` 单例 |
| `public fun <T : Comparable<*>> compareValues(a: T?, b: T?): Int` | `-`（切片缺失） | 源文件 `Comparisons.kt:72`；切片中仅出现其在 `compareValuesByImpl` 内的调用 |
| `public expect inline fun maxOf(a: Int, b: Int): Int` | `1.1` | `Byte/Short/Int/Long/Float/Double` 各一份；`commonMain` 声明、`[JVM]` 有对应 `actual inline` |
| `public expect inline fun maxOf(a: Int, b: Int, c: Int): Int` | `1.1` | 三参数版本，六个基本类型 |
| `public fun <T> maxOf(a: T, b: T, comparator: Comparator<in T>): T` / `(a, b, c, comparator)` | `1.1` | 比较器版本 |
| `public expect fun maxOf(a: Int, vararg other: Int): Int` | `1.4` | 变长版本，六个基本类型 |
| `public fun <T> maxOf(a: T, vararg other: T, comparator: Comparator<in T>): T` | `1.4` | 变长 + 比较器 |
| `public fun maxOf(a: UInt, b: UInt): UInt` 等 | `1.5` / `1.4` | `UInt/ULong/UByte/UShort` 的二参、三参（1.5）与 vararg（1.4）版本；`minOf` 完全对称 |
| `public inline infix fun <T> Comparable<T>.compareTo(other: T): Int` | `1.6`（属包 `kotlin`，不在本切片） | 源文件 `commonMain/kotlin/comparisons/compareTo.kt`，infix 调用 `Comparable.compareTo` |

```kotlin
// 示例 6：比较器组合
data class Person(val name: String, val age: Int, val city: String?)

val byAgeThenName = compareByDescending<Person> { it.age }
    .thenBy { it.name }
    .thenComparator { a, b -> a.city.orEmpty().length - b.city.orEmpty().length }

val people = listOf(Person("ann", 31, null), Person("bob", 25, "x"), Person("cid", 31, "y"))
println(people.sortedWith(byAgeThenName).map { it.name })
println(people.maxWith(byAgeThenName).name)
println(people.sortedWith(nullsLast(compareBy { it.city })).map { it.city })
```

```kotlin
// 示例 7：maxOf / minOf
val hi = maxOf(3, 7, 5)                        // 1.1 三参数
val lo = minOf(3.5, 1.5, 2.0)                  // Double 版本
val any = maxOf(2, 9, 4, comparator = compareBy { -it })   // 带 Comparator 的重载
val widest = maxOf(1, 2, 300, 40)              // 1.4 vararg
println(hi to lo to any to widest)
println(minOf(7u, 2u, 5u) to maxOf(3u, 8u))    // 1.5 / 1.4 无符号版本
println(compareValuesBy("a", "b") { it.length } )
```

---

## 数据来源与校验方法

- **jar 版本**：`kotlin-stdlib-2.2.10-sources.jar` + `kotlin-stdlib-2.2.10.jar`，解包到 `C:/Users/wdidada/AppData/Local/Temp/kstdlib/`（`src/` 为源码，`out2/slices/` 为按包切分的 TSV；本文用 v2 切片）。
- **本文使用的切片文件与实测行数**（`wc -l`，含表头行）：
  - `out2/slices/kotlin_sequences.tsv` — 574 行
  - `out2/slices/kotlin_ranges.tsv` — 310 行
  - `out2/slices/kotlin_comparisons.tsv` — 179 行
- **TSV 列序**：切片（`slices/*.tsv`，带表头）= `since`\t `kind`\t `name`\t `receiver`\t `arity`\t `sig`\t `loc`；全量 `out2/api.tsv`（无表头）= `since`\t `pkg`\t `kind`\t `name`\t `receiver`\t `loc`\t `sig`。
- **抽取方式**：脚本遍历 `src/**.kt`，用正则抓取顶层 `public/internal/expect/actual` 的 `fun`/`val`/`var`/`class`/`interface`/`object`/`typealias` 声明行，并把紧邻其上的 `@SinceKotlin("x.y")` 绑定到 `since` 列；按 `package` 语句切分为各包 TSV。
- **补充核实手段**：对截断签名、`@SinceKotlin`/`@WasExperimental` 归属存疑处直接读源文件，例如
  `commonMain/kotlin/collections/Sequence.kt`、`SequenceBuilder.kt`、`Sequences.kt`、`SequencesH.kt`、
  `jvmMain/kotlin/collections/SequencesJVM.kt`、`commonMain/kotlin/ranges/{Range,Ranges,Progressions,PrimitiveRanges,ProgressionIterators}.kt`、
  `commonMain/kotlin/{UIntRange,ULongRange}.kt`、`commonMain/kotlin/comparisons/{Comparisons,compareTo}.kt`、`commonMain/generated/_Sequences.kt`。
  实验性判定：`grep -n "Experimental\|WasExperimental"` 结果——`sequences/comparisons` 源码无 `Experimental*`（仅 `SequenceBuilder.kt` 的 `@file:OptIn(ExperimentalTypeInference::class)`）；`ranges` 侧 `OpenEndRange`（Range.kt:47）、`rangeUntil`（Ranges.kt:66/166/248/268）与 `endExclusive`（PrimitiveRanges.kt:21/60/99、UIntRange.kt:25、ULongRange.kt:25）均为 `@WasExperimental(ExperimentalStdlibApi::class)`，即"曾实验、现已稳定"，其中 `endExclusive` 另带 `@Deprecated`。
- **已知抽取缺陷（v2 解析器，本文如实标注）**：
  1. 函数体内的局部 `val`/`var` 仍被当作条目收录：`kotlin_sequences.tsv` 实测 `fun=242`、`val=178`、`var=130`、`class=18`、`interface=2`、`object=2`、`typealias=1`，其中 `val`/`var` 绝大多数是 `val iterator = iterator()` 这类局部变量；统计公开属性时必须结合 `sig` 列的 `public`/`get()` 判定。`kotlin_ranges.tsv` 为 `fun=231`、`val=44`、`class=21`、`var=10`、`interface=3`；`kotlin_comparisons.tsv` 为 `fun=141`、`var=24`、`val=10`、`object=2`、`class=1`。
  2. 名字抽取仍会把少数带接收者的声明落在接收者类型上（如 `Int`、`Char`、`Boolean`），这类条目不是 API，已在正文剔除。
  3. v1 解析器的两处缺陷（扩展函数名落在接收者类型；带 `Comparable<...>` 约束接收者的 `sorted`/`maxBy`/`minOf` 族、`naturalOrder()`/`reverseOrder()`/`compareValues`、通用 `T.rangeTo`/`T.rangeUntil` 整条漏掉）在 v2 已修复：这些名字现在都能在切片里 grep 到。
  4. **确实不存在**的名字（源码 jar `grep` 与 `javap` 字节码双重实测，不是切片缺陷）：`Sequence`/`Iterable` 上的 `whileTake`、`whileDrop`、`consume`、`distinctUntilChanged`、`cached`、`constrainTo`、`zipWith`（只有 `zipWithNext`）、`javaStream`/`javaIterator`/`asJavaStream`/`asJavaEnumeration`（stdlib 里的真实互转名是 `kotlin.streams` 包的 `asStream()`/`asSequence()`/`toList()`（均 `1.2`），以及 `Iterator<T>.asSequence()`、`java.util.Enumeration<T>.asSequence()`）；`kotlin.ranges` 无 `Progression` 接口与 `CollectionProgression`；`kotlin.math` 无 `SQRT2`/`LN2`/`LOG2E` 公开常量（`LN2` 仅 `internal val`）。这些是"从 RxJava / kotlinx.coroutines Flow / Java `Math` 外推"产生的常见误记，写代码前一律先 grep。
- **自查方式**：`grep -E "\t<api名>\t" out2/slices/<pkg>.tsv`（v2 的 `name` 列已是真实 API 名），或直接 `grep -rn "fun .*<api名>" src/`；凡只在源文件里出现、切片未收录的条目，正文已标注"（切片缺失）/※"。
