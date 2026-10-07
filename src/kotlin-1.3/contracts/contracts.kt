package learn.kotlin13.contracts

import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

// kotlin.contracts 整包 since=1.3（contract / CallsInPlace / Returns / ReturnsNotNull /
// ExperimentalContracts），但 ExperimentalContracts 到今天仍未转正，写契约必须 @OptIn。
// 复核：awk -F'\t' '{print $1"|"$2"|"$3}' docs/_data/slices/kotlin_contracts.tsv | sort -u

// returns(true) implies (x != null)：把"返回值"翻译成"调用后成立的断言"，从而解锁智能转换
@OptIn(ExperimentalContracts::class)
fun String?.notBlank(): Boolean {
    contract {
        returns(true) implies (this@notBlank != null)
    }
    return !this.isNullOrBlank()
}

// callsInPlace EXACTLY_ONCE：编译器据此认定 lambda 一定执行且只执行一次
@OptIn(ExperimentalContracts::class)
inline fun exactlyOnce(block: () -> Unit) {
    contract {
        callsInPlace(block, InvocationKind.EXACTLY_ONCE)
    }
    block()
}

fun main() {
    val s: String? = "kotlin"
    if (s.notBlank()) {
        check(s.length == 6)        // 没有契约这行编译不过：智能转换只认 isNullOrBlank 这类内置契约
    }
    val t: String? = null
    check(!t.notBlank())

    var n: Int
    exactlyOnce { n = 5 }
    check(n == 5)                   // 没有契约报 "Variable 'n' must be initialized"

    var hits = 0
    exactlyOnce { hits++ }
    check(hits == 1)

    println("contracts(1.3) OK")
}
