package learn.kotlin10.nulls

// Kotlin 1.0：空安全。归属 [文] 1.0 —— 语法层，stdlib 里找不到版本戳。
// 跑：./gradlew run -PmainClass=learn.kotlin10.nulls.NullSafetyKt

class Holder {
    lateinit var name: String            // 1.0 就有：非空但延迟赋值
}

fun main() {
    // 1) ?: 的右侧是惰性的：左边非空时右侧表达式根本不求值
    var elseBranchRan = 0
    val a: String? = "x"
    val b: String? = null
    val r1 = a ?: run { elseBranchRan++; "fallback" }
    val r2 = b ?: run { elseBranchRan++; "fallback" }
    check(r1 == "x" && r2 == "fallback")
    println("?: 惰性求值 -> elseBranchRan = $elseBranchRan （只有 b 为空时才+1）")

    // 2) ?. 链：任何一环是 null，整条链直接返回 null
    val maybe: String? = "  Hello  "
    val len = maybe?.trim()?.length
    println("?. 链 -> ${maybe?.trim()} 的长度 = $len")
    println("?. 链断掉 -> ${null?.trim()?.length}")

    // 3) !! 抛什么异常：版本敏感。1.0 抛 KotlinNullPointerException；本机 2.1.10 实测抛的是
    //    普通 NullPointerException 且 message 为 null（下面那行会把它打出来，别背结论，跑一遍）
    val n: String? = null
    val t = runCatching { n!! }.exceptionOrNull() ?: error("!! 必须抛异常")
    println("!! 抛出 -> ${t::class.simpleName}: ${t.message}")

    // 4) lateinit 未初始化就读
    val h = Holder()
    val late = runCatching { h.name }.exceptionOrNull()!!
    println("lateinit 未赋值 -> ${late::class.simpleName}: ${late.message}")

    // 5) 集合里的可空元素：filterNotNull / requireNotNull
    val mixed = listOf("a", null, "bb", null, "ccc")
    println("filterNotNull -> ${mixed.filterNotNull()}")
    println("requireNotNull 保留非空值 -> ${mixed.mapNotNull { it?.uppercase() }}")
}
