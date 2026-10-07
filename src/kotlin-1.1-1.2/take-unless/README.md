# take-unless

归属：[实] 1.1

要覆盖：同一批里的 `takeUnless`：反向条件写法，练一次就够。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="takeUnless"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.takeunless.TakeUnlessKt`）

- 对偶关系钉死：`n = 42`（偶数）时 `takeIf { it % 2 == 0 } == 42`、`takeUnless { it % 2 == 0 } == null`——同一谓词恰好一个非 null。
- 自己踩过的坑：第一版断言写成 `ifEven == null && unlessEven == 42`，`check` 当场失败。记住 takeUnless 的 true 等于丢弃。
- `sanitize(input) = input?.trim()?.takeUnless { it.isEmpty() }` 三例全过：空白串 → null，`" hi "` → `"hi"`，null → null。
- 结果一旦为 null，后续 `?.let { ... }` 不执行（实测计数器为 0），这是 takeUnless 比 if/else 好读的根本原因。
