# Kotlin 标准库（kotlin-stdlib）结构与包总览

本页是整套 API 文档的地图。所有数字都是**本机实测**：解包 Gradle 缓存里的 `kotlin-stdlib-2.2.10-sources.jar`，用脚本逐行解析 `package` / `@SinceKotlin` / 声明行得到的结果，不是二手资料。

## 1. 这份库到底是什么

| 维度 | 实测值 | 复核命令 |
|---|---|---|
| artifact | `org.jetbrains.kotlin:kotlin-stdlib:2.2.10` | 本地 Gradle 缓存路径见文末 |
| 二进制 jar 大小 | 1,750,374 字节 | `ls -la kotlin-stdlib-2.2.10.jar` |
| jar 内 class 条目 | 970 个 `.class` | `jar tf kotlin-stdlib-2.2.10.jar \| grep -c '\.class$'` |
| sources jar | 698,990 字节，373 个 `.kt` | `jar tf ...-sources.jar \| grep -c '\.kt$'` |
| 源码平台划分 | `commonMain` 199 个 `.kt` + `jvmMain` 174 个 `.kt`（含 `jdk7`/`jdk8` 子目录与 `generated`） | `find commonMain -name '*.kt' \| wc -l` |
| 顶层包数（按声明统计） | 42 个 `kotlin.*` 包 | 见下表 |
| 解析出的声明总数 | 13,045 条（含所有重载） | 脚本输出 |
| 带 `@SinceKotlin` 的声明 | 3,313 条；其余 9,732 条无版本戳（1.0 就有 / 内部实现） | 同上 |
| 去重后 API 名（`包 + 种类 + 名称`） | 3,486 个（主解析器）+ 26 个扩展属性 = **3,512 个** | `wc -l dedup_api.tsv`；扩展属性见 `out3/ext_props.tsv`，第 5 节 |

一个关键结构事实：**Kotlin 标准库是"按 common + 平台 actual 两层写的"**。`commonMain` 里是跨平台声明（大量 `expect`），`jvmMain` 里是 JVM 的实现与 JVM 独有 API。所以你在 JVM 项目里能用的 API 面 = 两层的并集，而"能不能在 Android/Native/JS 通用"要看它是否只存在于 `jvmMain`。文档各页都按 slice 里的 `loc` 列区分了 `commonMain/...` 与 `jvmMain/...`。

## 2. 包大小地图（decls = 重载级声明数；APIs = 去重名数；dated = 带版本戳数）

