# jvm-default

归属：[文] 1.4 起

要覆盖：`-jvm-default` 三种模式（`disable`/`enable`/`all`）与 `@JvmDefault`（1.2 注解戳）的关系；2.0 后 `all` 成为方向。放在本档学，但记一句最早是 1.4。


写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1719.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1719.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17）

- README 原来写的三种模式 `disable`/`enable`/`all` 是**错的**。本机实测 `enable` 直接被拒：
  `e: Unknown -Xjvm-default mode: enable, supported modes: [disable, all-compatibility, all]`
  正确清单是 `disable` / `all-compatibility` / `all`。
- 旧注解戳 `@JvmDefault` 也已废弃，编译信息原话：
  `'annotation class JvmDefault : Annotation' is deprecated. Switch to new -Xjvm-default modes: 'all' or 'all-compatibility'.`
- 注入开关的办法（Gradle 里）：init script 用 `tasks.matching { it.name == "compileKotlin" }.configureEach { kotlinOptions { freeCompilerArgs += ["-Xjvm-default=" + mode] } }`，跑 `./gradlew -q -I <init> -Pjvmdefmode=<mode> build`。注意 `tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile)` 写法在这套 KGP 上拿不到 `kotlinOptions`，别用。
- 三种模式的字节码指纹（同一个样本 `learn.kotlin1719.jvmdef.JvmDefaultModesKt`，字段依次是：接口默认方法经接口引用调用的 `bridge` / 伴生接口同样的调用 `companionBridge` / 接口方法在 JVM 上是否真的是 default `ifaceDefault` / 实现类里是否重复生成了 `implBridges`）：

  | `-Xjvm-default` | bridge | companionBridge | ifaceDefault | implBridges | `methods` |
  |---|---|---|---|---|---|
  | `disable`（= 2.1 默认） | true | false | false | true | `[(greet,false),(hello,false)]` |
  | `all-compatibility` | true | false | true | false | `[(greet,true),(hello,true)]` |
  | `all` | false | false | true | false | `[(greet,true),(hello,true)]` |

  默认（不传 `-Pjvmdefmode`）与 `disable` 指纹一致：`bridge=true companionBridge=false ifaceDefault=false implBridges=true`。
- 读法：`disable` 走 DefaultImpls 静态类，所以调用点必须生成桥、实现类里重复出一份方法；`all-compatibility` 把接口方法变成真正的 JVM default（`isDefault` true），同时**保留** DefaultImpls 兼容层（所以旧二进制不会炸，这就是 "compatibility" 的含义）；`all` 连兼容层都去掉（`bridge=false`，纯 JVM default），代价是重新编译前编出来的调用方会 `IncompatibleClassChangeError` 级别的不兼容。
- 观测坑：`java.lang.reflect.Modifier` **没有** `isDefault`，判断默认方法要用 `java.lang.reflect.Method.isDefault()`。
- 样本断言刻意写成模式无关：`check(ifaceIsDefault || bridge.isSuccess)` 加行为断言，所以同一份代码在三种模式都能跑，指纹靠打印而不是硬编码。
