# 按版本查 API：@SinceKotlin 清单（2.2.10 源码实测）

用法：想知道某个 API 从哪个 Kotlin 版本开始能用（以及"我的项目锁在 1.x 时能不能用它"），查本页。数据来自 `kotlin-stdlib-2.2.10-sources.jar` 的注解解析，完整原始清单在脚本产物 `out2/api_by_version.tsv`（3,486 行去重 API 里带版本戳的部分）。本页下表的"去重 API 名 / 带戳声明"都只算**主解析器**口径；扩展属性（`val Recv.name`）另有 26 个名字不在其中，见 `00-overview.md` 第 5.1 节与各版本条目里的"扩展属性"补充。

## 1. 每个版本进库多少 API（去重名数 / 重载级声明数）

| 版本 | 去重 API 名 | 带戳声明 | 主要落点（该版本涉及的包） |
|---|---|---|---|
| 1.1 | 118 | 310 | `kotlin`（`takeIf`/`also`）、`kotlin.collections`（大量）、`kotlin.text`、`kotlin.reflect`、异常 `typealias` |
| 1.2 | 96 | 293 | `kotlin.math`（占大头）、`kotlin.text`、`kotlin.collections`、`kotlin.streams`、`kotlin.jvm` |
| **1.3** | **261** | 761 | 最大的扩张：`kotlin.coroutines`(25+)、`kotlin.contracts`、`kotlin.time`、`kotlin.random`、`kotlin.collections`、`Result`、unsigned 数组 |
| 1.4 | 172 | 1086 | `kotlin.text`(53)、`kotlin.sequences`(24)、`kotlin.collections`、`kotlin.coroutines.cancellation`、`kotlin.io.path`(2) |
| 1.5 | 153 | 359 | `kotlin.io.path`（63 个名，Path API 首次成批）、`kotlin.text`(32)、`kotlin.ranges`(14)、unsigned 标量转正 |
| 1.6 | 23 | 52 | `buildList`/`buildMap`/`buildSet`、`readln`/`readlnOrNull`、`typeOf`、`Duration`/`DurationUnit`、`rotateLeft/Right` |
| 1.7 | 30 | 140 | `DeepRecursiveFunction`/`DeepRecursiveScope`、`ContextFunctionTypeParams`、集合/序列/字符串的 `max*/min*` 家族、`matchAt`/`matchesAt` |
| 1.8 | 36 | 46 | `kotlin.jvm.optionals`（`Optional` 互转）、`kotlin.io.encoding`（`ExperimentalEncodingApi` 等 8 条）、`copyToRecursively`/`deleteRecursively`、`enumEntries`(internal) |
| 1.9 | 33 | 109 | `rangeUntil`/`..<` 的库支持、`EnumEntries`、`kotlin.time` 重构批（`TimeSource`/`measureTimedValue`/`TestTimeSource`）、`@Volatile`、`listOf`/`mapOf`/`setOf` 的新重载 |
| 2.0 | 23 | 26 | `kotlin.uuid.Uuid`、common `AutoCloseable` + `use`、`removeRange`、`PaddingOption`/`withPadding`、`enumEntries()`、`@JsStatic` |
| 2.1 | 41 | 76 | `kotlin.concurrent.atomics` 整包（24 个名）、`kotlin.io.path` 的 `walk`/`visitFileTree`/`fileVisitor`、`kotlin.time.Instant`/`Clock`、`Uuid` 互转 4 条 |
| 2.2 | 22 | 55 | `kotlin.text.HexFormat` 与 `hexToXxx`/`toHexString`（12 个名）、`kotlin.context`/`contextOf`、`Base64`（`@SinceKotlin("2.2")`+`@WasExperimental`）、`@MustUseReturnValue`/`@IgnorableReturnValue`、`asClock`/`parseOrNull` |

> 提醒：**"带戳声明"是重载级，"去重 API 名"才是你直觉里的"API 个数"**。1.4 的声明数（1086）远大于 1.3（761），但新 API 名反而更少（172 vs 261）—— 1.4 那批主要是给已有 API 补 common 声明戳。

## 2. 2.x 全量清单（名数少，可以整份列出）

### 2.0（23 个名）

