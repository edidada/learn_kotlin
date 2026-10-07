package learn.kotlin1416.record

// @JvmRecord（1.5，kotlin.jvm 包）：让 data class 直接编成 JDK record（需 JDK 14+，本机 17）。
// 复核：awk -F'\t' '$3=="JvmRecord"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_jvm.tsv

@JvmRecord
data class PointRec(val x: Int, val y: Int)

// 对照组：同样的 data class，不加注解
data class PointPlain(val x: Int, val y: Int)

fun main() {
    val p = PointRec(1, 2)
    check(p.x == 1 && p.y == 2)
    check(p == PointRec(1, 2))
    check(p.copy(x = 5) == PointRec(5, 2))
    check(p.toString() == "PointRec(x=1, y=2)")   // record 的 toString 形状与 data class 一致

    // 实测判别：JVM 侧是不是真的 record
    check(PointRec::class.java.isRecord)
    check(!PointPlain::class.java.isRecord)
    check(PointRec::class.java.superclass.name == "java.lang.Record")
    check(PointPlain::class.java.superclass.name == "java.lang.Object")

    // record 的字段是 final 的，访问器名字就是属性名（没有 getter 前缀）
    val accessorNames = PointRec::class.java.methods.map { it.name }.toSet()
    check(accessorNames.contains("x") && !accessorNames.contains("getX"))

    // 实测 JVM 形状（反射）：recordComponents 可读、只有 1 个规范化构造器、类是 final、不额外实现接口；
    // 但 Kotlin 侧的 data class 成员还在 —— component1/component2/copy/copy$default 都生成了，
    // 也就是说 @JvmRecord 换的是"字节码身份"，不牺牲 data class 的语法糖。
    val comps = PointRec::class.java.recordComponents.map { "${it.name}:${it.type.simpleName}" }
    check(comps == listOf("x:int", "y:int"))
    check(PointRec::class.java.declaredConstructors.size == 1)
    check(java.lang.reflect.Modifier.isFinal(PointRec::class.java.modifiers))
    check(PointRec::class.java.interfaces.isEmpty())
    val declared = PointRec::class.java.declaredMethods.map { it.name }.toSet()
    check(declared.containsAll(setOf("component1", "component2", "copy", "equals", "hashCode", "toString", "x", "y")))

    println("JvmRecord(1.5) OK: rec=$p plain=${PointPlain(1, 2)}")
}
