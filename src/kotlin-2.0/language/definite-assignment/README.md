# 确定性赋值（definite assignment）

`val x: T` 先声明、后在分支里赋值时，编译器要证明"每次读取之前恰好赋值一次"。
Kotlin 2.0 的 K2 前端重写了这段控制流分析，1.x 的 K1 在这里明显更保守。

## 实测结论

测的是本目录 `definiteAssignment.kt`，三个语言版本各跑一次
`./gradlew -q --rerun-tasks compileKotlin [-Plv=...]`，只保留 `^e:` / `^w:` 行。

| 语言版本 | 前端 | 结果 |
| --- | --- | --- |
| 默认（2.1.10） | K2 | 零条 `e:`、零条 `w:`，编译通过 |
| `-Plv=1.9` | K1 | `e: .../definiteAssignment.kt:49:13 Val cannot be reassigned` |
| `-Plv=1.6` | K1 | 同上那一条（另外会带上 1.7 才有的 `T!!` 等其它目录的版本门槛错误） |

也就是说这个文件里唯一一处"K2 收、K1 拒"的写法是第 4 个函数：

```kotlin
fun doWhileFalse(b: Boolean): Int {
    val x: Int
    do {
        if (b) { x = 1 } else { x = 2 }   // 49:13 就是这里的 x = 1
    } while (false)
    return x
}
```

K1 按"循环体可能执行多次"来做分析，第二圈再执行 `x = 1` 就是对 `val` 的重复赋值，
于是直接判错，哪怕条件恒为 `false`。K2 看懂了 `while (false)` 只走一遍，接受。

通吃两个前端的写法是把赋值挪出循环，用表达式一次到位：

```kotlin
val x = if (b) 1 else 2
```

其余四种形状（`else` 分支以 `return` 收尾、`while(true)` 里 break/return 分头走、
`x = v ?: return -1`、复合 `if` 里两条路各赋值一次）在 K1/K2 上都干净通过，
说明这些并不是 1.x 的误判点。

## 追加：try/catch 里对同一个 `val` 写两次，两个前端都是 error

```kotlin
val s: String
try { s = if (b) "try" else throw IllegalStateException("boom") }
catch (e: IllegalStateException) { s = "catch" }        // 报错行
```

- K2（默认 LV）：`e: file:///.../_probe/da.kt:35:9 'val' cannot be reassigned.`
- K1（`-Plv=1.9`）：`e: file:///.../_probe/da.kt:35:9 Val cannot be reassigned`

编译器认为 `try` 里的赋值可能已经完成（异常来自赋值右侧的表达式而不是赋值本身），
所以 `catch` 里再写一次是重复赋值。结论：这种形状只能声明成 `var`，
本文件的 `tryCatchFallback` 就是这么写的。顺带一提两个前端的错误文本本身也不一样
（K2 带引号和句号：`'val' cannot be reassigned.`；K1 是 `Val cannot be reassigned`）。

## 追加：2.0 宣传的 "this 逃逸分析"，编译器侧实测没有诊断

Kotlin 2.0 的发布说明提到改进了构造期间 `this` 逃逸的检查。本机 2.1.10 上试了这些形状，
`--rerun-tasks` 强制重编后按 `^e:` / `^w:` 过滤：

- `open class` 的 `init { touch(this) }`
- `abstract class` / `open class` 的 `init` 里调用 `abstract` 或 `open` 成员
- `open val` / `open var` 在 `init` 里读出来传给外部函数
- `init { Handler().accept(this) }`（接收者有 `open` 成员）
- `init { registry = this }`（存进可变全局）
- `init { blocks += { sink(this) } }`（lambda 捕获后存进字段）
- 匿名对象 `object : Any() { init { sink(this) } }`
- 次构造器里 `sink(this)`

默认 LV（K2）与 `-Plv=1.9`（K1）下**一条告警都没有**。
所以这条特性在 2.1.10 的命令行编译器上不是以诊断形式落地的（IDE 检查是另一回事），
这里记录的是负结果，别照着发布说明去等一个 `w:` 行。

## 运行

```
definite assignment OK: ifElseReturn=1/2 breakReturn=1/2 elvis=-1/9 doWhile=1/2 tryCatch=try/catch:boom nested=both/flag-only/neither
```

复现要注意两点：

- 必须带 `--rerun-tasks`，否则 Gradle 按输入哈希判 up-to-date，`w:`/`e:` 都不会出现——那是"没编译"，不是"没告警"。
- 过滤编译输出时 `^e:` 和 `^w:` 要同时留，只抓 `e:` 会把 K2 的告警漏掉（这个坑在前面几节已经踩过一次）。
