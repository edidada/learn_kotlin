# `kotlin` 包：内置类型、作用域函数、转换与标准函数

包规模实测（2.2.10）：**1629 条声明 / 339 个去重 API / 433 条带 `@SinceKotlin`**。按去重 API 口径（`docs/_data/dedup_api.tsv` 中 `pkg=kotlin`）的种类分布：`fun` 156、`val` 52、`var` 8、`class` 95、`interface` 6、`object` 4、`typealias` 18；若按未去重的原始声明口径（`slices/kotlin.tsv`）则是 `fun` 1265、`val` 194、`class` 123、`var` 15、`interface` 9、`object` 5、`typealias` 18（合计 1629，差值即重载与平台特化展开）。

这一包是"没有 import 也在你手边"的那一层。下面每条都直接摘自 `out2/slices/kotlin.tsv` 的真实签名（`sig` 列），版本取自 `@SinceKotlin`；**版本列写 `1.0*` 表示源码里没有版本戳**，即随首版存在（Kotlin 1.0 的 API 不打戳）。

## 1. 内置类型族

| 类别 | 类型（`kind=class/interface/object`，实测存在） | 备注 |
|---|---|---|
| 顶/底类型 | `Any`、`Any?`（可空用法）、`Nothing`、`Unit`（`object Unit`） | `Nothing` 用于永不返回，`TODO()` 的返回类型就是它 |
| 布尔与字符 | `Boolean`、`Char`、`BooleanArray`、`CharArray` | JVM 上 `Char` = 2 字节 UTF-16 code unit |
| 有符号数值 | `Byte`/`Short`/`Int`/`Long`/`Float`/`Double` + 对应 `*Array`、`Number` | `Number` 是抽象父类（`toInt`/`toLong`… 的 `public abstract fun toInt(): Int` 就在它身上） |
| 无符号数值 | `UByte`/`UShort`/`UInt`/`ULong` = **1.5**；`UByteArray`/`UIntArray`/`ULongArray`/`UShortArray` = **1.3** | 数组类型比标量类型早两个版本进库（数组 1.3 是实验期先行的那批） |
| 字符串 | `String`、`CharSequence`（接口） | String 的**扩展函数族**几乎全在 `kotlin.text`，见 `04-text.md` |
| 数组 | `Array<T>`、`Array<*>` 用法、`arrayOf`/`emptyArray` 工厂 | JVM 上 `Array<T>` 就是 `T[]` |
| 迭代与函数 | `Iterator<T>`、`Iterable<T>`（collections 包）、`Function`（接口）、`Comparable<T>` | `Function` 是 lambda 类型的公共父接口 |
| 延迟求值 | `Lazy<T>`、`LazyThreadSafetyMode`、`InitializedLazyImpl`/`SynchronizedLazyImpl`/`SafePublicationLazyImpl`/`UnsafeLazyImpl` | 后四个是实现类，正常只通过 `lazy()` 接触 |
| 结果与递归 | `Result<T>`（**1.3**）、`Failure`、`UNINITIALIZED_VALUE`、`DeepRecursiveFunction`/`DeepRecursiveScope`（**1.7**） | `Result` 是内联类，装箱时才见 `Failure` |
| 元组 | `Pair<A,B>`、`Triple<A,B,C>` | 由 `to()` 中缀函数产生 |
| 版本自述 | `KotlinVersion`（**1.1**）、`KotlinVersionCurrentValue` | 运行时读编译器/stdlib 版本 |

## 2. 五个作用域函数（选对它就选对了写法）

| API（真实签名） | 版本 | 返回 | 典型用法 |
|---|---|---|---|
| `public inline fun <T> T.apply(block: T.() -> Unit): T` | 1.0* | 接收者本身 | 配置对象：`AlertDialog.Builder(ctx).apply { setTitle(t) }` |
| `public inline fun <T, R> T.let(block: (T) -> R): R` | 1.0* | lambda 结果 | 空安全 + 作用域收窄：`x?.let { use(it) }` |
| `public inline fun <T, R> T.run(block: T.() -> R): R`（另有无接收者版 `fun <R> run(block: () -> R): R`） | 1.0* | lambda 结果 | 需要"以 this 计算并返回结果" |
| `public inline fun <T, R> with(receiver: T, block: T.() -> R): R` | 1.0* | lambda 结果 | 对不便写成扩展调用的对象分组操作 |
| `public inline fun <T> T.also(block: (T) -> Unit): T` | **1.1** | 接收者本身 | 副作用/日志，链式不改返回值 |
| `public inline fun <T> T.takeIf(predicate: (T) -> Boolean): T?` | **1.1** | `T?` | 条件取值，替代小 if |
| `public inline fun <T> T.takeUnless(predicate: (T) -> Boolean): T?` | **1.1** | `T?` | takeIf 取反 |

