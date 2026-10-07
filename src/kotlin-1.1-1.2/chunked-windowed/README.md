# chunked-windowed

归属：[实] 1.2

要覆盖：`chunked`/`windowed`/`zipWithNext`/`shuffled`/`fill`，序列版与集合版签名不同。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="chunked" || $3=="windowed" || $3=="zipWithNext" || $3=="shuffled" || $3=="fill"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.chunkwin.ChunkWindowKt`）

- `chunked` 允许尾块不足：`(1..7).chunked(3) == [[1,2,3],[4,5,6],[7]]`；带 transform 时 `chunked(3) { it.sum() } == [6,15,7]`，中间 List 不再暴露。
- `windowed` 默认只保留完整窗口：7 个元素、size 3 → 5 个窗口；`step = 2` → 3 个；`partialWindows = true` 时末块是 `[7]`。
- transform 版支持解构形参：`windowed(3) { (a, b, c) -> a*100 + b*10 + c } == [123, 234, 345, 456, 567]`。
- CharSequence 版返回 `List<String>`：`"abcdefgh".chunked(3) == ["abc","def","gh"]`，`"abcde".windowed(2) == ["ab","bc","cd","de"]`。
- Sequence 版返回 `Sequence<List<T>>`（切片 sig 就是 `Sequence<T>.chunked(size: Int): Sequence<List<T>>`，2.2.10 里已不是早期的 `Sequence<Sequence<T>>`），实测 `xs.asSequence().chunked(2).map { it.sum() }.toList() == [3,7,11,7]` 惰性可用。
- `zipWithNext`：`xs.zipWithNext { p, q -> q - p }` 得到 6 个 1；CharSequence 版 `zipWithNext()` 出 `List<Pair<Char, Char>>`。
- 边界实测：`chunked(0)` 抛 `IllegalArgumentException`（用 `runCatching { ... }.exceptionOrNull() is IllegalArgumentException` 钉住）。
