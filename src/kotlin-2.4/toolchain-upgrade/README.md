# toolchain-upgrade

归属：待实测

要覆盖：插件与 stdlib 升到 2.4.x，顺手验证 `languageVersion` 下界是否又往上抬了一格（本仓库 2.1.10 的下界是 1.6，见 `docs/git-branch-strategy.md` 第 9 节）。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin24.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin24.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