记忆点：`apply`/`also` 返回对象，`run`/`let`/`with` 返回结果；`apply`/`run`/`with` 用 `this`，`let`/`also` 用 `it`。`also` 与 `takeIf/takeUnless` 是 1.1 才补的（源码戳为 `1.1`）。

## 3. 前置条件与异常抛出（全部 1.0*）

| API | 抛出的异常 |
|---|---|
| `public inline fun require(value: Boolean): Unit` / `require(value: Boolean, lazyMessage: () -> Any)` | `IllegalArgumentException` |
| `public inline fun <T : Any> requireNotNull(value: T?): T`（+ `lazyMessage` 版） | `IllegalArgumentException` |
| `public inline fun check(value: Boolean): Unit`（+ `lazyMessage` 版） | `IllegalStateException` |
| `public inline fun <T : Any> checkNotNull(value: T?): T`（+ `lazyMessage` 版） | `IllegalStateException` |
| `public inline fun error(message: Any): Nothing = throw IllegalStateException(message.toString())` | 直接 `throw IllegalStateException` |
| `public inline fun assert(value: Boolean)` / `assert(value: Boolean, lazyMessage: () -> Any)` | `AssertionError`，且**只在 JVM 断言开启时生效**（`-ea`） |

`require` 管"调用方给的参数对不对"，`check` 管"我这边状态能不能继续"，`error` 无条件抛 `IllegalStateException`，`assert` 是可被关掉的调试辅助 —— 这个分工在签名与 KDoc 里能读出来，别混用。

## 4. `Result` 与异常捕获（1.3）

```
public inline fun <R> runCatching(block: () -> R): Result<R>              // @SinceKotlin("1.3")
public inline fun <T, R> T.runCatching(block: T.() -> R): Result<R>       // @SinceKotlin("1.3")
public inline fun <T> success(value: T): Result<T>
public inline fun <T> failure(exception: Throwable): Result<T>
```

1.3 戳的 `Result` 成员族（实测全部存在）：`map`、`mapCatching`、`recover`、`recoverCatching`、`onSuccess`、`onFailure`、`fold`、`getOrDefault`、`getOrElse`、`getOrThrow`、`throwOnFailure`、`createFailure`（internal）。`exceptionOrNull`、`getOrNull` 在源码里无戳（1.0*/internal 侧写法），列在这里是为了别让你 grep 时以为漏了。

## 5. 数组工厂与数组操作

| API | 说明 |
|---|---|
| `arrayOf(vararg elements: T)`、`arrayOfNulls(size: Int)`、`emptyArray()` | 都需要 `reified T`，故为 `inline`；每个都有 `expect` + `actual` 两条声明 |
| `booleanArrayOf` / `byteArrayOf` / `charArrayOf` / `shortArrayOf` / `intArrayOf` / `longArrayOf` / `floatArrayOf` / `doubleArrayOf` | 基本类型数组工厂，避免装箱 |
| `ubyteArrayOf` / `ushortArrayOf` / `uintArrayOf` / `ulongArrayOf` | **1.3**（unsigned 数组） |
| `copyOf` / `copyOfRange` / `fill` / `set` / `get` / `iterator` / `toList` | 各基本类型数组都有重载；`iterator` 在 `kotlin` 包内实测 18 条声明 |
| `contentEquals` / `contentDeepEquals` / `contentToString` / `contentDeepToString` | 数组比较**必须**用这些，`==` 对数组是引用比较 |

## 6. 延迟与委托

```
public expect fun <T> lazy(initializer: () -> T): Lazy<T>
public expect fun <T> lazy(mode: LazyThreadSafetyMode, initializer: () -> T): Lazy<T>
public expect fun <T> lazy(lock: Any?, initializer: () -> T): Lazy<T>
public fun <T> lazyOf(value: T): Lazy<T> = InitializedLazyImpl(value)
```

`lazy { }` 默认是 `LazyThreadSafetyMode.SYNCHRONIZED`（JVM `actual` 实现直接构造 `SynchronizedLazyImpl(initializer)`，源码可见）。三种模式：`SYNCHRONIZED`、`PUBLICATION`、`NONE`。

委托工厂在 `kotlin.properties`（17 个 API）：`Delegates.observable(initialValue, onChange)`、`Delegates.vetoable(initialValue, onChange)`、`notNull()`，接口 `ReadOnlyProperty<T,V>` / `ReadWriteProperty<T,V>`（都是 `fun interface`）、`PropertyDelegateProvider`（**1.4**），以及 `provideDelegate` 运算符。抽象基类 `ObservableProperty<V>` 提供可覆写的 `beforeChange` / `afterChange`。

## 7. 类型转换（`toXxx` 家族）

