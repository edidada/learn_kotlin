package learn.kotlin10.smart

// Kotlin 1.0：智能转换（smart cast）。归属 [文] 1.0。
// 跑：./gradlew run -PmainClass=learn.kotlin10.smart.SmartCastKt

// 类默认 final（Kotlin 1.0 的默认封闭性），想被继承必须显式 open / abstract / sealed
sealed class Expr
class Num(val v: Int) : Expr()
class Sum(val a: Expr, val b: Expr) : Expr()

// 稳定值（val + 无自定义 getter + 同模块）才允许被智能转换
class Stable { val x: Any? = "str" }
class Unstable(private val hidden: Any?) { val x: Any? get() = hidden }   // 自定义 getter -> 不可智能转换

fun lenOf(any: Any): Int {
    // 1) if + is：条件成立后整个分支内 any 已是 String，不需要 (any as String)
    if (any is String) return any.length
    return -1
}

fun classify(e: Expr): String = when (e) {
    is Num -> "数字 ${e.v}"                            // when 分支里 e 已经是 Num
    is Sum -> "求和(${describe(e.a)}, ${describe(e.b)})" // 递归调用也能转
    else -> "未知"
}

fun describe(e: Expr): String = classify(e)

fun contractish(s: String?) {
    // 2) && 短路链路里的转换：右边的 s 已经是非空 String
    if (s != null && s.length > 0) println("非空且长度>0：$s")
    // 同理 !! 之后（1.7 起还能用 T!! 类型标注，见 version/1.7-1.9）
}

fun stabilityRules() {
    // 智能转换的前提是"值稳定"。局部 var 在同一流程段里是稳定的，后面的重新赋值不影响这段判断。
    var mutable: Any? = "x"
    if (mutable is String) println("局部 var，is 之后可直接用 -> 长度 ${mutable.length}")
    mutable = null

    // 类的属性带自定义 getter 就"不稳定"，编译器拒绝智能转换，必须显式强转
    val u = Unstable("y")
    if (u.x is String) {
        // println(u.x.length)          // 编译错误：Unresolved reference: length（自定义 getter）
        val v = u.x as String
        println("带 getter 的属性要显式强转 -> 长度 ${v.length}")
    }
    // 惯用解法：先拷进局部 val，副本就稳定了
    val copy = u.x
    if (copy is String) println("拷进局部 val 之后又能转了 -> 长度 ${copy.length}")
}

fun main() {
    println("lenOf(\"abcd\") = ${lenOf("abcd")}")
    println("classify -> ${classify(Sum(Num(1), Sum(Num(2), Num(3))))}")
    contractish("hello")
    contractish(null)

    val s = Stable()
    if (s.x is String) println("同模块 val 属性可智能转换 -> ${s.x.uppercase()}")   // 不需要 as
    stabilityRules()

    // 4) 转换只影响编译期检查，不产生任何强转指令（同类型名，反编译看得到差异）
    val any: Any = 42
    if (any is Int) println("any+1 = ${any + 1}")
}
