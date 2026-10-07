# contracts

归属：[实] 1.3

要覆盖：`contract { returns() implies ... }` 如何解锁智能转换；当时带 `@ExperimentalContracts`，1.9 转正。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="contract" || $3=="ExperimentalContracts"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_contracts.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin13.contracts.ContractsKt`）

- 写契约到今天仍是 **error 级** opt-in，探针原文：`This declaration needs opt-in. Its usage must be marked with '@kotlin.contracts.ExperimentalContracts' or '@OptIn(kotlin.contracts.ExperimentalContracts::class)'`。所以"1.9 转正"只体现在读契约方（智能转换）不需要任何标注，写契约方还得 @OptIn。
- A/B（同一调用点，只改函数体）：带 `returns(true) implies (this@notBlank != null)` 时 `if (s.notBlank()) s.length` 通过；去掉契约报 `Only safe (?.) or non-null asserted (!!.) calls are allowed on a nullable receiver of type 'kotlin.String?'`。
- A/B（`callsInPlace(block, InvocationKind.EXACTLY_ONCE)`）：有契约时 `var n: Int; exactlyOnce { n = 5 }; check(n == 5)` 通过；换成没有契约的 inline 函数报 `Variable 'n' must be initialized.`。
- 附带发现：把 lambda 交给**非 inline** 函数时，`noContractBody { n = 5 }` 之后再读 `n` 竟然不报未初始化——闭包赋值走了另一套判定。结论：契约真正解锁的是 inline lambda 场景，别把两件事混为一谈。
- 复核手段：临时把探针 `.kt` 放进 `src/kotlin-1.3/_probe/`（必须落在 build.gradle 已注册的 `src/kotlin-*` 里，放 `src/_probe/` 根本不会被编译），跑 `./gradlew compileKotlin` 抓原文，再删掉。