| 方向 | API | 版本 |
|---|---|---|
| 有符号互转 | `toByte/toShort/toInt/toLong/toFloat/toDouble/toChar/toUByte/toUShort/toUInt/toULong/toUShort` 各 8 个以上重载（`Int.toLong()`、`Byte.toInt()`…） | 有符号族 1.0*；**unsigned 目标 1.5**（`toUByte`/`toUInt`/`toULong`/`toUShort`） |
| 字符串→数字 | `String.toInt()` / `String.toInt(radix: Int)`（JVM 走 `java.lang.Integer`） | **1.1**（radix 版） |
| 字符串→布尔 | `public actual inline fun String?.toBoolean(): Boolean` | **1.4**（可空接收者）；严格版 `toBooleanStrict`/`toBooleanStrictOrNull` = **1.5** |
| 字符串→无符号 | `String.toUInt()` / `toUInt(radix)` / `toULong()` … | **1.5** |
| 大数 | `Int/Long/Float/Double/String.toBigDecimal()`、`toBigInteger()`（含 `kotlin.text.BigDecimal/BigInteger` typealias） | **1.2** |
| 高精度互转 | `Double.toRawBits()` / `Double.fromBits()` 等 | **1.2** |
| Duration | `Int.toDuration(unit)` 一族（在 `kotlin.time`） | 见 `06-...md` |

```kotlin
// 实测签名的直接用法
val u: UInt = 42.toUInt()                 // @SinceKotlin("1.5")
val bd = "3.14".toBigDecimal()            // @SinceKotlin("1.2")
val strict = "true".toBooleanStrictOrNull() // @SinceKotlin("1.5")
```

## 8. 异常类型与 common/JVM 双层写法

Kotlin 在 1.x 把大量 JVM 专有异常"提升到 common"，源码里表现为 **`class` + `typealias` 成对出现**，实测对：

| 异常 | common 声明 | JVM `typealias` |
|---|---|---|
| `IllegalArgumentException` / `IllegalStateException` / `IndexOutOfBoundsException` / `NullPointerException` / `NumberFormatException` / `RuntimeException` / `UnsupportedOperationException` / `ClassCastException` / `Error` / `Exception` / `AssertionError` | 1.0*（无戳） | **1.1** |
| `ArithmeticException` | **1.3** | **1.3** |
| `ConcurrentModificationException` | 1.0* | **1.3** |
| `NoSuchElementException` | **1.9** | **1.1** |
| `KotlinNullPointerException` / `TypeCastException` / `NoWhenBranchMatchedException` / `UninitializedPropertyAccessException` / `NotImplementedError`（`TODO()` 抛的就是它） | 1.0* | —（Kotlin 自有） |
| `KotlinNothingValueException` | **1.4** | — |

