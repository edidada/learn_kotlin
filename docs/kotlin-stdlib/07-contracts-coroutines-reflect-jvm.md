# Kotlin 标准库 API 大全（7）：kotlin.contracts / 协程基础设施 / kotlin.reflect / JVM 底层与元注解

> 本篇覆盖 stdlib 中"元/底层"一层的包：`kotlin.contracts`、`kotlin.coroutines`（含 `intrinsics`、`jvm.internal`、`cancellation`）、`kotlin.reflect`、`kotlin.jvm`（含 `jvm.internal`、`jvm.functions`）、`kotlin.annotation`、`kotlin.experimental`、`kotlin.internal`、`kotlin.system`，并对 `kotlin.concurrent` 做一句提示。内容只基于从 **kotlin-stdlib 2.2.10 sources.jar** 抽取的 slice TSV 与对应源文件；文末附《数据来源与校验方法》。

## 1. kotlin.contracts —— 函数契约 DSL

契约（contract）允许在 inline 函数上声明"该函数 invocation 的可观察效果"，编译器据此做智能类型收敛与流分析。以下 KDoc 要点全部摘自源文件 `commonMain/kotlin/contracts/ContractBuilder.kt` 与 `Effect.kt`。

### 1.1 API 一览（slice：kotlin_contracts.tsv，共 14 行）

| API / 注解（真实签名） | @SinceKotlin | experimental 与否 | 一句话说明 |
|---|---|---|---|
| `public interface ContractBuilder` | 1.3 | 是（`@ContractsDsl @ExperimentalContracts`） | 契约 DSL 的作用域，作为 `contract {}` lambda 的接收者 |
| `@ContractsDsl public fun returns(): Returns` | - | 是 | 描述"函数正常返回（未抛异常）"这一效果 |
| `@ContractsDsl public fun returns(value: Any?): Returns` | - | 是 | 描述"函数以指定值正常返回"；KDoc：value 仅限 `true`/`false`/`null` |
| `@ContractsDsl public fun returnsNotNull(): ReturnsNotNull` | - | 是 | 描述"函数以任意非 null 值正常返回" |
| `@ContractsDsl public fun <R> callsInPlace(lambda: Function<R>, kind: InvocationKind = InvocationKind.UNKNOWN): CallsInPlace` | - | 是 | 声明 lambda 参数"就地调用"；KDoc 明确：声明该效果的函数必须是 _inline_ |
| `public inline fun contract(builder: ContractBuilder.() -> Unit)` | 1.3 | 是 | 指定函数契约；KDoc：契约描述必须位于函数开头且至少含一个效果，目前只有顶层函数可声明契约 |
| `public interface Effect` | 1.3 | 是 | invocation 效果之根类型（可直接观察的返回，或副作用如就地调用） |
| `public interface ConditionalEffect : Effect` | 1.3 | 是 | "观察到某效果后某条件为真"的效果，经 `implies` 挂到 SimpleEffect 上 |
| `public interface SimpleEffect : Effect` | 1.3 | 是 | 可在函数 invocation 后直接观察到的效果 |
| `public infix fun implies(booleanExpression: Boolean): ConditionalEffect`（SimpleEffect 成员） | - | 是 | 观察到该效果即保证 booleanExpression 为真；KDoc 限定表达式子集：true/false 判断、`== null`/`!= null`、`is`/`!is`，及其 `&&`/`||`/`!` 组合 |
| `public interface Returns : SimpleEffect` | 1.3 | 是 | 以给定返回值正常返回的效果 |
| `public interface ReturnsNotNull : SimpleEffect` | 1.3 | 是 | 以任意非 null 返回值正常返回的效果（注意：真实名称是 `ReturnsNotNull`，数据中不存在 `ReturnsNotNullable`） |
| `public interface CallsInPlace : Effect` | 1.3 | 是 | KDoc：函数参数仅在函数执行未返回之前被调用、函数完成后不可能再被调用，即"就地调用" |
| `public enum class InvocationKind { AT_MOST_ONCE, AT_LEAST_ONCE, EXACTLY_ONCE, UNKNOWN }` | 1.3（源码 ContractBuilder.kt:96；slice 未提取，见 §7 存疑点） | 是 | 声明就地调用次数：0 或 1 次 / ≥1 次 / 恰好 1 次 / 次数未知 |
| `public annotation class ExperimentalContracts` | 1.3（源码 ContractBuilder.kt:23；slice 未提取） | —— | `@RequiresOptIn @Retention(BINARY) @MustBeDocumented`；使用契约 API 需 `@OptIn(ExperimentalContracts::class)` 或 `-opt-in=kotlin.contracts.ExperimentalContracts` |

`callsInPlace` KDoc 的两点语义（原文要点）：1) lambda 只能在 owner 函数调用期间被调用，owner 调用完成后不会再被调用；2) （可选）按 `kind` 参数指定调用次数。

注：任务提示中的 `callableReferences` 在 slice 与整个抽取源码树中均 grep 不到（0 命中），`readsInFixedPosition` 同样 0 命中，故不收录。

### 1.2 示例（只用上表核实过的 API）

```kotlin
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

@OptIn(ExperimentalContracts::class)
inline fun <R> runExactlyOnce(block: () -> R): R {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    return block()
}

@OptIn(ExperimentalContracts::class)
fun parse(s: String): Int? = s.toIntOrNull()

@OptIn(ExperimentalContracts::class)
inline fun requireNotNullContract(value: Int?, block: (Int) -> Unit) {
    contract {
        // SimpleEffect.implies：观察到 returnsNotNull 即保证 value != null
        returnsNotNull() implies (value != null)
    }
    if (value != null) block(value)
}
```

## 2. kotlin.coroutines —— stdlib 里的协程"基础设施"

