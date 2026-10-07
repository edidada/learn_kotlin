# volatile

归属：[实] 1.9

要覆盖：`@Volatile`（`kotlin.concurrent`，1.9）替代 `@Volatile` 注解在 1.x 早期形态的写法。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="Volatile"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_concurrent.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.volat.VolatileFieldKt`）

输出：`volatile(1.9, JVM 侧是 kotlin.jvm.Volatile 别名) OK: member=true top=true seen=42`

- 归属要讲准：1.9 加的是 **common 侧的 `kotlin.concurrent.Volatile`**，切片复核 `$3=="Volatile"` → `1.9 class` + `1.9 typealias`；JVM 上它是 `actual typealias Volatile = kotlin.jvm.Volatile`，而 `kotlin.jvm.Volatile` 本身没有 since 戳（1.0 就在）。所以"1.9 才有 @Volatile"是错的，准确说法是"1.9 起 common 代码里也能写"。
- 反射验证注解真的落到 JVM 修饰符上：成员字段 `seen` 和顶层 `topFlag`（落在文件类 `learn.kotlin1719.volat.VolatileFieldKt` 上）都满足 `Modifier.isVolatile(...) == true`。
- `Volatile::class.java.name` 实测是 `kotlin.jvm.Volatile`（typealias 不产生新注解类），断言里同时容忍 `kotlin.concurrent.Volatile` 以防平台实现变化。
- 行为层面**不要写赌睡眠的可见性测试**。本样本做的是确定性验证：写线程 `bumpTo(42)` 后 `countDown()`，主线程 `ready.await()` + `worker.join()` 再读，happens-before 由 `join()` 提供，与 `@Volatile` 无关 —— 这条断言的作用正是反证"可见性没法这样量"。
