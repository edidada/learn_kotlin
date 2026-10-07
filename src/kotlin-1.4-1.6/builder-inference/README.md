# builder-inference

归属：[实] 1.6

要覆盖：`buildList`/`buildMap`/`buildSet`（`@BuilderInference` 注解本身 1.3 就有，但这批 DSL 是 1.6）。原方案在 1.7–1.9 档重复列了一次，只保留这里。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="buildList" || $3=="buildMap" || $3=="buildSet" || $3=="BuilderInference"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections_builders.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