**定位**：`kotlin.coroutines` 提供的是协程的底层机制——延续对象、上下文、拦截器、`suspendCoroutine` 桥接原语；它是 `kotlin.coroutines.Continuation` 类型被编译器内建识别的一层。

**必须先说清的实证结论**：`launch`/`async`/`withContext` 不在 kotlin-stdlib，而在 kotlinx-coroutines-core。对 4 个协程 slice（共 177 行）做词边界 grep：

```
$ grep -nw -E "launch|async|withContext" kotlin_coroutines.tsv kotlin_coroutines_intrinsics.tsv \
    kotlin_coroutines_jvm_internal.tsv kotlin_coroutines_cancellation.tsv
（无输出；0 行匹配）
$ grep -rn "fun launch(\|fun async(\|withContext(" \
    src/commonMain/kotlin/coroutines src/jvmMain/kotlin/coroutines out/slices/kotlin_coroutines*.tsv | wc -l
0
```

同理，slice 数据中也**没有** `startCoroutine`/`createCoroutine`（仅 `IntrinsicsJvm.kt` 内部有私有的 `createCoroutineFromSuspendFunction`/`createSimpleCoroutineForSuspendFunction`），没有 `AbstractContinuation`（`jvmMain/kotlin/coroutines/jvm/internal/` 目录下实测只有 ContinuationImpl.kt、CoroutineStackFrame.kt、DebugMetadata.kt、DebugProbes.kt、GeneratedCodeMarkers.kt、RunSuspend.kt、Spilling.kt、boxing.kt 八个文件，这些是 kotlinx 的公开类）。

### 2.1 Continuation 与挂起桥接（slice：kotlin_coroutines.tsv，66 行）

| API（真实签名） | @SinceKotlin | experimental 与否 | 一句话说明 |
|---|---|---|---|
| `public interface Continuation<in T>` | 1.3 | 否 | 协程计算"下一步"的表示；编译器内建识别 |
| `public val context: CoroutineContext`（Continuation 成员） | - | 否 | 该 continuation 的协程上下文 |
| `public fun resumeWith(result: Result<T>)` | - | 否 | 以 Result（成功或异常）恢复执行 |
| `public inline fun <T> Continuation<T>.resume(value: T): Unit` | 1.3 | 否 | 以正常值恢复的扩展（resumeWith(value) 的便捷形态） |
| `public inline fun <T> Continuation<T>.resumeWithException(exception: Throwable): Unit` | 1.3 | 否 | 以异常恢复的扩展 |
| `public inline fun <T> Continuation(context: CoroutineContext, resumeWith: (Result<T>) -> Unit)` | 1.3 | 否 | 工厂函数：由上下文 + resumeWith 回调构造 Continuation |
| `public suspend inline fun <T> suspendCoroutine(crossinline block: (Continuation<T>) -> Unit): T` | 1.3 | 否 | 把回调式 API 桥接为挂起调用；实现内部用 `SafeContinuation(c.intercepted())`（源码 Continuation.kt:145） |
| `public suspend inline val coroutineContext: CoroutineContext` | 1.3 | 否 | 挂起函数内取当前协程上下文 |

### 2.2 CoroutineContext 与其元素

| API（真实签名） | @SinceKotlin | experimental 与否 | 一句话说明 |
|---|---|---|---|
| `public interface CoroutineContext` | 1.3 | 否 | 协程上下文：元素容器，按 Key 索引 |
| `public operator fun <E : Element> get(key: Key<E>): E?` | - | 否 | 按 Key 取元素 |
| `public fun <R> fold(initial: R, operation: (R, Element) -> R): R` | - | 否 | 遍历所有元素做折叠 |
| `public operator fun plus(context: CoroutineContext): CoroutineContext` | - | 否 | 合并两个上下文（同 Key 取右侧；内部对 `minusKey`/`ContinuationInterceptor` 做处理，源码 CoroutineContext.kt:30-38） |
| `public fun minusKey(key: Key<*>): CoroutineContext` | - | 否 | 移除指定 Key 的元素 |
| `public interface Key<E : Element>` | - | 否 | 上下文元素的键类型 |
| `public interface Element : CoroutineContext` | - | 否 | 单个上下文元素；携带 `public val key: Key<*>` |
| `public abstract class AbstractCoroutineContextElement(public override val key: Key<*>) : Element` | 1.3 | 否 | Element 的常用抽象实现 |
| `public abstract class AbstractCoroutineContextKey<B : Element, E : B>(baseKey: Key<B>, safeCast: (element: Element) -> E?)` | 1.3 | 否 | 支持多态 Key 的抽象基类（内部 `topmostKey`/`tryCast`/`isSubKey`） |
| `public fun <E : Element> Element.getPolymorphicElement(key: Key<E>): E?` | 1.3 | 否 | 按多态 Key 在 Element 内查找 |
| `public fun Element.minusPolymorphicKey(key: Key<*>): CoroutineContext` | 1.3 | 否 | 按多态 Key 从 Element 移除 |
| `public object EmptyCoroutineContext : CoroutineContext, Serializable` | 1.3 | 否 | 不含任何元素的上下文（`readResolve` 保证单例语义） |
| `internal class CombinedContext(left: CoroutineContext, element: Element)` | 1.3 | 否 | `plus` 结果的内部二叉树表示（含私有 `Serialized` 支持序列化） |
| `public interface ContinuationInterceptor : CoroutineContext.Element` | 1.3 | 否 | 拦截 continuation 的上下文元素 |
| `public fun <T> interceptContinuation(continuation: Continuation<T>): Continuation<T>` | - | 否 | 返回被拦截包装后的 continuation |
| `public fun releaseInterceptedContinuation(continuation: Continuation<*>)` | - | 否 | 归还拦截产生的实例（复用池钩子） |
| `internal expect class SafeContinuation<in T> : Continuation<T>`（jvm actual 用 AtomicReferenceFieldUpdater 保证一次性 resume） | 1.3 | 否 | 防止同一 continuation 被恢复两次的内部包装 |

