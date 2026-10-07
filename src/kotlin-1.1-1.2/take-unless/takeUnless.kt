package learn.kotlin1112.takeunless

// takeUnless：@SinceKotlin("1.1")，声明位置 commonMain/kotlin/util/Standard.kt:136
// 语义与 takeIf 正好相反：predicate 为 true 时返回 null，否则返回自身。
// 复核：awk -F'\t' '$1=="1.1" && $3=="takeUnless"' docs/_data/slices/kotlin.tsv

fun main() {
    // 1) 最小语义
    val five = 5
    check(five.takeUnless { it % 2 == 0 } == 5)
    check(five.takeUnless { it % 2 == 1 } == null)

    // 2) 真实场景：把"坏值"折叠成 null，再配 ?: 兜底
    fun sanitize(input: String?): String? = input?.trim()?.takeUnless { it.isEmpty() }
    check(sanitize("   ") == null)
    check(sanitize(" hi ") == "hi")
    check(sanitize(null) == null)

    // 3) 与 takeIf 对偶：同一谓词下恰好一个非 null
    val n = 42
    val ifEven = n.takeIf { it % 2 == 0 }
    val unlessEven = n.takeUnless { it % 2 == 0 }
    check(ifEven == 42 && unlessEven == null)

    // 4) 结果一旦是 null，后续 ?. 链不执行（这是 takeUnless 好读的原因）
    var ran = 0
    val r = "x".takeUnless { true }?.let { ran++; it.uppercase() }
    check(r == null && ran == 0)

    println("takeUnless(1.1) OK: unlessEven=$unlessEven")
}
