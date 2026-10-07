# Kotlin 标准库速查表（按"我要做什么"查）

每条都是从 `kotlin-stdlib-2.2.10-sources.jar` 解析出的真实签名；`版本` 列 `1.0*` 表示源码无 `@SinceKotlin` 戳（随首版存在）。签名里第一个出现的重载可能是 `Array`/`Sequence`/`CharSequence` 版本，**同一个名字通常对 `Iterable`/`Collection`/`Sequence`/`CharSequence`/`Map` 各有一整套重载**，以 IDE 补全为准。

## 1. 建集合

| 要做 | API（真实签名摘录） | 版本 |
|---|---|---|
| 只读列表 | `public fun <T> listOf(vararg elements: T): List<T>` | 1.0* |
| 可变列表 | `public inline fun <T> mutableListOf(): MutableList<T> = ArrayList()` | **1.1** |
| 具体实现类型 | `public inline fun <T> arrayListOf(): ArrayList<T>`、`linkedSetOf(): LinkedHashSet<T>`、`sortedSetOf(vararg elements: T): java.util.TreeSet<T>` | 1.1 / 1.1 / 1.0* |
| 剔除 null | `public fun <T : Any> listOfNotNull(element: T?): List<T>` | 1.0* |
| 集合/Map | `setOf(vararg elements: T): Set<T>`、`mapOf(vararg pairs: Pair<K, V>): Map<K, V>` | 1.0* |
| 空集合 | `emptyList(): List<T> = EmptyList` | 1.0* |
| 构建后再冻结 | `public inline fun <E> buildList(@BuilderInference builderAction: MutableList<E>.() -> Unit): List<E>`；同族 `buildSet`、`buildMap` | **1.6** |
| 复制/转类型 | `toList`、`toMutableList`、`toSet`、`toMap`、`toCollection`、`toTypedArray`、`toHashSet`、`toSortedSet` | 多为 1.0*，`toTypedArray`/`toIntArray` 等 = **1.3** |
| Optional → 集合 | `public fun <T : Any> Optional<out T>.toList(): List<T>`、`.toSet()`、`.toCollection()` | **1.8** |
| Stream → 集合 | `public fun <T> Stream<T>.toList(): List<T> = collect(Collectors.toList<T>())` | **1.2** |

## 2. 变换与读取集合

| 要做 | API | 版本 |
|---|---|---|
| 映射/过滤 | `map`、`mapIndexed`、`mapNotNull`、`filter`、`filterIndexed`、`filterIsInstance`、`filterNotNull` | 1.0* |
| 展平 | `flatMap(transform: (T) -> Iterable<R>): List<R>`、`flatten()`、`flatMapIndexed`/`flatMapTo` | 1.0*（Sequence 版 `flatMap` 带 **1.4** 戳） |
| 分组 | `groupBy(keySelector: (T) -> K): Map<K, List<T>>`、`groupingBy(...): Grouping<T, K>`（配 `eachCount`/`fold`） | 1.0* / **1.1** |
| 关联 | `associateWith(valueSelector)`、`associate`、`associateBy` | **1.4**（Array 版）/ 1.0* |
| 二分 | `partition(predicate): Pair<List<T>, List<T>>` | 1.0* |
| 去重 | `distinct()`、`distinctBy(selector)` | 1.0* |
| 截取 | `take(n)`、`drop(n)`、`takeWhile`、`dropWhile`、`slice(range/indexes)` | 1.0* |
| 分块 | `chunked(size: Int): List<List<T>>`、`windowed(size, step = 1, partialWindows = false)` | **1.2** |
| 配对 | `zip(other)`、`zip(other) { a, b -> }`、`zipWithNext()`/`zipWithNext(transform)`、`unzip(): Pair<List<T>, List<R>>` | 1.0*（`zipWithNext` **1.2**；**没有** `zipWith`） |
| 排序 | `sorted()`、`sortedBy(crossinline selector: (T) -> R?)`、`sortedByDescending`、`sortedWith`、`shuffled(random: Random)`、`reversed()` | 1.0* / **1.3**（`shuffled`） |
| 遍历 | `forEach`、`forEachIndexed`、`onEach(action)`、`onEachIndexed` | 1.0* / **1.4**（Array 版 `onEach`）、**1.1**（Sequence 版） |
| 索引视角 | `withIndex(): Iterable<IndexedValue<T>>`、`indices`、`forEachIndexed` | 1.0* |
| 聚合 | `fold(initial, operation)`、`foldRight`、`reduce`、`reduceOrNull`、`runningFold`、`runningReduce`、`scan`、`sumOf(selector: (T) -> Double)`、`average()` | 1.0* / **1.4**（`reduceOrNull`/`runningReduce`/`scan`/`sumOf`） |
| 极值 | `maxOrNull()`、`minOrNull()`、`maxOf(a, b, ...)`、`minOf`、`maxByOrNull`、`minWithOrNull` | **1.4** |
| 判空判定 | `any()`/`any(predicate)`、`all(predicate)`、`none()`/`none(predicate)`、`count()`、`isEmpty()`/`isNotEmpty()` | 1.0* |
| 单元素 | `first()`、`singleOrNull()`、`findLast(predicate)`、`elementAtOrNull`、`getOrElse` | 1.0* / **1.4**（`findLast`） |
| 拼成字符串 | `joinToString(separator = ", ", prefix = "", postfix = "", limit, truncated, transform)` | 1.0* |

