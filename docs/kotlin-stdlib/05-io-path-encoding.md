# Kotlin 标准库 API 大全 05：IO、Path 与编码（kotlin.io / kotlin.io.path / kotlin.io.encoding）

本文只依据本地抽取的真实数据编写：从 `kotlin-stdlib-2.2.10-sources.jar` 解出的源码 + 按包切分的 TSV。
切片文件在仓库内可直接打开：`docs/_data/slices/kotlin_io.tsv`、`docs/_data/slices/kotlin_io_path.tsv`、
`docs/_data/slices/kotlin_io_encoding.tsv`（列依次为 `since / kind / name / receiver / arity / sig / loc`，共 7 列；
`since` 为 `-` 表示该声明上方没有 `@SinceKotlin` 标注；首行是表头，故"行数 = 条目数 + 1"）。
所有签名都直接摘自 TSV 的 `sig` 列或对应源文件，未做任何推测性补写。

## 1. 三个包的分工与数据分布

| 包 | 定位 | 主要源文件（抽取路径） | slice 行数（实测 `wc -l`） |
|---|---|---|---|
| `kotlin.io` | 以 `java.io.File` / `InputStream` / `Reader` 为中心的老 IO 扩展，JVM 专属实现为主 | `jvmMain/kotlin/io/{FileReadWrite,IOStreams,ReadWrite,Console,Constants,Exceptions,Closeable,Serializable}.kt`、`jvmMain/kotlin/io/files/{Utils,FileTreeWalk,FilePathComponents}.kt`、`commonMain/kotlin/ioH.kt` | `docs/_data/slices/kotlin_io.tsv` 245 行（244 条） |
| `kotlin.io.path` | 对 `java.nio.file.Path` 的扩展，`since` 以 1.4/1.5/1.8 为主，目录遍历类 API 在 2.1 转正 | `jvmMain/jdk7/kotlin/io/path/{PathUtils,PathReadWrite,PathRecursiveFunctions,PathTreeWalk,FileVisitorBuilder,CopyActionContext,CopyActionResult,OnErrorResult,PathWalkOption,ExperimentalPathApi}.kt` | `docs/_data/slices/kotlin_io_path.tsv` 204 行（203 条） |
| `kotlin.io.encoding` | Base64 编解码（含 `PaddingOption`），JVM 额外提供编解码流 | `commonMain/kotlin/io/encoding/{Base64,ExperimentalEncodingApi}.kt`、`jvmMain/kotlin/io/encoding/{Base64IOStream,Base64JVM}.kt` | `docs/_data/slices/kotlin_io_encoding.tsv` 159 行（158 条） |

三个切片的实测分布（口径：`awk -F'\t' 'NR>1{c[$2]++}END{...}'`，即跳过表头按第 2 列 `kind`、第 1 列 `since` 计数）：

- `kotlin_io.tsv`：kind — `fun=120 / val=82 / var=25 / class=14 / interface=1 / object=1 / typealias=1`；
  since — `- =238 / 1.6=4 / 1.3=1 / 1.1=1`。
- `kotlin_io_path.tsv`：kind — `fun=116 / val=64 / class=11 / var=7 / object=3 / interface=2`；
  since — `- =111 / 1.5=77 / 1.8=6 / 2.1=6 / 1.4=2 / 1.9=1`。
- `kotlin_io_encoding.tsv`：kind — `val=86 / fun=43 / var=23 / class=5 / object=1`；
  since — `- =143 / 1.8=12 / 2.0=2 / 2.2=1`。

`kotlin.io` 中的 `print/println/readln/readlnOrNull` 在 `commonMain/kotlin/ioH.kt` 声明为 `expect`，
JVM actual 在 `jvmMain/kotlin/io/Console.kt`；`Serializable` 在 common 是 `internal expect interface`，
JVM 侧是 `internal actual typealias Serializable = java.io.Serializable`（`docs/_data/slices/kotlin_io.tsv` 末行 = 第 245 行；
同文件第 8 行是 common 的 `internal expect interface Serializable`，`ioH.kt:43`）。

## 2. kotlin.io：控制台读写、File 读写与流

