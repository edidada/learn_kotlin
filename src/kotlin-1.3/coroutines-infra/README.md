# coroutines-infra

归属：[实] 1.3

要覆盖：`Continuation`/`CoroutineContext`/`suspendCoroutine` 整批戳 1.3。注意 `launch`/`async`/`withContext` 不在 stdlib，属 `kotlinx-coroutines`；这一档只写 `suspend` + `Continuation` 手撸，或加依赖。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="Continuation" || $3=="CoroutineContext" || $3=="suspendCoroutine"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_coroutines.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin13.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin13.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin13.coroutinesinfra.CoroutinesKt`）

- stdlib 只给基础设施：`suspendCoroutine` / `startCoroutine` / `Continuation` / `CoroutineContext` / `EmptyCoroutineContext` 都能用；`kotlin.coroutines.CoroutineName` 实测 Unresolved reference，直接证明命名与调度属 `kotlinx-coroutines`（这一档不引外部依赖，所以只能手撸）。
- `Continuation<Int> { ... }` 报 `No value passed for parameter 'context'`——它命中的是 stdlib 工厂函数 `Continuation(context, resumeWith)`，不是 SAM 构造（`Continuation` 有两个抽象成员，本来就不能 SAM）。写成 `Continuation<Int>(EmptyCoroutineContext) { r -> seen = r.getOrDefault(-1) }` 通过。
- 并发坑（第一次跑就挂在这）：`latch.countDown()` 放在 suspend 块末尾时先于终态 `resumeWith` 发生，主线程醒来 `tailOk` 还是 false，`check(tailOk)` 当场失败。countDown 必须放进终态 Continuation 的 `resumeWith` 里。
- `EmptyCoroutineContext + EmptyCoroutineContext === EmptyCoroutineContext`（实测同一实例），说明 CoroutineContext 这个"小 Map"对空并空做了恒等优化。
- 手撸路线可行且够用：`suspendCoroutine` 把回调转挂起，`startCoroutine(continuation)` 启动，`Thread` 负责恢复——这就是 launch 出现之前的样子。