`kotlin`: `AutoCloseable`(interface + typealias)、`use`、`ConsistentCopyVisibility`、`ExposedCopyVisibility`
`kotlin.collections`: `removeRange` · `kotlin.enums`: `enumEntries`
`kotlin.io.encoding`: `PaddingOption`、`withPadding` · `kotlin.text`: `toCharArray`
`kotlin.js`: `JsStatic`、`ExperimentalJsStatic`、`ExperimentalJsCollectionsApi`
`kotlin.uuid`: `Uuid`、`ExperimentalUuidApi`、`getUuid`、`putUuid`、`toJavaUuid`、`toKotlinUuid`

### 2.1（41 个名）

`kotlin.concurrent.atomics`（整包）：`AtomicInt`、`AtomicLong`、`AtomicBoolean`、`AtomicReference`、`AtomicIntArray`、`AtomicLongArray`、`AtomicArray`、`ExperimentalAtomicApi`、`fetchAndIncrement`/`fetchAndIncrementAt`、`incrementAndFetch`/`incrementAndFetchAt`、`decrementAndFetch`/`decrementAndFetchAt`、`fetchAndDecrement`/`fetchAndDecrementAt`、`plusAssign`、`minusAssign`、`asJavaAtomic`/`asKotlinAtomic`/`asJavaAtomicArray`/`asKotlinAtomicArray`
`kotlin.concurrent.internal`: `compareAndExchange`
`kotlin.io.path`: `walk`、`visitFileTree`、`fileVisitor`、`FileVisitorBuilder`、`PathWalkOption`
`kotlin.time`: `Instant`、`Clock`、`toJavaInstant`、`toKotlinInstant`、扩展属性 `Instant.isDistantFuture`/`Instant.isDistantPast`
`kotlin.uuid`: `parseHexDash`、`toHexDashString`、`toUByteArray`、`fromUByteArray`
`kotlin`: `SubclassOptInRequired`

### 2.2（22 个名）

`kotlin.text`: `HexFormat`、`toHexString`、`hexToInt`、`hexToLong`、`hexToShort`、`hexToByte`、`hexToUInt`、`hexToULong`、`hexToUShort`、`hexToUByte`、`hexToByteArray`、`hexToUByteArray`
`kotlin`: `context`、`contextOf`、`MustUseReturnValue`、`IgnorableReturnValue`
`kotlin.io.encoding`: `Base64` · `kotlin.sequences`: `sequenceOf`（空与单元素两个重载）· `kotlin.time`: `TimeSource.asClock`、`Instant.parseOrNull`（`Instant.kt:401`）· `kotlin.experimental`: `ExpectRefinement`

## 3. 1.x 各版本代表 API（挑能记住的，全部来自源码）