### 2.3 示例：只用 stdlib 基础设施驱动一段挂起代码

```kotlin
import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.COROUTINE_SUSPENDED
import kotlin.coroutines.intrinsics.intercepted
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

class GreetingElement(val text: String) : AbstractCoroutineContextElement(Key) {
    companion object Key : CoroutineContext.Key<GreetingElement>
}

suspend fun hello(): String = suspendCoroutine { cont ->
    // 回调式 API 的桥接：手动调用 resume
    cont.resume("hello, " + (cont.context[GreetingElement]?.text ?: "world"))
}

fun main() {
    val ctx: CoroutineContext = EmptyCoroutineContext + GreetingElement("kotlin")
    val completion = Continuation<Unit>(ctx) { result -> println("done: " + result.getOrNull()) }
    // 在挂起函数之外用 intrinsics 手工构造一次未拦截的挂起调用演示常量
    println(ctx.fold("keys:") { acc, el -> acc + el.key + "," })
    println(suspendFast())
    hello0(completion)
}

fun suspendFast(): String = run {
    // 仅演示 stdlib 常量与扩展的存在性：COROUTINE_SUSPENDED / intercepted 均可从 slice 数据核实
    val c = Continuation<String>(EmptyCoroutineContext) { }
    val before: Continuation<String> = c.intercepted()
    if (before === c) "intercepted(no-op)" else "intercepted(wrapped)"
}

fun hello0(completion: Continuation<Unit>) {
    // 以 CoroutineScope 之外的最原始方式：直接调用 resumeWith 结束
    completion.resumeWith(Result.success(Unit))
}
```

## 3. kotlin.coroutines.intrinsics / jvm.internal / cancellation

### 3.1 intrinsics（slice：kotlin_coroutines_intrinsics.tsv，16 行）

| API（真实签名） | @SinceKotlin | experimental 与否 | 一句话说明 |
|---|---|---|---|
| `public suspend inline fun <T> suspendCoroutineUninterceptedOrReturn(crossinline block: (Continuation<T>) -> Any?): T` | 1.3 | 否 | 不经过拦截器的挂起桥接；block 返回值即结果，返回 `COROUTINE_SUSPENDED` 才挂起 |
| `public val COROUTINE_SUSPENDED: Any` | 1.3 | 否 | 哨兵值，实际取自内部枚举 `CoroutineSingletons.COROUTINE_SUSPENDED`（UNDECIDED/RESUMED 同枚举，internal） |
| `public expect fun <T> Continuation<T>.intercepted(): Continuation<T>`（jvm actual 同表） | 1.3 | 否 | 按上下文中的 `ContinuationInterceptor` 包装 continuation |

### 3.2 kotlin.coroutines.jvm.internal（slice：kotlin_coroutines_jvm_internal.tsv，88 行）

面向编译器生成代码与调试器的一层，绝大多数为 `internal`；公开面只有 `CoroutineStackFrame`。

| API（真实签名） | @SinceKotlin | 可见性 | 一句话说明 |
|---|---|---|---|
| `public interface CoroutineStackFrame` | 1.3 | public | 协程调用栈帧协议：`callerFrame: CoroutineStackFrame?`、`getStackTraceElement(): StackTraceElement?` |
| `internal abstract class BaseContinuationImpl` | 1.3 | internal | 挂起函数状态机基类：`invokeSuspend(result: Result<Any?>)` 循环 + 完成传递 |
| `internal abstract class RestrictedContinuationImpl` / `internal abstract class ContinuationImpl`（字段 `_context`、缓存 `intercepted`） | 1.3 | internal | 上下文受限/完整的两层实现；`internal object CompletedContinuation` 作为完成哨兵 |
| `internal interface SuspendFunction`；`internal abstract class SuspendLambda` / `RestrictedSuspendLambda` | 1.3 | internal | 挂起 lambda 的运行时表示 |
| `internal annotation class DebugMetadata(version: Int = 1, sourceFile, lineNumbers: IntArray, localNames: Array<String>, spilled: Array<String>, indexToLabel: IntArray, methodName, className)` | 1.3 | internal | 编译器写入挂起类用于恢复"被优化掉的"栈行号的元数据（`COROUTINES_DEBUG_METADATA_VERSION = 1`） |
| `internal fun boxBoolean/boxByte/boxShort/boxInt/boxLong/boxFloat/boxDouble/boxChar` | 1.3 | internal | 挂起边界上的基本类型装箱 |
| `internal fun probeCoroutineCreated/Resumed/Suspended` | 1.3 | internal | DebugProbes 钩子（kotlinx 调试工具挂在这里） |
| `internal inline fun checkContinuation/lambdaArgumentsUnspilling/tableswitch/checkResult/checkCOROUTINE_SUSPENDED/unreachable` | - | internal | GeneratedCodeMarkers：给编译器生成代码打的标记函数 |
| `internal fun runSuspend(block: suspend () -> Unit)`（私有 `RunSuspend : Continuation<Unit>` + await 阻塞） | 1.3 | internal | 在阻塞线程上跑完一段挂起代码的内部工具 |
| `internal fun nullOutSpilledVariable(value: Any?): Any? = null` | - | internal | 变量溢出(spilling)后清引用 |

### 3.3 kotlin.coroutines.cancellation（slice：kotlin_coroutines_cancellation.tsv，7 行）

