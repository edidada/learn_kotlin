package learn.kotlin20.enumsfn

import kotlin.enums.EnumEntries
import kotlin.enums.enumEntries

// 顶层 enumEntries<T>()：2.0（切片复核：awk -F'\t' '$3=="enumEntries"{print $1"|"$6}' docs/_data/slices/kotlin_enums.tsv
//   -> 2.0|public inline fun <reified T : Enum<T>> enumEntries(): EnumEntries<T> = enumEntriesIntrinsic()
//      1.8 那两条 internal 的是 EnumEntries(entriesProvider)/(entries) 工厂，不是这个公开函数）
// 本机 stdlib 复核（javap -p kotlin.enums.EnumEntriesKt）确实有
//   public static final <T extends java.lang.Enum<T>> EnumEntries<T> enumEntries()
// 它的价值全在"泛型"上：`T.entries` 写不出来（type parameter 没有成员），只能靠 reified。

enum class Level { LOW, MID, HIGH }

enum class Code(val digit: Int) {
    A(1),
    B(2),
    C(3),
}

// 星投影读不到 name/ordinal，实测 K2 的报错原话是：
//   Cannot use 'T' as reified type parameter. Use a class instead.
// 所以通用入口要么写成 <E : Enum<E>>，要么退回 toString()。
fun <E : Enum<E>> describe(e: E): String = "${e.name}=${e.ordinal}"

// 泛型入口：这一句在 1.9 及以前只能写成 T::class.java.enumConstants（平台类型 + 数组）
// 注意 enumEntries<T>() 是 reified 的，所以包装它的函数也必须 inline + reified
inline fun <reified T : Enum<T>> names(): List<String> = enumEntries<T>().map { it.name }

fun <T> genericValue(value: T): String = when (value) {
    is Enum<*> -> "enum $value"           // 这里只能用 toString，理由见上面 describe 的注释
    else -> "other"
}

fun main() {
    // 1) 与 1.9 的 `X.entries` 是同一个对象，不是新副本
    val viaFn: EnumEntries<Level> = enumEntries<Level>()
    check(viaFn === Level.entries)
    check(viaFn.size == 3)

    // 2) reified 泛型是唯一"类型参数侧"的入口
    check(names<Level>() == listOf("LOW", "MID", "HIGH"))
    check(names<Code>() == listOf("A", "B", "C"))
    check(enumEntries<Code>().map { it.digit } == listOf(1, 2, 3))

    // 3) 通过 Enum<*> 引用的通用路径：describe 只在具体类型上可用
    check(genericValue(Level.HIGH) == "enum HIGH")
    check(describe(Level.HIGH) == "HIGH=2")
    check(describe(enumEntries<Code>().first()) == "A=0")

    // 4) 它是 List：集合操作、valueTo 映射、二分都直接可用
    val byName = enumEntries<Level>().associateBy { it.name }
    check(byName["MID"] == Level.MID)
    check(enumEntries<Level>().indexOf(Level.HIGH) == 2)
    check(enumEntries<Code>().filter { it.digit > 1 }.map { it.name } == listOf("B", "C"))
    check(enumEntries<Level>().toString() == "[LOW, MID, HIGH]")

    // 5) 对照 1.9 之前的写法：Class.enumConstants 每次给新数组，所以 !==
    check(Level::class.java.enumConstants !== Level::class.java.enumConstants)
    check(Level::class.java.enumConstants.toList() == enumEntries<Level>().toList())

    // 6) 空枚举边界：entries 的长度就是 0，类型仍然成立（这里用一个嵌套的空枚举演示）
    check(Empty.entries.isEmpty() && enumEntries<Empty>().size == 0)

    println("enumEntries<T>()(2.0) OK: same=${viaFn === Level.entries} names=${names<Level>()} empty=${enumEntries<Empty>().size}")
}

enum class Empty
