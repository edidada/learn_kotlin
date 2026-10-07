package learn.kotlin1112.aliases

// type alias 是 1.1 的语言特性；同一版本还给 JVM 平台类型提供了内置别名
// （ArrayList / HashMap / HashSet / Comparator / Random / AssertionError 等，切片里 since=1.1、kind=typealias）。
// 复核：awk -F'\t' '$1=="1.1" && $2=="typealias"{print $3}' docs/_data/slices/kotlin.tsv | sort -u

typealias UserHandler = (String) -> Unit
typealias Registry = MutableMap<String, MutableList<UserHandler>>
typealias IntList = ArrayList<Int>   // -> java.util.ArrayList<Int>

fun main() {
    // 1) 别名不创建新类型，只是可读性
    val r: Registry = mutableMapOf()
    r.getOrPut("a") { mutableListOf() }.add { name -> check(name == "kotlin") }
    val h: UserHandler = r.getValue("a").first()
    h("kotlin")

    // 2) 别名与展开形式双向可赋值（结构等价）
    val asMap: MutableMap<String, MutableList<(String) -> Unit>> = r
    check(asMap.size == 1)

    // 3) 1.1 的内置 JVM 别名：Kotlin 侧的 ArrayList 就是 java.util.ArrayList
    val l: IntList = arrayListOf(1, 2)
    val javaList: java.util.ArrayList<Int> = l
    check(javaList === l)

    // 4) Comparator 也是别名，所以它能直接喂给 Java API
    val cmp: Comparator<Int> = Comparator { a, b -> a - b }
    check(listOf(3, 1, 2).sortedWith(cmp) == listOf(1, 2, 3))

    // 5) 别名可以层层套，签名里仍然按展开后的类型检查
    val f: UserHandler = {}
    val g: (String) -> Unit = f
    g("x")

    println("typealias(1.1) OK: $javaList")
}