| API（真实签名） | @SinceKotlin | experimental 与否 | 一句话说明 |
|---|---|---|---|
| `public expect open class CancellationException : IllegalStateException` | 1.4 | 否 | 协程取消的标准异常 |
| `public expect fun CancellationException(message: String?, cause: Throwable?): CancellationException` | 1.4 | 否 | 工厂函数 |
| `public expect fun CancellationException(cause: Throwable?): CancellationException` | 1.4 | 否 | 工厂函数 |
| `public actual typealias CancellationException = java.util.concurrent.CancellationException` | 1.4 | 否 | JVM actual：直接复用 JUC 的 CancellationException（两个工厂函数同此映射） |

## 4. kotlin.reflect —— stdlib 自带的"最小反射面"

**与 kotlin-reflect 独立 artifact 的区别（实证）**：slice `kotlin_reflect.tsv` 共 **154 行**，其中类型声明行 **44 条**；源文件为 `commonMain/kotlin/reflect` 11 个 .kt + `jvmMain/kotlin/reflect` 11 个 .kt（共 22 个文件）。抽取数据中没有 `fullReflectionAvailable`/`libraryExists` 这类"完整反射是否可用"探测 API（grep 0 命中），`KCallable.call/callBy`、`KClass.constructors` 等成员在 stdlib 源码里只是**声明**（没有 `kotlin-reflect` jar 时运行时抛 `KotlinReflectionNotSupportedError`，该类位于 `jvmMain/kotlin/jvm/KotlinReflectionNotSupportedError.kt:10`，见 §5.1）。即：stdlib 只给类型骨架 + 轻量扩展，完整反射实现在另一个 artifact。

### 4.1 类型骨架（expect/common 与 JVM actual 并存）

| 类型（真实声明） | @SinceKotlin | 一句话说明 |
|---|---|---|
| `public expect interface KCallable<out R>` / JVM actual `: KAnnotatedElement` | - | 可调用声明之根：`name`、JVM 上还有 `parameters: List<KParameter>`、`returnType: KType`、`typeParameters`(1.1)、`call(vararg args: Any?): R`、`callBy(args: Map<KParameter, Any?>)`、`visibility`(1.1)、`isFinal/isOpen/isAbstract`(1.1)、`isSuspend`(1.3) |
| `public expect interface KClass<T : Any> : KClassifier` / JVM actual `: KDeclarationContainer, KAnnotatedElement, KClassifier` | - | 类：`simpleName`/`qualifiedName`；JVM 侧 `constructors`、`nestedClasses`、`objectInstance`、`isInstance`(1.1)、`typeParameters`/`supertypes`(1.1)、`sealedSubclasses`(1.3)、`isFinal/isOpen/isAbstract/isSealed/isData/isInner/isCompanion`(1.1)、`isFun`(1.4)、`isValue`(1.5) |
| `public fun <T : Any> KClass<T>.cast(value: Any?): T` | 1.4 | 失败抛 TypeCastException 的强转扩展（`KClasses.kt:24`） |
| `public fun <T : Any> KClass<T>.safeCast(value: Any?): T?` | 1.4 | 失败返回 null 的强转扩展（`KClasses.kt:43`） |
| `public interface KClassifier` | 1.1 | KClass/KTypeParameter 的公共父标记接口 |
| `public expect interface KFunction<out R> : KCallable<R>, Function<R>` | - | 函数；JVM actual 追加 `isInline/isExternal/isOperator/isInfix`(1.1) |
| `public expect interface KProperty<out V> : KCallable<V>` 及 `KMutableProperty<V>` | - | 属性；JVM 上 `isLateinit`(1.1)/`isConst`(1.1)/`getter: Getter<V>`/`setter: Setter<V>`，`interface Accessor<out V>`、`Getter : Accessor, KFunction`、`Setter : Accessor, KFunction<Unit>` |
| `KProperty0/KMutableProperty0`、`KProperty1<T,V>/KMutableProperty1<T,V>`、`KProperty2<D,E,V>/KMutableProperty2<D,E,V>` | - | 0/1/2 接收者形态属性引用：分别可 `get()`、`get(receiver)`、`get(receiver1, receiver2)`，可变版对应 `set(...)`；各档还有组合形态 `Getter/Setter` 与 1.1 的 `getDelegate(...)` |
| `public expect interface KType`（actual `: KAnnotatedElement`） | - | 类型：`classifier: KClassifier?`(1.1)、`arguments: List<KTypeProjection>`(1.1)、`isMarkedNullable: Boolean`；JVM 扩展 `public val KType.javaType: Type`(1.4, TypesJVM.kt:26) |
| `public interface KTypeParameter : KClassifier` | 1.1 | 类型参数：`name`、`upperBounds: List<KType>`、`variance: KVariance`、`isReified: Boolean` |
| `public data class KTypeProjection(variance: KVariance?, type: KType?)` | 1.1 | 类型投影；伴生：`STAR`(通配 `*`，内部 `star = KTypeProjection(null, null)`)、`invariant(type)`、`contravariant(type)`、`covariant(type)` |
| `public interface KAnnotatedElement`（jvmMain） | - | 只有 `public val annotations: List<Annotation>`（KAnnotatedElement.kt:17） |
| `public interface KDeclarationContainer`（jvmMain） | - | `members: Collection<KCallable<*>>`，KClass actual 的父接口 |
| `public interface KParameter : KAnnotatedElement`（jvmMain） | - | 参数：`index`、`name: String?`、`type`、`kind: Kind`、`isOptional`、`isVararg`(1.1) |
| `public inline fun <reified T> typeOf(): KType` | 1.6 | 由 reified 类型实参获得 KType（`reflect/typeOf.kt:15`） |

