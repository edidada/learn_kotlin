package learn.kotlin13.random

import kotlin.random.Random
import kotlin.random.asJavaRandom
import kotlin.random.asKotlinRandom

// kotlin.random 包整体 since=1.3：Random（抽象类）+ Random.Default + Random(seed) 工厂
// + asJavaRandom / asKotlinRandom 互转。
// 复核：awk -F'\t' '$1=="1.3"{print $2"|"$3"|"$4}' docs/_data/slices/kotlin_random.tsv | sort -u

fun main() {
    // 1) 同种子 => 同序列（可复现，这是它能当测试夹具的前提）
    val a = Random(42)
    val b = Random(42)
    check((0 until 5).map { a.nextInt() } == (0 until 5).map { b.nextInt() })

    // 2) bound 是半开区间 [0, bound)
    val c = Random(1)
    val within = (0 until 2000).map { c.nextInt(10) }
    check(within.min() >= 0 && within.max() < 10)

    // 3) nextDouble 在 [0.0, 1.0)，nextLong(bound) 同理
    val d = Random(7)
    repeat(500) { val x = d.nextDouble(); check(x >= 0.0 && x < 1.0) }
    val l = Random(7)
    repeat(500) { val x = l.nextLong(100); check(x in 0 until 100) }

    // 4) nextBytes 就地填充
    val buf = ByteArray(8)
    Random(3).nextBytes(buf)
    check(buf.size == 8)

    // 5) 它不是 java.util.Random 的包装：同种子给不同序列
    val k = Random(12345).nextInt()
    val j = java.util.Random(12345).nextInt()
    println("kotlin Random(12345).nextInt() = $k; java.util.Random(12345).nextInt() = $j; equal = ${k == j}")

    // 6) Default 的真实实现类（观察项）
    println("Random.Default -> ${Random.Default.javaClass.name}")

    // 7) 互转：把 Kotlin 的随机源交给 Java API，再拿回来
    val jr: java.util.Random = Random(5).asJavaRandom()
    check((0 until 100).map { jr.nextInt(2) }.all { it in 0..1 })
    val back: Random = jr.asKotlinRandom()
    check((0 until 100).map { back.nextInt(3) }.all { it in 0..2 })

    println("random(1.3) OK")
}
