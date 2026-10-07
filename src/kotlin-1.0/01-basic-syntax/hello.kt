package learn.kotlin10.basic

// Kotlin 1.0 底座：val/var、类型推断、字符串模板、智能转换。
// 归属：[文] 1.0 —— 这些语法在 stdlib 里没有 @SinceKotlin 戳（1.0 的 API 不打戳）。
fun main() {
    val name = "Kotlin"                 // 推断为 String
    val length: Int = name.length
    val nullable: String? = null
    val fallback = nullable ?: "default"

    println("$name 长度 $length，回退值 $fallback")

    // 智能转换：is 检查之后不需要再强转
    val any: Any = 42
    if (any is Int && any > 0) {
        println("any 在这里已是 Int，可直接用数值运算：${any + 1}")
    }
}
