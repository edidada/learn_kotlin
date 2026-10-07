# data-object

归属：[文] 1.9

要覆盖：`data object` 的 `toString`/`equals` 语义；与 `object` + 手动 equals 的差异。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.dataobj.DataObjectKt`）

输出：`data object(1.9) OK: toString=Loading hash=1599181449 nameHash=2001303836 label=prog 7`

- `toString()` 就是对象名本身：`State.Loading.toString() == "Loading"`，没有 `Loading@1b6d...` 那种默认形态。
- 生成物用反射数出来最清楚：`data object` 的 `declaredMethods` 排序后正好是 `[equals, hashCode, toString]`；对照普通 `object PlainObj` 的 `declaredMethods` 是**空**。
- 没有 `copy()`、没有 `componentN()`（`methods` 集合里都查不到），所以 `data object` 不是"data class 的无参特例"，只补了身份三件套。
- `interfaces` 只有声明的父接口 `[State]`，不会为"看起来像 data"额外实现 `Serializable`（对比 `enum entries` 那边倒是真带了 `Serializable`）。
- 相等语义在单例上没意义可讲：`a === b && a == b` 都成立，`Loading != Done`；差异只在 `toString`/可调试性。
- **坑**：`hashCode()` 稳定（同一份字节码重复跑得到同一个值），但**不是** `name.hashCode()`（实测 1599181449 ≠ 2001303836），而且相邻命名的两个 `data object` 的 hash 只差 1。断言只能写"稳定"和"彼此不等"，别按内容哈希去设计。
- 和 sealed 层级配合时，`data object` 分支写 `State.Done ->`（不是 `is`），穷尽检查照常；`when (val s: State = ...)` 形式在本档 K2 默认前端下编得过。

### 追加：同一份代码在 K1 / K2 上的差别（`-Plv` 窗口）

- `-Plv=1.6`：`e: The feature "data objects" is only available since language version 1.9`（第 7、8 行各报一次）—— 语言特性门槛，跟 stdlib 无关。
- `-Plv=1.9`（K1）：门槛过了，但第 30 行 `State.Loading != State.Done` 报
  `e: Operator '!=' cannot be applied to 'State.Loading' and 'State.Done'`；默认 K2 编得过、`check` 也成立。
  原因是 K1 把两个不同类型的数据对象当成"不可能相等的操作数"直接拒绝，K2 走普通的 `Any?.equals` 规则。
- 结论：`data object` 的**语法**门槛是 LV 1.9，但"能用得舒服"（跨类型 `!=`/`==`）实际要 K2，即 2.0 起。

### 追加：`when (val s: State = State.Prog(7))` 会让第一个分支告警

本机 `./gradlew compileKotlin --rerun-tasks` 的原话：

```
w: src/kotlin-1.7-1.9/data-object/dataObject.kt:42:9 Check for instance is always 'true'.
```

也就是说 K2 虽然接受 `when (val x = 具体子类型)` 这种写法（K1 直接 error，见上一节），但仍按初始化器把 subject 收窄，
于是第一个 `is State.Prog` 被判"恒真"、后续分支恒假。想在样本里避免这条告警，就把值显式 widen：
`val s: State = State.Prog(7)` 再 `when (s)`；这里保留原写法，因为"K1 error / K2 warning"正是这一档 Front-end 差异的样本。
（量的时候记得 `--rerun-tasks`：输入没变化时 Gradle 跳过编译，`w:` 一行都不会出。）