## 3. 字符串

| 要做 | API | 版本 |
|---|---|---|
| 去空白 | `trim(predicate)`（还有无参版）、`trimStart`、`trimEnd` | 1.0* |
| 补齐 | `padStart(length: Int, padChar: Char = ' ')`、`padEnd(...)` | 1.0* |
| 判空白 | `isBlank()`、`isNotBlank()`、`isEmpty()` | 1.0* |
| 取子串 | `substring(...)`、`subSequence(range: IntRange)`、`removePrefix`、`removeSuffix`、`removeSurrounding(prefix, suffix)` | 1.0* |
| 查找 | `indexOf(string)`、`lastIndexOf(string)`、`contains(char)`、`startsWith(char, ignoreCase = false)`、`endsWith`、`find(predicate: (Char) -> Boolean)`、`findLast` | 1.0*（`StringBuilder.indexOf/lastIndexOf` 带 **1.4** 戳） |
| 切分 | `split(vararg delimiters: String, ignoreCase = false, limit = 0)`、`splitToSequence(...)`、`lines()`、`lineSequence(): Sequence<String>` | 1.0* / `splitToSequence` **1.6** |
| 替换 | `replace(regex: Regex, replacement: String)`、`replaceFirst`、`String.replace(oldValue, newValue, ignoreCase)` | 1.0* |
| 大小写 | `public expect fun String.uppercase(): String`（`@SinceKotlin("1.5")`）、`lowercase()` 同批；`capitalize()`/`decapitalize()` 源码里带 `@Deprecated("Use replaceFirstChar instead.")` + `@DeprecatedSinceKotlin(warningSince = "1.5")`，所以新代码用 `uppercase()` / `replaceFirstChar { it.titlecase() }` | **1.5** |
| 拼接/重复 | `buildString(builderAction: StringBuilder.() -> Unit): String`、`CharSequence.repeat(n: Int): String`、`concatToString()`、`StringBuilder.append` 族、`appendLine`（**1.9**） | 1.0* |
| 格式化 | `public inline fun String.format(vararg args: Any?): String = java.lang.String.format(this, *args)`（JVM） | 1.0*（JVM 专属，见 `04-text.md`） |
| 正则 | `String.toRegex(): Regex`、`CharSequence.matches(regex)`、`Regex.find`/`matchEntire(input): MatchResult?`、`RegexOption` | 1.0* |
| 逐字符 | `chars()`、`codePointCount`、`Character` 类型族、`Char.isWhitespace`/`isDigit`/`isLetter`、`Char.code`（**1.5** 前后）、`Char.category`/`directionality`（**1.2**） | 见 `04-text.md` |
| 十六进制 | `toHexString(format: HexFormat = HexFormat.Default)`、`hexToByteArray(format: HexFormat = ...)`、`HexFormat` | **2.2** |
| 内部化 | `String.intern()`（JVM 桥接 `java.lang.String`） | 1.0* |

## 4. 数字与范围

