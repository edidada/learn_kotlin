package learn.kotlin1416.iopath

import java.nio.file.Paths
import kotlin.io.path.absolutePathString
import kotlin.io.path.appendText
import kotlin.io.path.copyTo
import kotlin.io.path.deleteIfExists
import kotlin.io.path.div
import kotlin.io.path.exists
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.forEachLine
import kotlin.io.path.isRegularFile
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.useDirectoryEntries
import kotlin.io.path.writeText

// kotlin.io.path 整包 since=1.5：Path.readText/writeText/copyTo/fileSize/deleteIfExists、
// pathString/name/extension、createTempFile/createTempDirectory、div 运算符 `/`。
// 复核：awk -F'\t' '$1=="1.5" && $3 ~ /^(readText|writeText|copyTo|fileSize|extension|div)$/{print $3"|"$4}' docs/_data/slices/kotlin_io_path.tsv | sort -u

fun main() {
    val base = kotlin.io.path.createTempDirectory(prefix = "k1416-")
    val note = base / "note.txt"                       // div 运算符拼路径
    try {
        check(!note.exists())
        note.writeText("kotlin 1.5")
        check(note.readText() == "kotlin 1.5")
        check(note.fileSize() == 10L)                  // 纯 ASCII：10 字符 = 10 字节
        check(note.name == "note.txt" && note.extension == "txt")
        check(note.isRegularFile())

        val copy = base / "copy.txt"
        note.copyTo(copy)
        check(copy.readText() == "kotlin 1.5")

        note.appendText(" tail")                       // 追加写
        check(note.readText().endsWith(" tail"))
        note.forEachLine { check(it.startsWith("kotlin")) }

        check(base.listDirectoryEntries().size == 2)
        // 实测：useDirectoryEntries 递给回调的是**惰性 Sequence**，不 toList 就没法直接比较
        check(base.useDirectoryEntries { entries -> entries.map { it.name }.sorted().toList() == listOf("copy.txt", "note.txt") })

        check(note.absolutePathString().isNotEmpty())
        check(Paths.get(note.toString()) == note)      // Path 的相等看的是路径本身

        check(note.deleteIfExists())
        check(!note.deleteIfExists())                  // 第二次返回 false（实测语义）
    } finally {
        copyOfCleanup(base)
    }
    println("kotlin.io.path(1.5) OK: dir=$base")
}

private fun copyOfCleanup(dir: java.nio.file.Path) {
    dir.listDirectoryEntries().forEach { it.deleteIfExists() }
    dir.deleteIfExists()
}
