package learn.kotlin13.batch

import kotlin.random.Random

// 1.3 stdlib 其余新增里最值得写的几族（实测条数：kotlin.collections 132、kotlin 37、
// kotlin.coroutines* 43、kotlin.contracts 10、kotlin.text 8、kotlin.sequences 7、
// kotlin.random 7、kotlin.time 5、kotlin.ranges 4）。
// 复核：awk -F'\t' '$1=="1.3" && $3 ~ /^(ifBlank|ifEmpty|associateWith|associateWithTo|copyInto|singleOrNull|random)$/{print $2"|"$3"|"$4}' docs/_data/slices/kotlin_text.tsv docs/_data/slices/kotlin_collections.tsv | sort -u

fun main() {
    // 1) ifBlank / ifEmpty：把"空值兜底"写成表达式
    check("   ".ifBlank { "blank" } == "blank")
    check("x".ifBlank { "blank" } == "x")
    check("".ifEmpty { "empty" } == "empty")
    val emptyList: List<Int> = emptyList()
    check(emptyList.ifEmpty { listOf(1, 2) } == listOf(1, 2))

    // 2) associateWith：只给 value 选择器，key 就是元素本身
    check(listOf("a", "bb").associateWith { it.length } == mapOf("a" to 1, "bb" to 2))
    val dest = linkedMapOf<String, Int>()
    listOf("ccc").associateWithTo(dest) { it.length }
    check(dest == mapOf("ccc" to 3))

    // 3) random 族：可注入 Random，因此可复现
    val pick = listOf("a", "b", "c").random(Random(42))
    check(listOf("a", "b", "c").random(Random(42)) == pick)
    check((1..6).random(Random(1)) in 1..6)
    check("abcd".random(Random(0)) in setOf('a', 'b', 'c', 'd'))
    check(intArrayOf(7, 8).random() == 7 || intArrayOf(7, 8).random() in setOf(7, 8))

    // 4) copyInto：数组区间原地拷贝，重叠区间也按语义工作
    val xs = intArrayOf(1, 2, 3, 4, 5)
    xs.copyInto(xs, 1, 0, 4)
    check(xs.toList() == listOf(1, 1, 2, 3, 4))
    val src = intArrayOf(9, 9)
    val dst = IntArray(3)
    src.copyInto(dst, destinationOffset = 1)
    check(dst.toList() == listOf(0, 9, 9))

    // 5) singleOrNull：把"至多一个"变成可判空的查询
    check(emptyList<Int>().singleOrNull() == null)
    check(listOf(5, 6).singleOrNull { it > 10 } == null)
    check(listOf(5, 6).singleOrNull { it > 5 } == 6)

    // 6) 注意：sortedWith 在切片里是无版本戳的（1.0 就有），别当 1.3 新增学
    check(listOf("bbb", "a").sortedWith(compareBy { it.length }) == listOf("a", "bbb"))

    println("stdlib batch(1.3) OK: pick=$pick xs=${xs.toList()}")
}