| 包 | decls | APIs | dated | 内容一句话 | 详细页 |
|---|---|---|---|---|---|
| `kotlin.collections` | 6327 | 586 | 1600 | 库的最大头：List/Set/Map 全套工厂 + 扩展操作 | `02-collections.md` |
| `kotlin` | 1629 | 339 | 433 | 内置类型、作用域函数、转换、异常、`Result`、注解本体 | `01-core-types.md` |
| `kotlin.text` | 1382 | 597 | 310 | String / StringBuilder / Regex / Char / HexFormat（API 名数第一） | `04-text.md` |
| `kotlin.sequences` | 573 | 236 | 87 | 惰性集合，与 collections 平行的一整套操作 | `03-sequences-ranges-comparisons.md` |
| `kotlin.time` | 417 | 278 | 46 | Duration / TimeSource / TimeMark，2.1 起加 `Instant`/`Clock` | `06-time-random-uuid-atomics.md` |
| `kotlin.ranges` | 309 | 69 | 106 | Range / Progression，`until` / `rangeUntil` / `coerceIn` | `03-...md` |
| `kotlin.io` | 244 | 162 | 6 | JVM 文件与控制台（`File` 扩展、`readLines`、`use`） | `05-io-path-encoding.md` |
| `kotlin.collections.builders` | 241 | 128 | 0 | `buildList`/`toMutableList` 背后的 builder 实现层 | `02-...md` |
| `kotlin.math` | 207 | 51 | 196 | `abs/ceil/pow/sqrt` 与常量，绝大多数有版本戳 | `01-core-types.md` 附 |
| `kotlin.io.path` | 203 | 173 | 92 | `java.nio.file.Path` 的 Kotlin 扩展（2.1 起，实验） | `05-...md` |
| `kotlin.jvm.internal` | 188 | 113 | 38 | 编译产物依赖的内部类（`Ref.ObjectRef` 等），不要直接用 | `07-...md` |
| `kotlin.comparisons` | 178 | 26 | 114 | `compareBy/thenBy/maxOf/minOf` 比较器组合子 | `03-...md` |
| `kotlin.io.encoding` | 158 | 115 | 15 | `Base64`（`@SinceKotlin("2.2")` + `@WasExperimental`） | `05-...md` |
| `kotlin.reflect` | 152 | 75 | 43 | `KClass`/`KProperty` 的**接口层**（实现在 `kotlin-reflect` artifact） | `07-...md` |
| `kotlin.concurrent.atomics` | 144 | 40 | 52 | 2.1 新增的 `AtomicInt/AtomicReference/AtomicArray` | `06-...md` |
| `kotlin.random` | 100 | 58 | 18 | `Random` 与 `XorWowRandom`，unsigned 变体（1.5） | `06-...md` |
| `kotlin.coroutines.jvm.internal` | 85 | 76 | 22 | JVM 协程基础设施内部类 | `07-...md` |
| `kotlin.uuid` | 84 | 63 | 12 | `Uuid`（2.0 起），2.1 加解析/字节互转 | `06-...md` |
| `kotlin.coroutines` | 66 | 56 | 17 | `Continuation`/`CoroutineContext`/`suspendCoroutine` 基础设施 | `07-...md` |
| `kotlin.internal` | 58 | 45 | 8 | `@InternalApi` 类实现细节 | — |
| `kotlin.jvm` | 52 | 29 | 14 | `@JvmStatic`/`@JvmField`/`@JvmName` 等互操作注解 | `07-...md` |
| `kotlin.jvm.functions` | 48 | 25 | 1 | `FunctionN` 接口族（lambda 的 JVM 表示） | `07-...md` |
| `kotlin.concurrent` | 34 | 18 | 2 | `Thread`/`Timer`/`Atomic*` 的 Kotlin 便利扩展（JVM） | `06/07` |
| `kotlin.properties` | 17 | 17 | 1 | `Delegates.observable/vetoable` | `01-...md` |
| `kotlin.enums` | 17 | 12 | 7 | `EnumEntries`（1.9 起 enum `entries` 的类型） | `01-...md` |
| `kotlin.coroutines.intrinsics` | 16 | 10 | 6 | `suspend fun` 状态机的底层入口 | `07-...md` |
| `kotlin.contracts` | 15 | 14 | 10 | `contract { returns() }` DSL（1.3） | `07-...md` |
| `kotlin.concurrent.internal` | 14 | 2 | 7 | 内部支撑 | — |
| `kotlin.experimental` | 13 | 9 | 13 | 按官方规则"包名含 experimental 即未稳定" | `07-...md` |
| `kotlin.js` | 10 | 10 | 9 | JS 平台互操作注解 | — |
| `kotlin.streams` | 9 | 3 | 9 | Java Stream ↔ 集合互转（1.2 起） | `05-...md` 附 |
| `kotlin.jvm.internal.markers` | 9 | 9 | 0 | `KMutableIterator` 等标记接口 | `07-...md` |
| `kotlin.native` / `kotlin.native.concurrent` | 7 / 2 | 7 / 2 | 6 / 2 | Native 平台专属 | — |
| `kotlin.jvm.optionals` | 7 | 7 | 7 | `Optional` ↔ List/Set/Map（`@SinceKotlin("1.8")`） | `05-...md` 附 |
| `kotlin.internal.jdk7` / `jdk8` | 4 / 6 | 4 / 6 | 0 / 0 | 原 jdk7/jdk8 artifact 合并后的内部实现 | — |
| `kotlin.coroutines.cancellation` | 6 | 3 | 6 | `CancellationException` 别名 | `07-...md` |
| `kotlin.annotation` | 6 | 6 | 0 | `AnnotationRetention`/`AnnotationTarget`/`@Target` 等元注解 | `07-...md` |
| `kotlin.system` | 5 | 4 | 0 | `exit()`/`measureTime()`(旧) 等进程级 API | `07-...md` |
| `kotlin.jvm.internal.unsafe` | 2 | 2 | 0 | 内存级内部工具 | — |
| `kotlin.random.jdk8` | 1 | 1 | 0 | JDK8 特化 | — |

