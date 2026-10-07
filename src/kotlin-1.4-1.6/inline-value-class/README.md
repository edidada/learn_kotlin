# inline-value-class

归属：[文] 1.5

要覆盖：`inline class`(1.5) → `value class`(1.5 experimental / 1.9 改名) 的装箱与类型擦除表现，配合 `-Xjvm-default` 观察。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.value.ValueClassKt`）

通过，输出（节选）：`value class(1.5) OK: learn.kotlin1416.value.Password methods=[(box-impl, java.lang.String), (getLength-impl, java.lang.String), (masked-impl, java.lang.String), ..., (equals, java.lang.Object), (getValue, <no-arg>), (unbox-impl, <no-arg>)]`。

- 反射断言别按"方法名 == masked"写：成员函数编译成 **static 的 `masked-impl`**，参数是底层类型 `java.lang.String`；`getLength-impl` 同理。实例侧只剩 `getValue/equals/hashCode/toString`。用 `it.parameterTypes[0]` 直接索引会 AIOOBE（`toString`/`hashCode`/`unbox-impl` 是零参），实测改用 `firstOrNull()`。
- 装箱/擦除的三件套：`constructor-impl(String) -> String`、`box-impl(String) -> Password`、`unbox-impl() -> String`。赋给 `Any` 才产生对象，所以 `boxed1 == boxed2` 成立而 `boxed1 !== boxed2` 也成立。
- 边界实测（探针原文照抄）：
  - `var bad: Int = 1` → `Value class cannot have properties with backing fields.`
  - `value class Multi(val v: Int, val w: Int)` → `Inline class must have exactly one primary constructor parameter.`（1.5 改名后消息里还留着 inline 这个旧词）
  - 不写 `@JvmInline` → `Value classes without '@JvmInline' annotation are not yet supported.` —— 所以 1.9"转正"转的是 `value` 关键字，注解到 2.1.10 仍然必需。
  - 允许：底层类型可空（`value class NullableUnder(val v: String?)` 无报错）、`init { require(...) }`、`companion object`、实现接口（样本里的 `Tagged` 实现 `Comparable<Tagged>` 并跑通 `<` 比较）。
- 顶层函数接收 value class 时 JVM 名字带 mangling：探针里 `fun takePW(p: PW)` 编成 `takePW-AkQ1Hjw(String)`，参数直接是底层类型 —— 这就是"value class 不参与 Java 互操作"的具体表现。
