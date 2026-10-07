package learn.kotlin1112.grouping

// 1.1 引入"分组收集器"家族：interface Grouping + groupingBy + fold/foldTo/
// aggregate/aggregateTo/eachCount/eachCountTo（切片里 since 全部是 1.1）。
// 复核：awk -F'\t' '$1=="1.1" && ($3=="groupingBy"||$3=="eachCount"||$3=="aggregate"){print $3"|"$2}' docs/_data/slices/kotlin_collections.tsv | sort -u

fun main() {
    val words = listOf("ki", "vi", "py", "th", "on", "lang", "kot")

    // 1) eachCount：分组计数，一次遍历
    val counts: Map<Int, Int> = words.groupingBy { it.length }.eachCount()
    check(counts == mapOf(2 to 5, 4 to 1, 3 to 1))

    // 2) fold：每组各自从一个初始值累加
    val byLen = words.groupingBy { it.length }
    val folded: Map<Int, Int> = byLen.fold(0) { acc, w -> acc + w.length }
    check(folded.getValue(2) == 10)

    // 3) aggregate：首元素时 accumulator 是 null，用 first 标志区分
    val longest: Map<Int, String> = byLen.aggregate { _, acc: String?, w, first ->
        when {
            first -> w
            w > acc!! -> w
            else -> acc!!
        }
    }
    check(longest.getValue(2) == "vi")
    check(longest.getValue(4) == "lang")

    // 4) *To 家族：结果写进调用方给的可变 Map，避免新建容器
    val dest = mutableMapOf<Int, Int>()
    val same = byLen.eachCountTo(dest)
    check(same === dest && dest.getValue(2) == 5)

    val foldDest = mutableMapOf<Int, Int>()
    byLen.foldTo(foldDest, 0) { acc, _ -> acc + 1 }
    check(foldDest == counts)

    // 5) 与 groupBy（1.0）对比：groupBy 先建每组一个 List，再要你手动收集
    val grouped: Map<Int, List<String>> = words.groupBy { it.length }
    check(grouped.keys == counts.keys)
    check(grouped.getValue(2).size == counts.getValue(2))

    println("grouping collectors(1.1) OK: $counts")
}
