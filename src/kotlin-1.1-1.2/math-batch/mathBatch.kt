package learn.kotlin1112.mathbatch

import java.math.BigDecimal
import java.math.BigInteger
import kotlin.math.acosh
import kotlin.math.atanh
import kotlin.math.expm1
import kotlin.math.hypot
import kotlin.math.ln1p
import kotlin.math.log2
import kotlin.math.nextDown
import kotlin.math.nextUp
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.withSign

// 1.2 一次性把数学库补齐：kotlin.math 包本身 since=1.2，
// abs/round/ceil/floor/sign/expm1/ln1p/log2/hypot/acosh/atanh 等成批出现，
// Double/Float 上多了 pow/nextUp/nextDown/roundToInt/roundToLong/toBits/fromBits/withSign，
// 大数侧补了 Int/Long/Double -> toBigInteger/toBigDecimal。
// 复核：awk -F'\t' '$1=="1.2"{print $3}' docs/_data/slices/kotlin_math.tsv | sort -u | paste -sd' '

fun main() {
    // 1) 三角/双曲/对数族（kotlin.math，1.2）
    check(sign(-3.5) == -1.0)
    check(expm1(0.0) == 0.0)
    check(ln1p(0.0) == 0.0)
    check(log2(8.0) == 3.0)
    check(hypot(3.0, 4.0) == 5.0)
    check(acosh(1.0) == 0.0)
    check(atanh(0.0) == 0.0)

    // 2) round 走的是 Math.rint，"银行家舍入"：.5 取偶数（实测，容易记错）
    check(round(2.5) == 2.0)
    check(round(3.5) == 4.0)
    // roundToInt/roundToLong 走 Math.round，".5 向上"
    check(2.5.roundToInt() == 3)
    check(3.5.roundToInt() == 4)

    // 3) 幂与相邻可表示值（nextUp/nextDown 也是 1.2）
    check(2.0.pow(10) == 1024.0)
    check(1.0.nextUp() > 1.0)
    check(1.0.nextDown() < 1.0)
    check(0.0.nextUp() > 0.0)

    // 4) 位级互转：toBits / fromBits（1.2），IEEE754 调试常用
    val bits = (-1.0).toRawBits()
    check(Double.fromBits(bits) == -1.0)

    // 5) withSign 保留数值只改符号（1.2）
    check(3.0.withSign(-1.0) == -3.0)
    check((-3.0).withSign(2.0) == 3.0)

    // 6) 大数转换族（1.2）
    check(1234567890123L.toBigInteger() * BigInteger.TEN == BigInteger("12345678901230"))
    check("0.1".toBigDecimal() + "0.2".toBigDecimal() == BigDecimal("0.3"))
    // 实测纠正：Double.toBigDecimal() 的实现是 BigDecimal(this.toString())，
    // 所以它和字符串版完全相等；真正带出二进制误差的是 Java 的 BigDecimal(Double) 构造器。
    check(0.1.toBigDecimal() == "0.1".toBigDecimal())
    check(0.1.toBigDecimal().scale() == 1)
    check(BigDecimal(0.1).scale() > 1)
    check(BigDecimal(0.1).compareTo("0.1".toBigDecimal()) > 0)
    check("42".toBigIntegerOrNull() == BigInteger.valueOf(42))
    check("x".toBigDecimalOrNull() == null)

    // 7) inc/dec 运算符（1.2，BigInteger/BigDecimal 上的 +-1）
    val big = BigInteger("99")
    check(big.inc() == BigInteger("100"))

    println("math batch(1.2) OK: round(2.5)=${round(2.5)} roundToInt(2.5)=${2.5.roundToInt()}")
}
