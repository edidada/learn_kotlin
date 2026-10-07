# random

归属：[实] 1.3

要覆盖：`Random.Default`/`Random(seed)`/`nextInt(bound)`/`nextDouble`，以及它为什么不是 `java.util.Random` 的包装。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="Random"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_random.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin13.random.RandomKt`）

- 同种子同序列：两个 `Random(42)` 各取 5 个 `nextInt()` 完全一致，这是它能当测试夹具的前提。
- bound 是半开区间：2000 次 `nextInt(10)` 实测落在 `[0,10)`；500 次 `nextDouble()` 全在 `[0.0, 1.0)`；`nextLong(100)` 同理 `in 0 until 100`。
- 它不是 `java.util.Random` 的薄包装：同种子 12345，`Random(12345).nextInt() = -984488024`，`java.util.Random(12345).nextInt() = 1553932502`，实测不相等。
- `Random.Default.javaClass.name` 实测是 `kotlin.random.Random$Default`——Default 是伴生对象，具体算法在内部再按平台挑选，所以别拿类名去猜算法。
- `asJavaRandom()` / `asKotlinRandom()` 互转可用（切片里这两条也戳 1.3），把 Kotlin 的随机源交给 Java API 再拿回来没有损失。
- `kotlin.random` 整包 since=1.3：`Random`（class）+ 工厂 `Random(seed)` + `nextInt/nextLong/nextBytes` 一族。