- **1.1**：`takeIf`、`takeUnless`、`also`、`enumValues`、`enumValueOf`、`addSuppressed`、`IllegalArgumentException`/`IllegalStateException`/`NullPointerException` 等异常的 common `typealias`、`KProperty` 反射接口一批、`String` 的一批文本 API。
- **1.2**：`PI`、`E`（`public const val`）、`absoluteValue`/`sign`/`ulp` 属性族、`toBigDecimal`/`toBigInteger`、`Double.toRawBits`/`fromBits`、`String.toInt(radix)`、`Closeable.use`、`java.util.stream` 互转（`kotlin.streams`）。
- **1.3**：协程基础设施（`Continuation`/`CoroutineContext`/`suspendCoroutine` 等 25+ 个名）、`contract {}` DSL（`ContractsKt`：`contract`、`ContractBuilder`、`CallsInPlace`、`Returns`、`ReturnsNotNull`、`ConditionalEffect`、`SimpleEffect`、`Effect`、`InvocationKind`、`ExperimentalContracts`）、`Random`（+ `asJavaRandom`/`asKotlinRandom`/`nextInt`/`nextLong`/`nextUBytes`）、`ExperimentalTime` 与 `MonotonicTimeSource`/`AbstractDoubleTimeSource`（这两个名字的戳是 1.3，1.9 才换成 `TimeSource` 体系）、`Result`、`UByteArray`/`UIntArray`/`ULongArray`/`UShortArray`、`RequiresOptIn`/`OptIn`/`ExperimentalStdlibApi`、`Metadata`、`BuilderInference`、`ArithmeticException` 的 common 声明。注意 `kotlin.concurrent.atomics` **不在** 1.3，它是 2.1 才成包进库的（见上表）。
- **1.4**：`maxOf`/`minOf`/`maxOrNull`/`minOrNull`/`maxByOrNull`/`minByOrNull`/`maxWithOrNull`/`minWithOrNull`、`flatMap*` 族、`runningFold`/`runningReduce`/`scan`/`scanIndexed`、`shuffled`、`sumOf`、`reduceOrNull`/`reduceIndexedOrNull`、扩展属性侧 `Throwable.suppressedExceptions` 与 `KType.javaType`、`onEach`（集合版 1.1 就有，1.4 是补齐各类型数组重载）、`kotlin.text` 侧 53 个名（`StringBuilder` 的 `append`/`appendLine`/`insert`/`set`/`deleteRange`/`setRange` 族、`String.format`、`sumOf`/`runningFold`/`scan`）、`@DeprecatedSinceKotlin`、`@OverloadResolutionByLambdaReturnType`、`@Throws` 的 common 声明、`KotlinNothingValueException`、`stackTraceToString`、`PropertyDelegateProvider`、`CancellationException` 相关 3 个名。
- **1.5**：`kotlin.io.path` 首次成批（63 个名：`Path`、`toPath`、`exists`、`isRegularFile`、`isDirectory`、`isSymbolicLink`、`isSameFileAs`、`createFile`、`createDirectory`、`createDirectories`、`createTempFile`、`createTempDirectory`、`createSymbolicLinkPointingTo`、`createLinkPointingTo`、`deleteIfExists`、`deleteExisting`、`moveTo`、`copyTo`、`fileSize`、`fileStore`、`getAttribute`、`setAttribute`(批内)、`getLastModifiedTime`、`getOwner`、`getPosixFilePermissions`、`readText`/`readLines`/`readBytes`、`appendText`/`appendBytes`/`appendLines`、`writeBytes`/`writeText`(批内)、`bufferedReader`/`bufferedWriter`、`inputStream`/`outputStream`、`forEachLine`、`forEachDirectoryEntry`、`listDirectoryEntries`、`absolutePathString`、`div`、`absolute`、`notExists`、`fileAttributesView`/`fileAttributesViewOrNull`）；`UByte`/`UShort`/`UInt`/`ULong` 标量转正、`floorDiv`/`mod`、`Random.nextUInt`/`nextULong`、`toUByte`/`toUInt`/`toULong`/`toUShort`；这一批里还有一串**扩展属性**（主解析器按接收者归档，需用 `out3/ext_props.tsv` 查）：`Path.pathString`、`Path.name`、`Path.extension`、`Path.nameWithoutExtension`、`Path.invariantSeparatorsPathString`（均 1.5）；同版本还有 `Char.code`（1.5）。注意：`toFile` **不是** stdlib API（`Path.toFile()` 是 `java.nio.file.Path` 自带的方法），stdlib 侧只有 `URI.toPath()`（1.5）。`rangeUntil` 也不属于 1.5，它是 1.9。
- **1.6**：`buildList`、`buildSet`、`buildMap`、`readln`、`readlnOrNull`、`typeOf`、`Duration`、`DurationUnit`、`toDuration`、`times`、`toJavaDuration`/`toKotlinDuration`、`rotateLeft`/`rotateRight`、`splitToSequence`、`JvmDefaultWithCompatibility`、`JvmRepeatable`。
- **1.7**：`DeepRecursiveFunction`、`DeepRecursiveScope`、`ContextFunctionTypeParams`（context receiver 的地基）、`@IntrinsicConstEvaluation`、集合/序列/字符串的 `max`/`maxBy`/`maxWith`/`min`/`minBy`/`minWith` 收敛（旧的 `maxBy` 语义在这次被拆成 `maxBy`/`maxByOrNull`）、`matchAt`/`matchesAt`、`Progression.first/firstOrNull/last/lastOrNull`、`invoke`。
- **1.8**：`kotlin.jvm.optionals`（`Optional.getOrNull`/`getOrElse`/`getOrDefault`/`asSequence`/`toCollection`/`toList`/`toSet`）、`kotlin.io.encoding` 雏形 8 条（`ExperimentalEncodingApi`、`encodingWith`/`decodingWith`、`isInMimeAlphabet`、`platformEncodeToString` 等 internal）、`copyToRecursively`/`deleteRecursively`/`CopyActionContext`/`CopyActionResult`/`OnErrorResult`、`enumEntries`(internal)/`EnumEntriesList`、`@ExperimentalSubclassOptIn`、`@JvmSerializableLambda`、`@SourceDebugExtension`、`@ExperimentalObjCName`/`@ExperimentalObjCRefinement`、Native 侧 5 个名。
- **1.9**：`rangeUntil`（配合 `..<`）、`contains`/`OpenEndRange`、`EnumEntries`/`enumEntriesIntrinsic`、`NoSuchElementException` 的 common 声明、`@Volatile`、`listOf`/`mapOf`/`setOf` 新重载、`kotlin.time` 大重构（1.9 批共 10 个名：`TimeSource`、`TimeMark`、`ComparableTimeMark`、`ValueTimeMark`、`AbstractLongTimeSource`、`WithComparableMarks`、`measureTime`、`measureTimedValue`、`TimedValue`、`TestTimeSource`；而 `MonotonicTimeSource`、`AbstractDoubleTimeSource` 是 1.3 的旧戳，不在 1.9 批内）、`appendLine`/`append`/`insert`/`get`/`regionMatches`（文本侧）、`ExperimentalNativeApi`、`@JsFileName`、`ImplicitlyActualizedByJvmDeclaration`。