| 要做 | API | 版本 |
|---|---|---|
| 造范围 | `infix fun Int.until(to: Byte): IntRange`、`1..10`（`rangeTo`）、`downTo`、`1 step 3`（`IntProgression.step`）、`coerceIn`、`coerceAtLeast` | 1.0* |
| 半开区间 | `public operator fun <T : Comparable<T>> T.rangeUntil(that: T): OpenEndRange<T>`（配合 `..<`） | **1.9** |
| 通用区间 | `rangeTo(that: T): ClosedRange<T>` | 1.0* |
| 范围随机 | `public inline fun IntRange.random(): Int`（`LongRange.random` 同族，另有 `random(random: Random)`） | **1.3** |
| 上下界 | `Progression.first()/firstOrNull()/last()/lastOrNull()`、`reversed()`（`IntProgression`） | **1.7** / 1.0* |
| 数值转换 | `toInt()`/`toLong()`/`toDouble()`/…（1.0*）、`toUInt()`/`toULong()`（**1.5**）、`toBigDecimal()`/`toBigInteger()`（**1.2**）、`floorDiv`/`mod`（**1.5**）、`rotateLeft/rotateRight`（**1.5** 声明、**1.6** 收敛） |
| 数学 | `abs`、`sqrt`、`cbrt`、`pow`、`exp`、`ln`、`ln1p`、`log2`、`log10`、`hypot`、`roundToInt`、`truncate`、`withSign`、`nextUp`、`nextDown`、`nextTowards`、常量 `PI`/`E` | 多为 **1.2**，见 `01-core-types.md` 第 10 节 |
| 位运算 | `shl`/`shr`/`ushr`、`and`/`or`/`xor`/`inv`、`countOneBits`/`countLeadingZeroBits`/`countTrailingZeroBits`、`takeHighestOneBit`/`takeLowestOneBit`（**1.4**/**1.5**） | 1.0* / 1.4 |

## 5. 惰性序列（大数据量/无限流优先用 Sequence）

| API | 签名摘录 | 版本 |
|---|---|---|
| `sequenceOf` | `public fun <T> sequenceOf(vararg elements: T): Sequence<T> = elements.asSequence()` | 1.0*（2.2 另有新重载） |
| `asSequence` | `public inline fun <T> Sequence<T>.asSequence(): Sequence<T>`；集合侧 `Iterable<T>.asSequence()` | 1.0* |
| `generateSequence` | `public fun <T : Any> generateSequence(nextFunction: () -> T?): Sequence<T>` | 1.0* |
| 惰性变换 | `map`/`filter`/`flatMap`/`flatten`/`distinct`/`take`/`drop`/`chunked`/`windowed`/`zip` | 1.0*（`chunked`/`windowed` **1.2**） |
| 条件截断 | `takeWhile(predicate)`、`dropWhile(predicate)` | 1.0*（**不存在** `whileTake`/`whileDrop`/`consume`：源码 jar 与 `javap kotlin.sequences.SequencesKt___SequencesKt` 双向实测均无，那是 RxJava/Flow 的名字） |
| 副作用与消费 | `onEach`（**1.1**）、`onEachIndexed`（**1.4**）、`forEach`、`toList()` 作为终端操作（数据里不存在 `consume`，不要写） | — |
| 极值/聚合 | `maxOrNull`/`minOrNull`/`sumOf`/`reduceOrNull`/`runningReduce`/`shuffled()`（均 **1.4**） | 1.4 |

## 6. 文件与 IO（JVM）

| 要做 | API | 版本 |
|---|---|---|
| 整读整写 | `File.readText(charset = Charsets.UTF_8): String`、`File.writeText(text, charset)`、`appendText`、`readBytes()`、`writeBytes(array)` | 1.0* |
| 按行 | `File.readLines(charset)`、`File.useLines(block: (Sequence<String>) -> T)`、`File.forEachLine(charset, action)` | 1.0* |
| 流与包装 | `File.bufferedReader(charset = UTF_8, bufferSize = DEFAULT_BUFFER_SIZE)`、`inputStream().use { }` | 1.0* |
| 目录操作 | `File.copyTo(target, overwrite = false)`、`File.deleteRecursively()`、`File.walk(FileWalkDirection.TOP_DOWN)`、`walkTopDown()`、`walkBottomUp()` | 1.0* |
| Path 侧 | `Path.readText`、`Path.writeText`、`Path.appendText`、`Path.copyTo`、`Path.deleteIfExists`、`Path.exists`、`Path.isRegularFile`、`absolutePathString`、`Path.pathString`（扩展属性，1.5）、`fileSize`、`listDirectoryEntries`、`createDirectories`、`moveTo`；stdlib 只提供了 `URI.toPath()`（1.5），而 `File.toPath()`/`Path.toFile()` 是 JDK 自带方法，不算 Kotlin API | **1.5** 起大批进入（`@SinceKotlin` 见 `08-versioned-api.md`），实验注解见 `05-io-path-encoding.md` |
| 递归复制 | `copyToRecursively`、`deleteRecursively`（Path 版） | **1.8** |
| 树遍历 | `walk(...)`、`visitFileTree`、`fileVisitor { }`、`FileVisitorBuilder`、`PathWalkOption` | **2.1** |
| Base64 | `Base64`（`@SinceKotlin("2.2")` + `@WasExperimental(ExperimentalEncodingApi::class)`）、`encodingWith`/`decodingWith`、`PaddingOption`、`withPadding`、`isInMimeAlphabet` | 2.2 / `PaddingOption`+`withPadding` **2.0** |

## 7. 时间、随机、UUID、原子

| API | 签名摘录 | 版本 |
|---|---|---|
| `Duration` | `public value class Duration internal constructor(private val rawValue: Long) : Comparable<Duration>` | **1.6**（1.3 起为实验 `ExperimentalTime`） |
| 构造时长 | `public fun Int.toDuration(unit: DurationUnit): Duration`、`toString(unit: DurationUnit, decimals: Int = 0)`、`parse(value: String): Duration` | 1.6 / 1.0* |
| 计时 | `public inline fun measureTime(block: () -> Unit): Duration`、`measureTimedValue(block: () -> T): TimedValue<T>`、`public interface TimeSource`、`object Monotonic : TimeSource.WithComparableMarks`、`abstract fun elapsedNow(): Duration` | **1.9**（`Monotonic`/`elapsedNow` 无戳，属接口内成员） |
| 时刻 | `Instant`、`Clock`、`toJavaInstant`/`toKotlinInstant`、`asClock`（**2.2**）、`Duration.parseOrNull`（**2.2**） | **2.1** |
| 随机 | `public abstract class Random`（**1.3**）、`nextInt()`、`nextDouble()`、`Random(seed: Int)`/`Random(seed: Long)`（**1.3**）、`asJavaRandom`/`asKotlinRandom`（**1.3**）、`nextUInt`/`nextULong`（**1.5**）、`nextUBytes`（**1.3**） | 见 `06-...md` |
| UUID | `public class Uuid private constructor(...)`（**2.0**，`@ExperimentalUuidApi`）、`Uuid.random()`、`Uuid.parse(uuidString)`、`toJavaUuid`/`toKotlinUuid`（**2.0**）、`parseHexDash`/`toHexDashString`/`toUByteArray`/`fromUByteArray`（**2.1**） | 2.0→2.1 |
| 原子 | `AtomicInt`、`AtomicLong`、`AtomicBoolean`、`AtomicReference`、`AtomicIntArray`、`AtomicLongArray`、`AtomicArray`、`fetchAndIncrement`/`incrementAndFetch`/`compareAndSet`/`compareAndExchange`、`asJavaAtomic`/`asKotlinAtomic`、`ExperimentalAtomicApi` | **2.1** |

## 8. 空安全与错误处理速记

```kotlin
val len = name?.length ?: 0                 // Elvis
user?.let { render(it) }                    // 1.0*
path?.takeIf { it.exists() }                // 1.1
val r = line.runCatching { toInt() }        // 1.3：不抛异常拿 Result
    .getOrElse { -1 }
val v = map[key] ?: error("missing")        // error -> IllegalStateException
require(index in 0..size) { "bad index" }   // require -> IllegalArgumentException
check(state == READY)                       // check -> IllegalStateException
resource.use { read(it) }                   // 1.2 起可用，2.0 提升为 common AutoCloseable
```

## 数据来源与校验方法

- 唯一来源：`out2/api.tsv`（13,045 行）与 `out2/slices/*.tsv`（42 个包）。复核任一条：`awk -F'\t' '$4=="chunked"' out2/api.tsv`。
- `版本=1.0*` 的判读规则：该声明行及其上 6 行内没有 `@SinceKotlin`（Kotlin 惯例：1.0 就有的 API 不打戳）。
- 同一名字在不同接收者（`Array`/`Iterable`/`Collection`/`Sequence`/`CharSequence`/`Map`/基本类型）上版本可能不同，表中若写了具体戳，取的是该重载族里**带戳**的那批；无戳成员视为 1.0*。
- `consume`、`SQRT2` 这类"直觉上存在但数据里没有"的名字，本页一律不列（详见 `01-core-types.md` 第 10 节、`08-versioned-api.md` 的说明）。
