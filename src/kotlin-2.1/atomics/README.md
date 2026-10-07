# atomics

归属：[实] 2.1

要覆盖：`kotlin.concurrent.atomics`：`AtomicInt`/`AtomicLong`/`AtomicBoolean`/`AtomicReference` 与 `load`/`store`/`exchange`/`compareAndSet`/`compareAndExchange`/`fetchAndAdd`/`addAndFetch`，数组版带 `At(index)` 后缀；`asJavaAtomic`/`asKotlinAtomic` 互转。注意成员函数本身没有 `@SinceKotlin`，整个包由 `@ExperimentalAtomicApi`(2.1) 门控——`getAndSet` 这类 Java 命名并不存在。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="ExperimentalAtomicApi" || $3=="asJavaAtomic" || $3=="fetchAndAdd"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_concurrent_atomics.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin21.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin21.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。
