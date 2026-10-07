# Kotlin 按什么排版本？（与 Java / C++ 的对照）

> 结论先行：**Kotlin 不按"标准年份"排版本，也不像 Java 那样由 JSR/JEP 驱动。它是"编译器 + 标准库 + 工具链"绑成一列的发布列车（release train），版本号是 `MAJOR.MINOR.PATCH`，而且 MINOR 的最后一位有额外语义（`.0` = 语言版本，`.20` = 工具版本）。**
>
> 在"某个 API 从哪个版本开始能用"这个粒度上，Kotlin 靠标准库源码里的 **`@SinceKotlin("1.x")` 注解**记录，靠 **language version / API version** 两个编译器开关控制"你能用哪一年的语法 / 哪一版 stdlib API"，靠 **`@RequiresOptIn` 系注解**管理实验特性。这三件事在不同语言里分别对应：C++ 用 `__cpp_lib_*` 特性测试宏、Java 用 JDK 版本号 + `@since` Javadoc 标签。

## 1. 三种语言的版本逻辑放在一起看

| | C++ | Java | Kotlin |
|---|---|---|---|
| 版本号的含义 | ISO 标准的**批准年份**：C++98 / C++03 / C++11 / C++14 / C++17 / C++20 / C++23（即 ISO/IEC 14882 的各次修订） | **JDK 发布序号**：8 / 11 / 17 / 21 是 LTS 节点，语言特性以 JEP 形式绑定到某个 JDK（`var`=9、switch 表达式=14、Sealed=17、Record 落在 16） | **产品发布序号** `MAJOR.MINOR.PATCH`，`2` 之后 MINOR 位有语义（`.0`/`.20`）；不是标准，也不是年份 |
| 谁定规格 | ISO/IEC JTC1/SC22 WG21 投票定稿，编译器再实现 | OpenJDK JEP 流程 + JSR（2006 年后基本被 JEP 取代） | JetBrains 主导 + **KEEP**（Kotlin Evolution and Promotion Process）公开提案流程 |
| 语言代际标记 | `-std=c++17` 这类标准模式开关 | `--release 17` / `-source/-target` | `-language-version 2.1`（语法）+ `-api-version 2.1`（可用 stdlib API） |
| 单个 API 的引入版本 | 特性测试宏 `<version>`，如 `__cpp_lib_optional >= 201606` | Javadoc `@since 17` | 源码注解 `@SinceKotlin("1.3")`，配合 `@WasExperimental(...)` 记录从实验转正式 |
| 未稳定特性怎么隔离 | 无统一机制（编译器 `-fconcepts` 之类私有开关） | 预览特性 `--enable-preview`，且**预览版字节码跨版本不可用** | `@RequiresOptIn` 注解（如 `@ExperimentalStdlibApi`），可只给单个 API 加，且能分"实验 / Beta" |
| 标准库与语言的关系 | 语言与标准库同在一次 ISO 修订里 | 语言在 JLS，库在 JDK API，两者同版本号 | **编译器与 stdlib 同一版本号、同一次发布**，stdlib 的 API 版本用注解单独标注 |
| 二进制兼容 | 无 ABI 标准，各实现不保证 | class 文件有版本号，向上不兼容 | JVM 侧 ABI 明确承诺稳定；Native 的 klib 自 **1.9.20** 起稳定 |

一句话记忆：**C++ 报"年份"，Java 报"JDK 号"，Kotlin 报"发布号 + API 版本戳"。**

## 2. Kotlin 版本号到底怎么读

