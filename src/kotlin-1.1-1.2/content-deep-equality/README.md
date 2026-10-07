# content-deep-equality

归属：[实] 1.1

要覆盖：`contentDeepEquals`/`contentDeepToString`/`contentDeepHashCode`：多维数组的结构比较。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="contentDeepEquals" || $3=="contentDeepHashCode" || $3=="contentDeepToString"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.content.ContentDeepKt`）

- 陷阱坐实：内容完全相同的两个二维数组，`a contentEquals b` 是 false，`a == b` 也是 false（内层数组按引用比）；只有 1.1 的 `a contentDeepEquals b` 是 true。
- `contentDeepToString()` 输出 `[[0, 1], [10, 11]]`，形状等同 `java.util.Arrays.deepToString`。
- `contentDeepHashCode()` 对内容相同的嵌套数组给同一个哈希，所以能安全当 Map/Set 的键（1.1 之前只能自己拼）。
- 内层是基本类型数组时深比较退到 `contentEquals`：`arrayOf(intArrayOf(1,2), intArrayOf(3))` 两份实例 `contentDeepEquals` 为 true。
- `Array<IntArray?>` 里放 null 不抛异常，深比较照常成立。
- 深比较比内容不比容器类型：`arrayOf(listOf(1,2), "s")` 两份不同实例也相等。
