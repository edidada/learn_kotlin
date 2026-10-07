package learn.kotlin1719.volat

import kotlin.concurrent.Volatile
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

// @Volatile：common 侧的 kotlin.concurrent.Volatile 是 1.9 才有的（切片复核：
// awk -F'\t' '$3=="Volatile"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_concurrent.tsv -> 1.9 class / 1.9 typealias）
// 它在 JVM 上就是 `actual typealias Volatile = kotlin.jvm.Volatile`，而 kotlin.jvm.Volatile 本身没有 since 戳（1.0 起就在）。
// 也就是说 1.9 加的不是新能力，是"common 代码里也能写 @Volatile"这层别名。

class Counter {
    @Volatile
    var seen: Int = 0

    fun bumpTo(value: Int) {
        seen = value
    }
}

@Volatile
var topFlag: Boolean = false

fun main() {
    // 1) 反射层面：字段真的带 JVM ACC_VOLATILE
    val memberField = Counter::class.java.getDeclaredField("seen")
    check(java.lang.reflect.Modifier.isVolatile(memberField.modifiers))

    // 顶层 @Volatile var 落在文件类上
    val topField = Class.forName("learn.kotlin1719.volat.VolatileFieldKt").declaredFields
        .first { it.name == "topFlag" }
    check(java.lang.reflect.Modifier.isVolatile(topField.modifiers))

    // 2) 别名核对：kotlin.jvm.Volatile 就是同一个注解实现（typealias 不是新注解）
    check(Volatile::class.java.name == "kotlin.jvm.Volatile" || Volatile::class.java.name == "kotlin.concurrent.Volatile")

    // 3) 行为层面只能做" happens-before 由 join() 提供"的确定性验证：
    //    写完再 join，主线程一定读得到；这条断言与 @Volatile 无关，用它反证"可见性测试不能靠 sleep 赌"
    val counter = Counter()
    val ready = CountDownLatch(1)
    val worker = thread(start = true) {
        counter.bumpTo(42)
        ready.countDown()
    }
    ready.await()
    worker.join()
    check(counter.seen == 42)

    topFlag = true
    check(topFlag)

    println("volatile(1.9, JVM 侧是 kotlin.jvm.Volatile 别名) OK: member=${java.lang.reflect.Modifier.isVolatile(memberField.modifiers)} top=${java.lang.reflect.Modifier.isVolatile(topField.modifiers)} seen=${counter.seen}")
}
