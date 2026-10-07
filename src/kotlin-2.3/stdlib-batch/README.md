# stdlib-batch

归属：待实测

要覆盖：升级后重抽数据：`cp docs/_data/scripts/extract2.mjs` 那套指向新的 sources jar，产出新的 `slices/*.tsv`，再回来填这档的主题。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin23.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin23.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