私有实现注脚：TypesJVM.kt 内有一组 `private` 的 `java.lang.reflect.Type` 实现（`TypeImpl/TypeVariableImpl/GenericArrayTypeImpl/WildcardTypeImpl/ParameterizedTypeImpl`），是 `KType.javaType` 桥接 Java Type 的方式，非公共 API。

### 4.2 示例

```kotlin
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1
import kotlin.reflect.typeOf

data class Person(val name: String, val age: Int)

fun reflectBasics(p: Person) {
    val prop: KProperty1<Person, String> = Person::name
    println(prop.get(p))                       // KProperty1.get(receiver)
    println(prop(p))                            // KProperty1 本身是 (T) -> V
    val cls: KClass<Person> = prop.returnType.classifier as KClass<Person>
    println(cls.simpleName)                     // "Person"
    println(cls.isData)                         // 1.1: data class 探测
    println(Person::class.isInstance(p))        // 1.1 isInstance
    println(Person::class.cast(p).age)          // 1.4 cast
    val t = typeOf<List<String>>()              // 1.6 typeOf
    println(t.arguments.first().variance)       // KTypeProjection.variance
}
```

## 5. kotlin.jvm / kotlin.jvm.functions / kotlin.jvm.internal / kotlin.annotation

### 5.1 kotlin.jvm 注解与映射（kotlin_jvm.tsv 13 行 + 源码补充；见 §7 存疑点）

`kotlin_jvm.tsv` 只抽到了类映射扩展和错误类；`Jvm*` 注解声明在源码 `jvmMain/kotlin/jvm/annotations/*.kt`、`jvmMain/kotlin/jvm/JvmDefault.kt`、`commonMain/kotlin/JvmAnnotationsH.kt`（expect 侧），以下按源码逐个标注 file:line：

| 注解（真实声明） | @SinceKotlin | 一句话说明（依据 KDoc） |
|---|---|---|
| `annotation class JvmOverloads`（JvmPlatformAnnotations.kt:19） | - | 为默认参数生成重载：N 参 M 个默认 → 生成 M 个重载 |
| `annotation class JvmStatic`（:31） | - | 为函数/属性生成额外 static 方法（含 getter/setter） |
| `annotation class JvmName(val name: String)`（:43） | - | 指定生成 JVM 类/方法的名字，处理签名冲突 |
| `annotation class JvmMultifileClass`（:52） | - | 文件顶层成员并入多文件类，类名由 JvmName 提供 |
| `internal annotation class JvmPackageName(val name: String)`（:64） | 1.2 | 改 .class 的 JVM 包名（仅 Java 侧可见）；internal |
| `annotation class JvmSynthetic`（:77） | - | 生成对 Java 可见但对 Kotlin 隐藏的声明 |
| `annotation class Throws(vararg exceptionClasses: KClass<out Throwable>)`（:99） | - | 让 Kotlin 检查调用受检异常 |
| `annotation class JvmField`（:142） | - | 属性不生成访问器、直接暴露为 Java 字段 |
| `annotation class JvmSuppressWildcards(val suppress: Boolean = true)`（:160） | - | 抑制泛型签名中的通配符 |
| `annotation class JvmWildcard`（:173） | - | 显式对该位置使用通配符 |
| `annotation class JvmInline`（:185） | 1.5 | 标记生成内联 class 字节码形态 |
| `annotation class JvmRecord`（:194） | 1.5 | 标记生成为 Java record |
| `annotation class JvmSerializableLambda`（JvmFlagAnnotations.kt:70） | 1.8 | 使 lambda 生成的类可序列化 |
| `annotation class JvmExposeBoxed(val jvmName: String = "")`（JvmAnnotationsH.kt:258 expect / JvmPlatformAnnotations.kt:227 actual） | **2.2** | 为 value class 向 Java 暴露装箱 API；**experimental**：源码带 `@ExperimentalStdlibApi`（slice 未提取该注解，见 §7） |
| `internal annotation class ImplicitlyActualizedByJvmDeclaration` | 1.9 | 编译器内部标记；actual 侧 `@DeprecatedSinceKotlin(errorSince="2.1")` |
| `annotation class Volatile`（JvmFlagAnnotations.kt:22） | - | 字段加 ACC_VOLATILE；expect 侧已 `@DeprecatedSinceKotlin(warningSince="1.9", errorSince="2.1")`（JvmAnnotationsH.kt:165） |
| `annotation class Transient`（:37） / `Strictfp`（:47） / `Synchronized`（:61） | - | 对应 JVM 字段/方法标志；Synchronized expect 侧 `warningSince="1.8", errorSince="2.1"` |
| `annotation class JvmDefault`（JvmDefault.kt:18） | 1.2 | 已废弃的接口默认方法策略（@Deprecated，见文件 KDoc） |
| `annotation class JvmDefaultWithoutCompatibility`（:34） | 1.4 | 生成 JVM 默认方法但不生成 DefaultImpls 兼容 |
| `annotation class JvmDefaultWithCompatibility`（:49） | 1.6 | 同时生成 DefaultImpls 兼容 |
| `annotation class PurelyImplements(val value: String)`（PurelyImplements.kt:39） | - | 声明"仅实现某 Java 接口"的历史机制 |
| `typealias JvmRepeatable = java.lang.annotation.Repeatable` | 1.6 | JDK8 下直接别名为 Java 的 @Repeatable |
| `annotation class Target/Retention/Repeatable/MustBeDocumented`、`enum AnnotationTarget/AnnotationRetention` | - | 元注解在 `kotlin.annotation` 包，见 §5.5 |

kotlin_jvm.tsv 中抽到的映射 API（均无 @SinceKotlin 标注）：

