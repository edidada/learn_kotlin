package learn.kotlin1719.jvmdef

// -jvm-default 的三种模式（1.4 起有开关，2.0 之后 all 是方向；本档只做"怎么判别当前编译进了哪种形态"）
// 判别只看两件事：
//   A. 接口自己声明的方法是不是 JVM default 方法（Modifier.isDefault）
//   B. 有没有 `接口名$DefaultImpls` 这个桥类
// 三种模式在本机 2.1.10 的实测值都记在同目录 README 里。

interface Greeter {
    fun hello(): String = "hello"
    fun greet(): String = "${hello()}, world"

    companion object {
        fun shout(): String = "hey"
    }
}

class Impl : Greeter

fun main() {
    val impl = Impl()
    check(impl.greet() == "hello, world")
    check(impl.hello() == "hello")
    check(Greeter.shout() == "hey")

    val bridge = runCatching { Class.forName("learn.kotlin1719.jvmdef.Greeter\$DefaultImpls") }
    // 注意：java.lang.reflect.Modifier 没有 isDefault()，默认方法要用 Method.isDefault
    val methods: List<Pair<String, Boolean>> = Greeter::class.java.declaredMethods
        .filter { !it.name.contains("$") }
        .map { Pair(it.name, it.isDefault) }
        .sortedBy { it.first }
    val companionBridge = runCatching {
        Class.forName("learn.kotlin1719.jvmdef.Greeter\$Companion\$DefaultImpls")
    }

    // 实现类的形态是"模式指纹"之一。本机 2.1.10 支持的开关取值实测只有三个：
    //   `Unknown -Xjvm-default mode: enable, supported modes: [disable, all-compatibility, all]`
    //   （老教材里的 enable 模式已经没有了，@JvmDefault 注解也早退役）
    // 三种取值下这份代码跑出来的指纹行（bridge / companionBridge / methods / implMethods）逐个记在 README，
    // 所以样本里只断言"行为不变"和"两种形态二选一"，不把某一模式的形状写死。
    val implMethods = Impl::class.java.declaredMethods.map { it.name }.sorted()
    val hasBridges = implMethods.containsAll(listOf("greet", "hello"))
    val ifaceIsDefault = methods.all { it.second }
    check(ifaceIsDefault || bridge.isSuccess)           // 默认实现总得在某个地方：接口 default 方法或 $DefaultImpls 桥类
    check(impl.greet() == "hello, world" && impl.hello() == "hello")

    println("jvm-default OK: bridge=${bridge.isSuccess} companionBridge=${companionBridge.isSuccess} ifaceDefault=$ifaceIsDefault implBridges=$hasBridges methods=$methods")
}
