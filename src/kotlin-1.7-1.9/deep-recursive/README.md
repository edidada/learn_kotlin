# deep-recursive

归属：[实] 1.7

要覆盖：`DeepRecursiveFunction`/`DeepRecursiveScope`/`callRecursive`：堆外栈递归改写，用 10 万层嵌套验证它不 StackOverflow。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="DeepRecursiveFunction" || $3=="DeepRecursiveScope" || $3=="callRecursive"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1719.deep.DeepRecursiveKt`）

输出：`deep-recursive(1.7) OK: plain=StackOverflowError deep100k=100000 deep1M=1000000 fib20=6765`

- 同一个深度（10 万层）普通递归 `plainRec` 直接 `StackOverflowError`，`DeepRecursiveFunction` 版本正常返回 100000；再乘 10（100 万层）也照样过 —— 机制是把递归搬进堆上的显式栈，不吃 JVM 线程栈。
- 递归 lambda 内部直接写 `callRecursive(n - 1)`，它是 `DeepRecursiveScope` 的成员，不需要限定名。
- 深嵌套 `List` 求和同样能改写：`callRecursive(item as List<Any>)`。**必须显式转换** —— `item is List<*>` 智能转换后仍是 `List<*>`，直接传给要 `List<Any>` 的 `callRecursive` 编译不过。实测 `nested(50_000)`（5 万层嵌套）返回 1。
- 分支返回两个 `callRecursive` 也可以（fib 写法），`withStack(20) == 6765` 对上标准值。
- 代价侧的直觉：`callRecursive` 是函数调用+堆栈对象分配，浅递归用它只会更慢，它只在"会爆栈"的深度上划算。

### 追加：`callRecursive` 传嵌套集合时的 unchecked cast

原来直接写 `callRecursive(item as List<Any>)`，编译器给：

```
w: Unchecked cast of 'kotlin.collections.List<*>' to 'kotlin.collections.List<kotlin.Any>'.
```

处理方式是把它收进一个单独的 `@Suppress("UNCHECKED_CAST")` 辅助函数（`asAnyList`），让抑制范围只覆盖那一行转换，
而不是给整个 `deepSum` 或整个文件加抑制 —— 这个转换在擦除后确实成立（元素本来就是 `List<*>`），但类型系统给不出这个证明。
