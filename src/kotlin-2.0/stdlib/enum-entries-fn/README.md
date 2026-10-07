# stdlib/enum-entries-fn

归属：[实] 2.0

要覆盖：顶层 `enumEntries<T>()` 函数（`EnumEntries` 类型本体是 1.9，见上一档）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="enumEntries"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_enums.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin20.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin20.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin20.enumsfn.EnumEntriesFnKt`）

输出：`enumEntries<T>()(2.0) OK: same=true names=[LOW, MID, HIGH] empty=0`

- 归属复核：`awk -F'\t' '$3=="enumEntries"{print $1"|"$6}' docs/_data/slices/kotlin_enums.tsv` →
  `2.0|public inline fun <reified T : Enum<T>> enumEntries(): EnumEntries<T> = enumEntriesIntrinsic()`；
  1.8 那两条是 internal 的 `EnumEntries(entriesProvider)/(entries)` 工厂，别当成公开 API。
  javap 复核（`kotlin.enums.EnumEntriesKt`）确实有 `public static final <T extends Enum<T>> EnumEntries<T> enumEntries()`。
- **`enumEntries<T>() === T.entries` 为 true**：函数返回的就是 1.9 那份缓存对象，不是新副本（实测 `same=true`）。
- 它唯一的增量价值在类型参数侧：`T.entries` 语法上写不出来（类型参数没有静态成员），只能靠 reified。
  所以包装函数必须一起 reified：`inline fun <reified T : Enum<T>> names() = enumEntries<T>().map{it.name}`；
  写成普通 `fun <T : Enum<T>>` 会直接编译失败。
- 顺带量到一条 K2 诊断（星投影读枚举成员）：`fun describe(e: Enum<*>) { e.ordinal }` 报
  `Cannot use 'T' as reified type parameter. Use a class instead.` —— 措辞很怪，但结论明确：
  通用入口要么 `<E : Enum<E>>`，要么退回 `toString()`。
- 对照旧写法：`Level::class.java.enumConstants` 每次返回新数组（`!==`），内容却和 `enumEntries<Level>()` 一致。
- 集合语义齐全：`associateBy`、`indexOf`、`filter`、`toString() == "[LOW, MID, HIGH]"`；空枚举 `entries.isEmpty()` 且 `enumEntries<Empty>().size == 0`。
