# unsigned-arrays

归属：[实] 1.3

要覆盖：无符号**数组**先于标量类型出现：`UByteArray`/`UShortArray`/`UIntArray`/`ULongArray`（标量 `UInt` 本体是 1.5，见下一档）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="UIntArray" || $3=="UByteArray" || $3=="UShortArray" || $3=="ULongArray"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin13.unsigned.UnsignedArraysKt`）

- 版本分层钉死：`UByteArray/UIntArray/UShortArray/ULongArray` 的 class 条目戳 1.3，`UByte/UInt/UShort/ULong` 标量类戳 1.5（切片可查）。所以 1.3 只能"造出数组、按无符号读"：`byteArrayOf(1, -1).toUByteArray()` 里 `-1` 读成 `255`，`intArrayOf(-1).toUIntArray()` 读成 `4294967295`，但拿不到任何字面量。
- marker 的真实 FQN 是 `kotlin.ExperimentalUnsignedTypes`（默认导入即可）；我按直觉写的 `import kotlin.experimental.ExperimentalUnsignedTypes` 报 Unresolved reference。
- opt-in 级别不一样：无符号这边漏标 `@OptIn` 只出 **warning**（`This declaration needs opt-in. Its usage should be marked with ...`），而 contracts 是 **error**。级别由 marker 自己的 `@RequiresOptIn(level = ...)` 决定，别把两者当同一档。
- 视图 vs 拷贝实测对照：`backed.asUIntArray()` 改源数组后视图跟着变（`9`），`backed.toUIntArray()` 改源数组后拷贝仍是旧值（`9` 而源已变 `1`）。
- 1.5 才通的部分同样实测：`(-1).toUByte() == UByte.MAX_VALUE`、`1u + 2u == 3u`、`uintArrayOf(0u, 1u)`——工厂签名 1.3 就在，但参数类型 `UInt` 要 1.5 才构造得出来，这正是"数组先行一年"的实际后果。