## 4. 怎么把这些规则用到项目里

1. **锁版本**：`-api-version 1.9` 会让编译器直接无视 2.x 才有的声明（例如 `HexFormat`、`AtomicInt`、`Uuid`），这是"我的库要发给还在用 Kotlin 1.9 的用户"的标准做法。
2. **查单个 API**：IDE 里把光标停在函数上查看声明（会看到 `@SinceKotlin`），或本机 grep：
   ```bash
   grep -P '\tHexFormat\t' ~/.gradle/caches/**/kotlin-stdlib-*-sources.tsv  # 用本页脚本产物更快
   awk -F'\t' '$3=="buildList"{print $1,$6}' docs/.../slices/kotlin_collections.tsv
   ```
3. **别把 `@SinceKotlin` 当"引入即稳定"**：一个 API 可能先以实验形态存在（`kotlin.io.encoding` 1.8 打了 internal 批的戳，`Base64` 类本体在 2.2.10 源码里标 `@SinceKotlin("2.2")` 且带 `@WasExperimental(ExperimentalEncodingApi::class)`）。判断"能不能不带 opt-in 直接用"，要看它当前的 `@RequiresOptIn` 注解，而不是戳的早晚。
4. **实验包/内部包不算 API**：按官方规则，包名含 `internal`、`experimental` 的都不算公开 API（本页数据里对应 `kotlin.internal`、`kotlin.jvm.internal`、`kotlin.experimental`、`kotlin.concurrent.internal`、`kotlin.native`）。

## 数据来源与校验方法

- 唯一来源：`kotlin-stdlib-2.2.10-sources.jar` 的逐行解析（脚本见 `00-overview.md` 第 5 节），产物 `api_by_version.tsv`。
- 复核某一行：`awk -F'\t' '$1=="2.2" && $2=="kotlin.text"' out2/api_by_version.tsv`。
- 计数口径：`去重 API 名` = `包|种类|名称` 三元组唯一；`带戳声明` = 该版本注解命中的声明行数。
- 已知解析噪声：少数条目名字落在接收者类型上（如 `kotlin.collections.Int`、`kotlin.text.Char`、`kotlin.time.Boolean`），是自动名抽取在特殊签名行上的产物，本页清单里已剔除这些明显非 API 名；扩展属性（`val Recv.name`）主解析器也会落在接收者上，已用 `out3/ext_props.tsv` 单独补齐（26 个名字，含 `Path.pathString`、`Char.code`、`Double.absoluteValue`）。若某条与你的项目实际不符，以 IDE 跳转到的源码注解为准。
