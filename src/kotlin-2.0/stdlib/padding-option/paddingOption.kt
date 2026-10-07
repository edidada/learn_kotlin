@file:OptIn(ExperimentalEncodingApi::class)

package learn.kotlin20.pad

import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

// Base64.withPadding(PaddingOption)：2.0（切片复核：
// awk -F'\t' '$3=="PaddingOption" || $3=="withPadding"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_io_encoding.tsv
//   -> 2.0 class PaddingOption / 2.0 fun withPadding）
// 本机 stdlib 2.1.10 用 javap 数出来的枚举常量是四个：
//   PRESENT / ABSENT / PRESENT_OPTIONAL / ABSENT_OPTIONAL
// README 原来写的 ABSENT_OR_1_AND_2_EQUAL_SIGNS / ALWAYS / IGNORE 在本机**一个都不存在**（逐条实测见同目录 README）。
// 另外 2.1.10 上 Base64 还是实验 API，opt-in 标记叫 ExperimentalEncodingApi（不是 ExperimentalBase64Api）；
// paddingOption 属性本身是 internal（字节码 getPaddingOption\$kotlin_stdlib），源码里读不到，
// 所以这里用"编一个 2 字节串看有没有 ="当配置指纹来观测。

val two = "ab".toByteArray()            // 2 字节 -> 补齐时要有 1 个 '='
val one = "a".toByteArray()             // 1 字节 -> 补齐时要有 2 个 '='
val three = "abc".toByteArray()         // 3 字节 -> 天然不需要补位

fun Base64.fingerprint(source: ByteArray = two): String = encode(source)

fun main() {
    // 1) 默认：补齐到 4 的倍数
    check(Base64.encode(two) == "YWI=")
    check(Base64.encode(three) == "YWJj")
    check(Base64.encode(one) == "YQ==")

    // 2) withPadding 返回新实例，不改原对象
    val absent = Base64.Default.withPadding(Base64.PaddingOption.ABSENT)
    check(absent !== Base64.Default)
    check(Base64.Default.fingerprint() == "YWI=")
    check(absent.fingerprint() == "YWI")
    check(absent.encode(one) == "YQ")
    check(absent.encode(three) == "YWJj")           // 本来不需要补的，输出不变

    // 3) 四个选项各自的编码形态，逐个实测（README 记这张表）
    val byOption = Base64.PaddingOption.entries.associateWith { Base64.Default.withPadding(it) }
    val table = byOption.entries.associate { (option, engine) ->
        option.name to listOf(engine.fingerprint(one), engine.fingerprint(two), engine.fingerprint(three))
    }
    check(table["PRESENT"] == listOf("YQ==", "YWI=", "YWJj"))
    check(table["ABSENT"] == listOf("YQ", "YWI", "YWJj"))

    // 4) 解码侧的严格/宽松：同一段无填充文本，看四种配置各自接不接
    val decodeNoPad = byOption.entries.associate { (option, engine) ->
        option.name to runCatching { engine.decode("YWI".toByteArray()).toList() == two.toList() }
    }
    val decodeWithPad = byOption.entries.associate { (option, engine) ->
        option.name to runCatching { engine.decode("YWI=".toByteArray()).toList() == two.toList() }
    }
    // 往返一律成立（自己编的自己能解），这是唯一能安全断言的部分
    byOption.values.forEach { engine ->
        check(engine.decode(engine.encode(two)).toList() == two.toList())
        check(engine.decode(engine.encode(one)).toList() == one.toList())
        check(engine.decode(engine.encode(three)).toList() == three.toList())
    }

    // 5) Default / UrlSafe / Mime 是三个独立配置（2.1.10 没有 Pem，那是 2.2 才补的）
    check(Base64.UrlSafe.fingerprint() == "YWI=")
    check(Base64.Mime.fingerprint() == "YWI=")
    check(Base64.Mime.withPadding(Base64.PaddingOption.ABSENT).fingerprint() == "YWI")
    val long = ByteArray(60) { (it % 26 + 97).toByte() }
    check(Base64.Mime.encode(long).contains('\n'))
    check(!Base64.Default.encode(long).contains('\n'))

    // 6) 枚举本体：1.9 的 entries 语法在 2.0 的嵌套枚举上照样好用
    check(Base64.PaddingOption.entries.map { it.name } ==
        listOf("PRESENT", "ABSENT", "PRESENT_OPTIONAL", "ABSENT_OPTIONAL"))
    check(Base64.PaddingOption.entries === Base64.PaddingOption.entries)

    println("PaddingOption/withPadding(2.0) OK: encodeTable=$table")
    println("  decodeNoPad=${decodeNoPad.entries.joinToString { "${it.key}=${it.value.getOrNull()}" }}")
    println("  decodeWithPad=${decodeWithPad.entries.joinToString { "${it.key}=${it.value.getOrNull()}" }}")
}
