package learn.kotlin13.coroutinesinfra

import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine
import kotlin.coroutines.suspendCoroutine
import java.util.concurrent.CountDownLatch

// 1.3 进 stdlib 的是协程基础设施：kotlin.coroutines / kotlin.coroutines.jvm.internal
// 整批戳 1.3（实测 43 条）。launch / async / withContext 从来不在 stdlib，属 kotlinx-coroutines。

suspend fun fetchName(): String = suspendCoroutine { cont ->
    Thread {
        Thread.sleep(20)
        cont.resume("kotlin")
    }.start()
}

fun main() {
    val latch = CountDownLatch(1)
    var result = ""
    var tailOk = false

    val body: suspend () -> Unit = { result = fetchName() }

    // 手撸终态 Continuation：stdlib 只给接口，不给线程调度。
    // 实测坑：latch 必须在终态 resumeWith 里 countDown。放在 suspend 块末尾会先于 resumeWith 发生，
    // 主线程醒来时 tailOk 还是 false——第一次跑就是挂在 check(tailOk) 上的。
    body.startCoroutine(object : Continuation<Unit> {
        override val context: CoroutineContext = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            tailOk = result.isSuccess
            latch.countDown()
        }
    })

    latch.await()
    check(result == "kotlin")
    check(tailOk)

    // 直接用一个一次性 Continuation 消费结果。
    // 实测：`Continuation<Int> { ... }` 报 No value passed for parameter 'context'，
    // 说明它命中的是 stdlib 的工厂函数 Continuation(context, resumeWith)，不是 SAM 构造。
    var seen = -1
    val oneShot = Continuation<Int>(EmptyCoroutineContext) { r -> seen = r.getOrDefault(-1) }
    oneShot.resume(7)
    check(seen == 7)

    // CoroutineContext 自身就是一个不可变小 Map（1.3）：+ 拼装、get(Key) 取值。
    // 实测：CoroutineName / ContinuationInterceptor 的实现都不在 stdlib 的可用形态里，
    // stdlib 只给接口和 EmptyCoroutineContext，调度与命名属 kotlinx-coroutines。
    val merged: CoroutineContext = EmptyCoroutineContext + EmptyCoroutineContext
    check(merged === EmptyCoroutineContext)
    check(kotlin.coroutines.ContinuationInterceptor::class.qualifiedName == "kotlin.coroutines.ContinuationInterceptor")

    println("coroutines infra(1.3) OK: result=$result")
}
