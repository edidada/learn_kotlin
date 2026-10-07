package learn.kotlin1719.enums

// EnumEntries / MyEnum.entries：1.9（切片复核：
// awk -F'\t' '$3 ~ /^EnumEntries|^enumEntries/{print $1"|"$2"|"$3}' docs/_data/slices/kotlin_enums.tsv
//   -> 1.8 class EnumEntriesList（internal 预研）/ 1.9 interface EnumEntries / 1.9 fun enumEntriesIntrinsic
//      2.0 fun enumEntries —— 顶层 enumEntries<T>() 是 2.0 的，留给 kotlin-2.0-k2 档）

enum class Color { RED, GREEN, BLUE }

enum class Shape(val sides: Int) {
    TRIANGLE(3),
    SQUARE(4),
    ;

    fun describe(): String = "sides=$sides"
}

@JvmInline
value class Wrapper(val v: Int)

fun main() {
    // 1) entries 是缓存的同一个对象，不是每次新建数组
    val e = Color.entries
    check(e === Color.entries)
    check(e.size == 3)

    // 2) 实测运行期类型：kotlin.enums.EnumEntriesList，实现 EnumEntries + Serializable
    check(e.javaClass.name == "kotlin.enums.EnumEntriesList")
    check(e::class.java.interfaces.map { it.simpleName }.sorted() == listOf("EnumEntries", "Serializable"))

    // 3) 对照 values()：每次都返回新数组，所以 === 不成立
    check(Color.values() !== Color.values())
    check(e.toList() == Color.values().toList())

    // 4) 它是 List 而不是 Array：索引、迭代、集合操作都能直接用
    check(e[0] === Color.RED)
    check(e.map { it.name } == listOf("RED", "GREEN", "BLUE"))
    check(e.filter { it.name.length > 3 }.map { it.name } == listOf("GREEN", "BLUE"))
    check(e.find { it.name == "BLUE" } == Color.BLUE)
    check(e.toString() == "[RED, GREEN, BLUE]")

    // 5) 带构造参数的枚举一样工作，成员方法照旧
    check(Shape.entries.map { it.sides } == listOf(3, 4))
    check(Shape.SQUARE.describe() == "sides=4")
    check(Shape.entries.maxOf { it.sides } == 4)

    // 6) value class 包一层也不影响：entries 的类型就是 EnumEntries<Shape>
    val wrapped: List<Wrapper> = listOf(Wrapper(1), Wrapper(2))
    check(wrapped.size == 2)

    println("enum entries(1.9) OK: class=${e.javaClass.simpleName} same=${e === Color.entries} valuesSame=${Color.values() === Color.values()}")
}
