package learn.kotlin20.removerange

// 归属要先纠一句：README 里写的 `MutableList.removeRange(range)` 在本机 stdlib 2.1.10 里**不存在**。
// 复核手段（两条都跑了）：
//   1) javap -p -classpath <kotlin-stdlib-2.1.10> kotlin.collections.CollectionsKt___CollectionsKt   -> 没有任何 removeRange
//   2) 源码里直接写 listOf(1,2,3).mutable().removeRange(0..1) -> 编译器原话：
//      Unresolved reference 'removeRange'.
// docs/_data/slices/kotlin_collections.tsv 里那条 2.0 的 removeRange 是这样一行：
//   2.0|fun|protected open fun removeRange(fromIndex: Int, toIndex: Int): Unit
// 它是 **protected**、参数是两个 Int，不是"集合的范围删除扩展"。javap 显示它声明在
// kotlin.collections.ArrayDeque 上（`protected void removeRange(int,int)`，从 java.util.ArrayList 那条线继承来的形态），
// 而 ArrayDeque 是 final 类，Kotlin 源码里既不能子类化也够不到 protected —— 所以"2.0 有公开 removeRange(range)"这个说法站不住。
// 本机真正可用的范围删除路径见下面四段。

class JdkBacked<T> : ArrayList<T>() {
    fun cut(fromIndex: Int, toIndex: Int) = removeRange(fromIndex, toIndex)   // protected，够得着
}

fun main() {
    // 1) 文本侧的 removeRange 一直是 1.0 的（切片：kotlin_text.tsv 里 since 为空）
    check("abcdef".removeRange(1..3) == "aef")            // 删索引 1、2、3
    check("abcdef".removeRange(1, 4) == "aef")            // 区间版：[1,4)
    check("abcdef".removeRange(0..5) == "")
    val cs = ("abcdef" as CharSequence).removeRange(IntRange(2, 3))
    check(cs.toString() == "abef")                     // 静态类型是 CharSequence，只能按值比
    // 实测运行期类型是 java.lang.StringBuilder，不是 String —— 所以 `cs == "abef"` 直接为 false（踩过）
    check(cs.javaClass.name == "java.lang.StringBuilder")
    check("abcdef".removeRange(2..3) == "abef")        // String 版才返回 String

    // 2) 集合侧 2.1.10 的可用路径之一：subList().clear()
    val bySubList = mutableListOf(1, 2, 3, 4, 5)
    bySubList.subList(1, 4).clear()
    check(bySubList == listOf(1, 5))
    check(runCatching { mutableListOf(1).subList(0, 3).clear() }.exceptionOrNull()?.javaClass?.simpleName == "IndexOutOfBoundsException")

    // 3) 之二：removeAll(集合) / removeAll { 谓词 }
    val byRemoveAll = mutableListOf(1, 2, 3, 4, 5)
    byRemoveAll.removeAll(setOf(2, 3, 4))
    check(byRemoveAll == listOf(1, 5))
    val byPredicate = mutableListOf(1, 2, 3, 4, 5)
    byPredicate.removeAll { it in 2..4 }
    check(byPredicate == listOf(1, 5))
    check(byPredicate.removeAll { it > 100 }.not())        // 没删到就返回 false

    // 4) 之三：不可变侧用减法/切片，不动原表
    val src = listOf(1, 2, 3, 4, 5)
    check((src - setOf(2, 3, 4)) == listOf(1, 5))
    check((src.subList(0, 1) + src.subList(4, 5)) == listOf(1, 5))

    // 5) protected removeRange 的真实存在形态：反射看 ArrayDeque
    val adRemoveRange = ArrayDeque::class.java.declaredMethods
        .filter { it.name == "removeRange" }
        .map { java.lang.reflect.Modifier.toString(it.modifiers) + " " + it.parameterCount }
    check(adRemoveRange.isNotEmpty())
    check(adRemoveRange.all { it.startsWith("protected") })
    check(ArrayDeque::class.java.modifiers.let { java.lang.reflect.Modifier.isFinal(it) })  // final -> 子类化够不到 protected

    // 6) 但 ArrayList 那条线可以：JDK 的 protected removeRange 在 Kotlin 子类里能直接调
    val jdk = JdkBacked<Int>().apply { addAll(listOf(1, 2, 3, 4, 5)) }
    jdk.cut(1, 4)
    check(jdk == listOf(1, 5))

    println("removeRange OK: text=${"abcdef".removeRange(1..3)} csType=${cs.javaClass.name} subList=$bySubList arrayDeque=${adRemoveRange} jdkCut=$jdk")
}
