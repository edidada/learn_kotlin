package learn.kotlin20.copyvis

// data class 的 copy 可见性（2.0）：@ConsistentCopyVisibility / @ExposedCopyVisibility
// 切片复核：awk -F'\t' '$3=="ConsistentCopyVisibility" || $3=="ExposedCopyVisibility"{print $1"|"$6}' docs/_data/slices/kotlin.tsv
//   -> 两个都是 2.0|public annotation class ...
// 规则：构造器私有/受保护时，copy() 默认仍然是 public，于是"私有构造"名不副实。
// 想要一致性就标 @ConsistentCopyVisibility（copy 一起变私有）；想承认暴露就标 @ExposedCopyVisibility。

@ConsistentCopyVisibility
data class Secured private constructor(val id: Int, val label: String) {
    companion object {
        fun of(id: Int, label: String): Secured {
            require(label.isNotBlank())
            return Secured(id, label)
        }
    }
}

@ExposedCopyVisibility
data class Loosely private constructor(val n: Int) {
    companion object {
        fun of(n: Int) = Loosely(n)
    }
}

// 不加注解的对照组：2.0 的默认行为就是"copy 保持 public"
data class Bare private constructor(val v: Int) {
    companion object {
        fun of(v: Int) = Bare(v)
    }
}

private fun copyModifiers(clazz: Class<*>): List<String> =
    clazz.declaredMethods.filter { it.name == "copy" }
        .map { java.lang.reflect.Modifier.toString(it.modifiers) }

fun main() {
    val a = Secured.of(1, "alpha")

    // 1) 工厂 + 值语义照常
    check(a.id == 1 && a.label == "alpha")
    check(a == Secured.of(1, "alpha"))
    check(a.component1() == 1 && a.component2() == "alpha")
    check(a.toString() == "Secured(id=1, label=alpha)")

    // 2) 字节码层面看可见性：标了 @ConsistentCopyVisibility，copy 是 private
    //    （编译期跨文件调用它，报错原话见 README：
    //     "Cannot access 'fun copy(id: Int = ..., label: String = ...): Secured': it is private in ..."）
    val securedCopy = copyModifiers(Secured::class.java)
    check(securedCopy.isNotEmpty())
    check(securedCopy.all { it.contains("private") })

    // 3) 对照：@ExposedCopyVisibility 与不加注解都是 public copy
    val looseCopy = copyModifiers(Loosely::class.java)
    val bareCopy = copyModifiers(Bare::class.java)
    check(looseCopy.isNotEmpty() && looseCopy.none { it.contains("private") })
    check(bareCopy.isNotEmpty() && bareCopy.none { it.contains("private") })
    check(Loosely.of(2).copy(n = 3).n == 3)          // 暴露版：外部照样能 copy
    check(Bare.of(5).copy(v = 6).v == 6)             // 不标注解：行为不变

    // 4) 这两个注解是纯编译期标记：运行期类上只剩 @Metadata，实测（断言见下）
    val securedAnnos = Secured::class.java.annotations.map { it.annotationClass.simpleName }
    val retention = ConsistentCopyVisibility::class.java
        .getAnnotation(java.lang.annotation.Retention::class.java)?.nameOf()
    check(securedAnnos == listOf("Metadata"))
    check(Secured::class.java.declaredAnnotations.map { it.annotationClass.simpleName } == listOf("Metadata"))

    // 5) 私有构造仍然"关得上"：反射能拿到构造器（字节码私有），但 Kotlin 源码层写不出来
    //    注意 declaredConstructors 不止一个：data class 还会生成带 DefaultConstructorMarker 的合成构造器
    val realCtors = Secured::class.java.declaredConstructors.filter { it.parameterCount == 2 }
    val synthetic = Secured::class.java.declaredConstructors.filter { it.parameterCount != 2 }
    check(realCtors.size == 1)
    check(java.lang.reflect.Modifier.isPrivate(realCtors.single().modifiers))
    check(synthetic.isNotEmpty())

    println("copy visibility(2.0) OK: secured=${securedCopy} exposed=${looseCopy} bare=${bareCopy} runtimeAnnos=${securedAnnos} retention=${retention}")
}

private fun java.lang.annotation.Retention.nameOf() = value.toString()
