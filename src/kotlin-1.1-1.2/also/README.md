# also

归属：[实] 1.1

要覆盖：`also`/`apply` 是 1.1 补的，`let`/`with`/`run`/`to` 早在 1.0 就有；把作用域函数全家桶按版本戳排一遍。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="also" || $3=="apply" || $3=="let" || $3=="run" || $3=="with"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.also.ScopeFunctionsKt`）

- `also` 返回接收者本身（`T`），lambda 形参是 `it`（名字），所以链式末尾还能保持原类型：`mutableListOf(1,2,3).also { it.add(4) }.also { ... }.size == 4`。
- 遮蔽实测：`Builder().also { it.value = 7 }` 里的 `it` 明确指向外部 `Builder`；换成 `apply` 时同名的接收者成员会抢走 `this`——这就是"日志和校验用 also"的依据。
- inline 能力：`fun zeroOrNull(n: Int): Int? = n.also { if (it == 0) return null }` 编译通过且行为正确（0 → null，5 → 5）。非局部 return 只在 inline 函数里合法，而切片里 `also` 的 sig 正是 `public inline fun <T> T.also(block: (T) -> Unit): T`。
- `also` 只做副作用、不改类型，因此可以和 1.1 的另一对 `takeIf`/`takeUnless` 直接串联。
