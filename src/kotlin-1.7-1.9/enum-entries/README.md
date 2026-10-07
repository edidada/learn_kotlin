# enum-entries

归属：[实] 1.9

要覆盖：`EnumEntries` 类型与 `MyEnum.entries`（戳 1.9）。顶层函数 `enumEntries<T>()` 是 2.0 的，留给 `src/kotlin-2.0-k2/`；1.8 里那个 1.8 戳的 `EnumEntriesList` 是 internal 预研。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="EnumEntries" || $3=="EnumEntriesList" || $3=="enumEntries"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_enums.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.enums.EnumEntriesKt`）

输出：`enum entries(1.9) OK: class=EnumEntriesList same=true valuesSame=false`

- `Color.entries` 是**缓存对象**：`Color.entries === Color.entries` 为 true；对照 `Color.values() !== Color.values()`（每次新数组）。这是 `entries` 省掉克隆开销的全部理由。
- 运行期类型实测 `kotlin.enums.EnumEntriesList`，`interfaces` 排序后是 `[EnumEntries, Serializable]`。
- 它是 `List` 不是 `Array`：`e[0] === Color.RED`、`map/filter/find/size` 全部直接可用，`e.toString() == "[RED, GREEN, BLUE]"`（List 的 toString，不是数组的 `@` 形态）。
- 带构造参数的枚举一样：`Shape.entries.map { it.sides } == listOf(3, 4)`，成员方法 `describe()` 照旧，`maxOf { it.sides } == 4`。
- 切片复核归属：`1.8 class EnumEntriesList`（internal 预研）/ `1.9 interface EnumEntries` / `1.9 fun enumEntriesIntrinsic`；**顶层 `enumEntries<T>()` 打的是 2.0 戳**，留给 `src/kotlin-2.0-k2/` 那档，本样本不用。
