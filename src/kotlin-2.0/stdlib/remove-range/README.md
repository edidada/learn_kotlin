# stdlib/remove-range

归属：[实] 2.0

要覆盖：`MutableList.removeRange(range)`（2.0）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="removeRange"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin20.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin20.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin20.removerange.RemoveRangeKt`）

输出：`removeRange OK: text=aef csType=java.lang.StringBuilder subList=[1, 5] arrayDeque=[protected 2] jdkCut=[1, 5]`

- **先纠归属**：README 期望的 `MutableList.removeRange(range)` 在本机 stdlib 2.1.10 里不存在，两条证据：
  1. `javap -p kotlin.collections.CollectionsKt___CollectionsKt | grep -i removerange` → 无输出；
  2. 源码里写 `mutableListOf(1,2,3).removeRange(0..1)` → `e: Unresolved reference 'removeRange'.`
- 切片里那条 2.0 记录长这样：`2.0|fun|protected open fun removeRange(fromIndex: Int, toIndex: Int): Unit`
  —— **protected**、两个 Int 参数，是 `ArrayDeque` 上继承自 JDK 那条线的方法（javap：`protected void removeRange(int,int)`），
  不是"集合范围删除扩展"。反射实测修饰符与元数：`[protected 2]`。
- 而 `kotlin.collections.ArrayDeque` 是 final 类（`Modifier.isFinal` 实测 true），Kotlin 里既不能子类化也够不到 protected，
  所以这条 2.0 记录在源码层等于不可用 —— 别把它写进"2.0 新 API"清单。
- 真正可用的三条范围删除路径（都跑过，结果 `[1, 5]`）：
  - `list.subList(1, 4).clear()`（越界时 `IndexOutOfBoundsException`，实测 `subList(0,3)` 在单元素表上就抛）；
  - `list.removeAll(setOf(2,3,4))` / `list.removeAll { it in 2..4 }`（没删到返回 false）；
  - 不可变侧 `list - setOf(2,3,4)`。
  另加一条 JVM 专属：`class JdkBacked<T> : ArrayList<T>()` 里可以直接调 `removeRange(fromIndex, toIndex)`，
  因为 `kotlin.collections.ArrayList` 是 `java.util.ArrayList` 的 typealias，protected 够得着（实测生效）。
- 文本侧的 `removeRange` 一直是 1.0 的（`kotlin_text.tsv` 里 since 为空）：`"abcdef".removeRange(1..3) == "aef"`，
  区间版是**含尾**的（`IntRange(2,3)` 删索引 2、3），与 `removeRange(1, 4)` 的半开区间等价。
- **坑**：`CharSequence.removeRange` 的运行期类型是 `java.lang.StringBuilder`，不是 `String`，
  所以 `cs == "abef"` 为 false（实测就栽在这一行），要比 `cs.toString()`；`String.removeRange` 才返回 `String`。