| API（真实签名） | 说明 |
|---|---|
| `public val <T> KClass<T>.java: Class<T>` | KClass → Java Class（JvmClassMapping.kt:27） |
| `public val <T : Any> KClass<T>.javaPrimitiveType: Class<T>?` | 基本类型映射，非 primitive 返回 null（:34） |
| `public val <T : Any> KClass<T>.javaObjectType: Class<T>` | 包装类 Class（:57） |
| `public val <T : Any> Class<T>.kotlin: KClass<T>` | Java Class → KClass（:79） |
| `public inline val <T : Any> T.javaClass: Class<T>` | 任意对象的 javaClass（:87） |
| `public inline val <T : Any> KClass<T>.javaClass: Class<KClass<T>>` | KClass 自身的 javaClass（:92） |
| `public fun <reified T : Any> Array<*>.isArrayOf(): Boolean` | 数组运行时元素类型判断（:101） |
| `public val <T : Annotation> T.annotationClass: KClass<out T>` | 注解实例 → 注解类（:107） |
| `public open class KotlinReflectionNotSupportedError : Error` | 缺 kotlin-reflect 时由骨架声明抛出的 Error（KotlinReflectionNotSupportedError.kt:10） |

### 5.2 kotlin.jvm.functions（slice：kotlin_jvm_functions.tsv，49 行）

`public interface Function0<out R> : Function<R>` 直到 `Function22<in P1..P22, out R>`，共 **23 个接口**（`jvmMain/kotlin/jvm/functions/Functions.kt`，每个接口协变返回 `out R`、逆变参数 `in Pi`），各带 `public operator fun invoke(p1: P1, ..., pN: PN): R`（slice 中 invoke 行 24 条）。另含 `public interface FunctionN<out R> : Function<R>, FunctionBase<R>`（`FunctionN.kt:16`，@SinceKotlin 1.3），用于 >22 参数的 arity 可变形态。这是 JVM 上非挂起函数类型的字节码底座（与 §3.2 的 internal `SuspendFunction` 标记相对）。

### 5.3 kotlin.jvm.internal（slice：kotlin_jvm_internal.tsv，185 行）

| API（真实声明，节选 slice 中的 public/internal 类型） | @SinceKotlin | 说明 |
|---|---|---|
| `public class ClassReference(override val jClass: Class<*>) : KClass<Any>, ClassBasedDeclarationContainer` | - | KClass 的轻量实现；未加载 kotlin-reflect 时成员 `error(): Nothing = throw KotlinReflectionNotSupportedError()`(1.5)；工具函数 `getClassSimpleName/getClassQualifiedName/isInstance` |
| `public interface ClassBasedDeclarationContainer : KDeclarationContainer` | - | 持有 `jClass: Class<*>` 的内部桥接接口 |
| `public interface FunctionBase<out R> : Function<R>` | - | 声明 `arity: Int` 的函数基接口 |
| `public abstract class Lambda<out R>(override val arity: Int) : FunctionBase<R>, Serializable` | - | 用户 lambda 的运行时基类 |
| `public class PackageReference(override val jClass: Class<*>, private val moduleName: String) : ClassBasedDeclarationContainer` | 1.1 | 包级引用的运行时对象；`members` 直接 `throw KotlinReflectionNotSupportedError()`（PackageReference.kt:11-16） |
| `public open class LocalVariableReference : PropertyReference0()` / `MutableLocalVariableReference` | 1.1 | 1.1 起局部变量可被属性引用；不支持的读取走 `notSupportedError()` |
| `public interface KTypeBase : KType` | 1.4 | 供编译器生成代码实现的 KType 底座（暴露 `type`/`projection` 内部工厂） |
| `public class TypeParameterReference(container: Any?, name: String, variance: KVariance, isReified: Boolean) : KTypeParameter` | 1.4 | 类型参数的轻量 KTypeParameter 实现（container 为 ClassReference 或 CallableReference，源码注释）；`setUpperBounds`/`toString(typeParameter)` 见 slice |
| `internal class BoxingConstructorMarker private constructor()` | - | 装箱构造器哨兵 |
| `public fun collectionToArray(collection: Collection<*>, ...)` | - | `Collection.toArray` 的 Kotlin 特化 |
| `public abstract class PrimitiveSpreadBuilder<T : Any>(private val size: Int)` 及 Byte/Char/Double/Float/Int/Long/Short/Boolean 各 SpreadBuilder | - | `*spread` 传入 vararg 的运行时累加器 |
| `internal object {Double,Float,Int,Long,Short,Byte,Char,String,Enum,Boolean}CompanionObject` | Boolean 为 1.3，余无 | 基本类型伴生对象的运行时镜像 |
| 私有数组迭代器 `ArrayIterator/ArrayByteIterator/.../ArrayBooleanIterator` | - | `asList().iterator()` 的实现 |
| `SerializedIr.kt` / `TypeReference.kt`（private `asString`、`KTypeProjection.asString`） | - | Kotlin/Native IR 序列化桥与类型文本化 |

**MemberReference 说明（实证）**：抽取目录 `src/jvmMain/kotlin/jvm/internal/` 中 `ls | grep -i member` **0 命中**——2.2.10 sources 里已无 MemberReference.*；实际存在的是 Java 源文件 `CallableReference.java、FunctionReference.java、FunctionReferenceImpl.java、PropertyReference[0-2](Impl).java、MutablePropertyReference*.java、Ref.java、Reflection.java、ReflectionFactory.java、Intrinsics.java、TypeIntrinsics.java、FunctionAdapter.java、FunctionImpl.java、AdaptedFunctionReference.java、SpreadBuilder.java、DefaultConstructorMarker.java、InlineMarker.java、MagicApiIntrinsics.java、FunInterfaceConstructorReference.java、RepeatableContainer.java`。slice 脚本只解析 .kt，这些 .java 未入库，属于本层最大的抽取盲区（详见 §7）。

