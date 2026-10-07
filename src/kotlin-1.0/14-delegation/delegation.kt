package learn.kotlin10.deleg

import kotlin.properties.Delegates

// Kotlin 1.0：属性委托（by lazy / observable / vetoable / 自定义 Delegate）。归属 [文] 1.0。
// 跑：./gradlew run -PmainClass=learn.kotlin10.deleg.DelegationKt

class Expensive(val label: String) {
    init { println("  [$label] 真的被构造了") }
    override fun toString() = "[$label]"
}

// 自定义委托：实现 getValue / setValue 就行，不需要实现任何接口
class TraceVar(private var back: Int = 0) {
    operator fun getValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>): Int {
        println("  读 ${property.name} -> $back")
        return back
    }
    operator fun setValue(thisRef: Any?, property: kotlin.reflect.KProperty<*>, value: Int) {
        println("  写 ${property.name}：$back -> $value")
        back = value * 2          // 委托可以偷偷改值
    }
}

class Model {
    var events: Int by Delegates.observable(0) { prop, old, new ->
        println("  observable: ${prop.name} $old -> $new")     // 先赋值再回调
    }
    var positive: Int by Delegates.vetoable(1) { _, _, new ->
        println("  vetoable 收到 $new，接受？${new > 0}")
        new > 0                                                // 返回 false 就拒绝这次写入
    }
    var traced: Int by TraceVar()
    val lazyValue: Expensive by lazy { Expensive("只在第一次读时创建") }
}

fun main() {
    val m = Model()

    // 1) lazy：默认线程安全（LazyThreadSafetyMode.SYNCHRONIZED），第一次读才初始化，之后不再进 lambda
    println("1) 还没读 lazyValue")
    println("   第一次读 -> ${m.lazyValue}")
    println("   第二次读 -> ${m.lazyValue}")        // 初始化日志只出现一次
    check(m.lazyValue === m.lazyValue)              // 同一个实例

    // 2) lazy 的模式差异：NONE 最快但不保证，VALUES 允许跑两次 lambda 但只发布一个值
    var creations = 0
    val lazyNone by lazy(LazyThreadSafetyMode.NONE) { ++creations; "none" }
    val first = lazyNone
    val second = lazyNone
    println("2) NONE 模式：$first / $second，lambda 实际执行次数=$creations")

    // 3) observable：写入生效在前，回调在后
    println("3) events 当前=${m.events}，写入 5")
    m.events = 5
    println("   写完再读 -> ${m.events}")

    // 4) vetoable：拒绝时值保持不变
    println("4) positive 当前=${m.positive}，写入 -3（应被拒）")
    m.positive = -3
    println("   被拒之后 -> ${m.positive}")
    check(m.positive == 1)
    m.positive = 7
    println("   接受 7 之后 -> ${m.positive}")

    // 5) 自定义委托：setValue 里做变换，读到的和写进去的不是一个值
    m.traced = 21
    println("5) traced 写入 21，读回 -> ${m.traced}")
    check(m.traced == 42)

    // 6) 委托对象可以被复用（provideDelegate / map 委托），这里用标准库的 map 委托收个尾
    val map = mutableMapOf<String, Any?>()
    class FromMap(val map: MutableMap<String, Any?>) {
        var name: String? by map
        var age: Int? by map
    }
    val p = FromMap(map)
    p.name = "ada"; p.age = 36
    println("6) map 委托：底层 map = $map")
}
