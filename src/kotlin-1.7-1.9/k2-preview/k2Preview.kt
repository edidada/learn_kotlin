package learn.kotlin1719.k2

import kotlin.Metadata

// K2 预览线：1.7.0 alpha 里用 -Xuse-k2 打开，1.9.20 进 beta，2.0.0 起是默认前端。
// 本机（Kotlin 2.1.10 / KGP 2.1.10）实测：`-Xuse-k2` 已经不是一个可用开关，编译器原话
//   e: Compiler flag -Xuse-k2 is no more supported. Compiler versions 2.0+ use K2 by default,
//      unless the language version is set to 1.9 or earlier
// 所以 2.x 上"回到 K1"的唯一办法是把 language version 压到 1.9 及以下 ——
// 而 2.1.10 能接受的最低语言版本是 1.6（-Plv=1.3/1.4/1.5 一律 "no longer supported; please, use version 1.6 or greater"），
// 于是本机有一个真实可跑的 K1/K2 对照窗口：-Plv=1.6 / 1.9（K1）vs 默认（K2）。
// 同目录 README 记了这个窗口里量到的诊断差异（含逐字报错）。

class FrontEndProbe

object MarkerInstance

fun main() {
    // 1) 类文件里的 @Metadata 会写出"编译它的前端所对应的语言版本"，这是运行期能拿到的最硬证据
    val md = FrontEndProbe::class.java.getAnnotation(Metadata::class.java)
    val mv = md.metadataVersion.joinToString(".")
    check(md.kind == 1)                       // 1 = CLASS
    check(mv.startsWith("2."))                // 默认 LV(2.1) 编译：实测 mv=2.1.0

    // 2) 运行期版本
    check(KotlinVersion.CURRENT.major == 2)
    println("runtime version=${KotlinVersion.CURRENT} stdlib=${KotlinVersion::class.java.`package`.implementationVersion}")
    println("K2 metadata mv=$mv jdk=${System.getProperty("java.version")}")

    // 3) 前端差异的证据不在运行期，而在编译期：见同目录 README 的 K1(-Plv=1.6/1.9) vs K2(默认) 逐字对照，
    //    以及 version/1.4-1.6 的 sealed-interface 篇（`when (val n = Expr.Num(7))` 只在 K2 编得过）。
    val smart = if (out() == MarkerInstance) "marker" else "other"
    check(smart == "marker")

    println("k2 preview probe OK: mv=$mv kotlin=${KotlinVersion.CURRENT}")
}

fun out(): Any = MarkerInstance
