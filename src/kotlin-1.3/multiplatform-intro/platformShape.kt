package learn.kotlin13.mpp

// 1.3 才是"能真正跑起来的多平台形态"成型的那一版：commonMain/commonTest 源集 + expect/actual
// + Multiplatform 插件（1.1 只有 idea，1.3 才有 metadata 编译和 expect/actual 校验）。
// 本模块只有 JVM 一个 target，所以 expect 关键字只能当反例记录（原文报错见 README 的实测结论）。
// 这里用 JVM 侧的等价物演示 expect/actual 的"形状"：common 侧只声明，平台侧给实现。

interface PlatformContract {
    fun name(): String
    fun lineSeparator(): String
}

class JvmPlatform : PlatformContract {
    override fun name() = "jvm"
    override fun lineSeparator() = System.lineSeparator()
}

fun main() {
    val p: PlatformContract = JvmPlatform()
    check(p.name() == "jvm")
    check(p.lineSeparator().isNotEmpty())

    // 差别在于约束发生的时机：interface 是运行期/类型系统的约定，
    // expect/actual 是编译期强制"每个 target 都必须给 actual"，缺一即报错。
    println("mpp shape(1.3) OK: name=${p.name()} sep=${p.lineSeparator().toList()}")
}
