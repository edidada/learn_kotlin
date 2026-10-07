# stdlib-batch

归属：待实测

要覆盖：按 `docs/_data/README.md` 的脚本重跑抽取，用 `count_by_pkg_since.tsv` 对比 2.3/2.4 的增量。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin24.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin24.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
