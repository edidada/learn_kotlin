# jvm-record

归属：[文] 1.5

要覆盖：`@JvmRecord` 让 data class 编成 JDK record（需 JDK 14+；本仓库 toolchain 是 17，可直接跑）。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.record.JvmRecordKt`）

通过，输出：`JvmRecord(1.5) OK: rec=PointRec(x=1, y=2) plain=PointPlain(x=1, y=2)`。

- 判别只认 JVM 侧事实，不认外观：`PointRec::class.java.isRecord == true`、`superclass.name == "java.lang.Record"`；不加注解的对照组 `isRecord == false`、父类是 `java.lang.Object`。
- `recordComponents` 实测拿到 `[x:int, y:int]`；`declaredConstructors.size == 1`（只有规范化构造器 `(int, int)`）；类是 `final`；`interfaces` 为空 —— 不会因为"看起来像 data class"就顺带实现 `Serializable`。
- 关键观察（很多人以为加了 `@JvmRecord` 就没有 data class 那套糖了）：`declaredMethods` 里 **`component1/component2/copy/copy$default` 全都还在**，同时访问器是 `x()`/`y()`、没有 `getX()`。也就是说这个注解换的是"字节码身份"，Kotlin 语法层一点没少。
- `toString()` 实测是 `PointRec(x=1, y=2)`，和对照组 data class 一模一样；反射里 `toString` 就在 `declaredMethods` 中，说明这个字符串是 Kotlin 生成并覆盖的，别拿 toString 当"是不是 record"的判别（判别只认 `isRecord`/父类，见上面两条）。
- 前置条件：JDK 14+（本机 17）；`p.copy(x = 5)`、`==` 都正常，因为走的是 Kotlin 生成的成员而不是 record 的 equals。
