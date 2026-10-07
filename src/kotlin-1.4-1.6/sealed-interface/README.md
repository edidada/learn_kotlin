# sealed-interface

归属：[文] 1.5

要覆盖：`sealed interface`：受限继承 + `when` 穷尽检查的组合。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.sealed.SealedInterfaceKt`）

通过，输出：`sealed interface(1.5) OK: subclasses=[Add, Neg, Num, Skip]`。

- `Expr::class.java.declaredClasses` 里 `object Skip` 也在，排序后就是 `[Add, Neg, Num, Skip]`，可以直接拿它当"层级没有偷偷长出新分支"的回归断言。
- 跨包实现的探针原文：`class UsesRemote : Remote` → `A class can only extend a sealed class or interface declared in the same package.`（K2 的措辞，比 1.5 当年的报错更直白）。
- 穷尽性少写分支的措辞**随语言版本变**（同一份代码，只换 `-Plv`）：
  - 默认 LV(2.1)：`'when' expression must be exhaustive. Add the 'Skip' branch or an 'else' branch.` —— 点名缺哪个分支。
  - `-Plv=1.9`：`'when' expression must be exhaustive, add necessary 'else' branch` —— 只会让你补 else。
- 一个真踩到的 K1/K2 差异：`when (val n = Expr.Num(7)) { ... }` 这种 subject 是"变量声明 + 具体表达式"的写法，`-Plv=1.6/1.9` 编不过（除了上面那条不穷尽，还有 `Incompatible types: Expr.Neg and Expr.Num` 等逐分支报错）——老 Inference 把 subject 类型收窄成了 `Expr.Num`；默认 LV(2.1) 按声明的密封层级判定，四分支穷尽通过。写教程里"当表达式用不需要 else"的例子时，这条在老语言版本上要另加说明。

### 追加：这条差异的"两前端通吃"写法（同一 `-Plv` 窗口复核）

- 把 subject 的类型显式标出来即可：`when (val n: Expr = Expr.Num(7)) { … }`。
  本机对照探针（同一个 sealed 层级、同样的四分支）实测：`-Plv=1.9`（K1）**无报错**，默认 LV(2.1) 也通过并打印正确值。
- 反过来说明上一节那四条报错的根因不是"穷尽检查变严了"，而是 **subject 的类型推断**：
  不标类型时 K1 把 `val n = Expr.Num(7)` 推成 `Expr.Num`，父层级信息在 `when` 的穷尽判定里就丢了；标上 `: Expr` 后两个前端拿到的是同一个起点。
- 样本本身保留不标类型的写法（默认 LV 通过），只在注释里记下通吃写法，这样这个坑不会因为"修好"而从代码里消失。

### 追加：默认 LV(2.1 / K2) 编译这段时会打三条告警

`./gradlew compileKotlin --rerun-tasks` 的原始输出（本机，未经任何过滤）：

```
w: src/kotlin-1.4-1.6/sealed-interface/sealedInterface.kt:47:9 Check for instance is always 'true'.
w: src/kotlin-1.4-1.6/sealed-interface/sealedInterface.kt:48:9 Check for instance is always 'false'.
w: src/kotlin-1.4-1.6/sealed-interface/sealedInterface.kt:49:9 Check for instance is always 'false'.
```

- 必须带 `--rerun-tasks`：只 `touch` 源文件不改内容时，Gradle 按输入哈希判 up-to-date，直接跳过编译，`w:` 一条都不会出现——
  这不是"没有告警"，是"没编译"。（本机实测踩过，第一次量就是空输出。）

- 对应的是 `is Expr.Num` / `is Expr.Neg` / `is Expr.Add` 三行；`Expr.Skip ->` 那行不报（对象相等比较，不是 `is` 检查）。
- 也就是说 K2 并不是"干净通过"，而是**接受这种写法但提醒 subject 已被收窄**：
  K1 在这里是 error（四分支全报 Incompatible types + 不穷尽），K2 降成 warning 并让代码继续编。
  上一节写的"通吃写法"`when (val n: Expr = Expr.Num(7))` 同时能把这三条告警也消掉（实测无 `w:` 行）。
- 之前"默认 LV 通过且没有任何告警"的说法是错的，成因是把 Gradle 输出用 grep 过滤时漏了 `w:` 前缀行；
  教训：过滤编译输出必须同时留 `e:` 和 `w:`，否则等于没测。
