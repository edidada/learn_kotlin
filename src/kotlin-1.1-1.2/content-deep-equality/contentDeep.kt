package learn.kotlin1112.content

// 1.1 新增：contentDeepEquals / contentDeepHashCode / contentDeepToString（切片 since=1.1）
// 复核：awk -F'\t' '$1=="1.1" && $3 ~ /^contentDeep/{print $3}' docs/_data/slices/kotlin_collections.tsv | sort -u

fun main() {
    // 1) 陷阱：二维数组内容相同，浅比较仍然 false（内层数组按引用比）
    val a = Array(2) { i -> Array(2) { j -> i * 10 + j } }
    val b = Array(2) { i -> Array(2) { j -> i * 10 + j } }
    check(!(a contentEquals b))
    check(a contentDeepEquals b)
    check(a != b)

    // 2) 深哈希：内容相同的嵌套数组哈希一致，可以安全放进 Set/Map 的键
    check(a.contentDeepHashCode() == b.contentDeepHashCode())

    // 3) 深字符串就是 java.util.Arrays.deepToString 的形状
    check(a.contentDeepToString() == "[[0, 1], [10, 11]]")

    // 4) 三维也行
    val c = Array(1) { arrayOf(arrayOf("x", "y")) }
    val d = Array(1) { arrayOf(arrayOf("x", "y")) }
    check(c contentDeepEquals d)

    // 5) 内层是基本类型数组（IntArray）时，深比较会退到 contentEquals
    val e = arrayOf(intArrayOf(1, 2), intArrayOf(3))
    val f = arrayOf(intArrayOf(1, 2), intArrayOf(3))
    check(e contentDeepEquals f)
    check(e.contentDeepToString() == "[[1, 2], [3]]")

    // 6) null 元素不炸
    val g: Array<IntArray?> = arrayOf(intArrayOf(1), null)
    val h: Array<IntArray?> = arrayOf(intArrayOf(1), null)
    check(g contentDeepEquals h)

    // 7) 深比较只比内容，不比容器类型：listOf 与 ArrayList 混不了，但 Array<Any> 里混 List 可以
    val mixed = arrayOf(listOf(1, 2), "s")
    val mixed2 = arrayOf(listOf(1, 2), "s")
    check(mixed contentDeepEquals mixed2)

    println("contentDeep(1.1) OK: ${a.contentDeepToString()}")
}
