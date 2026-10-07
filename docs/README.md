# docs —— Kotlin 版本模型 + 标准库 API 大全

这套文档回答两个问题：**Kotlin 到底按什么组织语言特性**（对应你熟悉的 Java 按 5/6/8/11/17、C++ 按 98/11/14/17/21），以及 **kotlin-stdlib 里到底有哪些 API、分别从哪个版本开始能用**。

所有结论都是实测出来的：数据来自本机 Gradle 缓存里的 `kotlin-stdlib-2.2.10-sources.jar` / `.jar`，逐行解析 + `javap` 字节码双向复核，脚本和中间产物都在 [`_data/`](_data/README.md) 里，任何人都能重跑。凡"直觉上存在但数据里没有"的名字（`SQRT2`、`Path.toFile`、`Sequence.consume`、`whileTake`、`Base64.encodeToString`、`Progression` 接口……）一律不写进正文当 API，只写进"常见误记"。

## 目录

| 文件 | 内容 | 什么时候看 |
|---|---|---|
| [kotlin-versioning.md](kotlin-versioning.md) | **Kotlin 按什么来的**：语言版本 / 工具版本 / bugfix 版三段式，2.x.0 → 2.x.20 的半年节奏，`@SinceKotlin`、opt-in 注解家族、语言版本 vs API 版本 vs progressive mode、K1→K2 时间线；附 C++ 与 Java 的对照表 | 想建立"Kotlin 的版本心智模型" |
| [git-branch-strategy.md](git-branch-strategy.md) | 用串行 `version/*` 分支把 Git 历史变成 Kotlin 1.0→2.4 演进时间轴：已建好的 10 档分支骨架、6 条操作规则、低版本补漏的逐级 merge、以及**用 `@SinceKotlin` 实测核对过的版本归属表**（纠正了 `Duration` 归 1.3、unsigned 标量归 1.3、`builder-inference` 重复等错档） | 开始按版本学习之前先读这页 |
| [kotlin-stdlib/00-overview.md](kotlin-stdlib/00-overview.md) | 库的结构地图：42 个包的大小（声明数 / 去重 API 数 / 带戳数）、jar 与 sources jar 的实测条目、jdk7/jdk8 artifact 合并史、抽取脚本要点与已知盲区 | 先看这页建立全景 |
| [kotlin-stdlib/01-core-types.md](kotlin-stdlib/01-core-types.md) | `kotlin` 包：内置类型、作用域函数（`apply`/`let`/`run`/`with`/`also`/`takeIf`/`takeUnless`）、`require`/`check`/`error`、`Result`/`runCatching`、数组工厂、`lazy`/委托、`toXxx` 转换族、异常与 `typealias`、枚举 API、`kotlin.math` | 每天写代码都要用的那层 |
| [kotlin-stdlib/02-collections.md](kotlin-stdlib/02-collections.md) | `kotlin.collections`：工厂、List/Set/Map 本体、增删改查/分组/聚合/排序扩展、无符号集合、builders | 集合操作 |
| [kotlin-stdlib/03-sequences-ranges-comparisons.md](kotlin-stdlib/03-sequences-ranges-comparisons.md) | `Sequence` 惰性链、`kotlin.ranges`（区间/等差序列的真实类层次）、`kotlin.comparisons` | 需要惰性求值或区间语义 |
| [kotlin-stdlib/04-text.md](kotlin-stdlib/04-text.md) | `kotlin.text`：字符串查询/变换/大小写（含 `capitalize` 的废弃史）、数字解析、格式化、`Regex`、2.2 的 `HexFormat` | 文本处理 |
| [kotlin-stdlib/05-io-path-encoding.md](kotlin-stdlib/05-io-path-encoding.md) | `kotlin.io`（`File` 扩展）、`kotlin.io.path`（NIO.2 Path API，1.5 首批 + 2.1 的 `walk`/`visitFileTree`）、`kotlin.io.encoding`（`Base64`，2.2 转正） | 文件与编解码 |
| [kotlin-stdlib/06-time-random-uuid-atomics.md](kotlin-stdlib/06-time-random-uuid-atomics.md) | `kotlin.time`（`Duration` 1.6、`TimeSource` 体系 1.9、`Instant`/`Clock` 2.1）、`kotlin.random`、`kotlin.uuid.Uuid`（2.0）、`kotlin.concurrent.atomics`（2.1） | 时间/随机/UUID/原子变量 |
| [kotlin-stdlib/07-contracts-coroutines-reflect-jvm.md](kotlin-stdlib/07-contracts-coroutines-reflect-jvm.md) | `kotlin.contracts`、`kotlin.coroutines` 基础设施（**注意 `launch`/`async` 不在 stdlib**）、`kotlin.reflect` 轻量反射、`kotlin.jvm` 注解族 | 写库、写 DSL、互操作 |
| [kotlin-stdlib/08-versioned-api.md](kotlin-stdlib/08-versioned-api.md) | 按版本查 API：每个版本进了多少个、2.0/2.1/2.2 的全量名字清单、1.x 各版本代表 API、`-api-version` 锁定与 opt-in 机制 | "这个 API 我在 1.x 能用吗" |
| [kotlin-stdlib/99-cheatsheet.md](kotlin-stdlib/99-cheatsheet.md) | 按任务查函数的一页速查表，带真实签名和版本戳 | 日常当小抄 |
| [_data/README.md](_data/README.md) | 原始证据：`jar tf` 清单、42 个包切片的 TSV、解析脚本、复现命令、已知的 5 个解析坑 | 想验证某个数字，或想给文档加新页 |

## 一句话结论（细节在各页）

- **Kotlin 不按"标准年份"也不按"JEP"组织，它按"版本列车"组织**：每半年一趟车（`2.x.0` 语言 + 编译器），3 个月后跟一趟工具版（`2.x.20`），中间随时发 bugfix（`2.x.yz`）。库 API 的引入时间戳直接写在源码注解 `@SinceKotlin("1.5")` 上，所以"哪个版本有什么 API"是可以机器读的，本套文档就是这么做出来的。
- 语言特性和库 API 是**两条独立的轨**：`..<`（语法）由编译器版本决定，`rangeUntil`（库函数）由 stdlib 版本决定，所以 Kotlin 1.9 的编译器 + 1.6 的 stdlib 会"语法能用、函数找不到"。
- 标准库的量级（2.2.10 实测）：42 个 `kotlin.*` 包，13,045 条重载级声明，3,486 个去重 API 名 + 26 个主解析器漏收的扩展属性 = **3,512 个**，其中只有 3,313 条声明带 `@SinceKotlin`。
- `kotlin-stdlib-jdk7`/`-jdk8` 从 1.8.22 起是**空壳 artifact**（实测 class 条目 0），内容早已并入主库——老教程里让你额外加这两个依赖的部分可以删了。

## 建议阅读顺序

先 `kotlin-versioning.md` 建立版本心智模型 → `00-overview.md` 看全景 → 按需查具体分册 → 日常开发把 `99-cheatsheet.md` 开着当小抄 → 遇到"我这版本能不能用"再回 `08-versioned-api.md`。
