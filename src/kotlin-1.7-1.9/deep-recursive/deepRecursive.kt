package learn.kotlin1719.deep

// DeepRecursiveFunction / DeepRecursiveScope / callRecursive：1.7 起进 stdlib（切片复核：
// awk -F'\t' '$3 ~ /^DeepRecursive/ || $3=="callRecursive"{print $1"|"$2"|"$3}' docs/_data/slices/kotlin.tsv
//   -> 1.7 class DeepRecursiveFunction / 1.7 class DeepRecursiveScope / 1.7 fun callRecursive）
// 原理是把递归搬到堆上的显式栈，所以不吃 JVM 线程栈。

fun plainRec(n: Int): Int = if (n == 0) 0 else 1 + plainRec(n - 1)

val deepCount = DeepRecursiveFunction<Int, Int> { n ->
    if (n == 0) 0 else callRecursive(n - 1) + 1
}

// 嵌套结构也能改写：深嵌套 List 求和
val deepSum = DeepRecursiveFunction<List<Any>, Int> { list ->
    var acc = 0
    for (item in list) {
        acc += if (item is List<*>) callRecursive(item as List<Any>) else item as Int
    }
    acc
}

fun nested(depth: Int): List<Any> {
    var node: List<Any> = listOf(1)
    repeat(depth) { node = listOf(node) }
    return node
}

fun main() {
    // 1) 对照：普通递归 10 万层直接 StackOverflowError（本机实测，加大 -XX:ThreadStackSize 也没救回来）
    val plain = runCatching { plainRec(100_000) }
    check(plain.exceptionOrNull()?.javaClass?.simpleName == "StackOverflowError")

    // 2) DeepRecursiveFunction 同深度正常返回
    check(deepCount(100_000) == 100_000)

    // 3) 一百万层也照样过 —— 这就是它存在的意义
    check(deepCount(1_000_000) == 1_000_000)

    // 4) 递归块里可以用普通局部变量累加，callRecursive 的返回值参与运算
    check(deepSum(nested(50_000)) == 1)

    // 5) 观察：DeepRecursiveScope 上的 callRecursiveWithStack 是同一套机制的另一入口
    val withStack = DeepRecursiveFunction<Int, Int> { n ->
        if (n <= 1) n else callRecursive(n - 1) + callRecursive(n - 2)
    }
    check(withStack(20) == 6765)

    println("deep-recursive(1.7) OK: plain=${plain.exceptionOrNull()?.javaClass?.simpleName} deep100k=${deepCount(100_000)} deep1M=${deepCount(1_000_000)} fib20=${withStack(20)}")
}
