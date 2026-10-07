package learn.kotlin10.ext

// Kotlin 1.0：扩展函数。归属 [文] 1.0。
// 跑：./gradlew run -PmainClass=learn.kotlin10.ext.ExtensionDispatchKt

open class Shape {
    fun callMember(): String = "成员方法被调用"
}
class Circle : Shape()

fun Shape.describe(): String = "Shape 的扩展方法"       // 扩展不参与多态
fun Circle.describe(): String = "Circle 的扩展方法"
fun Shape.callMember(): String = "扩展方法被调用"

// 可空接收者：扩展可以定义在 T? 上，成员不行
fun String?.orPlaceholder(): String = this ?: "<null>"

// 给泛型加扩展，且能拿到 this 的类型信息（inline + reified 才拿得到运行时类型）
fun <T> List<T>.firstOrNullLabel(): String = if (isEmpty()) "空" else "首个=${first()}"

inline fun <reified T> Any.isA(): Boolean = this is T

fun main() {
    // 1) 扩展按"声明时的静态类型"解析，不按运行时类型 —— 这是最容易踩的一条
    val s: Shape = Circle()
    println("s.describe() -> ${s.describe()}")          // Shape 的扩展，不是 Circle 的
    val c: Circle = Circle()
    println("c.describe() -> ${c.describe()}")

    // 2) 成员永远优先于同名扩展（这里用接收者侧调用演示优先级规则）
    val sh = Shape()
    with(sh) {
        println("在 Shape 作用域内调用 callMember() -> ${callMember()}")   // 成员赢
    }
    println("在 Shape 作用域外调用 callMember() -> ${sh.callMember()}")     // 同一个成员，扩展没机会

    // 3) 可空接收者扩展：省掉一串 ?:
    println("null.orPlaceholder() -> ${null.orPlaceholder()}，\"x\".orPlaceholder() -> ${"x".orPlaceholder()}")

    // 4) 泛型扩展 + reified：类型判断在编译期内联，能写真泛型
    println(listOf(1, 2, 3).firstOrNullLabel())
    println("ArrayList<Int> 是不是 Collection<Int> -> ${ArrayList<Int>().isA<Collection<Int>>()}")

    // 5) 扩展是静态解析的语法糖：编出来就是 ShapeKt.describe(shape)，字节码里没有"扩展方法"这个概念
    println("扩展的编译形态 -> 静态方法接收者作第一参数，所以它不能被子类重写")
}
