# stdlib/uuid

归属：[实] 2.0

要覆盖：`kotlin.uuid.Uuid`（`@ExperimentalUuidApi`，2.0）：`parse`/`random`/`toULong`/`getUuid`。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="Uuid" || $3=="ExperimentalUuidApi" || $3=="getUuid" || $3=="putUuid"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_uuid.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin20.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin20.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin20.uuid.UuidKt`）

输出：`Uuid(2.0) OK: nil=00000000-0000-0000-0000-000000000000 parse=123e4567-e89b-12d3-a456-426614174000 random=df71de84-a5c2-4bf6-b99d-293f062634ec bytes=16`

- 归属复核（切片）：`awk -F'\t' '$3=="Uuid"{print $1}' docs/_data/slices/kotlin_uuid.tsv` → `2.0`；opt-in 标记 `ExperimentalUuidApi` 同为 2.0，源码里必须 `@file:OptIn(ExperimentalUuidApi::class)`。
- **比切片更硬的证据是 javap**（`kotlin-stdlib-2.1.10.jar`）：
  - `kotlin.uuid.Uuid implements java.io.Serializable` —— **没有实现 Comparable**，所以 `a < b` 写不出来，排序只能用 `Uuid.LEXICAL_ORDER`（实测 `javap` 里既无 `compareTo` 也无 `Comparable`）。
  - 实例成员只有 `toString / toHexString / toByteArray` + 两个 `@PublishedApi internal` 的 Long。
  - `Uuid$Companion`：`getNIL / parse / parseHex / random / fromLongs / fromULongs-eb3DHEI / fromByteArray / getLEXICAL_ORDER`（`fromULongs` 的字节码名带 mangle 后缀）。
- 切片里有、本机 2.1.10 **没有**的成员（逐个实测都是 `Unresolved reference '...'`）：
  `toLongs`、`toULongs`、`toHexDashString`、`parseHexDash`、`toUByteArray`、`fromUByteArray`。
  其中 `toHexDashString/parseHexDash/toUByteArray/fromUByteArray` 在切片里标的还是 2.1 —— 说明那份数据集比本机工具链新，`since` 不能当"我手里这个 jar 有没有"来用；要判断可用性，量 jar。
- 位模式坑：低 64 位最高位是 1 时，`"a456426614174000".toLong(16)` 直接 `NumberFormatException`（超 Long 范围），必须 `toULong(16).toLong()`；读回十六进制用 `Long.toHexString` 得到补码形态。
- `toString()` 就是 36 字符 hex-dash（`[8]` 和 `[23]` 是 `-`），`toHexString()` 是 32 字符无连字符，`parseHex` 能把它解回来。
- v4 形态可以断言：去掉连字符后第 12 位是版本 nibble（`'4'`），第 16 位在 `8..b`（variant 10xx）；`Uuid.random()` 每次不同。
- 与 JDK 的边界：`java.util.UUID` 和 `kotlin.uuid.Uuid` 是两个类，只能靠字符串/字节互通（`UUID.fromString(t).toString() == Uuid.parse(t).toString()`，但 `javaClass` 不同）。
- `ByteBuffer` 那一对是顶层扩展，**要显式 import**：`import kotlin.uuid.getUuid` / `import kotlin.uuid.putUuid`，否则 `Unresolved reference 'putUuid'`；`putUuid(uuid)` 顺序写，`putUuid(index, uuid)` 定点写，`getUuid(index)` 读回都实测通过。
