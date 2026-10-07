# contracts

归属：[实] 1.3

要覆盖：`contract { returns() implies ... }` 如何解锁智能转换；当时带 `@ExperimentalContracts`，1.9 转正。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="contract" || $3=="ExperimentalContracts"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_contracts.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
