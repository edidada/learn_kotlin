# context-parameters

归属：[实] 2.2（API）/ [文]（语言）

要覆盖：`context` 相关：stdlib 里的 `context`/`contextOf`（2.2 戳）配合 context receivers/parameters 的语言实验；先分清"实验开关"和"能用的 API"。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="context" || $3=="contextOf"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin22.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin22.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
