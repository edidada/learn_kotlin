# content-deep-equality

归属：[实] 1.1

要覆盖：`contentDeepEquals`/`contentDeepToString`/`contentDeepHashCode`：多维数组的结构比较。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="contentDeepEquals" || $3=="contentDeepHashCode" || $3=="contentDeepToString"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
