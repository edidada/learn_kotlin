# stdlib/copy-visibility

归属：[实] 2.0

要覆盖：`@ConsistentCopyVisibility`/`@ExposedCopyVisibility`：data class `copy` 与私有构造的可见性一致性检查。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="ConsistentCopyVisibility" || $3=="ExposedCopyVisibility"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin20.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin20.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin20.copyvis.CopyVisibilityKt`）

输出：`copy visibility(2.0) OK: secured=[private final] exposed=[public final] bare=[public final] runtimeAnnos=[Metadata] retention=SOURCE`

- 两个注解在字节码里是 **SOURCE 保留**：`ConsistentCopyVisibility::class.java.getAnnotation(java.lang.annotation.Retention::class.java)?.value == SOURCE`，
  类上的运行期注解只剩 `@kotlin.Metadata` —— 也就是说这功能是纯编译期的，反射看不到标记，只能看 `copy` 的可见性。
- `copy` 的实际可见性（反射量出来的，三种情况各一条）：
  | 声明 | `copy` 的修饰符 |
  |---|---|
  | 不加注解（`Bare`） | `public final`（默认就是暴露的） |
  | `@ConsistentCopyVisibility` | **`private final`** |
  | `@ExposedCopyVisibility` | `public final` |
- 编译期告警（K2 2.1.10，逐字）：
  - 声明处：`w: Non-public primary constructor is exposed via the generated 'copy()' method of the 'data' class.`（只有不加注解的那个类报；`@ConsistentCopyVisibility`/`@ExposedCopyVisibility` 都不报）
  - 调用处：`w: This 'copy()' exposes the non-public primary constructor of a 'data class'. Please migrate the usage. ... This will become an error in Kotlin 2.2.`
  - **注意**：调用处的告警对 `@ExposedCopyVisibility` 的类**照样发**（实测本文件两行调用都报），所以"标了 Exposed 就安静"是错的：Exposed 只让声明处安静，使用点仍然提醒迁移，且 2.2 起升级为 error。
- 调用被 `@ConsistentCopyVisibility` 私有化后的 copy，编译器原话（实测就在同文件里，跨文件自然也一样）：
  `e: Cannot access 'fun copy(id: Int = ..., label: String = ...): Secured': it is private in 'learn/kotlin20/copyvis/Secured'.`
- 顺带一条 data class 反射常识：`declaredConstructors` 不止一个，除私有主构造器外还有带 `DefaultConstructorMarker` 的合成构造器，
  所以 `declaredConstructors.single()` 会抛 `IllegalArgumentException: Array has more than one element.`（实测踩过），要按 `parameterCount` 过滤。