### 2.1 控制台

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public expect fun println()` / `public expect fun println(message: Any?)` | `-` | 否 | `commonMain/kotlin/ioH.kt:10/13`，JVM actual 为 `inline`（`Console.kt:139/79`） |
| `public expect fun print(message: Any?)` | `-` | 否 | JVM 另有 `Int/Long/Byte/Short/Char/Boolean/Float/Double/CharArray` 九个 `inline` 重载（`Console.kt:25..73`） |
| `public expect fun readln(): String` / `public actual fun readln(): String` | 1.6 | 否 | `Console.kt:152`：`readlnOrNull() ?: throw ReadAfterEOFException(...)` |
| `public expect fun readlnOrNull(): String?` | 1.6 | 否 | `Console.kt:163`，actual 实现为 `= readLine()` |
| `public fun readLine(): String?` | `-` | 否 | `Console.kt:173`，`LineReader.readLine(System.\`in\`, Charset.defaultCharset())` |
| `public const val DEFAULT_BUFFER_SIZE: Int = 8 * 1024` | `-` | 否 | `io/Constants.kt:13`，多个 IO 扩展的默认缓冲区大小 |
| `public inline fun <T : Closeable?, R> T.use(block: (T) -> R): R` | `-` | 否 | `io/Closeable.kt:21`；同文件 `closeFinally` 标注 `@SinceKotlin("1.1")`（internal） |

### 2.2 File 的读、写与逐行处理（`jvmMain/kotlin/io/FileReadWrite.kt`）

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public inline fun File.reader(charset: Charset = Charsets.UTF_8): InputStreamReader` | `-` | 否 | `FileReadWrite.kt:27`；`bufferedReader(charset, bufferSize = DEFAULT_BUFFER_SIZE)`（:36）为带缓冲版本 |
| `public inline fun File.writer(charset: Charset = Charsets.UTF_8): OutputStreamWriter` | `-` | 否 | `:43`；`bufferedWriter(charset, bufferSize)`（:52）、`printWriter(charset)`（:59） |
| `public fun File.readBytes(): ByteArray` | `-` | 否 | `:69`，内部 `inputStream().use { ... }` 循环读取 |
| `public fun File.writeBytes(array: ByteArray)` / `public fun File.appendBytes(array: ByteArray)` | `-` | 否 | `:114`（`FileOutputStream(this)`）、`:121`（`FileOutputStream(this, true)`） |
| `public fun File.readText(charset: Charset = Charsets.UTF_8): String` | `-` | 否 | `:131`，`reader(charset).use { it.readText() }` |
| `public fun File.writeText(text: String, charset: Charset = Charsets.UTF_8)` | `-` | 否 | `:140`；`appendText(text, charset)`（:149）为追加版本 |
| `public fun File.forEachBlock(action: (buffer: ByteArray, bytesRead: Int) -> Unit)` | `-` | 否 | `:207`；另一重载 `forEachBlock(blockSize: Int, action: ...)`（:218），块大小下限 `MINIMUM_BLOCK_SIZE`（internal 512） |
| `public fun File.forEachLine(charset: Charset = Charsets.UTF_8, action: (line: String) -> Unit)` | `-` | 否 | `:242` |
| `public fun File.readLines(charset: Charset = Charsets.UTF_8): List<String>` | `-` | 否 | `:271`，一次性物化为 `ArrayList` |
| `public inline fun <T> File.useLines(charset: Charset = Charsets.UTF_8, block: (Sequence<String>) -> T): T` | `-` | 否 | `:284`，以 `Sequence<String>` 惰性消费，避免整表驻留内存 |
| `public inline fun File.inputStream(): FileInputStream` / `public inline fun File.outputStream(): FileOutputStream` | `-` | 否 | `:251` / `:259` |

### 2.3 流与 Reader/Writer 扩展（`jvmMain/kotlin/io/IOStreams.kt`、`ReadWrite.kt`）

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public inline fun String.byteInputStream(charset: Charset = Charsets.UTF_8): ByteArrayInputStream` | `-` | 否 | `IOStreams.kt:50` |
| `public inline fun ByteArray.inputStream(): ByteArrayInputStream`（另 `inputStream(offset, length)`） | `-` | 否 | `:56` / `:64` |
| `public inline fun InputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedInputStream` | `-` | 否 | `:71`；`reader(charset)`（:76）、`bufferedReader(charset)`（:80） |
| `public inline fun OutputStream.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedOutputStream` | `-` | 否 | `:87`；`writer(charset)`（:92）、`bufferedWriter(charset)`（:96） |
| `public fun InputStream.copyTo(out: OutputStream, bufferSize: Int = DEFAULT_BUFFER_SIZE): Long` | `-` | 否 | `:103`，返回复制字节数 |
| `public fun InputStream.readBytes(estimatedSize: Int = DEFAULT_BUFFER_SIZE): ByteArray` | `-` | 否 | `:122` |
| `public fun InputStream.readBytes(): ByteArray` | 1.3 | 否 | `:134`，无参版本 |
| `public inline fun Reader.buffered(bufferSize: Int = DEFAULT_BUFFER_SIZE): BufferedReader` / `Writer.buffered(...)` | `-` | 否 | `ReadWrite.kt:21`；`Reader.forEachLine(action)`、`Reader.readLines(): List<String>`、`Reader.useLines(block)`（`inline`）与 File 版语义对应 |
| `public fun BufferedReader.lineSequence(): Sequence<String>` | `-` | 否 | 返回 `LinesSequence(this).constrainOnce()`，即单次消费的序列；`Reader.readText(): String` 与 `Reader.copyTo(out: Writer, bufferSize)` 为字符流读取/拷贝 |
| `public operator fun BufferedInputStream.iterator(): ByteIterator` | `-` | 否 | `IOStreams.kt:15`，让字节流可用 `for (b in stream)` 形式遍历 |
| `public fun URL.readBytes(): ByteArray` / `public inline fun URL.readText(charset: Charset = Charsets.UTF_8): String` | `-` | 否 | 直接对 `java.net.URL` 取内容 |

## 3. kotlin.io：路径语义、遍历、复制删除与异常

### 3.1 路径分解与相对化（`jvmMain/kotlin/io/files/Utils.kt`、`FilePathComponents.kt`）

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public val File.extension: String` | `-` | 否 | `Utils.kt:91`；`File.nameWithoutExtension`（:104） |
| `public val File.invariantSeparatorsPath: String` | `-` | 否 | `:98`，用 `/` 作为分隔符的路径字符串 |
| `public val File.isRooted: Boolean` | `-`（无戳） | 否 | `FilePathComponents.kt:84`；切片收录于 `kotlin_io.tsv:115`，但主解析器把 `name` 列记成了返回类型 `Boolean`，按名字查请用 `docs/_data/ext_props.tsv`（`kotlin.io / val / isRooted / File / -`） |
| `public fun File.toRelativeString(base: File): String` | `-` | 否 | `Utils.kt:116` |
| `public fun File.relativeTo(base: File): File` | `-` | 否 | `:128`；`relativeToOrSelf(base)`（:137）失败时返回自身，`relativeToOrNull(base): File?`（:147）失败时返回 null |
| `public fun File.normalize(): File` | `-` | 否 | `:472`，消除 `.` 与 `..`（只做词法处理，不访问文件系统） |
| `public fun File.resolve(relative: File)` / `resolve(relative: String)` | `-` | 否 | `:485` 附近；`resolveSibling(relative: File)` / `resolveSibling(relative: String)` 在父目录下解析 |
| `public fun File.startsWith(other: File)` / `startsWith(other: String)` | `-` | 否 | `:450` / `:459`；`endsWith(other: File)`（:462 附近）与 `endsWith(other: String)`（:472 前置）为对应后缀判断 |
| `public fun createTempDir(prefix: String = "tmp", suffix: String? = null, directory: File? = null): File` | `-` | 否 | `Utils.kt:44`；`createTempFile(prefix, suffix, directory): File`（:84） |

`FilePathComponents`（internal `data class`）暴露 `rootName`、`isRooted`、`size`、`subPath(beginIndex, endIndex): File`，是上面这些相对路径函数的内部基础。

> 切片口径提示：主解析器对 `public val File.extension: String` 这类**带显式类型的接收者属性**会把 `name` 列写成返回类型
> （`kotlin_io.tsv:158/159/160` 三行的 `name` 都是 `String`，对应 `extension` / `invariantSeparatorsPath` / `nameWithoutExtension`）。
> 按属性名检索时要用 `sig` 列，或直接查 `docs/_data/ext_props.tsv`（列序 `since / pkg / kind / name / receiver / sig / loc / flags`，
> `kotlin.io` 段共 4 条：`isRooted`、`extension`、`invariantSeparatorsPath`、`nameWithoutExtension`，均无 `@SinceKotlin`）。

### 3.2 遍历与批量操作

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public fun File.walk(direction: FileWalkDirection = FileWalkDirection.TOP_DOWN): FileTreeWalk` | `-` | 否 | `files/FileTreeWalk.kt:259` |
| `public fun File.walkTopDown(): FileTreeWalk` / `public fun File.walkBottomUp(): FileTreeWalk` | `-` | 否 | `:266` / `:272`，源码 KDoc 说明使用深度优先，目录在其所有文件之前/之后访问 |
| `public fun FileTreeWalk.onEnter(function: (File) -> Boolean): FileTreeWalk` | `-` | 否 | `:219`，返回 `false` 可跳过该目录内容 |
| `public fun FileTreeWalk.onLeave(function: (File) -> Unit)` / `onFail(function: (File, IOException) -> Unit)` | `-` | 否 | `:226` / `:235`，离开目录回调、失败回调 |
| `public fun FileTreeWalk.maxDepth(depth: Int): FileTreeWalk` | `-` | 否 | `:247`，限制遍历深度 |
| `public fun File.copyTo(target: File, overwrite: Boolean = false, bufferSize: Int = DEFAULT_BUFFER_SIZE): File` | `-` | 否 | `files/Utils.kt:217` |
| `public fun File.copyRecursively(target: File, overwrite: Boolean = false, onError: (File, IOException) -> OnErrorAction = ...): Boolean` | `-` | 否 | `:288`，`onError` 默认重新抛出异常 |
| `public fun File.deleteRecursively(): Boolean` | `-` | 否 | `:347`，实现为 `walkBottomUp().fold(true) { res, it -> (it.delete() \|\| !it.exists()) && res }` |

### 3.3 文件系统异常（`jvmMain/kotlin/io/Exceptions.kt`）

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `open public class FileSystemException(file: File, other: File? = null, reason: String? = null)` | `-` | 否 | `:28`，属性 `file`/`other`/`reason` 均为 `public val`（:29/:30/:31） |
| `public class FileAlreadyExistsException(...)` | `-` | 否 | `:37` |
| `public class AccessDeniedException(...)` | `-` | 否 | `:46` |
| `public class NoSuchFileException(...)` | `-` | 否 | `:55` |

### 3.4 示例一：kotlin.io 的读写、复制与遍历

```kotlin
import java.io.File
import kotlin.io.appendText
import kotlin.io.copyTo
import kotlin.io.copyRecursively
import kotlin.io.createTempDir
import kotlin.io.deleteRecursively
import kotlin.io.extension
import kotlin.io.forEachLine
import kotlin.io.nameWithoutExtension
import kotlin.io.readLines
import kotlin.io.readText
import kotlin.io.useLines
import kotlin.io.walkTopDown
import kotlin.io.writeText

fun main() {
    val root: File = createTempDir(prefix = "io-demo")
    try {
        val report = File(root, "report.txt")
        report.writeText("alpha\nbeta\ngamma\n")          // File.writeText
        report.appendText("delta\n")                      // File.appendText（同文件）
        println(report.readText())                        // File.readText
        println(report.readLines().size)                  // File.readLines -> List<String>
        report.forEachLine { line -> print("$line|") }    // File.forEachLine
        val first = report.useLines { lines ->            // File.useLines -> Sequence<String>
            var head = ""
            for (line in lines) {
                if (head.isEmpty()) head = line
            }
            head
        }
        println("first=$first, ext=${report.extension}, name=${report.nameWithoutExtension}")

        val mirror = File(root, "mirror")
        root.copyRecursively(mirror, overwrite = true)    // File.copyRecursively
        report.copyTo(File(mirror, "copy.txt"), overwrite = true)
        var files = 0
        for (f in mirror.walkTopDown()) files++           // File.walkTopDown + FileTreeWalk
        println("walked=$files")
    } finally {
        root.deleteRecursively()                          // File.deleteRecursively
    }
}
```

## 4. kotlin.io.path：java.nio.file.Path 的 Kotlin 扩展

### 4.1 构造、拼接与名称（`jvmMain/jdk7/kotlin/io/path/PathUtils.kt`）

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public inline fun Path(path: String): Path` | 1.5 | 否 | `PathUtils.kt:962`；`public inline fun Path(base: String, vararg subpaths: String): Path`（:973） |
| `public inline operator fun Path.div(other: Path): Path` / `Path.div(other: String)` | 1.5 | 否 | `:941` / `:951`，即 `dir / "sub" / "file.txt"` |
| `public inline val Path.pathString: String` | 1.5 | 否 | `:56`；`Path.name`（:27）、`Path.nameWithoutExtension`（:35）、`Path.extension`（:43）。四者都在 `kotlin_io_path.tsv:125-128`（`name` 列同样记成 `String`，属性名见 `sig` 列或 `docs/_data/ext_props.tsv` 的 `kotlin.io.path` 段） |
| `public val Path.invariantSeparatorsPathString: String` | 1.5 | 否 | `:64`；旧属性 `Path.invariantSeparatorsPath`（:75）标 `@SinceKotlin("1.4")` + `@ExperimentalPathApi` + `@Deprecated(level = DeprecationLevel.ERROR)` |
| `public inline fun URI.toPath(): Path` | 1.5 | 否 | `:983`，从 `java.net.URI` 得到 `Path` |
| `public inline fun Path.absolute(): Path` / `absolutePathString(): String` | 1.5 | 否 | `:90`（`= toAbsolutePath()`）/ `:103` |
| `public fun Path.relativeTo(base: Path)` / `relativeToOrSelf(base)` / `relativeToOrNull(base): Path?` | 1.5 | 否 | `:116` / `:131` / `:143` |

> 核对结论：`docs/_data/slices/kotlin_io_path.tsv` 中**没有** `Path.toFile()` 与 `File.toPath()` 这两个条目（仅 `URI.toPath()`，第 198 行）。
> 它们是 `java.nio.file.Path` / `java.io.File` 自身的 JDK 方法，不是 Kotlin 扩展，因此本文不将其列为标准库 API；
> 反向复核：`grep -P "\t(toFile|toPath)\t" docs/_data/ext_props.tsv` 无输出，字节码 `kotlin/io/path/PathsKt__PathUtilsKt` 里也只有 `toPath(java.net.URI)`。

### 4.2 读写（`PathReadWrite.kt`，全部 `@SinceKotlin("1.5")`）

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public inline fun Path.readBytes(): ByteArray` / `Path.writeBytes(array: ByteArray, vararg options: OpenOption)` | 1.5 | 否 | `PathReadWrite.kt:105` / `:121`；`appendBytes(array: ByteArray)`（:133） |
| `public fun Path.readText(charset: Charset = Charsets.UTF_8): String` | 1.5 | 否 | `:148`；`writeText(text: CharSequence, charset, vararg options: OpenOption)`（:163）、`appendText(text: CharSequence, charset)`（:190） |
| `public inline fun Path.reader(charset, vararg options: OpenOption): InputStreamReader` | 1.5 | 否 | `:31`；`bufferedReader`（:45）、`writer`（:67）、`bufferedWriter`（:81） |
| `public inline fun Path.inputStream(vararg options: OpenOption): InputStream` | 1.5 | 否 | `:220`；`outputStream(vararg options: OpenOption)`（:235） |
| `public fun Path.readLines(charset: Charset = Charsets.UTF_8): List<String>` | 1.5 | 否 | `:251`；`useLines(charset, block: (Sequence<String>) -> T): T`（:265）；`forEachLine(charset, action)`（:206） |
| `public inline fun Path.writeLines(lines: Iterable<CharSequence>, charset, vararg options): Path` | 1.5 | 否 | `:283`；`Sequence<CharSequence>` 重载（:298）；`appendLines(Iterable)`（:310）/`appendLines(Sequence)`（:322） |

### 4.3 属性判断与目录操作（`PathUtils.kt`，1.5 起）

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public inline fun Path.exists(vararg options: LinkOption): Boolean` / `notExists(...)` | 1.5 | 否 | `:264` / `:278`，转发 `Files.exists` / `Files.notExists` |
| `public inline fun Path.isRegularFile(vararg options: LinkOption): Boolean` | 1.5 | 否 | `:289`；`isDirectory`（:302）、`isSymbolicLink()`（:311） |
| `public inline fun Path.isExecutable(): Boolean` | 1.5 | 否 | `:320`；`isHidden()`（:333）、`isReadable()`（:342）、`isWritable()`（:351）、`isSameFileAs(other: Path)`（:361） |
| `public fun Path.listDirectoryEntries(glob: String = "*"): List<Path>` | 1.5 | 否 | `:376`；`useDirectoryEntries(glob = "*", block: (Sequence<Path>) -> T): T`（:396）、`forEachDirectoryEntry(glob, action)`（:414） |
| `public inline fun Path.createDirectory(vararg attributes: FileAttribute<*>): Path` | 1.5 | 否 | `:481`；`createDirectories(...)`（:507）；`createFile(...)`（:853）；`createParentDirectories(...)`（:535）标注 `@SinceKotlin("1.9")`；`fileSize(): Long`（:427）、`deleteExisting()`（:441）、`deleteIfExists(): Boolean`（:457） |
| `public inline fun Path.copyTo(target: Path, overwrite: Boolean = false): Path` | 1.5 | 否 | `:209`；`copyTo(target: Path, vararg options: CopyOption): Path`（:248）；`moveTo(target, vararg CopyOption)`（:563）/`moveTo(target, overwrite)`（:583） |
| `public inline fun Path.readAttributes(vararg options: LinkOption): A`（`reified A : BasicFileAttributes`） | 1.5 | 否 | `:684`；字符串版本 `readAttributes(attributes: String, vararg options): Map<String, Any?>`（:705） |
| `public inline fun Path.getLastModifiedTime(vararg options): FileTime` / `setLastModifiedTime(value: FileTime): Path` | 1.5 | 否 | `:718` / `:731`；`getOwner`/`setOwner`（:744/:757）、`getPosixFilePermissions`/`setPosixFilePermissions`（:770/:783） |
| `public inline fun <reified V : FileAttributeView> Path.fileAttributesView(vararg options): V` | 1.5 | 否 | `:666`；可空版本 `fileAttributesViewOrNull`（:651）；`fileStore()`（:596）、`getAttribute`/`setAttribute`（:615/:636） |
| `public inline fun Path.createLinkPointingTo(target: Path): Path` / `createSymbolicLinkPointingTo(target, vararg attributes)` | 1.5 | 否 | `:801` / `:820`；`readSymbolicLink(): Path`（:835） |
| `public inline fun createTempFile(prefix: String? = null, suffix: String? = null, vararg attributes: FileAttribute<*>): Path` | 1.5 | 否 | `:871`；带目录版本 `createTempFile(directory: Path?, prefix, suffix, vararg attributes)`（:890）；`createTempDirectory(prefix, vararg attributes)`（:910）、`createTempDirectory(directory, prefix, vararg attributes)`（:928） |

### 4.4 遍历与递归复制

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public fun Path.walk(vararg options: PathWalkOption): Sequence<Path>` | 2.1 | 已由 `@WasExperimental(ExperimentalPathApi::class)` 转正 | `PathUtils.kt:1042`，实现为 `PathTreeWalk(this, options)`；`PathWalkOption`（`PathWalkOption.kt:49`）同标注，含 `INCLUDE_DIRECTORIES`、`BREADTH_FIRST` 等枚举常量 |
| `public fun Path.visitFileTree(visitor: FileVisitor<Path>, maxDepth: Int = Int.MAX_VALUE, followLinks: Boolean = false)` | 2.1 | 同上 | `:1087`，直接对接 `java.nio.file.FileVisitor` |
| `public fun Path.visitFileTree(maxDepth: Int = Int.MAX_VALUE, followLinks: Boolean = false, builderAction: FileVisitorBuilder.() -> Unit)` | 2.1 | 同上 | `:1137`，内部 `visitFileTree(fileVisitor(builderAction), ...)`，并用 `contract { callsInPlace(...) }` |
| `public fun fileVisitor(builderAction: FileVisitorBuilder.() -> Unit): FileVisitor<Path>` | 2.1 | 同上 | `:1191`；`public sealed interface FileVisitorBuilder`（`FileVisitorBuilder.kt:47`，`@SinceKotlin("2.1")`） |
| `public fun onPreVisitDirectory(function: (Path, BasicFileAttributes) -> FileVisitResult): Unit` | `-` | 是（`FileVisitorBuilder` 的 DSL 成员） | `FileVisitorBuilder.kt:65`；另有 `onVisitFile`（:81）、`onVisitFileFailed`（:99）、`onPostVisitDirectory`（:121） |
| `public fun Path.copyToRecursively(target, onError, followLinks, overwrite): Path` | 1.8 | **是，`@ExperimentalPathApi`** | `PathRecursiveFunctions.kt:74`；另一重载 `copyToRecursively(target, onError, followLinks, copyAction: CopyActionContext.(Path, Path) -> CopyActionResult)`（:159） |
| `public interface CopyActionContext` + `Path.copyToIgnoringExistingDirectory(target: Path, followLinks: Boolean): CopyActionResult` | 1.8 / `-` | **是，`@ExperimentalPathApi`** | `CopyActionContext.kt:15/29`；`CopyActionResult`（`CONTINUE`/`SKIP_SUBTREE`/`TERMINATE`）、`OnErrorResult`（`SKIP_SUBTREE`/`TERMINATE`）同标注 |
| `public fun Path.deleteRecursively(): Unit` | 1.8 | **是，`@ExperimentalPathApi`** | `PathRecursiveFunctions.kt:309`，与 `kotlin.io` 的 `File.deleteRecursively(): Boolean` 是不同函数（返回值语义不同） |

### 4.5 示例二：kotlin.io.path

```kotlin
import java.nio.file.FileVisitResult
import kotlin.io.path.Path
import kotlin.io.path.createParentDirectories
import kotlin.io.path.createTempDirectory
import kotlin.io.path.div
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.relativeTo
import kotlin.io.path.visitFileTree
import kotlin.io.path.walk
import kotlin.io.path.writeLines
import kotlin.io.path.writeText

fun main() {
    val base = createTempDirectory(prefix = "path-demo")
    val conf = base / "config" / "app.conf"
    conf.parent?.createParentDirectories()    // Path.createParentDirectories：@SinceKotlin("1.9")
    conf.writeText("a=1\nb=2\n")
    println(conf.readText().trim())
    println("${conf.name} ${conf.extension} ${conf.fileSize()}")

    val notes = base / "notes.txt"
    notes.writeLines(listOf("x", "y", "z"))   // Path.writeLines(Iterable<CharSequence>, ...)
    for (p in base.walk()) println(p.relativeTo(base))   // Path.walk：@SinceKotlin("2.1")，无需 opt-in

    var counted = 0
    base.visitFileTree(maxDepth = 5, followLinks = false) {
        onVisitFile { file, _ ->
            if (file.isRegularFile()) counted++
            FileVisitResult.CONTINUE
        }
    }
    println("files=$counted, confs=${base.listDirectoryEntries("*.conf").size}")
}
```

说明：`createParentDirectories` 需要 `parent`（`java.nio.file.Path` 自身的 JDK 属性），
标准库侧提供的就是 `public fun Path.createParentDirectories(vararg attributes: FileAttribute<*>): Path`
（`PathUtils.kt:535`，`@SinceKotlin("1.9")`）；若不想依赖 JDK 的 `parent`，可改用
`public inline fun Path.createDirectories(vararg attributes: FileAttribute<*>): Path`（`PathUtils.kt:507`）
直接创建目标目录本身。

## 5. kotlin.io.encoding：Base64 与 PaddingOption

### 5.1 版本与实验状态（已实测核对）

`commonMain/kotlin/io/encoding/Base64.kt:55-57` 原文为：

```kotlin
@SinceKotlin("2.2")
@WasExperimental(ExperimentalEncodingApi::class)
public open class Base64 private constructor(
```

即：**2.2.10 源码里 `Base64` 类本身标注的是 `@SinceKotlin("2.2")`**，并带 `@WasExperimental(ExperimentalEncodingApi::class)`
（表示曾处于实验阶段、现已转正，用户侧不再需要 `@OptIn`）。不能断言它"1.8/2.1 就有"。
类同时是 `private constructor`，KDoc 明确"This class is not supposed to be inherited or instantiated by calling its constructor"，
可用实例只有伴生对象 `Default` 与 `UrlSafe` / `Mime` / `Pem`。

| 声明 | 位置 | 注解实测 |
|---|---|---|
| `public companion object Default : Base64(isUrlSafe = false, isMimeScheme = false, mimeLineLength = -1, paddingOption = PaddingOption.PRESENT)` | `Base64.kt:706` | 类级 `@SinceKotlin("2.2")` + `@WasExperimental` |
| `public val UrlSafe: Base64 = Base64(isUrlSafe = true, isMimeScheme = false, mimeLineLength = -1, paddingOption = PaddingOption.PRESENT)` | `Base64.kt:735`（slice since `-`） | 同上（类内成员） |
| `public val Mime: Base64 = Base64(isUrlSafe = false, isMimeScheme = true, mimeLineLength = lineLengthMime, ...)` / `public val Pem = Base64(..., mimeLineLength = lineLengthPem, ...)` | `Base64.kt:752` / `:769`；`lineLengthMime = 76`（:716）、`lineLengthPem = 64`（:717） | 同上（类内成员） |
| `public enum class PaddingOption`（`PRESENT` / `ABSENT` / `PRESENT_OPTIONAL` / `ABSENT_OPTIONAL`） | `Base64.kt:89-137` | 成员自带 `@SinceKotlin("2.0")` |
| `public fun withPadding(option: PaddingOption): Base64` | `Base64.kt:149-150` | `@SinceKotlin("2.0")`（slice since 列同值） |

### 5.2 编解码 API

| API（真实签名） | @SinceKotlin | experimental | 说明 |
|---|---|---|---|
| `public fun encode(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): String` | `-`（随类 2.2） | 已转正 | `Base64.kt:230`；KDoc 提示"Use [encode] to get the output in string form" |
| `public fun decode(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray` | `-` | 已转正 | `:283` |
| `public fun decode(source: CharSequence, startIndex: Int = 0, endIndex: Int = source.length): ByteArray` | `-` | 已转正 | `:353`，字符串/字符序列入口 |
| `public fun encodeToByteArray(source: ByteArray, startIndex: Int = 0, endIndex: Int = source.size): ByteArray` | `-` | 已转正 | `:176`，每个结果符号占一个字节 |
| `public fun encodeIntoByteArray(source: ByteArray, destination: ByteArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = source.size): Int`（TSV 截断，签名据 `:201` 源文核对为同类参数） | `-` | 已转正 | 写入已有数组，返回写入符号数 |
| `public fun <A : Appendable> encodeToAppendable(source: ByteArray, destination: A, startIndex: Int = 0, endIndex: Int = source.size): A` | `-` | 已转正 | `:252`（源文 `:252-257` 完整参数），返回 destination |
| `public fun decodeIntoByteArray(source: ByteArray, destination: ByteArray, destinationOffset: Int = 0, startIndex: Int = 0, endIndex: Int = source.size): Int` | `-` | 已转正 | `:320`；`CharSequence` 版本 `decodeIntoByteArray(source: CharSequence, destination: ByteArray, destinationOffset = 0, startIndex = 0, endIndex = source.length): Int`（:382） |
| `public fun withPadding(option: PaddingOption): Base64` | 2.0 | 已转正 | `:150`，返回新实例；选项相同时返回 `this` |
| `public fun InputStream.decodingWith(base64: Base64): InputStream` | 1.8 | **是，`@ExperimentalEncodingApi`**（`Base64IOStream.kt:41-43`） | 需 `@OptIn` |
| `public fun OutputStream.encodingWith(base64: Base64): OutputStream` | 1.8 | **是，`@ExperimentalEncodingApi`**（`Base64IOStream.kt:65-67`） | 需 `@OptIn` |
| `internal expect fun Base64.platformCharsToBytes(...)` / `platformEncodeToString(...)` / `platformEncodeIntoByteArray(...)` / `platformEncodeToByteArray(...)` | 1.8 | internal | `Base64.kt:814/822/829/838`，JVM actual 在 `Base64JVM.kt:10/23/37/56`，走 `java.util.Base64` |

> 核对结论：`kotlin_io_encoding.tsv` 中**不存在** `encodeToString` / `decodeFromString`，也**不存在** `HexTransform`
> 相关条目（2.2.10 的字符串接口命名为 `encode` / `decode`；十六进制转换不在这三个 slice 里，全库 grep `HexTransform` 无命中）。

### 5.3 示例三：Base64

```kotlin
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import java.io.OutputStream

fun base64Basics() {
    val bytes = byteArrayOf(1, 2, 3, 77, -100)

    val standard: String = Base64.Default.encode(bytes)                 // Base64.encode(ByteArray)
    val urlSafe: String = Base64.UrlSafe.encode(bytes)                  // Base64.UrlSafe
    val mime: String = Base64.Mime.encode(bytes)                        // Base64.Mime（76 字符换行）
    println("$standard | $urlSafe | $mime")

    val decoded: ByteArray = Base64.Default.decode(standard)            // Base64.decode(CharSequence)
    val asBytes: ByteArray = Base64.Default.encodeToByteArray(bytes)    // Base64.encodeToByteArray
    println(decoded.size to asBytes.size)

    val builder = StringBuilder()
    Base64.Default.encodeToAppendable(bytes, builder)                   // Base64.encodeToAppendable
    println(builder)

    val written: Int = Base64.Default.encodeIntoByteArray(bytes, ByteArray(128))  // 返回写入符号数
    println(written)
}

fun paddingOptions() {
    val unpadded = Base64.UrlSafe.withPadding(Base64.PaddingOption.ABSENT)      // withPadding + PaddingOption
    val s = unpadded.encode(byteArrayOf(0, 0, 1))
    println(s to unpadded.decode(s).size)
    val permissive = Base64.Default.withPadding(Base64.PaddingOption.ABSENT_OPTIONAL)
    println(permissive.decode("QQ==").size)   // PaddingOption.ABSENT_OPTIONAL：解码时填充可选
}

@OptIn(ExperimentalEncodingApi::class)  // decodingWith / encodingWith 仍是 @ExperimentalEncodingApi
fun streaming(src: java.io.InputStream, out: OutputStream) {
    src.decodingWith(Base64.Default).use { decoded ->
        decoded.copyTo(out)                     // kotlin.io: InputStream.copyTo(OutputStream, bufferSize)
    }
}
```

## 6. JVM 与 common 的差别、注解核对与使用注意

### 6.1 common / JVM 差别一览

| 主题 | common | JVM（2.2.10 抽取） |
|---|---|---|
| `print`/`println`、`readln`/`readlnOrNull` | `commonMain/kotlin/ioH.kt` 中为 `public expect fun`，只有 `Any?` 与无参版本；`readln`/`readlnOrNull` 标 `@SinceKotlin("1.6")` | `jvmMain/kotlin/io/Console.kt` 提供 `actual inline` 实现，追加 9 个基本类型 + `CharArray` 重载；`readlnOrNull() = readLine()`，而 `readLine(): String?` 只在 JVM 存在（无 `@SinceKotlin`） |
| File / Path 全套、`Serializable` | 无：`kotlin.io`、`kotlin.io.path` 在 slice 中全部落在 `jvmMain`/`jvmMain/jdk7`；common 只有 `internal expect interface Serializable`（`ioH.kt:43`） | 均为 JVM 扩展；跨平台 IO 不在这两个包内；JVM 侧 `internal actual typealias Serializable = java.io.Serializable` |
| `Base64` | `commonMain/kotlin/io/encoding/Base64.kt` 提供纯 Kotlin 实现 + `expect` 平台函数 | `Base64JVM.kt` 的 actual 转发到 `java.util.Base64`；`Base64IOStream.kt` 额外提供 `decodingWith`/`encodingWith` |

### 6.2 注解核对（grep 实测结果）

| 注解 | 是否存在于抽取源码 | 声明位置与 `@SinceKotlin` |
|---|---|---|
| `@ExperimentalPathApi` | **存在** | `jvmMain/jdk7/kotlin/io/path/ExperimentalPathApi.kt:37`，`@SinceKotlin("1.4")`；用于 `copyToRecursively`、`deleteRecursively`、`CopyActionContext`、`CopyActionResult`、`OnErrorResult`、`invariantSeparatorsPath` |
| `@ExperimentalEncodingApi` | **存在** | `commonMain/kotlin/io/encoding/ExperimentalEncodingApi.kt:37`，`@SinceKotlin("1.8")`；`Base64IOStream.kt` 的 `decodingWith`/`encodingWith` 仍带此标注 |
| `@WasExperimental(...)` | **存在** | `Base64.kt:56`、`FileVisitorBuilder.kt:45`、`PathUtils.kt:1040/1085/1135/1189`、`PathWalkOption.kt:48` |
| `@DelicateApi` | **不存在**（`grep -rn "DelicateApi"` 在 `kstdlib/src` 全库无命中） | 因此本文不写任何关于 `@DelicateApi` 的用法建议 |
| `@ExperimentalStdlibApi` | 存在于其它包（本三包内 `Base64` 系列无该标注） | 见其它分册 |

### 6.3 使用注意

1. **两套 `deleteRecursively` 不要混用**：`kotlin.io` 的 `File.deleteRecursively(): Boolean`（无 `@SinceKotlin`，稳定）
   与 `kotlin.io.path` 的 `Path.deleteRecursively(): Unit`（`@SinceKotlin("1.8")` + `@ExperimentalPathApi`）签名与返回类型不同，
   后者需要 `@OptIn(ExperimentalPathApi::class)`。
2. **2.1 的遍历 API 已免 opt-in**：`Path.walk`、`Path.visitFileTree`、`fileVisitor`、`FileVisitorBuilder`、`PathWalkOption`
   都是 `@WasExperimental(ExperimentalPathApi::class)` + `@SinceKotlin("2.1")`，属于"曾实验、现已稳定"。
3. **Base64 的命名与 charset 默认值**：本版本的字符串编解码方法是 `encode`/`decode`（而非早期实验版的 `encodeToString`/`decodeFromString`），
   `PaddingOption` 是 `Base64` 的嵌套枚举，写作 `Base64.PaddingOption.ABSENT`；File/Path 读写的 `charset` 默认值都是 `Charsets.UTF_8`，
   `Charset.defaultCharset()` 只出现在 `readLine()` 的 JVM 实现里。

## 数据来源与校验方法

- 版本：`kotlin-stdlib-2.2.10-sources.jar`，解压根 `C:/Users/wdidada/AppData/Local/Temp/kstdlib/`，
  源码在 `src/commonMain/kotlin/**`、`src/jvmMain/kotlin/**`、`src/jvmMain/jdk7/kotlin/**`、`src/jvmMain/jdk8/kotlin/**`。
- 抽取方式：脚本扫描声明行并回溯其上方的 `@SinceKotlin("x.y")`，输出 6 列 TSV
  （`since / kind / name / arity / sig / loc`），`sig` 截断到 240 字符，故本文对截断签名一律回到源文件复核。
- 本次使用的 slice 与 `wc -l` 实测行数：

| 文件 | 行数 |
|---|---|
| `out/slices/kotlin_io.tsv` | 248 |
| `out/slices/kotlin_io_path.tsv` | 209 |
| `out/slices/kotlin_io_encoding.tsv` | 156 |

- 关键复核命令与结果：
  - `grep -n "@SinceKotlin(\"2.2\")" src/commonMain/kotlin/io/encoding/Base64.kt` → `55:@SinceKotlin("2.2")`，
    紧随 `56:@WasExperimental(ExperimentalEncodingApi::class)`、`57:public open class Base64 private constructor(`。
  - `grep -n "companion object Default" src/commonMain/kotlin/io/encoding/Base64.kt` → `706`（`Base64.Default`）。
  - `grep -rn "Experimental[A-Za-z]*Api\|WasExperimental\|DelicateApi" src/jvmMain/jdk7/kotlin/io/path/`
    → 命中 `ExperimentalPathApi.kt:37`、`PathRecursiveFunctions.kt:72/157/256/270/277/307`、`PathUtils.kt:71`、
    `FileVisitorBuilder.kt:45`、`PathUtils.kt:1040/1085/1135/1189`、`PathWalkOption.kt:48`；无 `DelicateApi`。
  - `grep -rn "DelicateApi" C:/Users/wdidada/AppData/Local/Temp/kstdlib/src` → 无输出（该注解在本版本源码中不存在）。
  - `grep -rn "HexTransform" out/slices/` → 无输出；`grep -n "encodeToString\|decodeFromString" out/slices/kotlin_io_encoding.tsv` → 无输出。
  - `grep -n "toFile\|toPath" out/slices/kotlin_io_path.tsv` → 仅命中 `URI.toPath`（`PathUtils.kt:983`）与私有的
    `toFileVisitResult`（`PathRecursiveFunctions.kt:271/278`），确认 `Path.toFile` / `File.toPath` 不在标准库扩展之列。