自 Kotlin 2.0.0 起，官方把发布固定成三类（下表日期来自官方 [Kotlin release process](https://kotlinlang.org/docs/releases.html) 与 JetBrains/kotlin 的 GitHub release tag，实测 `gh api repos/JetBrains/kotlin/releases/tags/vX.Y.Z`）：

| 类型 | 号段 | 节奏 | 内容 | 实测日期 |
|---|---|---|---|---|
| 语言版本（Language release） | `2.x.0` | 每 6 个月 | 新语言特性、新工具链，可能带不兼容变更 | 2.0.0 = 2024-05-21；2.1.0 = 2024-11-27；2.2.0 = 2025-06-23；2.3.0 = 2025-12-16；2.4.0 = 2026-06-03 |
| 工具版本（Tooling release） | `2.x.20` | 语言版本后 3 个月 | 工具链更新、性能改进、bug 修复，也可能引入新实验特性 | 2.4.20 = 2026-09-07 |
| 修复版本（Bug fix release） | `2.x.1x` / `2.x.21` | 按需 | 只修 bug | 2.4.10 = 2026-07-14；2.3.21 = 2026-04-23 |

按这个节奏，**2026 年 10 月的当前稳定版是 2.4.20（2026-09-07），下一代 2.5.0 计划 2026 年 12 月**。

1.x 时代的间隔并不严格（把 GitHub tag 日期相减实测）：1.2.0 = 2017-11-28 → 1.3.0 = 2018-10-29（约 11 个月）→ 1.4.0 = 2020-08-14（约 21 个月，1.4 是里程碑版）→ 1.5.0 = 2021-05-05 → 1.6.0 = 2021-11-16 → 1.7.0 = 2022-06-09 → 1.8.0 = 2022-12-28 → 1.9.0 = 2023-07-06（稳定在 6~10 个月）→ 2.0.0 = 2024-05-21。也就是说"半年一版"是 **2.0 之后才正式制度化的**。

> 注意：1.x 里第三段（`1.4.30`、`1.9.21` 这种）曾经表示"特性补丁"，与 2.x 的 `.20` 语义不同；1.x 的 `.x0` 号段（如 1.3.70、1.9.20）历史上承担过"带新实验特性的小版本"角色，1.9.20 就是 K2 Beta + klib 格式稳定的那个版本。

## 3. 标准库 API 的"版本戳"：@SinceKotlin（本节数字全是本机实测）

`MAJOR.MINOR` 只告诉你列车时刻表。要知道 `buildList()` 或 `Uuid` 是哪个版本进库的，看的是 stdlib 源码里的注解：

```kotlin
@SinceKotlin("1.6")           // 1.6 起可用
public inline fun <E> buildList(builderAction: MutableList<E>.() -> Unit): List<E>

@SinceKotlin("1.1")           // 1.1 起可用
public inline fun <T> T.takeIf(predicate: (T) -> Boolean): T?
```

我把手上这份 `kotlin-stdlib-2.2.10-sources.jar`（Gradle 缓存里，698,990 字节，373 个 `.kt` 文件）解析了一遍，共抽出 **13045 条声明**，其中 **3313 条带 `@SinceKotlin`**，其余 9731 条是 1.0 就有、或内部实现（无版本戳即视为随首版存在）。分布是：

| 版本 | 带戳声明数 | 这一版大致加了什么（依据源码注解，不靠记忆） |
|---|---|---|
| 1.1 | 310 | `takeIf` / `takeUnless` / `also`、`enumValues`/`enumValueOf`、`addSuppressed`、`Publisher`-无关的 `Throwable` 扩展、异常 `typealias`（`IllegalArgumentException` 等 common 侧） |
| 1.2 | 293 | `Result` 前身 API、`BitSet` 相关、`toBigDecimal`/`toBigInteger`（1.2）、`and/or/xor/invoke` 运算符族、`ushr`/`shl`/`shr`、`Char.category/directionality`、`use`（Closeable 版） |
| 1.3 | 761 | `kotlin.time`（Duration）、`kotlin.random.Random`、`kotlin.contracts`、`Result`、unsigned 数组类型（`UByteArray` 等）、`@RequiresOptIn`/`@ExperimentalStdlibApi`、`ArithmeticException` 的 common 声明 |
| 1.4 | 1086 | **量最大的一次标准库扩张**：`@DeprecatedSinceKotlin`、`@OverloadResolutionByLambdaReturnType`、`printStackTrace` 扩展、`getValue/setValue` 委托细节、`KotlinNothingValueException`、`@Throws` 的 common 声明 |
| 1.5 | 359 | unsigned 标量类型转正（`UByte/UInt/ULong/UShort` = 1.5）、`floorDiv`/`mod`、`Random.nextUInt`/`nextULong`、位运算 `countZeroBits`/`rotateLeft`/`rotateRight` 一族 |
| 1.6 | 52 | `buildList`/`buildSet`/`buildMap`、`@BuilderInference`（1.6 侧声明）、`rotateLeft/Right` 的收敛 |
| 1.7 | 140 | `DeepRecursiveFunction`/`DeepRecursiveScope`、`@ContextFunctionTypeParams`（context receiver 的地基）、`invoke` 相关 |
| 1.8 | 46 | `@ExperimentalSubclassOptIn`、少量收尾 |
| 1.9 | 109 | `rangeUntil`（`..<` 的库支持）、`NoSuchElementException` 的 common 声明转正 |
| 2.0 | 26 | `kotlin.uuid.Uuid`（带 `@SinceKotlin("2.0")`）、`AutoCloseable` 的 common 接口 + `use` 的第二个重载、`@ConsistentCopyVisibility`/`@ExposedCopyVisibility` |
| 2.1 | 76 | `kotlin.concurrent.atomics`（`AtomicInt/AtomicLong/AtomicReference/AtomicArray` 共 52 条）、`kotlin.io.path`（6 条）、`kotlin.time` 的 `Instant`/`Clock`（6 条）、`Uuid` 的 `parseHexDash`/`toUByteArray`（4 条）、`@SubclassOptInRequired` |
| 2.2 | 55 | `kotlin.text` 的 `HexFormat` 族（24 条）、`kotlin` 包的 `context`/`contextOf`（25 条，context parameters 的库面）、`kotlin.io.encoding.Base64`（`@SinceKotlin("2.2")` + `@WasExperimental(ExperimentalEncodingApi::class)`）、`@MustUseReturnValue`/`@IgnorableReturnValue`、`TimeSource.asClock`、`Instant.parseOrNull`（`Instant.kt:401` 的 `@SinceKotlin("2.2")`）；按包拆：`kotlin` 25 + `kotlin.text` 24 + `kotlin.time` 2 + `kotlin.sequences` 2 + `kotlin.io.encoding` 1 + `kotlin.experimental` 1 = 55 |

这张表有个反直觉的读法：**带戳声明数不等于"新增 API 数"**，一个函数有 N 个重载就出现 N 条；且 1.x 时期 JetBrains 给大量"把 JVM 专有 API 提升到 common"的声明补了戳（1.4 的 1086 条主要是这类），所以 1.4 看起来比 1.3 热闹，实际语言层面无大变化。真要跨版本比较 API 面，可以直接对比两份 stdlib 源码，我做过一次：

- `kotlin-stdlib-2.2.10` 去重后 **3486** 个 API 名（`包 + kind + 名称` 维度），`kotlin-stdlib-2.1.10` 是 **3290** 个。
- 差集：**2.2 相比 2.1 净新增 210 个 API 名**（`HexFormat`、`Base64` 稳定面、`context/contextOf`、`AtomicXxx` 的方法族等），同时 2.1.10 里有 14 个名字在 2.2.10 中消失，全部是 `readObject`/`primitiveFqNames`/`fpRegex` 这类内部实现或私有声明，不是公开 API 被删。

## 4. 控制"我用哪一版 Kotlin"的两个开关

官方 [Kotlin evolution principles](https://kotlinlang.org/docs/kotlin-evolution-principles.html) 给的定义（原页是 `compatibility-modes.html` 的跳转目标）：

- **Language version** —— 管**语法**。设成 `2.1` 就拒绝使用 2.1 之后引入的语言特性（编译期报错）。命令行 `-language-version 2.1`。
- **API version** —— 管**能用哪一版标准库 API**。设成 `2.1` 时，编译器会忽略 2.1 之后引入的 stdlib 声明（包括编译器生成代码里也受限）。命令行 `-api-version 2.1`。这正是 `@SinceKotlin` 的兑现处：**版本戳是数据，API version 是执行它的机制。**
- **Progressive mode**（`-progressive` / `progressiveMode = true`）—— 提前采用**工具版本**里已稳定的 bug 修复与默认行为变更，不用等下一个语言版本。
- 支持窗口：JVM 平台承诺"当前稳定版 + 至少 3 个先前的语言版本与 API 版本"可选，这样库作者可以升到新编译器，同时继续产出给老编译器用户用的二进制。

本仓库的写法（Gradle Groovy DSL，`build.gradle` 里 `kotlin { jvmToolchain(17) }` 旁边）：

```groovy
kotlin {
    compilerOptions {
        languageVersion = org.jetbrains.kotlin.dsl.support.KotlinCompilerVersion.VERSION_2_1
        apiVersion = org.jetbrains.kotlin.config.LanguageVersion.KOTLIN_2_1
        progressiveMode = true
    }
}
```

> 上面这段属于"配置形态"示例，具体枚举类名在不同 Kotlin Gradle 插件版本上有差异；实践上更常用、更稳的形式是 IDE 帮你生成的写法或 `kotlinOptions { languageVersion = '2.1'; apiVersion = '2.1' }`。**在本仓库真正验证过的只有 `jvmToolchain(17)`**（CI 里 2.4.20 之前的 2.2.10 工具链构建通过），language/api version 这两行要用时请让编译器自己告诉你正确的类型名。

## 5. 实验特性：为什么"版本号"不够用

Kotlin 用 **opt-in 注解**而不是"某个 beta 版本号"来隔离未稳定 API，规则写在 evolution principles 里：

1. 声明一个 `@RequiresOptIn` 的注解（stdlib 里实测存在 `@ExperimentalStdlibApi`，戳是 1.3；还有 `@ExperimentalSubclassOptIn` 1.8、`@ExperimentalUnsignedTypes`、`@ExperimentalMultiplatform` 等）。
2. 特性转正时，把使用点上的注解换成无注解版本，并用 **`@WasExperimental`** 记录"它曾经需要什么 opt-in"（实测 `Base64` 就是 `@SinceKotlin("2.2")` + `@WasExperimental(ExperimentalEncodingApi::class)`）。
3. 命名规则也是硬约定：包名含 `internal` 的不算公开 API，包名含 `experimental` 的视为未稳定、随时可变 —— 这就是为什么本仓库实测到 `kotlin.experimental`、`kotlin.internal`、`kotlin.concurrent.internal` 这些包，它们不该被你依赖。
4. 编译器选项也分档：不带前缀的选项只能随语言版本增加且要走弃用流程；`-X` / `-XX` 前缀的随时可增删。

与 Java 对照：Java 的 `--enable-preview` 是**整体开关**且预览版 class 文件带"预览"标记、不能在下一版直接使用；Kotlin 的 opt-in 是**逐个 API 的注解**，实验期产物可以被正常编译进同一份字节码，代价是你的代码必须显式写明"我知道这是实验的"。

## 6. 标准库自身的"版本号"历史小坑

- 曾经有 `kotlin-stdlib-jdk7` / `kotlin-stdlib-jdk8` 两个附加 artifact。本机缓存里实测：`kotlin-stdlib-jdk7-1.7.10.jar` 有 **8** 个 `kotlin/*.class`，`kotlin-stdlib-jdk8-1.7.10.jar` 有 **13** 个；而从 `1.8.22` 开始，两者的 class 数都是 **0**（只剩 `META-INF/versions/9/module-info.class`）。也就是说 **1.8.0 起这些 API 已合并进主 `kotlin-stdlib`**，两个 artifact 变成兼容用的空壳，新项目只需要一个 `kotlin-stdlib`。
- stdlib 与编译器同版本号，所以 `<dependency>org.jetbrains.kotlin:kotlin-stdlib:2.2.10</dependency>` 对应的就是 Kotlin 2.2.10 那次发布的库面；Gradle 里通常由插件自动带入，不用手写。
- 自 2.4.0 起，官方给 JVM 标准库 **18 个月安全支持窗口**：安全漏洞会同时在最新版与窗口内的各活跃线上发修复版；但窗口外的老版本不再回补 —— 这是"能不能长期停在旧版本"的硬约束。

## 7. 回到你的三个问题

1. **Kotlin 按什么排？** 按发布列车号 `MAJOR.MINOR.PATCH`，2.x 里 `.0` 是语言版本（半年一班）、`.20` 是工具版本（中间一班）、其余是修复版；不是年份、不是标准编号。
2. **API 的"版本"从哪查？** stdlib 源码的 `@SinceKotlin`，配合 `-api-version` 由编译器强制执行；实验 API 看 `@RequiresOptIn` 注解与 `@WasExperimental` 的"毕业记录"。
3. **和 Java 的 5/6/7/8/11/14/17、C++ 的 98/11/14/17/21 最本质的差别？** Java 与 C++ 的号码指向"一份规格文档的一次修订"，Kotlin 的号码指向"一次产品发布"，语言规格、编译器、标准库、IDE 插件全在同一列火车上；因此 Kotlin 才需要额外发明 language version / API version 这套"我实际按哪一版行事"的机制。

## 数据来源与校验方法

| 事实 | 来源 | 可复现命令 |
|---|---|---|
| 三类发布节奏、`.0/.20/补丁`语义、18 个月 stdlib 安全窗口、2.5.0 计划 | 官方 [Kotlin release process](https://kotlinlang.org/docs/releases.html) | 直接访问；或 `WebFetch` 该页 |
| 语言版本/API 版本/progressive/支持窗口/包命名规则/opt-in/KEEP 流程/klib 1.9.20 稳定 | 官方 [Kotlin evolution principles](https://kotlinlang.org/docs/kotlin-evolution-principles.html) | 直接访问（`compatibility-modes.html` 是跳转到这页的壳） |
| 各版本发布日期 | JetBrains/kotlin GitHub Release | `gh api repos/JetBrains/kotlin/releases/tags/v2.4.20 --jq .published_at`（1.x 里 `v1.0.0`/`v1.1.0` 没有 release 对象，返回 404，所以文中 1.0/1.1 的具体日期不写） |
| `@SinceKotlin` 分布、声明数、包大小、2.1.10↔2.2.10 差集 | 本机 Gradle 缓存里的 `kotlin-stdlib-2.2.10-sources.jar` 与 `kotlin-stdlib-2.1.10-sources.jar`，脚本逐行解析（先处理块注释状态机，再匹配 `@SinceKotlin` + 声明行） | 见本目录 `kotlin-stdlib/00-overview.md` 的"抽取脚本要点" |
| jdk7/jdk8 artifact 合并 | 本机缓存 jar 实测 class 计数 | `jar tf kotlin-stdlib-jdk7-1.7.10.jar \| grep -c 'kotlin/.*\.class$'` → 8；`...-1.8.22.jar` → 0 |

标注"实测"的数字都可在本机复核；标注"官方"的政策性描述以文档原文为准。第 4 节的 Gradle 代码片段是唯一带有不确定性的一段，已就地说明并以编译器为准。
