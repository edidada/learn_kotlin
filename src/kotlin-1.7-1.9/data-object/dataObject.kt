package learn.kotlin1719.dataobj

// data object：1.9 的语言特性（不是 stdlib API，所以切片里查不到符号，属于 [文] 1.9 档）。
// 生成的是 equals/hashCode/toString 三个成员；对照普通 object：一个都不生成。

sealed interface State {
    data object Loading : State
    data object Done : State
    data class Prog(val pct: Int) : State
}

object PlainObj

@JvmInline
value class Name(val v: String)

fun main() {
    // 1) toString = 名字本身（实测 "Loading" / "Done"）
    check(State.Loading.toString() == "Loading")
    check(State.Done.toString() == "Done")

    // 2) 反射看到的成员：data object 生成 equals/hashCode/toString，普通 object 一个都不生成
    check(State.Loading::class.java.declaredMethods.map { it.name }.sorted() == listOf("equals", "hashCode", "toString"))
    check(PlainObj::class.java.declaredMethods.isEmpty())

    // 3) 相等语义：单例本身就只有一个实例，所以 data 版本和普通 object 在这点上看不出差别
    val a: State = State.Loading
    val b: State = State.Loading
    check(a === b && a == b)
    check(State.Loading != State.Done)

    // 4) 实测坑：data object 的 hashCode 是**稳定的**（同一份代码跑两次 JVM 得到同一个值），
    //    但它**不等于**名字字符串的 hashCode —— 探针里 "A" 的 data object 得到 -767313049，
    //    而 "A".hashCode() 是 65；两个相邻命名的 data object 的 hash 只差 1，
    //    说明公式跟名字有关但不是 name.hashCode()。别按"内容哈希"去设计断言。
    check(State.Loading.hashCode() == State.Loading.hashCode())
    check(State.Loading.hashCode() != "Loading".hashCode())
    check(State.Loading.hashCode() != State.Done.hashCode())

    // 5) 与 sealed 层级的 when 穷尽配合：data object 分支仍然写 `State.Done ->`，不用 is
    val label = when (val s: State = State.Prog(7)) {
        is State.Prog -> "prog ${s.pct}"
        State.Done -> "done"
        State.Loading -> "loading"
    }
    check(label == "prog 7")

    // 6) 观察：data object 没有 copy()/componentN()；interfaces 只有它声明的父接口（探针实测 [State]），
    //    不会为了"看起来像 data"去额外实现 Serializable
    val members = State.Done::class.java.methods.map { it.name }.toSet()
    check(!members.contains("copy") && !members.contains("component1"))
    check(State.Done::class.java.interfaces.map { it.name } == listOf(State::class.java.name))

    println("data object(1.9) OK: toString=${State.Loading} hash=${State.Loading.hashCode()} nameHash=${"Loading".hashCode()} label=$label")
}
