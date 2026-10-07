package learn.kotlin1416.trailing

// 尾逗号（1.4 语法）：多行实参/形参/集合字面量/枚举条目后面可以留一个逗号。
// 好处是可读 diff：插入或删除一行不会顺带动到上一行的标点。

data class Row(
    val id: Int,
    val name: String,
    val score: Double,
)   // <- 尾逗号

enum class Level {
    LOW,
    MID,
    HIGH,
}

fun report(
    id: Int,
    name: String,
    level: Level,
): String = "$id/$name/${level.name}"   // <- 形参尾逗号

fun main() {
    val rows = listOf(
        Row(1, "a", 1.5),
        Row(2, "b", 2.5),
    )   // <- 实参尾逗号
    check(rows.size == 2)

    val m = mapOf(
        1 to "one",
        2 to "two",
    )
    check(m[2] == "two")

    val s = setOf(
        "x",
        "y",
    )
    check(s.size == 2)

    check(report(3, "c", Level.HIGH) == "3/c/HIGH")

    // 边界实测（本机 2.1.10）：解构声明、类型实参列表都允许尾逗号；
    // 唯独继承/实现列表不允许 —— `class X : Runnable,` 报 Syntax error: Type expected.
    val (destructuredA, destructuredB,) = Pair(1, 2)
    check(destructuredA + destructuredB == 3)
    val typedArgs = listOf<String>(
        "z",
    )
    check(typedArgs.single() == "z")

    val one = intArrayOf(
        1,
    )
    check(one[0] == 1)

    println("trailing comma(1.4) OK: ${rows.map { it.name }}")
}
