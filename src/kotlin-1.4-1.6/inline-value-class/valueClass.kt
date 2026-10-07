package learn.kotlin1416.value

// inline/value class：1.5 先叫 inline class，同版本内改名 value class 并要求 @JvmInline，
// 1.9 把 value 关键字转正。实测本机 2.1.10：@JvmInline 仍然必需，
// 只写 `value class NoAnno(val v: Int)` 报 Value classes without '@JvmInline' annotation are not yet supported.
// 这里量的是"擦除与装箱"的真实表现。

@JvmInline
value class Password(val value: String) {
    val length: Int get() = value.length
    fun masked(): String = "*".repeat(value.length)
}

@JvmInline
value class UserId(val raw: Long)

// 探针转正的样本：底层类型可空、有 init、有 companion、实现接口 —— 实测全部编译通过
@JvmInline
value class Tagged(val value: String?) : Comparable<Tagged> {
    init {
        require(value == null || value.length <= 8)
    }

    fun size(): Int = value?.length ?: 0

    companion object {
        val NONE = Tagged(null)
    }

    override fun compareTo(other: Tagged): Int = size().compareTo(other.size())
}

fun take(p: Password): String = p.masked()

fun eraseIt(p: Password): Any = p        // 赋给 Any 就装箱

fun main() {
    val p = Password("kotlin")
    check(take(p) == "******")
    check(p.length == 6)

    // 1) 观察：装箱后才有身份，== 走 equals，=== 只在未装箱时可靠
    val boxed1: Any = Password("abc")
    val boxed2: Any = Password("abc")
    check(boxed1 == boxed2)
    check(boxed1 !== boxed2)
    check(boxed1.javaClass.name.contains("Password"))

    // 2) 擦除：成员函数在字节码里变成 **static 的 `名字-impl`**，参数是底层类型 String 而不是 Password
    //    实测方法表：masked-impl(String) / getLength-impl(String) / constructor-impl(String)
    //                / box-impl(String)->Password / unbox-impl()->String / equals-impl / hashCode-impl
    //    所以别找 "masked"，它不存在；对外可见的实例方法只剩 getValue/equals/hashCode/toString。
    val m = Password::class.java.declaredMethods.map { it.name to (it.parameterTypes.firstOrNull()?.name ?: "<no-arg>") }
    check(m.any { it.first == "masked-impl" && it.second == "java.lang.String" })
    check(m.any { it.first == "getLength-impl" && it.second == "java.lang.String" })
    check(m.any { it.first == "box-impl" })
    check(!m.any { it.first == "masked" })
    val staticImpl = Password::class.java.getDeclaredMethod("masked-impl", String::class.java)
    check(java.lang.reflect.Modifier.isStatic(staticImpl.modifiers))

    // 3) 边界实测（探针转正）：底层类型可空、可以 init、可以 companion、可以实现接口
    check(Tagged(null).value == null)
    check(Tagged.NONE.size() == 0 && Tagged("abcd").size() == 4)
    check(Tagged.NONE < Tagged("a"))
    check(UserId(7L).raw == 7L)
    check(UserId(7L) == UserId(7L))
    //    反过来这两条编译不过（探针原文照抄）：
    //      var bad: Int = 1
    //        -> Value class cannot have properties with backing fields.
    //      value class Two(val a: Int, val b: Int)
    //        -> Inline class must have exactly one primary constructor parameter.
    //    注意消息里还留着 1.5 之前的老名字 "inline class"，措辞没跟着改名。

    // 4) 装箱后才谈身份：Any 上的 is 判定可用；擦除后的值本身是原生类型
    val q: Any = UserId(1L)
    check(q is UserId)
    val boxedAgain = eraseIt(p)
    check(boxedAgain == p && boxedAgain.javaClass.name.endsWith("Password"))

    println("value class(1.5) OK: ${Password::class.java.name} methods=$m")
}