三个容易被忽略的事实：

1. **`kotlin.text` 的去重 API 数（597）比 `kotlin.collections`（586）还多**，但 decls 只有 1382 对 6327 —— 集合操作的"重载爆炸"才是 stdlib 体积的主要来源。
2. `kotlin.collections.builders` 的 241 条声明**全部没有版本戳**，因为它是 `buildList` 的实现层而非公开 API；公开的是 `kotlin.collections` 里那 3 个 `@SinceKotlin("1.6")` 的 `buildXxx`。
3. 带版本戳比例最高的是 `kotlin.math`（196/207）和 `kotlin.comparisons`（114/178），说明这两个包是"后来才从 JVM 专有提升为 common"的典型；而 `kotlin` 包本体（433/1629）大头是无戳的 1.0 原语。

## 3. jar 层结构（二进制侧，实测 class 条目 Top）

```
kotlin/collections          120   kotlin/time                 48
kotlin                      119   kotlin/ranges               42
kotlin/jvm/internal          92   kotlin/io                   35
kotlin/text                  68   kotlin/io/path             28
kotlin/sequences             67   kotlin/jvm                 25
kotlin/reflect               51   kotlin/jvm/functions       24
```

> `kotlin-stdlib` 的 `kotlin/reflect` 只提供 `KClass` 等**接口**；真正能反射的能力要额外依赖 `kotlin-reflect`（另一个 artifact，体积大得多，Android 上常靠 R8 规则裁剪）。这一点从 jar 类名里就能看出来：stdlib 里没有 `kotlin.reflect.jvm.internal` 之类的实现包。

## 4. stdlib artifact 的合并史（实测）

| artifact | 1.7.10 里的 `kotlin/*.class` 数 | 1.8.22 里的数量 |
|---|---|---|
| `kotlin-stdlib-jdk7` | 8 | **0** |
| `kotlin-stdlib-jdk8` | 13 | **0** |

1.8.0 起这两处的 API 已并入主 `kotlin-stdlib`，空壳 artifact 只为兼容老构建脚本保留（里面只剩 `META-INF/versions/9/module-info.class`）。**新项目只依赖 `kotlin-stdlib` 一个。**

## 5. 抽取脚本要点（本页所有数字的复现方法）

脚本和全部中间产物已经入库到 [`docs/_data/`](_data/README.md)：切片在 `_data/slices/*.tsv`，扩展属性在 `_data/ext_props.tsv`，脚本在 `_data/scripts/`。重跑只需要本机 Gradle 缓存里的源码 jar：

```bash
SRC=$(ls ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.10/*/kotlin-stdlib-2.2.10-sources.jar | head -1)
mkdir -p /tmp/kstdlib/src && cd /tmp/kstdlib/src && jar xf "$SRC"
node docs/_data/scripts/extract2.mjs    /tmp/kstdlib/src /tmp/kstdlib/out2      # 主解析
node docs/_data/scripts/extract_ext.mjs /tmp/kstdlib /tmp/kstdlib/src /tmp/kstdlib/out3   # 扩展属性补抽
```

脚本做四件事，按顺序：① 用块注释状态机跳过 license header 与 KDoc（**顺序很重要**：license 头的结束行 ` */` 本身长得像 KDoc 行，若先判 KDoc 就会把注释状态永久卡住，实测导致声明数从 13045 掉到 887）；② 记录每个文件的 `package`；③ 抓到 `@SinceKotlin("x.y")` 后，绑定其后 6 行内的第一个声明；④ 解析名字时先剥泛型参数，再按 `接收者.函数名` 取**最后一段**作为名字（否则 `fun <T, R> T.let(...)` 会被误抽成名字 `T`）。

产物：`api.tsv`（13045 行逐声明，体积 2.4 MB 未入库，可重跑生成）、`dedup_api.tsv`（3486 行去重 API）、`slices/<包名>.tsv`（42 个按包切分的文件，共 13,087 行，列 `since kind name receiver arity sig loc`）、`count_by_since.tsv`、`count_by_pkg.tsv`（无表头，42 行 `包 声明数`）、`api_by_version.tsv`；`extract_ext.mjs` 另有 `ext_props.tsv`（88 行扩展属性）。

