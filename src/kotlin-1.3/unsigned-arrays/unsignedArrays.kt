package learn.kotlin13.unsigned

// 实测订正：marker 的真实 FQN 是 `kotlin.ExperimentalUnsignedTypes`（默认导入，不用 import），
// 我一开始写的 `import kotlin.experimental.ExperimentalUnsignedTypes` 报 Unresolved reference。
// 而且 2.1.10 里漏标 @OptIn 只降级成 warning：This declaration needs opt-in. Its usage should be
// marked with '@kotlin.ExperimentalUnsignedTypes' or '@OptIn(kotlin.ExperimentalUnsignedTypes::class)'。
//
// 1.3 只给了无符号**数组**：UByteArray / UShortArray / UIntArray / ULongArray 及 as*/to* 转换
// （切片里 kind=class、since=1.3）；标量类 UByte/UInt/ULong/UShort 是 1.5 才公开的。
// 复核：awk -F'\t' '$3 ~ /^(UByteArray|UIntArray|UByte|UInt)$/ {print $1"|"$2"|"$3}' docs/_data/slices/kotlin.tsv | sort -u

@OptIn(ExperimentalUnsignedTypes::class)
fun main() {
    // 1) 数组类型本体（1.3）
    val ub: UByteArray = byteArrayOf(1, -1).toUByteArray()
    check(ub.size == 2)
    check(ub[1].toString() == "255")             // 同一位模式，换成无符号读法
    val ui: UIntArray = intArrayOf(1, -1).toUIntArray()
    check(ui[1].toString() == "4294967295")

    // 2) as*Array 是共享底层的视图，to*Array 是拷贝（1.3）
    val backed = intArrayOf(7)
    val view = backed.asUIntArray()
    backed[0] = 9
    check(view[0].toString() == "9")             // 视图跟着源数组变
    val copied = backed.toUIntArray()
    backed[0] = 1
    check(copied[0].toString() == "9")           // 拷贝不受影响

    // 3) 集合操作在 1.3 就为无符号数组补齐了整套重载（fill / copyInto / random / sort ...）
    check(ub.sortedArrayDescending()[0].toString() == "255")
    check(ub.maxOrNull()?.toString() == "255")
    check(ub.count { it.toString() == "255" } == 1)

    // 4) 1.5 才有的部分：无符号字面量、toUByte()/toUInt() 转换、MAX_VALUE
    val scalar = (-1).toUByte()
    check(scalar == UByte.MAX_VALUE)
    check(1u + 2u == 3u)
    check(uintArrayOf(0u, 1u).toList().size == 2)   // 工厂签名是 1.3 的，但 UInt 参数要 1.5 才构造得出来

    println("unsigned arrays(1.3, 标量 1.5) OK: ub=${ub.toList()} ui=${ui.toList()}")
}
