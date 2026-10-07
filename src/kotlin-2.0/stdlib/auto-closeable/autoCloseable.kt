package learn.kotlin20.autoclose

import kotlin.AutoCloseable

// AutoCloseable 进 common（expect）+ 通用 use 扩展，都是 2.0（切片复核：
// awk -F'\t' '$3=="AutoCloseable" || $3=="use"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
//   -> 2.0 expect interface AutoCloseable / 2.0 expect inline fun AutoCloseable(crossinline closeAction) / 2.0 expect inline fun <T:AutoCloseable?,R> T.use
//      而 JVM 侧 actual 是 2.0|typealias AutoCloseable = java.lang.AutoCloseable，
//      且 <T : Closeable?, R> T.use 这条老扩展早在 1.2 就有 —— 2.0 补的是"跨平台那一份"。）

class Recorder {
    val log = mutableListOf<String>()
}

val rec = Recorder()

fun closing(tag: String, log: Recorder = rec): AutoCloseable =
    AutoCloseable { log.log += "close:$tag" }

class Boom : AutoCloseable {
    override fun close() {
        rec.log += "close:boom"
        throw IllegalStateException("close failed")
    }
}

fun main() {
    // 1) use 正常路径：先块后关，返回值就是块的返回值
    rec.log.clear()
    val r = closing("a").use { x -> rec.log += "body:a"; "returned" }
    check(r == "returned")
    check(rec.log == listOf("body:a", "close:a"))

    // 2) 可空接收者：null 时既不抛也不关，块照样执行
    rec.log.clear()
    val none: AutoCloseable? = null
    val rn = none.use { rec.log += "body:null"; 7 }
    check(rn == 7 && rec.log == listOf("body:null"))

    // 3) 块抛异常：close 仍然执行，异常原样往上抛（runCatching 抓到的是块里的异常）
    rec.log.clear()
    val bodyFail = runCatching<Int> { closing("b").use { throw IllegalArgumentException("body") } }
    check(bodyFail.exceptionOrNull()?.javaClass?.simpleName == "IllegalArgumentException")
    check(rec.log == listOf("close:b"))

    // 4) 块和 close 都抛：主异常是块里的，close 的异常挂成 suppressed —— 这是 use 的语义核心
    rec.log.clear()
    val both = runCatching<Int> { Boom().use { throw IllegalArgumentException("body") } }
    val primary = both.exceptionOrNull()!!
    check(primary.javaClass.simpleName == "IllegalArgumentException")
    check(primary.suppressed.map { it.message } == listOf("close failed"))
    check(rec.log == listOf("close:boom"))

    // 5) 只有 close 抛：异常直接来自 close
    rec.log.clear()
    val onlyClose = runCatching { Boom().use { "ok" } }
    check(onlyClose.exceptionOrNull()?.javaClass?.simpleName == "IllegalStateException")
    check(onlyClose.exceptionOrNull()?.suppressed?.isEmpty() == true)

    // 6) JVM 侧它就是 java.lang.AutoCloseable 的别名，所以和 JDK 资源可以混用
    check(AutoCloseable::class.java.name == "java.lang.AutoCloseable")
    check(AutoCloseable::class.java.methods.map { it.name }.contains("close"))

    // 7) 嵌套 use：关闭顺序是后开先关
    rec.log.clear()
    closing("outer").use { rec.log += "o"; closing("inner").use { rec.log += "i" } }
    check(rec.log == listOf("o", "i", "close:inner", "close:outer"))

    println("AutoCloseable/use(2.0) OK: jvm=${AutoCloseable::class.java.name} order=${rec.log} suppressed=${primary.suppressed.size}")
}
