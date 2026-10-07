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
