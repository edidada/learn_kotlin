package learn.kotlin1416.sam

import java.util.concurrent.Callable

// SAM 转换规则（1.4 之后 Kotlin 侧的接口也要显式 fun interface 才参与）：
// - Java 接口/函数式类型：一直可以
// - Kotlin 接口：1.4 起必须 fun interface
// - 生成形态由 -Xsam-conversions 控制：class（生成匿名内部类）/ indy（invokedynamic）
//   实测本机 2.1.10 默认走 indy：Runnable 的 javaClass 是 `...Kt$$Lambda$9/0x...`；
//   加 `-Xsam-conversions=class` 后同一行变成 `...Kt$main$probe$1`（匿名内部类）。
fun interface Action {
    fun run(message: String)
}

fun main() {
    // 1) Java 函数式接口：实参位置可以直接给 lambda（SAM 转换）
    //    但"声明/赋值位置"不行，探针原文：
    //      val asAssignment: Runnable = { println("hi") }
    //        -> Initializer type mismatch: expected 'java.lang.Runnable', actual 'kotlin.Function0<kotlin.Unit>'.
    //      val callableAssign: Callable<Int> = { 1 }
    //        -> Initializer type mismatch: expected 'java.util.concurrent.Callable<kotlin.Int>', actual 'kotlin.Function0<kotlin.Int>'.
    //    所以声明处必须写 SAM 构造器 Runnable { } / Callable { 42 }。
    runLaterRunnable { check(true) }
    val call = Callable { 42 }                          // 声明位置要用 SAM 构造器
    check(call.call() == 42)
    check(listOf("bb", "a").sortedWith { x, y -> x.length.compareTo(y.length) } == listOf("a", "bb"))

    // 2) Kotlin fun interface
    val log = StringBuilder()
    val action = Action { log.append(it) }
    action.run("kotlin")
    check(log.toString() == "kotlin")

    // 3) 形参位置上的隐式转换：函数类型 -> Java SAM
    check(runLater { true })

    // 4) 观察生成形态（README 里配 javap 对照）
    val probe = Runnable { }
    println("sam OK: Runnable impl = ${probe.javaClass.name}")
}

fun runLaterRunnable(block: () -> Unit) {
    Runnable(block).run()
}

fun runLater(block: () -> Boolean): Boolean {
    val callable = Callable { block() }
    return callable.call()
}
