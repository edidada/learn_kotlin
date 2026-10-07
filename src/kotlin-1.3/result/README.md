# result

归属：[实] 1.3

要覆盖：`Result`/`runCatching`/`mapCatching`/`isSuccess`/`exceptionOrNull`；`value` 与 `exception` 的表示技巧。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="Result" || $3=="runCatching" || $3=="mapCatching" || $3=="exceptionOrNull" || $3=="isSuccess"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin13.result.ResultKt`）

- 成功和失败都是值：`runCatching { "42".toInt() }.getOrNull() == 42`；`runCatching { "x".toInt() }.exceptionOrNull() is NumberFormatException`。
- `mapCatching` 只在成功路径上继续算，失败原样传递；块里再抛会被接住（`error("boom")` → `message == "boom"`）。`recover` / `recoverCatching` 负责兜值和兜异常。
- `fold(onSuccess, onFailure)` 能把两个分支收敛成同一个类型，省去 if/else。
- `getOrThrow()` 抛出的异常和 `exceptionOrNull()` 是同一个实例（用 `===` 钉住），所以重新包一层 `runCatching` 不会丢身份信息。
- 表示技巧坐实：同一个 `IllegalStateException` 既能当 success 的值（`runCatching { e }`，`isSuccess` 且 `getOrNull() === e`），也能当 failure 的原因（`Result.failure(e)`）。反射读私有字段 `value`：failure 侧是 `kotlin.Result$Failure`，success 侧直接是 `java.lang.IllegalStateException`——这就是两者能区分的原因。
- 观察项（不断言）：`Result<Unit>.value` 实测存的是 `kotlin.Unit`，2.1.10 并没有把 Result 自身塞进 value 的省分配技巧。
