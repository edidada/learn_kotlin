# stdlib/padding-option

归属：[实] 2.0

要覆盖：`Base64.withPadding(PaddingOption.ABSENT_OR_1_AND_2_EQUAL_SIGNS)`（2.0 戳）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="PaddingOption" || $3=="withPadding"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_io_encoding.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin20.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin20.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin20.pad.PaddingOptionKt`）

输出：

```
PaddingOption/withPadding(2.0) OK: encodeTable={PRESENT=[YQ==, YWI=, YWJj], ABSENT=[YQ, YWI, YWJj], PRESENT_OPTIONAL=[YQ==, YWI=, YWJj], ABSENT_OPTIONAL=[YQ, YWI, YWJj]}
  decodeNoPad=PRESENT=null, ABSENT=true, PRESENT_OPTIONAL=true, ABSENT_OPTIONAL=true
  decodeWithPad=PRESENT=true, ABSENT=null, PRESENT_OPTIONAL=true, ABSENT_OPTIONAL=true
```

（`null` 表示这一次 `decode` 抛异常，被 `runCatching` 吃掉；输入分别是 1/2/3 字节，2 字节才需要补位。）

- 本机四个常量（javap `kotlin.io.encoding.Base64$PaddingOption`）：`PRESENT / ABSENT / PRESENT_OPTIONAL / ABSENT_OPTIONAL`。
  README 原来写的 `ABSENT_OR_1_AND_2_EQUAL_SIGNS`、`ALWAYS`、`IGNORE` **一个都不存在**，实测逐字报错：
  ```
  e: Unresolved reference 'ABSENT_OR_1_AND_2_EQUAL_SIGNS'.
  e: Unresolved reference 'ALWAYS'.
  e: Unresolved reference 'IGNORE'.
  ```
- 编码侧只看 `*_OPTIONAL` 是不是"更激进"：不是。`PRESENT_OPTIONAL` 与 `PRESENT` 编码结果相同，`ABSENT_OPTIONAL` 与 `ABSENT` 相同 ——
  `OPTIONAL` 修饰的是**解码端**接不接受缺/多填充。
- 解码侧的分工（上面的表）：`PRESENT` 遇到无填充输入抛异常；`ABSENT` 遇到带填充输入抛异常；
  两个 `*_OPTIONAL` 两种输入都接。想要"宽松解析外部数据"就选 `PRESENT_OPTIONAL`（自己输出仍带填充）。
- `withPadding` 返回**新实例**，不修改 receiver（实测 `absent !== Base64.Default`）；`paddingOption` 属性本身是 internal
  （字节码 `getPaddingOption$kotlin_stdlib`），源码层读不到，所以样本用"编一段 2 字节看有没有 `=`"当配置指纹。
- 2.1.10 的 opt-in 标记叫 `ExperimentalEncodingApi`（不是 `ExperimentalBase64Api`），文件级 `@file:OptIn(ExperimentalEncodingApi::class)` 才不报"needs opt-in"。
- 2.1.10 有 `Base64.Default / UrlSafe / Mime`，**没有 `Pem`**（切片里 `Pem` 那条没有 since，属于比本机更新的数据集）；
  `Mime` 输出带 `\n`，`Default` 不带 —— 实测过。
- 附带一条：`Base64.PaddingOption.entries`（1.9 语法）在嵌套枚举上照样可用，且 `entries === entries`。
