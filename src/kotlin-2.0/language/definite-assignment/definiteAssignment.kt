package learn.kotlin20.definite

// Kotlin 2.0（K2）重写了"确定性赋值"（definite assignment）分析：
// 编译器要证明一个 `val x: T` 声明后，在每次读取之前恰好被赋值一次。
// 1.x 的 K1 前端在这件事上过于保守，某些"显然已经赋值"的写法会被误判成重复赋值。
//
// 本文件里的每个函数都单独测过三种语言版本：
//   默认（2.1 / K2）、-Plv=1.9（K1）、-Plv=1.6（K1）
// 结论见同目录 README.md（含原样错误文本）。

// 1) else 分支以 return 结束：if 是唯一的赋值路径。
fun ifReturnsElseAssigns(b: Boolean): Int {
    val x: Int
    if (b) {
        x = 1
    } else {
        return 2
    }
    return x
}

// 2) while(true) 里一条路 break、一条路 return。
fun breakOrReturn(b: Boolean): Int {
    val x: Int
    while (true) {
        if (b) {
            x = 1
            break
        } else {
            return 2
        }
    }
    return x
}

// 3) elvis 右侧是 return，所以赋值只可能发生一次。
fun elvisReturns(v: Int?): Int {
    val x: Int
    x = v ?: return -1
    return x
}

// 4) do/while(false)：两个分支各赋值一次，循环体只走一遍。
//    这是本文件唯一一处 K1 报错、K2 接受的写法。
fun doWhileFalse(b: Boolean): Int {
    val x: Int
    do {
        if (b) {
            x = 1
        } else {
            x = 2
        }
    } while (false)
    return x
}

// 5) try 里赋值可能抛出，catch 里再兜一次：这种"两条路径都写同一个 val"
//    在 K1/K2 上都是 error（K2: 'val' cannot be reassigned.），所以只能声明成 var。
fun tryCatchFallback(b: Boolean): String {
    var s: String
    try {
        s = if (b) "try" else throw IllegalStateException("boom")
    } catch (e: IllegalStateException) {
        s = "catch:${e.message}"
    }
    return s
}

// 6) 复合条件里赋值：K1/K2 都要求每个可达分支都覆盖到。
fun nestedConditions(flag: Boolean, other: Boolean): String {
    val msg: String
    if (flag) {
        if (other) {
            msg = "both"
        } else {
            msg = "flag-only"
        }
    } else {
        return "neither"
    }
    return msg
}

fun main() {
    check(ifReturnsElseAssigns(true) == 1)
    check(ifReturnsElseAssigns(false) == 2)
    check(breakOrReturn(true) == 1)
    check(breakOrReturn(false) == 2)
    check(elvisReturns(null) == -1)
    check(elvisReturns(9) == 9)
    check(doWhileFalse(true) == 1)
    check(doWhileFalse(false) == 2)
    check(tryCatchFallback(true) == "try")
    check(tryCatchFallback(false) == "catch:boom")
    check(nestedConditions(true, true) == "both")
    check(nestedConditions(true, false) == "flag-only")
    check(nestedConditions(false, true) == "neither")

    println("definite assignment OK: ifElseReturn=${ifReturnsElseAssigns(true)}/${ifReturnsElseAssigns(false)} " +
            "breakReturn=${breakOrReturn(true)}/${breakOrReturn(false)} " +
            "elvis=${elvisReturns(null)}/${elvisReturns(9)} " +
            "doWhile=${doWhileFalse(true)}/${doWhileFalse(false)} " +
            "tryCatch=${tryCatchFallback(true)}/${tryCatchFallback(false)} " +
            "nested=${nestedConditions(true, true)}/${nestedConditions(true, false)}/${nestedConditions(false, true)}")
}
