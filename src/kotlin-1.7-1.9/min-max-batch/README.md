# min-max-batch

归属：[实] 1.7

要覆盖：`max`/`min`/`maxBy`/`minWith` 一族在 1.7 补齐到集合与序列上（配合可空版 `maxOrNull` 的对比）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="max" || $3=="min" || $3=="maxBy" || $3=="minWith"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.minmax.MinMaxBatchKt`）

输出：`min/max batch(1.7) OK: max=3 maxBy=ccc emptyGuard=NoSuchElementException`

- 非空版一族都可用且直接给值：`listOf(3,1,2).max() == 3`、`min() == 1`、`maxBy { it.length } == "ccc"`、`minWith(compareBy { it.length }) == "a"`。
- **空集合的失败形态**：`emptyList<Int>().max()` 抛 `NoSuchElementException`，而且实测 **`message == null`** —— 老文档里那句 `Empty collection can't be reduced.` 是 `reduce` 的消息，别照抄到断言里。`min()/maxBy{}/intArrayOf().max()/sequenceOf<Int>().max()` 抛的是同一个无消息异常。
- 分工核对：可空版 `maxOrNull()/minOrNull()/maxByOrNull{}` 空集合给 `null`（这几个 1.4 就有，切片查得到）。
- 数组版齐全且含无符号：`intArrayOf(5,9,2).max() == 9`、`longArrayOf(1L,7L).min() == 1L`、`uintArrayOf(1u,8u).max() == 8u`、`charArrayOf('z','a').max() == 'z'`；切片复核这批 receiver 全打 1.7 戳。
- `Sequence` 版有：`sequenceOf(1,5,3).max() == 5`；`Map` 上的 `maxBy` 选择器作用在 `Map.Entry` 上，`map.maxBy { it.value }.key == "b"`。
- 对照项：双/多元 `maxOf/minOf` 是 1.1 就在的，**不属于**本档新增，样本里放在一起只为区分 `max()`（集合）与 `maxOf()`（变长参数）。
- NaN 行为（实测）：`listOf(1.0, Double.NaN, 3.0).max()` 返回 NaN，`maxOrNull()` 同样返回 NaN —— 非空版没有帮你过滤 NaN。

### 追加：无符号数组那两行仍然要 opt-in

`uintArrayOf(1u, 8u).max()` 在本机（2.1.10）会报：

```
w: This declaration needs opt-in. Its usage should be marked with '@kotlin.ExperimentalUnsignedTypes' or '@OptIn(kotlin.ExperimentalUnsignedTypes::class)'
```

所以样本文件头加了 `@file:OptIn(ExperimentalUnsignedTypes::class)`。顺带记一句边界：无符号**标量**（`UInt` 等）1.5 就转正了，但无符号**数组**这条线到 2.1 仍然挂着实验标记，两者不是一回事。