`Throwable` 侧的扩展（实测）：`printStackTrace()`（expect **1.4** / JVM actual）、`Throwable.printStackTrace(stream: PrintStream)`、`printStackTrace(writer: PrintWriter)`、`stackTraceToString()`（**1.4**）、`addSuppressed`（**1.1**/**1.4** 两批）。

## 9. 生命周期与工具函数

| API | 版本 | 说明 |
|---|---|---|
| `public inline fun <T : AutoCloseable?, R> T.use(block: (T) -> R): R` | **2.0**（common `expect`）/ **1.2**（JVM `actual`） | 2.0 那次是随 `kotlin.AutoCloseable` 提升到 common 才打的戳，不是"2.0 才有 use" |
| `public interface AutoCloseable` + `public typealias AutoCloseable` | **2.0** | 同上，common 化的证据 |
| `public inline fun <R> synchronized(lock: Any, block: () -> R): R` | 1.0* | JVM `synchronized` 块的表达式化 |
| `public inline fun repeat(times: Int, action: (Int) -> Unit)` | 1.0* | 带下标的循环，不产生中间集合 |
| `public inline fun TODO(reason: String): Nothing` | 1.0* | 抛 `NotImplementedError` |
| `print` / `println` / `printStackTrace` | 1.0* | `readLine()`（`String?`）与 `readln()`（非空，`readln` 系列实测在 `kotlin.io`） |
| `enumValues<T>()` / `enumValueOf<T>(name)` | **1.1** | 反射式枚举遍历/查名 |
| `EnumEntries<E> : List<E>` | **1.9** | `entries` 属性的类型（1.9 起 `values()` 之外优先用 `entries`） |
| `enumEntries<T>()` | **2.0** | 顶层 reified 版；1.8 里已有 internal `enumEntries(...)` 与 `EnumEntriesList` |
| `public typealias Comparator<T> = java.util.Comparator<T>` | **1.1** | SAM 友好写法来自这个 typealias |

## 10. `kotlin.math`（207 声明 / 51 API / 196 带戳）

实测函数名清单（`slices/kotlin_math.tsv` 里 `kind=fun` 的 name 去重）：`abs`、`absoluteValue`、`sign`、`ceil`、`floor`、`round`、`roundToInt`、`roundToLong`、`truncate`、`withSign`、`sqrt`、`cbrt`、`pow`、`exp`、`expm1`、`ln`、`ln1p`、`log`、`log10`、`log2`、`sin`、`cos`、`tan`、`asin`、`acos`、`atan`、`atan2`、`sinh`、`cosh`、`tanh`、`asinh`、`acosh`、`atanh`、`max`、`min`、`hypot`、`nextUp`、`nextDown`、`nextTowards`、`IEEErem`。

公开常量只有两个，且都是 **1.2**：

```
public const val PI: Double = 3.141592653589793
public const val E: Double = 2.718281828459045
```

> 我一开始以为 `SQRT2`/`LN2`/`LOG2E` 这些 Java `Math` 里有的常量也在 Kotlin `kotlin.math` 顶层 —— 实测**不成立**：slice 里 `LN2` 只以 `internal val LN2: Double = ln(2.0)` 存在（内部实现，不是公开 API）。要写 `sqrt(2.0)` 或 `1.0 / sqrt(2.0)`。这类"从 Java 直觉外推"的坑，正是这份文档坚持逐条 grep 的原因。

另有 1.2 的属性扩展：`Double.absoluteValue`、`Int.absoluteValue`、`Long.absoluteValue`、`Float.absoluteValue`、`Double.sign`、`Int.sign`、`Long.sign`、`Double.ulp`（`expect` + JVM `actual inline val ... get()` 成对出现）。

这个包 **94% 的声明都带版本戳**（196/207），是"从 JVM 专有提升为 common"改造最彻底的包之一。

## 11. 完整示例

```kotlin
// 1) 作用域函数选型：apply 配对象，let 判空，also 打日志，takeIf 做条件
fun describe(n: Int?) =
    n?.takeIf { it > 0 }
        ?.let { "$it is positive" }
        .also { println("computed: $it") }
        ?: "null or non-positive"

// 2) Result + contracts 风格的安全解析（只用实测存在的 API）
fun parseOrError(text: String): Result<Int> =
    text.runCatching { toInt() }                    // @SinceKotlin("1.3")
        .recover { IllegalArgumentException("bad number: $text") }

// 3) 委托 + 数组比较
import kotlin.properties.Delegates
var name: String by Delegates.observable("<init>") { _, old, new -> println("$old -> $new") }
val same = intArrayOf(1, 2, 3).contentEquals(intArrayOf(1, 2, 3))  // 不要用 ==
```

## 数据来源与校验方法

- 版本：`kotlin-stdlib-2.2.10-sources.jar`（本机 Gradle 缓存），源文件主要是 `commonMain/kotlin/{KotlinH,Array,Arrays,Numbers,Result,Lazy,Standard,Tuples,Comparisons,Character,Duration,...}.kt`、`commonMain/kotlin/properties/*.kt`、`commonMain/kotlin/enums/*.kt`、`jvmMain/kotlin/{NumberJVM,Throwable,StaticBackend,...}.kt`。
- 本页每条签名来自仓库内的切片 [`docs/_data/slices/kotlin.tsv`](../_data/slices/kotlin.tsv)（1,630 行 = 表头 + 1,629 条声明）、`kotlin_properties.tsv`（18 行）、`kotlin_enums.tsv`（18 行）、`kotlin_math.tsv`（208 行）、`kotlin_annotation.tsv`（7 行，`kotlin.annotation` 包共 6 条声明）；复核：`awk -F'\t' '$3=="takeIf"' docs/_data/slices/kotlin.tsv`。列序：`since kind name receiver arity sig loc`，`loc` 已改成相对源码 jar 根目录的路径。
- 本页涉及的**扩展属性**（`Char.code` 1.5、`Char.directionality` 无戳、`Throwable.stackTrace` 无戳、`Throwable.suppressedExceptions` 1.4、`Double.absoluteValue`/`sign`/`ulp` 1.2）主切片没收录，单列在 [`docs/_data/ext_props.tsv`](../_data/ext_props.tsv)，用 `awk -F'\t' '$4=="absoluteValue"' docs/_data/ext_props.tsv` 复核。
- 统计口径与脚本细节见 [`00-overview.md` 第 5 节](00-overview.md)；"1.0*"的判读规则：源码该声明行与其上 6 行内没有 `@SinceKotlin`，按 Kotlin 惯例即随 1.0 存在。
- `kotlin.math` 函数名清单是 `slices/kotlin_math.tsv` 里 `kind=fun` 的 `name` 列去重结果，可直接 `awk -F'\t' '$2=="fun"{print $3}' | sort -u` 复现。
