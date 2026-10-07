# jvm-default-annotation

归属：[实] 1.2

要覆盖：`@JvmDefault` 注解本身戳在 1.2（后被判 deprecated），而 `-jvm-default` 编译选项的语义是 1.4 才成型的——学到 `src/kotlin-1.7-1.9/jvm-default/` 时记得回来对照。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="JvmDefault"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_jvm.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1112.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1112.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1112.jvmdefault.JvmDefaultKt`）

- 切片证据：`awk -F'\t' '$3=="JvmDefault"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv` → `1.2|class|public annotation class JvmDefault`。
- 注解在 2.1.10 还在，但已经不能贴在接口上，实测两条真实报错：`This annotation is not applicable to target 'interface'. Applicable targets: function, property`（@Target 相比 1.2 缩水），以及 `'annotation class JvmDefault : Annotation' is deprecated. Switch to new -Xjvm-default modes: all or all-compatibility.`
- A/B 实测（同一份 `interface Plain`，只改编译参数）：
  - 默认模式：产出 `Plain.class` + `Plain$DefaultImpls.class`，`javap` 看到 `public abstract java.lang.String hello();`，运行时 `Class.forName("...Plain$DefaultImpls")` 成功。
  - `-Xjvm-default=all`：不再生成 `Plain$DefaultImpls`，`javap` 看到 `public default java.lang.String hello();`（接口 flags `0x0601` = ACC_PUBLIC|ACC_INTERFACE|ACC_ABSTRACT），`Class.forName` 失败——桥接类真的消失了。
- 复现命令：`./gradlew compileKotlin -I <init.gradle>`，init 脚本里要用 `tasks.matching { it.name == "compileKotlin" }`；直接写 `tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile)` 在 init 脚本里会报 `Could not get unknown property 'org'`，因为 KGP 不在 init 脚本的 classpath 上。
- 分层结论：接口的默认实现是 1.0 的语言能力（Kotlin 侧行为两种模式完全一致），1.2 的 `@JvmDefault` 只管 JVM 字节码怎么编码；2.x 的正解是编译器级 `-Xjvm-default=all`，注解本身已废弃。
