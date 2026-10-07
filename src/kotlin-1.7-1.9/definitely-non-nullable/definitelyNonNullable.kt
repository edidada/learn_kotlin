package learn.kotlin1719.dnn

// definitely non-nullable types（1.7 stable）。
// 重要更正：这个特性的语法是 **`T & Any`**，不是 `T!!`。
// 在 2.1.10 上写 `String!!` / `lateinit var v: String!!` / `external var f: String!!` 一律
// 报 Syntax error: Property getter or setter expected.（K1 with -Plv=1.9 同样报），`!!` 从来不是类型语法。
// 语法限制（探针原文）：
//   val x: String & Any = "a"                 -> Intersection types are supported only for definitely
//       non-nullable types: left part should be a type parameter with nullable bounds.
//   fun <T : Number> f(y: T & Any)            -> 同上（T 的上界已经非空，不允许再交）
//   -Plv=1.6 时                          -> The feature "definitely non nullable types" is only
//       available since language version 1.7
// 复核归属：awk -F'\t' '$3 ~ /elvisLike|&/{print}' docs/_data/slices/kotlin.tsv 查不到符号，
// 因为它是语言层类型系统能力，不是 stdlib API —— 归属看编译器，不看 jar。

fun <T> elvisLike(x: T, y: T & Any): T & Any = x ?: y

// 真实用途：扩展"泛型参数可空但你想保证非空"的 Java 类型
fun <T> nonNullComparator(cmp: Comparator<T & Any>): Comparator<T> = Comparator { a, b -> cmp.compare(a, b) }

class Bag<T>(val item: T)

fun <T> Bag<T & Any>.lengthOf(): Int = (item as String).length

fun main() {
    // 1) 右操作数必须 definitely 非空；左边的 `T` 允许是可空值，`?:` 因此总能落到非空的 y
    check(elvisLike("", "b") == "")            // x 非空（空串也算非空），直接返回 x
    check(elvisLike("k", "b") == "k")
    check(elvisLike("", "b").length == 0)

    // 2) 左边可以是可空/null，T 被推成 String?，返回值仍是非空
    val fromNull: String = elvisLike<String?>(null, "c")
    check(fromNull == "c" && fromNull.length == 1)

    // 3) 传可空值给 `T & Any` 形参：编译期就拦（探针原文）
    //      Argument type mismatch: actual type is 'kotlin.String?', but 'T & Any' was expected.
    //    所以这条不是运行期异常，是写不出来的代码 —— 样本里只留注释。

    // 4) 与 lateinit 的分工（README 关注点）：lateinit 是"我在别处赋值"，运行期没赋值就抛；
    //    `T & Any` 是"类型层面保证非空"，编译期判定。实测 lateinit 的失败形态：
    val holder = Late()
    val ex = runCatching { holder.name }.exceptionOrNull()
    check(ex?.javaClass?.simpleName == "UninitializedPropertyAccessException")
    check(ex?.message == "lateinit property name has not been initialized")
    holder.name = "now-set"
    check(holder.name == "now-set")

    println("definitely-non-nullable(1.7, 语法 T & Any) OK: ${elvisLike("", "b")} $fromNull lateinitGuard=${ex?.javaClass?.simpleName}")
}

class Late {
    lateinit var name: String
}
