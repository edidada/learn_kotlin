# unsigned-scalars

归属：[实] 1.5

要覆盖：无符号**标量**类型本体：`UInt`/`ULong`/`UShort`/`UByte`，以及 `toUInt`/`uintToInt` 的位重解释。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="UInt" || $3=="ULong" || $3=="UShort" || $3=="UByte" || $3=="toUInt"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.uscalars.UnsignedScalarsKt`）

通过，输出：`unsigned scalars(1.5) OK: UInt -> kotlin.UInt, ULong -> kotlin.ULong, max=4294967295`。

- 切片复核（数据在仓库里）：`awk -F'\t' '$1=="1.5" && $2=="class" && $3 ~ /^U(Int|Long|Byte|Short)$/{print $3}' docs/_data/slices/kotlin.tsv` —— 标量类是 1.5；无符号**数组**早在 1.3（见 version/1.3 的 unsigned-arrays）。两档的口径在 README 里分别记，别混。
- 装箱类名是 `kotlin.UInt`/`kotlin.ULong`，未装箱时底层是原生 `int`/`long`；`(-1).toUInt() == UInt.MAX_VALUE`、`UInt.MAX_VALUE.toInt() == -1` 证明 toXxx 改的是**读法**不是数值。
- 回绕不抛不置负：`UInt.MAX_VALUE + 1u == 0u`、`0u - 1u == UInt.MAX_VALUE`。
- 移位量必须是 `Int`：`0xFFu shr 4u` 的探针原文 `Argument type mismatch: actual type is 'kotlin.UInt', but 'kotlin.Int' was expected.`，写成 `shr 4` 才对。`countOneBits()` 的 Int 版 1.4 就有，UInt 版要靠 `.toInt()` 或直接用无符号扩展（切片可查）。
- 宽度提升实测：`3u.toUShort() + 4u.toUShort()` 的运行期对象 `javaClass.name == "kotlin.UInt"` —— 不是 UShort，跨类型的无符号算术一律先加宽。
- 解析拒绝负号：`"-1".toUIntOrNull() == null`，`"4294967295".toULongOrNull()` 正常。
- 本档**不需要** `@OptIn(ExperimentalUnsignedTypes::class)`：上一档已实测 marker `kotlin.ExperimentalUnsignedTypes` 在 2.1.10 的 stdlib 里已经不存在（import 直接 Unresolved reference），1.3/1.5 当年强制的标注释现在写了反而编不过。
