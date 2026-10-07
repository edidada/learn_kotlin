# grouping-collectors

归属：[实] 1.1

要覆盖：`groupingBy` + `eachCount`/`foldTo`/`reduceTo`/`aggregate`，对比 `groupBy` 的内存差异。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="groupingBy" || $3=="eachCount" || $3=="foldTo" || $3=="reduceTo" || $3=="aggregate"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.grouping.GroupingKt`）

- `words.groupingBy { it.length }.eachCount()` → `{2=5, 4=1, 3=1}`，一次遍历，不建每组的中间 List。
- `fold` 必须给初始值：`fold(0) { acc, w -> acc + w.length }`；`*To` 版同理，实测 `foldTo(dest) { ... }` 少写 initial 直接 Unresolved，`foldTo(dest, 0) { acc, _ -> acc + 1 }` 才通过。
- `aggregate` 的 lambda 第四个参数 `first` 是必需的：首元素时 accumulator 是 null，所以写成 `when { first -> w; w > acc!! -> w; else -> acc!! }`。
- 类型推断要帮忙：`aggregate { _, acc, w, first -> ... }` 报 "Cannot infer type"，给 `acc` 显式标成 `String?` 后 `Map<Int, String>` 才落定。
- `eachCountTo(dest)` 返回的对象 `=== dest`（就地写入而不是新 Map），复用容器确实省一次分配。
- 与 1.0 的 `groupBy` 对比实测：`groupBy.keys == eachCount().keys`，`groupBy(k).size == eachCount()[k]`——两者可互相验证，但 groupingBy 只做一趟。
