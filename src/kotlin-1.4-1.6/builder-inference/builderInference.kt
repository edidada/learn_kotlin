package learn.kotlin1416.builder

import kotlin.experimental.ExperimentalTypeInference

// buildList / buildMap / buildSet 是 1.6 的 stdlib DSL；
// @BuilderInference 注解本身 1.3 就有（切片里 kotlin.tsv since=1.3），作用是让
// "lambda 里只调用不产生类型信息的方法"也能推断出元素类型。
// 复核：awk -F'\t' '$1=="1.6" && $3 ~ /^(buildList|buildMap|buildSet)$/{print $3}' docs/_data/slices/kotlin_collections_builders.tsv | sort -u

// 实测：@BuilderInference 自己还挂着 ExperimentalTypeInference，用之前要 @OptIn，
// 否则报 This declaration needs opt-in. Its usage must be marked with
// '@kotlin.experimental.ExperimentalTypeInference'。
@OptIn(ExperimentalTypeInference::class)
@BuilderInference
fun <T> buildLike(builder: MutableCollection<T>.() -> Unit): List<T> = ArrayList<T>().apply(builder)

fun main() {
    // 1) 三个构建器
    val l = buildList {
        add(1)
        addAll(listOf(2, 3))
    }
    check(l == listOf(1, 2, 3))

    val m = buildMap {
        put("k", 1)
        putAll(mapOf("j" to 2))
    }
    check(m == mapOf("k" to 1, "j" to 2))

    val s = buildSet {
        add(1)
        add(1)
    }
    check(s == setOf(1))

    // 2) 只从 lambda 里的值推类型
    val typed: List<String> = buildList { add("a") }
    check(typed == listOf("a"))

    // 3) 显式类型实参：空 lambda 也合法
    val empty = buildList<Int> { }
    check(empty.isEmpty())

    // 4) 自定义 DSL 加 @BuilderInference（并 OptIn）后同样能推
    //    A/B 实测（同一个 2.1.10 编译器，只换 language/api version）：
    //      默认 LV(2.1)：带注解和不带注解的 `val x = f { add(1) }` 都能把 T 推成 Int —— 看不出注解的差异
    //      -Plv=1.6 -Pav=1.6：两种写法一起报 Not enough information to infer type variable T
    //    另外 2.1.10 已经不接受更老的语言版本：-Plv=1.3/1.4/1.5 一律
    //    "Language version 1.x is no longer supported; please, use version 1.6 or greater."
    val like = buildLike { add("x") }
    check(like == listOf("x"))

    // 5) 实测：结果不是 ArrayList，而是 kotlin.collections.builders.ListBuilder / MapBuilder / SetBuilder
    //    编译期只给 List/Map/Set，强转成 MutableList 再 add 会 UnsupportedOperationException
    check(l.javaClass.name == "kotlin.collections.builders.ListBuilder")
    check(m.javaClass.name == "kotlin.collections.builders.MapBuilder")
    check(s.javaClass.name == "kotlin.collections.builders.SetBuilder")
    val castBoom = runCatching { (l as MutableList<Int>).add(4) }.exceptionOrNull()
    check(castBoom is UnsupportedOperationException)

    // 6) 实测（推翻常见说法）：本机 2.1.10 的编译器下，"没有 @BuilderInference 就推不出元素类型"
    //    这个坑复现不出来 —— 同样的 plainBox/MutableCollection 签名，不写注解、不给期望类型，
    //    val x = plain { add("x") } / val b = plainBox { value = 1 } 都能把 T 推成 String/Int。
    //    注解真正的硬约束反而是 opt-in：漏了 @OptIn(ExperimentalTypeInference::class) 直接编译不过。
    val readOnly: List<Int> = l
    check(readOnly.size == 3)

    println("builder inference(1.6) OK: $l $m $s castGuard=${castBoom?.javaClass?.simpleName}")
}
