package learn.kotlin1416.suspendconv

import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine

// suspend 转换（1.4）：suspend 能贴在 lambda 类型、函数类型和函数引用上，
// 于是"高阶函数接收挂起代码"这件事在 stdlib 层就成立（不需要 kotlinx-coroutines）。

suspend fun currentName(): String = "kotlin"

suspend fun upper(): String = currentName().uppercase()

fun main() {
    // 1) suspend lambda 类型
    val block: suspend () -> String = { currentName() }

    // 2) 挂起函数引用原样能用；真正的"suspend 转换"是第 5) 段：把普通函数/lambda 贴到 suspend 类型上
    val ref: suspend () -> String = ::upper
    check(runBlocking(ref) == "KOTLIN")

    // 3) 高阶函数：形参声明成 suspend 函数类型，调用点直接传挂起 lambda
    val out = runBlocking { describe("x") }
    check(out == "describe x")

    // 4) 结果仍然靠 Continuation 落地——这就是 CPS 的入口
    var done = ""
    var resumed = false
    val sink: suspend () -> Unit = { done = ref() }
    sink.startCoroutine(Continuation(EmptyCoroutineContext) { r -> resumed = r.isSuccess })
    check(done == "KOTLIN" && resumed)

    // 5) 方向实测（探针）：转换只能"非挂起 -> 挂起"，反过来不行
    //      val bad: () -> String = ::upper
    //        -> Initializer type mismatch: expected 'kotlin.Function0<kotlin.String>',
    //           actual 'kotlin.reflect.KSuspendFunction0<kotlin.String>'.
    //    另外把 suspend 函数当普通函数调用也会拦：
    //        Suspend function 'suspend fun invoke(): String' should be called only from a coroutine
    //        or another suspend function.
    val lifted: suspend () -> String = ::plainSide
    check(runBlocking(lifted) == "plain")

    println("suspend conversion(1.4) OK: out=$out")
}

fun plainSide(): String = "plain"

suspend fun describe(tag: String): String = "describe $tag"

// 手撸一个最小"运行器"：同步执行到底，因为这段代码不会真的挂起
fun <T> runBlockingLike(block: suspend () -> T): T {
    var result: Any? = null
    block.startCoroutine(Continuation(EmptyCoroutineContext) { r -> result = r.getOrThrow() })
    @Suppress("UNCHECKED_CAST")
    return result as T
}

fun <T> runBlocking(block: suspend () -> T): T = runBlockingLike(block)
