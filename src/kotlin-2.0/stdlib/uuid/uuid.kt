@file:OptIn(ExperimentalUuidApi::class)

package learn.kotlin20.uuid

import java.nio.ByteBuffer
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlin.uuid.getUuid
import kotlin.uuid.putUuid

// kotlin.uuid.Uuid：2.0 进 stdlib（切片复核：awk -F'\t' '$3=="Uuid"{print $1}' docs/_data/slices/kotlin_uuid.tsv -> 2.0）。
// 本机 stdlib 是 2.1.10，用 javap 直接数了一遍真实成员（别信二手清单）：
//   kotlin.uuid.Uuid            implements java.io.Serializable   // 注意：**没有** Comparable
//   成员：toString/toHexString/toByteArray + @PublishedApi internal 的两个 Long
//   Companion：NIL / parse / parseHex / random / fromLongs / fromULongs / fromByteArray / LEXICAL_ORDER
//   顶层：ByteBuffer.getUuid()/getUuid(index)/putUuid(uuid)/putUuid(index, uuid)
// 而 docs/_data/slices/kotlin_uuid.tsv 里的 toLongs / toULongs / toHexDashString / parseHexDash /
// toUByteArray / fromUByteArray 在本机 2.1.10 全是 Unresolved reference —— 那份切片数据集比工具链新，
// 说明"切片给的 since"必须再拿本机 jar 复核一次（这里逐条实测见同目录 README）。

fun main() {
    // 1) NIL 与字符串形态
    check(Uuid.NIL.toString() == "00000000-0000-0000-0000-000000000000")
    val text = "123e4567-e89b-12d3-a456-426614174000"
    val u = Uuid.parse(text)
    check(u.toString() == text)
    check(u.toHexString().length == 32 && '-' !in u.toHexString())
    check(Uuid.parseHex(u.toHexString()) == u)
    check(u.toString().length == 36 && u.toString()[8] == '-' && u.toString()[23] == '-')

    // 2) 内部就是两个 Long（@PublishedApi internal，公开面只有 toString/toHexString/toByteArray）
    //    低 64 位最高位是 1，所以有符号读出来是负数；十六进制字符串只能用 toULong(16) 解，
    //    "a456426614174000".toLong(16) 会 NumberFormatException（超范围），实测过。
    val bits = u.toString().replace("-", "")
    val msb = bits.substring(0, 16).toULong(16).toLong()
    val lsb = bits.substring(16).toULong(16).toLong()
    check(Uuid.fromLongs(msb, lsb) == u)
    check(java.lang.Long.toHexString(msb) == "123e4567e89b12d3")
    check(java.lang.Long.toHexString(lsb) == "a456426614174000")
    check(lsb < 0L && msb > 0L)

    // 3) ULong 入口（名字在字节码里带 mangle：fromULongs-eb3DHEI）
    val msbU = bits.substring(0, 16).toULong(16)
    val lsbU = bits.substring(16).toULong(16)
    check(Uuid.fromULongs(msbU, lsbU) == u)
    check(lsbU > 0uL)                                 // 同一串位换成无符号就是正数

    // 4) 字节形态：固定 16 字节
    check(Uuid.SIZE_BYTES == 16 && Uuid.SIZE_BITS == 128)
    val bytes = u.toByteArray()
    check(bytes.size == 16 && Uuid.fromByteArray(bytes) == u)

    // 5) random()：版本位与变体位（UUID v4 / RFC 4122）
    val r = Uuid.random()
    val rb = r.toString().replace("-", "")
    check(rb[12] == '4')                              // version = 4（随机）
    check(rb[16] in '8'..'b')                         // variant 10xx
    check(r != Uuid.random())

    // 6) 排序只有显式比较器：Uuid **没有**实现 Comparable，`<` 写不出来
    val sorted = listOf(Uuid.NIL, Uuid.parse("ffffffff-ffff-ffff-ffff-ffffffffffff"), r)
        .sortedWith(Uuid.LEXICAL_ORDER)
    check(sorted.first() == Uuid.NIL && sorted.last().toString().startsWith("f"))
    check(u.hashCode() == Uuid.parse(text).hashCode() && u == Uuid.parse(text))
    check(u != Uuid.parse("123e4567-e89b-12d3-a456-426614174001"))
    val asAny: Any = u
    check(asAny is java.io.Serializable)      // 经由 Any 判定，避开 K2 的 "Check for instance is always 'true'"
    check(Uuid::class.java.interfaces.map { it.name }.contains("java.io.Serializable"))

    // 7) 与 JDK 的边界：Uuid 不是 java.util.UUID，只能靠字符串/字节互通
    val jdk = java.util.UUID.fromString(text)
    check(jdk.toString() == u.toString())
    check(jdk.javaClass.name == "java.util.UUID" && u.javaClass.name == "kotlin.uuid.Uuid")
    check(u.javaClass != jdk.javaClass)

    // 8) ByteBuffer 侧的 getUuid/putUuid（切片：2.0）
    val buf = ByteBuffer.allocate(16)
    buf.putUuid(u)
    buf.flip()
    check(buf.getUuid() == u)
    val buf2 = ByteBuffer.allocate(24)
    buf2.putUuid(8, u)
    check(buf2.getUuid(8) == u)

    println("Uuid(2.0) OK: nil=${Uuid.NIL} parse=$u random=$r bytes=${bytes.size}")
}
