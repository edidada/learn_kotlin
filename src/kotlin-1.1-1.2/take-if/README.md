# take-if

归属：[实] 1.1

要覆盖：`takeIf`/`takeUnless` 的返回是 `T?`，链式写法与 `let` 的配合。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="takeIf" || $3=="takeUnless"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.takeif.TakeIfKt`）

- 返回类型是 `T?` 而不是 `T`：`takeIf` 之后必须继续 `?.` 或 `?:`，否则空安全立刻拦你。
- `"42".toIntOrNull()?.takeIf { it in 1..100 }` → 42；`"500".toIntOrNull()?.takeIf { it in 1..100 }` → null。整条链没有一个 `if`。
- 短路实测：`null?.takeIf { calls++; ... }` 的 lambda 执行次数为 0（用计数器 `check(calls == 0)` 钉死），谓词里的副作用在 null 输入时不会发生。
- 谓词参数就是接收者本身（`it`），能直接喂区间判断、`isBlank()` 这类现成谓词。