### 5.4 kotlin.annotation（无独立 slice；数据源文件 `commonMain/kotlin/annotation/Annotations.kt`，见 §7）

| API（真实声明） | 位置 | 说明 |
|---|---|---|
| `public enum class AnnotationTarget` | Annotations.kt:15 | 注解可用目标枚举（含 `CLASS`、`ANNOTATION_CLASS`、`TYPE_PARAMETER`、`TYPE`、`TYPEALIAS` 等条目） |
| `public enum class AnnotationRetention` | :54 | `SOURCE` / `BINARY` / `RUNTIME` 三种保留策略 |
| `@Target(AnnotationTarget.ANNOTATION_CLASS) @MustBeDocumented public annotation class Target(vararg val allowedTargets: AnnotationTarget)` | :71-73 | 元注解：限定自定义注解的目标 |
| `@Target(AnnotationTarget.ANNOTATION_CLASS) public annotation class Retention(val value: AnnotationRetention = AnnotationRetention.RUNTIME)` | :80-81 | 元注解：保留策略，默认 RUNTIME |
| `@Target(AnnotationTarget.ANNOTATION_CLASS) public annotation class Repeatable` | :86-87 | 元注解：允许多次标注 |
| `@Target(AnnotationTarget.ANNOTATION_CLASS) public annotation class MustBeDocumented` | :93-94 | 元注解：纳入 KDoc/文档 |

实测注解（`grep -n "Experimental\|Delicate\|MustBeDocumented\|Target(" Annotations.kt`）：文件内仅 `Target(...)` 与 `MustBeDocumented` 出现（共 4 处 `@Target(AnnotationTarget.ANNOTATION_CLASS)`），**无** Experimental/Delicate 注解。

### 5.5 示例：JVM 注解 + 映射扩展

```kotlin
object Metrics {
    @JvmStatic fun snapshot(): Map<String, Long> = emptyMap()   // JvmPlatformAnnotations.kt:31
    @JvmField var counter: Int = 0                              // :142
    @JvmName("resetAll") fun reset() { counter = 0 }            // :43
}

fun mapping() {
    val jClass: Class<Metrics> = Metrics::class.java            // JvmClassMapping.kt:27
    println(Int::class.javaPrimitiveType)                        // int（:34）
    println(Int::class.javaObjectType)                           // java.lang.Integer（:57）
    println(String::class.kotlin.qualifiedName)                  // Class → KClass（:79）
}
```

## 6. kotlin.experimental / kotlin.internal / kotlin.system / kotlin.concurrent（速览）

### 6.1 kotlin.experimental（slice：kotlin_jvm… 之 kotlin_experimental.tsv，9 行 + 源码）

slice 只抽到 8 个 1.1 时代的位运算扩展（`bitwiseOperations.kt`，均 `@SinceKotlin("1.1")`，无 @Deprecated 标注）：

| API | 说明 |
|---|---|
| `Byte.and/Byte.or/Byte.xor: inline infix fun(...): Byte`、`Byte.inv(): inline fun: Byte` | Byte 位运算（内部经 Int 转回） |
| `Short.and/Short.or/Short.xor: inline infix fun(...): Short`、`Short.inv(): inline fun: Short` | Short 位运算 |

源码补充的 @RequiresOptIn 标记注解（slice 未提取，实测 grep 于 `commonMain/kotlin/experimental/*.kt`）：

| 注解 | @SinceKotlin | experimental 机制 |
|---|---|---|
| `ExperimentalNativeApi` | 1.9 | `@RequiresOptIn(level = ERROR) @Retention(BINARY)` |
| `ExperimentalObjCName` | 1.8 | `@RequiresOptIn @Retention(BINARY)` |
| `ExperimentalObjCRefinement` | 1.8 | `@RequiresOptIn @Retention(BINARY)` |
| `ExperimentalTypeInference` | 1.3 | `@RequiresOptIn(level = ERROR) @Retention(BINARY)`（标注在 `overloadsFor` 类推断标记上） |
| `ExpectRefinement` | 2.2 | `@Retention(SOURCE)`，配合 `@ExperimentalMultiplatform` 使用（源码 :55-59） |

**@ObsoleteWorkersApi（实证）**：`grep -rn "ObsoleteWorkersApi" src/` 0 命中、slice 0 命中，2.2.10 抽取数据中不存在该注解，不收录。

### 6.2 kotlin.internal（slice：kotlin_internal.tsv，42 行）

全部 internal/private，无用户 API。构成：`Annotations.kt` 里 internal 注解的参数面（`RequireKotlin(version, message, level, versionKind, errorCode)`，1.2；`ContractsDsl` 1.2 等，注解类本身声明为 `internal annotation class`，slice 只抽到了其参数属性）；progression 取模工具 `mod/differenceModulo/getProgressionLastElement`（UInt/ULong 版 1.3）；序列化桥 `throwReadObjectNotSupported`/`ReadObjectParameterType`（JVM actual typealias `java.io.ObjectInputStream`）；JVM `PlatformImplementations`（addSuppressed/getSuppressed、`getMatchResultNamedGroup`、`defaultPlatformRandom`、`getSystemClock`，及 1.2 的 `apiVersionIsAtLeast(major, minor, patch)`）。

### 6.3 kotlin.system（slice：kotlin_system.tsv，6 行）

只有 2 个源文件（Process.kt / Timing.kt）、3 个公开函数，无 @SinceKotlin、无 Delicate/Experimental 标注（实测 grep）；任务提到的 "TIMESTAMPS-support 类 API" 在抽取数据中无对应物，未收录：

