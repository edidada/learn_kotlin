package learn.kotlin1416.sealed

// sealed interface 是 1.5 放开的（1.0 起只有 sealed class）：
// 受限继承 + when 穷尽检查，用来替代"表达式层级 + 抽象类"的写法。

sealed interface Expr {
    data class Num(val value: Int) : Expr
    data class Neg(val inner: Expr) : Expr
    data class Add(val left: Expr, val right: Expr) : Expr
    object Skip : Expr
}

fun eval(e: Expr): Int = when (e) {
    is Expr.Num -> e.value
    is Expr.Neg -> -eval(e.inner)
    is Expr.Add -> eval(e.left) + eval(e.right)
    Expr.Skip -> 0
    // 少写分支就是编译期错误，且消息措辞按语言版本变（实测同一份代码）：
    //   默认 LV(2.1/K2)：'when' expression must be exhaustive. Add the 'Skip' branch or an 'else' branch.
    //   -Plv=1.9       ：'when' expression must be exhaustive, add necessary 'else' branch
    // 也就是说老 K1 只告诉你"补 else"，K2 会点名缺哪几个分支。
}

fun main() {
    val tree: Expr = Expr.Add(Expr.Num(3), Expr.Neg(Expr.Num(1)))
    check(eval(tree) == 2)
    check(eval(Expr.Skip) == 0)

    // sealed 的可达性：子类型只能和父接口同包同模块，外部无法再新增
    // 跨包实现的探针原文：A class can only extend a sealed class or interface declared in the same package.
    val classes = Expr::class.java.declaredClasses.map { it.simpleName }.sorted()
    check(classes == listOf("Add", "Neg", "Num", "Skip"))

    // 穷尽 when 还能当表达式用，不需要 else
    // 实测坑：subject 写成 `when (val n = Expr.Num(7))` 时，K1 老的行为会把 subject 类型收窄成
    // 表达式的具体类型 Expr.Num，于是其余分支报 Incompatible types、并说 when 不穷尽；
    // 同一份代码 -Plv=1.6/1.9 编译不过，默认 LV(2.1) 通过：
    //   'when' expression must be exhaustive, add necessary 'else' branch
    //   Incompatible types: Expr.Neg and Expr.Num
    val label: String = when (val n = Expr.Num(7)) {
        is Expr.Num -> "num ${n.value}"
        is Expr.Neg -> "neg"
        is Expr.Add -> "add"
        Expr.Skip -> "skip"
    }
    check(label == "num 7")

    println("sealed interface(1.5) OK: subclasses=$classes")
}
