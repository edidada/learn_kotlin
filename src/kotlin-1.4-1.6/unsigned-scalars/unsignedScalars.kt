package learn.kotlin1416.uscalars

// 无符号标量类 UInt/ULong/UByte/UShort 本体 since=1.5（数组早在 1.3，见上一档）。
// 复核：awk -F'\t' '$1=="1.5" && $2=="class" && $3 ~ /^U(Int|Long|Byte|Short)$/{print $3}' docs/_data/slices/kotlin.tsv

fun main() {
    // 1) 字面量与算术（1.5 的语言层能力）
    val u: UInt = 42u
    check(u + 1u == 43u)
    check(u * 2u == 84u)

    // 2) 回绕：无符号溢出不是异常也不是负数
    check(UInt.MAX_VALUE + 1u == 0u)
    check(0u - 1u == UInt.MAX_VALUE)

    // 3) 位重解释：toUInt/toInt 改的是读法，不是数值
    check((-1).toUInt() == UInt.MAX_VALUE)
    check(UInt.MAX_VALUE.toInt() == -1)
    check(255u.toUByte() == UByte.MAX_VALUE)
    check((-1).toUByte() == UByte.MAX_VALUE)

    // 4) 位运算：Int 版 1.4 就有；无符号版切片标 1.5，且移位量仍是 Int（实测 shr/shl 收 UInt 会报
    // "Argument type mismatch: actual type is 'kotlin.UInt', but 'kotlin.Int' was expected"）
    check(0b1011.countOneBits() == 3)
    check((0xFFu shr 4).toString() == "15")
    check((1u shl 31).toString() == "2147483648")
    check(0b1011u.toInt().countOneBits() == 3)

    // 5) 解析：负号一律拒绝
    check("12".toUIntOrNull() == 12u)
    check("-1".toUIntOrNull() == null)
    check("4294967295".toULongOrNull() == 4294967295uL)

    // 6) 与浮点/其它标量的互转；无符号加法会提升到更宽的类型
    check(UInt.MAX_VALUE.toDouble() > 4.0e9)
    check(1u.toUShort() + 255u.toUShort() == 256u)
    // 实测：UShort + UShort 的结果运行期就是 kotlin.UInt（不是 UShort），装箱类名可查
    val promoted: Any = 3u.toUShort() + 4u.toUShort()
    check(promoted.javaClass.name == "kotlin.UInt")
    check(UByte.MAX_VALUE.toString() == "255")
    check(UShort.MAX_VALUE.toString() == "65535")
    check(UInt.MAX_VALUE.toString() == "4294967295")

    // 7) 观察：装箱前不存在对象，擦除后底层就是 Int/Long
    println("unsigned scalars(1.5) OK: UInt -> ${UInt::class.java.name}, ULong -> ${ULong::class.java.name}, max=${UInt.MAX_VALUE}")
}
