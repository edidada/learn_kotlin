# stdlib-batch

归属：[实] 1.3

要覆盖：其余 1.3 新增按包铺开（实测条数：kotlin.collections 132、kotlin 37、kotlin.coroutines* 43、kotlin.contracts 10、kotlin.text 8、kotlin.sequences 7、kotlin.random 7、kotlin.time 5、kotlin.ranges 4）。text 侧确认有 `ifBlank`/`ifEmpty`/`String.random`，collections 侧有 `associateWith`/`associateWithTo`——先跑下面命令看全清单，再挑值得写的；注意 `sortedWith` 是 1.0 就有的（无版本戳），别当 1.3 新东西学。

复核归属（数据在仓库里，直接跑）：

```bash
# 这一档新增 API 的完整清单（api_by_version.tsv 无表头，列序 since\tpkg\tkind\tname\tsig）
awk -F'\t' '$1=="1.3"{print $3"\t"$4"\t"$2}' docs/_data/api_by_version.tsv | sort -u | head -60
# 按包看条数
awk -F'\t' '$1=="1.3"{c[$2]++} END{for(p in c) print c[p]"\t"p}' docs/_data/api_by_version.tsv | sort -rn
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin13.batch.Batch13Kt`）

- `ifBlank` / `ifEmpty`（含 Collection 版）实测：`"   ".ifBlank { "blank" } == "blank"`、`"x".ifBlank{...} == "x"`、`emptyList<Int>().ifEmpty { listOf(1,2) } == [1,2]`。切片里这两条 receiver 是 `C`，即"泛型上界自己"，所以集合版返回类型能保序到子类型。
- `associateWith` 只要 value 选择器（key 就是元素），`associateWithTo(dest)` 可指定目标容器并保持插入序（实测用 `linkedMapOf`）。
- `random` 一族（1.3）可注入 `Random`，因此可复现：两次 `listOf("a","b","c").random(Random(42))` 都得 `"c"`；`(1..6).random(Random(1))` 落在区间内；`"abcd".random(Random(0))` 落在字符集内。切片里 kotlin.collections 的 1.3 新增中 `random` 占 28 条，覆盖集合、数组、区间、CharSequence。
- `copyInto` 的 destination 是必填位置参数：`xs.copyInto(destinationOffset = 1, startIndex = 0, endIndex = 4)` 报 `No value passed for parameter 'destination'`——原地拷贝也得把自己显式传进去。实测 `[1,2,3,4,5]` 经 `copyInto(xs, 1, 0, 4)` 变 `[1,1,2,3,4]`（重叠区间按"逐元素搬"的语义成立）。
- `singleOrNull`：空集合给 null，`listOf(5,6).singleOrNull { it > 10 }` 给 null，`{ it > 5 }` 给 6。
- 反例确认：`sortedWith` 在切片里无版本戳（1.0 就有），实测能用，但不应写进"1.3 新增"。
