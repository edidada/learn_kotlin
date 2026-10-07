# readln-typeof

归属：[实] 1.6

要覆盖：`readln`/`readlnOrNull`（`readLine` 是 1.0 的）与 `typeOf<T>()`（kotlin-reflect 的轻量入口）。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="readln" || $3=="readlnOrNull" || $3=="typeOf"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.readln.ReadlnTypeOfKt`）

通过，输出：`readln/typeOf(1.6) OK`。样本用 `System.setIn(ByteArrayInputStream(...))` 喂输入，`finally` 里还原，可在 CI 里跑。

- 切片里直接给了实现原文：`public actual fun readln(): String = readlnOrNull() ?: throw ReadAfterEOFException("EOF has already been reached")`（`docs/_data/slices/kotlin_io.tsv`，1.6），`readlnOrNull() = readLine()`。
- 因此 EOF 行为实测：`readln()` 抛异常，异常 message 精确等于 `EOF has already been reached`；`readlnOrNull()` 给 `null`；1.0 的 `readLine()` 给 `null`。空行喂 `"\n"` 时 `readln()` 返回 `""`（不是 null，不是异常）。
- 反直觉的一条：`ReadAfterEOFException` **在 Kotlin 里抓不到**。字节码 `javap` 显示 `public final class kotlin.io.ReadAfterEOFException extends RuntimeException`，但编译器拒绝 `is kotlin.io.ReadAfterEOFException`：
  `Cannot access 'class ReadAfterEOFException : RuntimeException': it is internal in file.`
  实测只能按类名断言 `boom?.javaClass?.name == "kotlin.io.ReadAfterEOFException"`，或者兜父类 `RuntimeException`。样本里两种写法都留了。
- `typeOf<T>()`（1.6，`kotlin_reflect.tsv`）不需要 kotlin-reflect.jar：`classifier == List::class`、`arguments[0].type?.classifier == String::class`、`isMarkedNullable`（`Int?` true / `Int` false）全部正确。
- 但 `toString()` 会退化，实测原样是 `java.lang.String (Kotlin reflection is not available)`，`List<String>` 打印 `java.util.List<java.lang.String> (...)`。所以别对 `typeOf(...).toString()` 断言含 `kotlin.String` —— 这条一开始就是这么写的，跑出来才发现是假的。配套探针：`Class.forName("kotlin.reflect.full.KClasses")` 失败，证明运行期确实没挂 kotlin-reflect。
