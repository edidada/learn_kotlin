package learn.kotlin1112.refs

import kotlin.reflect.KFunction1
import kotlin.reflect.KFunction2
import kotlin.reflect.KProperty0

// 1.1 之前的 :: 只支持顶层函数和少数成员；1.1 补上"绑定引用"（接收者已固定）
// 以及属性引用、构造器引用当函数类型用。
// 语言特性（无 stdlib 声明可查），所以这里的证据是编译器本身：见 README 的实测结论。

class Counter(var count: Int = 0) {
    fun add(n: Int): Int {
        count += n
        return count
    }
}

fun twice(x: Int) = x * 2

fun main() {
    val c = Counter(1)

    // 1) 绑定的函数引用：参数表少掉接收者
    val add: (Int) -> Int = c::add
    check(add(2) == 3 && c.count == 3)

    // 2) 同一成员的非绑定引用：接收者变成第一个参数
    val addUnbound: KFunction2<Counter, Int, Int> = Counter::add
    check(addUnbound(Counter(10), 5) == 15)

    // 3) 绑定的属性引用（1.1）
    val prop: KProperty0<Int> = c::count
    check(prop.get() == 3 && prop.name == "count")

    // 4) 绑定引用也能当 KFunction 用（callableId / parameters 可见元信息）
    val fn: KFunction1<Int, Int> = c::add
    check(fn(1) == 4)

    // 5) 构造器引用当函数类型
    val mk: (Int) -> Counter = ::Counter
    check(mk(7).count == 7)

    // 6) 顶层函数引用喂给高阶函数
    check(listOf(1, 2, 3).map(::twice) == listOf(2, 4, 6))

    // 7) 任意接收者上的绑定属性引用
    val len: KProperty0<Int> = "kotlin"::length
    check(len.get() == 6)

    println("bound callable refs(1.1) OK: ${c.count}")
}
