# builder-inference

归属：[实] 1.6

要覆盖：`buildList`/`buildMap`/`buildSet`（`@BuilderInference` 注解本身 1.3 就有，但这批 DSL 是 1.6）。原方案在 1.7–1.9 档重复列了一次，只保留这里。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="buildList" || $3=="buildMap" || $3=="buildSet" || $3=="BuilderInference"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_collections_builders.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.builder.BuilderInferenceKt`）

通过，输出：`builder inference(1.6) OK: [1, 2, 3] {k=1, j=2} [1] castGuard=UnsupportedOperationException`。

- 构建器结果**不是** `ArrayList`：`javaClass.name` 实测 `kotlin.collections.builders.ListBuilder` / `MapBuilder` / `SetBuilder`（切片 `docs/_data/slices/kotlin_collections_builders.tsv`，1.6）。把它们强转成 `MutableList` 再 `add` 会 `UnsupportedOperationException`（message 为 null），所以"拿 buildList 的结果继续改"这条路要当场断掉，别留到运行期。
- `@BuilderInference` 的硬约束是 opt-in，不是可选糖：漏了 `@OptIn(ExperimentalTypeInference::class)` 时编译器原话
  `This declaration needs opt-in. Its usage must be marked with '@kotlin.experimental.ExperimentalTypeInference' or '@OptIn(kotlin.experimental.ExperimentalTypeInference::class)'`
  （marker 在 `kotlin.experimental` 包，切片 `kotlin_experimental.tsv` 可查）。
- 推翻一条常见说法：用"同一个 2.1.10 编译器只换语言版本"做 A/B，`val x = f { add(1) }` 这种"只从 lambda 体内推元素类型"的写法，**默认 LV(2.1) 下带注解和不带注解都能推成 Int**，看不出注解差别；而 `-Plv=1.6 -Pav=1.6` 下**两种写法一起**报 `Not enough information to infer type variable T`。也就是说在 K2 上这个推断缺口不是靠注解补的，注解的可观测价值要在老语言版本上才显现 —— 而老版本又已经不被接受到 1.6 以下（`Language version 1.4 is no longer supported; please, use version 1.6 or greater.`）。
- 空 lambda 必须显式类型实参：`buildList<Int> { }` 合法，`buildList { }` 推不出 T；`buildMap { put("k", 1); putAll(mapOf("j" to 2)) }` 与 `buildSet { add(1); add(1) }`（去重后 size 1）都按预期。