| API（真实签名） | 说明 |
|---|---|
| `public inline fun exitProcess(status: Int): Nothing`（Process.kt:18） | 终止 JVM（Runtime.exit） |
| `public inline fun measureTimeMillis(block: () -> Unit): Long`（Timing.kt:25） | `System.currentTimeMillis` 计时 |
| `public inline fun measureNanoTime(block: () -> Unit): Long`（Timing.kt:41） | `System.nanoTime` 计时 |

### 6.4 kotlin.concurrent（slice：kotlin_concurrent.tsv，34 行；一句提示）

stdlib 里线程/定时器便利层确实存在：`thread(...)` 工厂（Thread.kt:20）、`ThreadLocal<T>.getOrSet`、`Lock.withLock`、`ReentrantReadWriteLock.read/write`、`Timer.schedule/scheduleAtFixedRate` 扩展与 `timer/fixedRateTimer/timerTask` 工厂、`typealias Volatile = kotlin.jvm.Volatile`(1.9)。原子类（`kotlin.concurrent.atomics`）由 06 篇负责，此处不展开。

## 7. 数据来源与校验方法

**版本**：kotlin-stdlib 2.2.10（从 Gradle 缓存 `kotlin-stdlib-2.2.10-sources.jar` 解出到 `C:/Users/wdidada/AppData/Local/Temp/kstdlib/src/`，脚本抽取 TSV 到 `.../kstdlib/out/slices/`）。

**本篇用到的 slice 及 `wc -l` 实测行数**（目录 `C:/Users/wdidada/AppData/Local/Temp/kstdlib/out/slices/`）：

| 文件 | 行数 | 文件 | 行数 |
|---|---|---|---|
| kotlin_contracts.tsv | 14 | kotlin_jvm_functions.tsv | 49 |
| kotlin_coroutines.tsv | 66 | kotlin_jvm_internal.tsv | 185 |
| kotlin_coroutines_intrinsics.tsv | 16 | kotlin_experimental.tsv | 9 |
| kotlin_coroutines_jvm_internal.tsv | 88 | kotlin_internal.tsv | 42 |
| kotlin_coroutines_cancellation.tsv | 7 | kotlin_system.tsv | 6 |
| kotlin_reflect.tsv | 154 | kotlin_concurrent.tsv | 34 |
| kotlin_jvm.tsv | 13 | 4 个协程 slice 合计 | 177 |

**"stdlib 里没有 launch/async/withContext"的 grep 证据**：

```
$ grep -nw -E "launch|async|withContext" kotlin_coroutines.tsv kotlin_coroutines_intrinsics.tsv \
    kotlin_coroutines_jvm_internal.tsv kotlin_coroutines_cancellation.tsv
（无输出，匹配行数 0；上述四文件 wc -l 合计 177）
$ grep -rn "fun launch(\|fun async(\|withContext(" \
    src/commonMain/kotlin/coroutines src/jvmMain/kotlin/coroutines out/slices/kotlin_coroutines*.tsv | wc -l
0
```

**其他专项校验**：
- `grep -E "\t(interface|class|object)\t" kotlin_reflect.tsv | wc -l` → 44（类型声明行）；reflect 源文件 commonMain 11 + jvmMain 11 = 22 个 .kt；"51 个 class"的说法与 slice 实测不符，本文件按实测 154 行/44 类型条目书写。
- `grep -rn "ObsoleteWorkersApi\|readsInFixedPosition\|callableReferences" src/ out/slices/*.tsv` → 0；`MemberReference`：`ls src/jvmMain/kotlin/jvm/internal | grep -i member` → 0。
- `grep -n "Experimental|Delicate|MustBeDocumented|Target(" commonMain/kotlin/annotation/Annotations.kt` → 仅 `@Target(AnnotationTarget.ANNOTATION_CLASS)`（4 处）与 `@MustBeDocumented`（1 处），无 Experimental/Delicate。
- contracts 源码实测注解：`ExperimentalContracts` 带 `@Retention(BINARY) @SinceKotlin("1.3") @RequiresOptIn @MustBeDocumented`（ContractBuilder.kt:19-23）；`JvmExposeBoxed` 带 `@ExperimentalStdlibApi @SinceKotlin("2.2")`（JvmAnnotationsH.kt:248-258）。

**存疑点 / 抽取盲区（如实记录）**：
1. 任务指定的 `kotlin_annotation.tsv`、`kotlin_jvm_annotation.tsv` 两个 slice **在抽取目录中不存在**（`ls | grep -i annot` 0 命中）。§5.1 的 Jvm* 注解表与 §5.4 的元注解表因此以解包源码（文件路径:行号已逐条给出）为据，未凭记忆补充任何签名。
2. `kotlin_contracts.tsv` 漏抽了同文件中的 `ExperimentalContracts`(:23) 与 `InvocationKind`(:96)，上表已按源码补齐并注明。
3. `kotlin_jvm_functions.tsv` 的接口行为 24 条：Function0..Function22 之外还包含 `FunctionN`（1.3，位于单独的 FunctionN.kt）；invoke 行同为 24 条。
4. `kotlin.coroutines` slice 无 `startCoroutine`/`createCoroutine`/`AbstractContinuation`（属 kotlinx-coroutines）；文档示例因此只用 `suspendCoroutine`/`Continuation` 工厂/`intrinsics`。
5. `kotlin.jvm.internal` 的运行时引用类大量以 `.java` 形式存在于 sources.jar（§5.3 末尾清单），脚本只解析 .kt，导致该层呈现不完整。

**自查结果**：本文档表格与示例中出现的每个 API 名，均可在其对应 slice TSV 中以 `grep -w` 命中；源码补充项（Jvm* 注解、kotlin.annotation、ExperimentalContracts/InvocationKind、experimental 标记注解）均给出了源文件绝对路径与行号，已逐一回读原文核实。
