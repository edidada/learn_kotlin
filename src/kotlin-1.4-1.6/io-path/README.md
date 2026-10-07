# io-path

归属：[实] 1.5

要覆盖：`kotlin.io.path` 包整批进 stdlib：`Path.readText`/`writeText`/`copyTo`/`deleteIfExists`/`fileSize`、`Path.pathString`/`name`/`extension`。注意 `File.toPath()`/`Path.toFile()` 是 JDK 自带，stdlib 这边只有 `URI.toPath()`。

复核归属（数据在仓库里，直接跑）：

```bash
awk -F'\t' '$3=="pathString" || $3=="readText" || $3=="writeText" || $3=="copyTo" || $3=="fileSize" || $3=="toPath"{print $1"|"$2"|"$6}' docs/_data/slices/kotlin_io_path.tsv
```

写法约定：`.kt` 放本目录，包名统一 `learn.kotlin1416.*`（目录名里的 `-` 和 `.` 不能当包段，所以包名去掉它们）；一个知识点一个 `fun main()`，先猜输出再跑。

跑法：`./gradlew run -PmainClass=learn.kotlin1416.<文件>KT`（`src/kotlin-*` 已注册进 Gradle 的 main source set，`gradlew build` 会一并编译）。不要手拼 `java -cp`：只挂 kotlin-stdlib 会漏掉 `org.jetbrains:annotations`，报成 `kotlin/ranges/RangesKt` 的 NoClassDefFoundError，看着像 stdlib 缺类，其实是 classpath 不全（本机实测踩过）。

## 实测结论（本机 Kotlin 2.1.10 / JDK 17，跑 `./gradlew run -PmainClass=learn.kotlin1416.iopath.IoPathKt`）

通过，输出：`kotlin.io.path(1.5) OK: dir=C:\Users\wdidada\AppData\Local\Temp\k1416-8539654216718984060`。

- 切片复核：`awk -F'\t' '$1=="1.5" && $3 ~ /^(readText|writeText|copyTo|fileSize|extension|div)$/{print $3"|"$4}' docs/_data/slices/kotlin_io_path.tsv | sort -u` —— 整包 since=1.5。
- 最大的坑：`useDirectoryEntries` 递给回调的是**惰性 Sequence**，不是 List。直接 `entries.map { it.name }.sorted() == listOf(...)` 实测为 false，探针打印回调入参得到的是 `kotlin.sequences.SequencesKt___SequencesKt$sorted$1@6d86b085`。必须 `.toList()` 再比。对照 `listDirectoryEntries()` 就真的是 `List<Path>`（`size == 2` 直接可用）。
- `fileSize()` 是字节数：`"kotlin 1.5"` 10 个 ASCII 字符 = `10L`；这条留给"字节 vs 字符"的直觉校准（编码相关的更细口径见 `docs/kotlin-stdlib/05-io-path-encoding.md`）。
- `/` 运算符（`div`）拼路径可用：`base / "note.txt"`；`Paths.get(note.toString()) == note` 成立，Path 的相等看路径本身。
- `deleteIfExists()` 的幂等语义实测：第一次 `true`，第二次 `false`，不抛异常 —— 收尾清理可以直接遍历调用。
- `name` / `extension` / `isRegularFile` / `appendText` / `forEachLine` 都在同一个包（`kotlin.io.path.*`），逐个 import 才能用，写 `import kotlin.io.path.*` 会顺带把 `div` 也带进来。
- 样本自带临时目录清理（`finally` 里删条目再删目录），Windows 下跑完 `%TEMP%` 不残留 `k1416-*`。
