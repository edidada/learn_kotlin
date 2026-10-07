package learn.kotlin1112.takeif

// Kotlin 1.1：takeIf。归属 [实] 1.1 —— stdlib 里 `takeIf` 带 @SinceKotlin("1.1")。
// 复核：awk -F'\t' '$3=="takeIf"{print $1"|"$6}' docs/_data/slices/kotlin.tsv
// 跑：./gradlew run -PmainClass=learn.kotlin1112.takeif.TakeIfKt

fun main() {
    // 1) takeIf 的返回类型永远是 T?：条件不成立给 null，不成立时对象本身不产生新值
    val text = "hello kotlin"
    val short: String? = text.takeIf { it.length < 5 }
    val longish: String? = text.takeIf { it.length > 5 }
    println("takeIf 不成立 -> $short，成立 -> $longish")

    // 2) 与 if 表达式的等价写法：takeIf 让"判断主体"和"过滤条件"分开，链式更顺
    val dir = System.getProperty("user.dir")
    val project = dir.takeIf { it.contains("learn_kotlin") }
    println("路径含 learn_kotlin？ -> $project")

    // 3) takeIf + let 的组合是 1.1 之后最常见的空过滤管道
    val parsed = "42".toIntOrNull()?.takeIf { it in 1..100 }?.let { "区间内：$it" } ?: "不接受"
    println("\"42\" -> $parsed")
    val rejected = "500".toIntOrNull()?.takeIf { it in 1..100 }?.let { "区间内：$it" } ?: "不接受"
    println("\"500\" -> $rejected")

    // 4) takeIf 作用在可空接收者上：this 为 null 时 lambda 根本不执行（计数验证）
    var calls = 0
    val nothing: String? = null
    val r = nothing?.takeIf { calls++; it.isNotEmpty() }
    println("null?.takeIf -> $r，lambda 执行次数 = $calls")
    check(calls == 0)
}
