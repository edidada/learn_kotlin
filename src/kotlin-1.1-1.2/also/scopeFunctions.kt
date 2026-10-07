package learn.kotlin1112.also

// also：@SinceKotlin("1.1")，commonMain/kotlin/util/Standard.kt:94
// public inline fun <T> T.also(block: (T) -> Unit): T
// 对比：apply（1.0）lambda 内是 this 且返回接收者；let（1.0）返回 lambda 的结果。
// 复核：awk -F'\t' '$1=="1.1" && $3=="also"' docs/_data/slices/kotlin.tsv

class Conn(val id: Int) { override fun toString() = "Conn($id)" }

class Builder { var value = 0 }

fun zeroOrNull(n: Int): Int? = n.also { if (it == 0) return null }

fun main() {
    // 1) 插入副作用而不改变表达式类型
    var log = ""
    val c = Conn(1).also { log += "created $it" }
    check(c.id == 1 && log == "created Conn(1)")

    // 2) 链式中间观察：also 一路返回自身，最后才取 size
    val len = mutableListOf(1, 2, 3)
        .also { it.add(4) }
        .also { log += " size=${it.size}" }
        .size
    check(len == 4)

    // 3) also vs apply：also 用 it 显式指向外部对象，不会被接收者的同名成员遮蔽
    val b = Builder().also { it.value = 7 }
    check(b.value == 7)

    // 4) inline 带来的能力：lambda 里可以非局部 return（实测通过）
    check(zeroOrNull(0) == null)
    check(zeroOrNull(5) == 5)

    // 5) 与 takeIf 组合：既过滤又留痕
    val kept = "hello".also { log += " check=$it" }.takeIf { it.length > 3 }
    check(kept == "hello")

    println("also(1.1) OK; log=$log")
}
