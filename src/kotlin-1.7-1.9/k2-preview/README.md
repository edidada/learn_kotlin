# k2-preview

归属：[文] 1.7.0 alpha / 1.9.20 beta

要覆盖：用 `-language-version 1.9 -Xuse-k2` 观察 K1/K2 诊断信息差别；这条线在 `src/kotlin-2.0-k2/compiler/` 继续。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / KGP 2.1.10 / JDK 17）

- README 原计划用的 `-Xuse-k2` 在 2.x 上**已经不是可用开关**，编译器原话：
  `e: Compiler flag -Xuse-k2 is no more supported. Compiler versions 2.0+ use K2 by default, unless the language version is set to 1.9 or earlier`
  也就是说 2.x 上想回到 K1，唯一的办法是压低 language version，而不是加开关。
- 本机能压到的下限是 1.6，再低直接拒：`-Plv=1.4` →
  `e: Language version 1.4 is no longer supported; please, use version 1.6 or greater.`
  于是得到一个真实可跑的 **K1/K2 对照窗口**：`-Plv=1.6` / `-Plv=1.9`（走 K1）vs 默认（走 K2）。
- 运行期能拿到的最硬证据是类文件里的 `@Metadata.metadataVersion`：默认（LV 2.1）编译实测 `mv=2.1.0`；`KotlinVersion.CURRENT=2.1.10`，stdlib `implementationVersion=2.1.10-release-473`，`java.version=17.0.12`。样本 `K2PreviewKt` 打印：
  ```
  runtime version=2.1.10 stdlib=2.1.10-release-473
  K2 metadata mv=2.1.0 jdk=17.0.12
  k2 preview probe OK: mv=2.1.0 kotlin=2.1.10
  ```
- 前端差异的真正证据在**编译期诊断**，窗口里量到的一组逐字对照（同一份代码，只换 `-Plv`）：

  | 代码 | `-Plv=1.9`（K1） | 默认（K2） |
  |---|---|---|
  | `when (val n = Expr.Num(7)) { is Expr.Num -> …; is Expr.Neg -> …; … }` 作用于 sealed 层级 | `e: 'when' expression must be exhaustive, add necessary 'else' branch` + 每个分支 `e: Incompatible types: Expr.Neg and Expr.Num`（`Expr.Add`、`Expr.Skip` 同）| 编得过、跑得出正确值，**也没有告警** |
  | `State.Loading != State.Done`（两个不同类型的数据对象） | `e: Operator '!=' cannot be applied to 'State.Loading' and 'State.Done'` | 编得过（见 `data-object` 篇） |
  | `data object Loading : State` 在 `-Plv=1.6` | `e: The feature "data objects" is only available since language version 1.9` | — |
  | `T & Any` 在 `-Plv=1.6` | `e: The feature "definitely non nullable types" is only available since language version 1.7` | — |
  | 尾逗号、`builder inference` 的 `@ExperimentalTypeInference` | 见 `src/kotlin-1.4-1.6/trailing-comma/` 与 `builder-inference/` 的同一窗口对照 | 同左 |

  第一行是这条线最有信息量的一条：K1 会把 `when (val x = 具体子类型)` 的主体类型**固定**成那个子类型，于是既判"不穷尽"，又逐分支报"类型不兼容"；K2 按主体实际类型做穷尽与智能转换。写跨版本样本时这一处最容易踩。
- 归属注脚：`-Xuse-k2` 是 1.7.0 alpha 引入的预览开关，1.9.20 进 beta，2.0.0 起默认；K2 的编译器子专题在 `src/kotlin-2.0-k2/`（分支 `version/2.0-k2`）继续。
