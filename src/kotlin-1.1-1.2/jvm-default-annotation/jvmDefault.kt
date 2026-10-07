package learn.kotlin1112.jvmdefault

// JvmDefault：切片里 since=1.2、kind=class。它当年的用途是让 Kotlin 接口的默认实现
// 编译成 Java 8 的 default method，而不是走 DefaultImpls 桥接类。
// 注意：语言层面的"接口默认实现"1.0 就有，1.2 管的只是 JVM 字节码怎么编码。
// 复核：awk -F'\t' '$3=="JvmDefault"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv

interface Plain {
    fun hello(): String = "hello"
    fun greet(): String = "${hello()}, world"
}

class Use : Plain

fun main() {
    val u = Use()
    check(u.hello() == "hello")
    check(u.greet() == "hello, world")

    // 接口里方法声明本身是 abstract（默认实现放在别处），这是 DefaultImpls 模式的前提
    val declared = Plain::class.java.declaredMethods.map { it.name }.toSet()
    check(declared == setOf("hello", "greet"))

    // 实测：默认编译模式下存在 Plain$DefaultImpls 桥接类
    val bridge = runCatching { Class.forName("learn.kotlin1112.jvmdefault.Plain\$DefaultImpls") }
    println("DefaultImpls present = ${bridge.isSuccess}; methods = $declared")
}
