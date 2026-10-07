package learn.kotlin10.dataclass

// Kotlin 1.0：data class。归属 [文] 1.0。
// 跑：./gradlew run -PmainClass=learn.kotlin10.dataclass.DataClassKt

data class User(val id: Int, val name: String) {
    var loginCount: Int = 0                 // 关键坑：不在主构造里的属性，不参与 equals/hashCode/toString/copy
    val tag: String get() = "#$id"          // 计算属性同理，只影响它自己
}

fun main() {
    val u = User(1, "ada")
    val same = User(1, "ada")

    // 1) 自动生成成员：equals/hashCode/toString/copy/componentN 都只认主构造参数
    println("toString -> $u")
    println("equals（结构相等）-> ${u == same}，但并非同一对象：${u !== same}")
    check(u == same && u.hashCode() == same.hashCode())

    // 2) copy 只改得起主构造参数
    val renamed = u.copy(name = "grace")
    println("copy -> $renamed，id 保持 ${renamed.id}")

    // 3) 类体里的 var 不属于比较范围——"看起来不同，equals 却相等"
    u.loginCount = 42
    println("改了 loginCount 之后 -> u=$u same=$same, u==same 仍然 ${u == same}")
    check(u == same)

    // 4) 解构声明 = componentN 的语法糖，位置由主构造参数顺序决定
    val (id, name) = u
    println("解构 -> id=$id name=$name")

    // 5) copy 不会带上你之后改过的普通属性
    val copied = u.copy()
    println("copy() 后 loginCount = ${copied.loginCount}（原对象是 ${u.loginCount}）")
    check(copied.loginCount == 0)

    // 6) 计算属性不进 toString，容易误判"数据丢了"
    println("tag 是计算属性 -> ${u.tag}，但 toString 里没有它")
}
