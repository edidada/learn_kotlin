package learn.kotlin1416.readln

import java.io.ByteArrayInputStream
import kotlin.reflect.KType
import kotlin.reflect.typeOf

// readln / readlnOrNull 是 1.6（readLine 从 1.0 就有）；typeOf<T>() 也是 1.6，
// 它是 kotlin.reflect 的轻量入口（不需要 kotlin-reflect.jar）。
// 复核：awk -F'\t' '$1=="1.6" && $3 ~ /^(readln|readlnOrNull|typeOf)$/{print FILENAME"|"$3}' docs/_data/slices/kotlin_io.tsv docs/_data/slices/kotlin_reflect.tsv | sort -u

fun feed(text: String) {
    System.setIn(ByteArrayInputStream(text.toByteArray()))
}

inline fun <reified T> sameClassifier(t: KType): Boolean = t.classifier == T::class

fun main() {
    val original = System.`in`
    try {
        // 1) readln：拿到整行，行不存在直接抛（1.6 的"我要一行"语义）
        feed("kotlin\n")
        check(readln() == "kotlin")

        // 2) 空行是空串，不是 null
        feed("\n")
        check(readln() == "")

        // 3) EOF：readlnOrNull 给 null，readln 抛 ReadAfterEOFException（不是 NoSuchElementException）
        // 实测坑：这个类字节码是 public（javap 可见），但 Kotlin 元数据里是 internal，
        // 所以 `is kotlin.io.ReadAfterEOFException` 直接编译不过：Cannot access ... it is internal in file
        // 只能按类名匹配，或兜住 RuntimeException。
        feed("")
        check(readlnOrNull() == null)
        feed("")
        val boom = runCatching { readln() }.exceptionOrNull()
        check(boom?.javaClass?.name == "kotlin.io.ReadAfterEOFException")
        check(boom?.message == "EOF has already been reached")
        check(boom is RuntimeException)

        // 4) 对照 1.0 的 readLine：EOF 也给 null
        feed("a\n")
        check(readLine() == "a")

        // 5) typeOf：把泛型实参带进运行期（1.6）
        // 实测：classpath 上没有 kotlin-reflect.jar 时，KType 依然可用（classifier / arguments /
        // isMarkedNullable 都对），但 toString 会退化成 Java 类型并附一句
        // "(Kotlin reflection is not available)"，所以别指望它打印出 "kotlin.String"。
        val t = typeOf<List<String>>()
        check(t.classifier == List::class)
        check(t.arguments.size == 1 && t.arguments[0].type?.classifier == String::class)
        check(sameClassifier<String>(typeOf<String>()))
        check(typeOf<String>().toString().startsWith("java.lang.String"))
        check(typeOf<String>().toString().contains("Kotlin reflection is not available"))
        check(typeOf<Int?>().isMarkedNullable)
        check(!typeOf<Int>().isMarkedNullable)
        check(runCatching { Class.forName("kotlin.reflect.full.KClasses") }.isFailure)
    } finally {
        System.setIn(original)
    }
    println("readln/typeOf(1.6) OK")
}
