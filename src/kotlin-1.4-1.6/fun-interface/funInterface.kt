package learn.kotlin1416.funif

// fun interface 是 1.4 的语言特性：只有一个抽象方法的接口可以声明成 fun interface，
// 于是 lambda / 方法引用能直接当实现（SAM 构造）。stdlib 侧同期配套：
// kotlin.collections 里 1.4 的 `@OverloadResolutionByLambdaReturnType`（切片可查）帮重载按 lambda 返回类型消歧。

fun interface ClickHandler {
    fun onClick(x: Int, y: Int)
}

fun interface Converter<in I, out O> {
    fun convert(input: I): O
}

// 对照用：普通 interface，同样只有一个抽象方法，但没写 fun
// 探针原文：val p = PlainOne2 { 1 } -> Interface 'interface PlainOne2 : Any' does not have constructors.
// 也就是说 1.4 之后 Kotlin 侧的 SAM 只认 fun interface，"只有一个抽象方法"不再是充分条件。
interface PlainOne {
    fun get(): Int
}

fun strLen(s: String) = s.length

fun consumeInt(c: Converter<Int, String>): String = c.convert(5)
fun consumeRef(c: Converter<String, Int>): Int = c.convert("kotlin")

fun main() {
    // 1) lambda 直接实现（SAM 构造器写法）
    val h = ClickHandler { x, y -> check(x + y == 3) }
    h.onClick(1, 2)

    // 2) SAM 构造 + 泛型实参
    val c = Converter<Int, String> { it.toString(2) }
    check(consumeInt(c) == "101")

    // 3) 函数引用在"实参位置"完成 SAM 转换
    check(consumeInt { it.toString(16) } == "5")
    check(consumeRef(::strLen) == 6)

    // 4) 对象表达式仍然可用（fun interface 不排斥老写法）
    val obj = object : Converter<String, Int> {
        override fun convert(input: String) = input.hashCode()
    }
    check(consumeRef(obj) == "kotlin".hashCode())

    // 5) 观察：默认 indy 形态下，两个形状完全相同的 SAM lambda 落在不同调用点，
    //    生成的是两个不同的 proxy class（实测 same impl class = false），别指望 SAM 实例有类型可复用
    val a: ClickHandler = ClickHandler { _, _ -> }
    val b: ClickHandler = ClickHandler { _, _ -> }
    println("fun interface(1.4) OK: same impl class = ${a.javaClass == b.javaClass}, class = ${a.javaClass.simpleName}")
}
