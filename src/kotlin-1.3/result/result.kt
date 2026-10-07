package learn.kotlin13.result

// Result / runCatching / mapCatching / recover / fold / getOrDefault 整批 since=1.3
// 复核：awk -F'\t' '$1=="1.3" && $2=="fun" && $3 ~ /^(runCatching|mapCatching|recover|recoverCatching|fold|getOrDefault|getOrThrow|onSuccess|onFailure)$/ {print $3}' docs/_data/slices/kotlin.tsv | sort -u

fun main() {
    // 1) 最小闭环：成功与失败都是值，不再靠 try/catch 控制流
    val ok: Result<Int> = runCatching { "42".toInt() }
    val bad: Result<Int> = runCatching { "x".toInt() }
    check(ok.isSuccess && ok.getOrNull() == 42)
    check(bad.isFailure && bad.exceptionOrNull() is NumberFormatException)

    // 2) mapCatching：成功才继续算，失败原样传递；块里再抛会被接住
    check(ok.mapCatching { it * 2 }.getOrThrow() == 84)
    check(bad.mapCatching { it * 2 }.exceptionOrNull() is NumberFormatException)
    check(ok.mapCatching { error("boom") }.exceptionOrNull()?.message == "boom")

    // 3) recover / recoverCatching：给失败兜一个值
    check(bad.recover { 0 }.getOrThrow() == 0)
    check(bad.recoverCatching { error("again") }.exceptionOrNull()?.message == "again")

    // 4) fold：两个分支一次收敛成同一个类型
    val shown = ok.fold(onSuccess = { "v=$it" }, onFailure = { e -> "e=${e::class.simpleName}" })
    check(shown == "v=42")

    // 5) 取值三件套
    check(bad.getOrDefault(-1) == -1)
    check(bad.getOrNull() == null)
    val rethrown = runCatching { bad.getOrThrow() }
    check(rethrown.exceptionOrNull() === bad.exceptionOrNull())

    // 6) 表示技巧：同一个 Throwable 既能是 success 的值，也能是 failure 的原因
    val e = IllegalStateException("as-value")
    val asValue: Result<Throwable> = runCatching { e }
    val asError: Result<Throwable> = Result.failure(e)
    check(asValue.isSuccess && asValue.getOrNull() === e)
    check(asError.isFailure && asError.exceptionOrNull() === e)

    // 7) 实测私有字段 value 里到底装了什么（这就是上面能区分的原因）
    val field = Result::class.java.getDeclaredField("value").apply { isAccessible = true }
    val rawFailure = field.get(asError)?.javaClass?.name
    val rawSuccess = field.get(asValue)?.javaClass?.name
    check(rawFailure == "kotlin.Result\$Failure")
    check(rawSuccess == "java.lang.IllegalStateException")

    // 8) 观察项（不断言）：Result<Unit> 的 value 存的是什么
    val unit = runCatching { }
    println("Result(1.3) OK: failure.value -> $rawFailure; success.value -> $rawSuccess; Result<Unit>.value -> ${field.get(unit)?.javaClass?.name}")
}