各文档页的 API 表都是从这些 slice 复制的真实签名，可用 `awk -F'\t' '$3=="<API名>"' docs/_data/slices/kotlin_collections.tsv` 自行复核。

### 5.1 主解析器的一个已知盲区：扩展属性（已单独补抽）

`extract2.mjs` 把 `public inline val Path.pathString: String` 这类**带接收者的属性**的名字落在了接收者上（`name=Path`），于是 `Char.code`、`File.isRooted`、`Double.absoluteValue` 这些真 API 在 `dedup_api.tsv` 里查不到。为此又跑了一遍 `extract_ext.mjs`（同一套块注释状态机 + 6 行绑定规则，正则只匹配 `val/var 接收者.名字`），产物 `out3/ext_props.tsv`：**88 行、33 个去重 `(pkg,kind,name)`、其中 26 个是主解析器完全没有的名字**。

分布（`wc`/`awk` 实测）：按包 `kotlin.collections` 24、`kotlin.time` 23、`kotlin.math` 19、`kotlin.text` 7、`kotlin.io.path` 6、`kotlin` 4、`kotlin.io` 4、`kotlin.reflect` 1；按戳 无戳 47、1.2 20、1.3 8、1.4 4、1.5 7、2.1 2。

主解析器缺的 26 个名字（`包.名字(kind, 戳)`，`-` = 1.0 就有）：

| 包 | 名字 |
|---|---|
| `kotlin` | `stackTrace`(-)、`suppressedExceptions`(1.4)、`code`(1.5，即 `Char.code`) |
| `kotlin.collections` | `indices`(- 各基本类型数组；1.3 各 unsigned 数组)、`lastIndex`(同上) |
| `kotlin.io` | `extension`、`nameWithoutExtension`、`invariantSeparatorsPath`、`isRooted`（全部 `-`） |
| `kotlin.io.path` | `pathString`(1.5)、`name`(1.5)、`extension`(1.5)、`nameWithoutExtension`(1.5)、`invariantSeparatorsPathString`(1.5)、`invariantSeparatorsPath`(1.4，带 `@Deprecated`) |
| `kotlin.math` | `absoluteValue`、`sign`、`ulp`（均 1.2，`expect` + JVM `actual` 成对） |
| `kotlin.text` | `category`(1.5)、`directionality`(-)、`CASE_INSENSITIVE_ORDER`(1.2) |
| `kotlin.reflect` | `javaType`(1.4，`KType.javaType`) |
| `kotlin.time` | `days/hours/minutes/seconds/milliseconds/microseconds/nanoseconds`(`Duration.Companion` 内，无独立戳，随 1.6 的 `Duration` 一起)、`isDistantFuture`/`isDistantPast`(2.1) |

还有一个更小的盲区没有补抽：**接口体内不带修饰符的成员声明**（如 `KCallable.returnType`、`KClass.simpleName`）不会被主解析器收录（它只认 `public/internal/expect/actual/override` 开头的行），`kotlin_reflect.tsv` 里 grep `returnType` 为空即为证据；这部分在 `07-contracts-coroutines-reflect-jvm.md` 里是**直接读源文件**写的，不依赖 slice。

**教训**：切片"查不到"有两种完全不同的原因——API 真的不存在，或解析器漏了。区分方法是双向复核：正向 `grep -rn "名字" src/`（源码），反向 `javap -classpath . <类>`（字节码，如 `kotlin.sequences.SequencesKt___SequencesKt`）。两份文档里凡标"不存在"的名字都过了这两道，例如 `whileTake`/`consume`/`distinctUntilChanged` 两道全空，而 `pathString` 源码命中、字节码命中，只是主解析器漏了。

## 6. 阅读顺序建议

- 想弄清"Kotlin 的版本号到底按什么排" → [../kotlin-versioning.md](../kotlin-versioning.md)
- 刚上手，先建立 API 全景 → 本页 + `01` + `02`
- 查找具体函数 → `99-cheatsheet.md`
- 想知道"这个 API 我 1.7 能不能用" → `08-versioned-api.md`（按 `@SinceKotlin` 分版本清单）
