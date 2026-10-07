# `docs/_data` —— 实测数据与复现脚本

`docs/kotlin-versioning.md` 和 `docs/kotlin-stdlib/*.md` 里每一个数字、每一条签名，都来自下面这些文件；它们又全部来自同一份原始输入：**Gradle 缓存里的 `kotlin-stdlib` 源码 jar**。这里不放结论，只放证据和复现方法。

## 原始输入

| 文件 | 大小 | class/`.kt` 条目 | 本机位置（Gradle 缓存，哈希目录会变） |
|---|---|---|---|
| `kotlin-stdlib-2.2.10.jar` | 1,750,374 B | 970 个 `.class` | `~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.10/*/kotlin-stdlib-2.2.10.jar` |
| `kotlin-stdlib-2.2.10-sources.jar` | 698,990 B | 373 个 `.kt`（`commonMain` 199 + `jvmMain` 174） | 同目录 `…-sources.jar` |
| `kotlin-stdlib-2.1.10-sources.jar` | — | 用于跨版本 diff | `…/kotlin-stdlib/2.1.10/…-sources.jar` |
| `kotlin-stdlib-jdk7-1.7.10.jar` / `-1.8.22.jar` | — | 证明 jdk7/jdk8 artifact 被合并进主 stdlib | `…/kotlin-stdlib-jdk7/…` |

`bin_files.txt` = `jar tf kotlin-stdlib-2.2.10.jar` 的完整输出（970 个 class 条目 + 资源）；`sources_files.txt` = 源码 jar 的 `jar tf` 输出。这两个文件是"jar 层结构"的直接证据，正文里凡是说"某类在/不在 jar 里"都能在这里 grep。

## 数据文件（列序都是 `\t` 分隔）

| 文件 | 行数 | 列 | 用途 |
|---|---|---|---|
| `api.tsv`（**未入库**，2.4 MB，用脚本可再生成） | 13,045 | `since pkg kind name receiver loc sig` | 逐声明全量，含所有重载 |
| `dedup_api.tsv` | 3,486 + 表头 | `pkg kind name receiver versions overloads simplest_sig` | 去重 API 名（`包+kind+名称` 三元组），`versions` 是该名字在源码里出现过的所有 `@SinceKotlin` 值，`-` 表示无戳 |
| `api_by_version.tsv` | 1,007 + 表头 | `since pkg kind name sig` | 只保留带 `@SinceKotlin` 的名字，按版本排 |
| `slices/<包名>.tsv`（42 个文件） | 合计 13,087 | `since kind name receiver arity sig loc` | 按包切分，各分册的表都是从对应 slice 抄的真实签名 |
| `ext_props.tsv` | 88 + 表头 | `since pkg kind name receiver sig loc flags` | **扩展属性补抽**（`val Recv.name`），主解析器把这类声明的名字落在接收者上，故单独一遍；26 个名字是主切片完全没有的 |
| `count_by_pkg.tsv` | 42 | `pkg decls apis dated` | 包级统计，`00-overview.md` 的地图表 |
| `count_by_since.tsv` | 13 | `since decls` | 版本戳分布（`@SinceKotlin` 命中数） |
| `count_by_pkg_since.tsv` | — | `pkg since count` | 包 × 版本交叉表 |

`loc` 列已剥掉本机绝对路径前缀，现在是相对源码 jar 根目录的路径，例如 `commonMain/generated/_Collections.kt:3561`——拿着它可以直接在 [JetBrains/kotlin 仓库](https://github.com/JetBrains/kotlin) 里按 tag `v2.2.10` 定位同一个文件。

## 复现步骤

```bash
# 1) 找到源码 jar 并解包
SRC=$(ls ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.10/*/kotlin-stdlib-2.2.10-sources.jar | head -1)
mkdir -p /tmp/kstdlib/src && cd /tmp/kstdlib/src && jar xf "$SRC"

# 2) 主解析：api.tsv / dedup_api.tsv / slices/ / count_by_*.tsv / api_by_version.tsv
cd /tmp/kstdlib && node <本目录>/scripts/extract2.mjs /tmp/kstdlib/src /tmp/kstdlib/out2

# 3) 扩展属性补抽：out3/ext_props.tsv
node <本目录>/scripts/extract_ext.mjs /tmp/kstdlib /tmp/kstdlib/src /tmp/kstdlib/out3

# 4) 跨版本 diff（需再解一份 2.1.10 源码到 /tmp/kstdlib/src2110，跑两遍 extract2）
node <本目录>/scripts/slice.mjs /tmp/kstdlib
```

三个脚本的分工：`extract2.mjs` 逐行扫描 `.kt`，块注释状态机跳过 license header 与 KDoc，抓到 `@SinceKotlin("x.y")` 后绑定其后 6 行内的第一个声明，按 `package` 切分输出；`extract_ext.mjs` 用同一套规则只匹配 `val/var 接收者.名字`；`slice.mjs` 比较两个版本目录，算"新增/删除的 API 名"。

## 已知的坑（写在这里，免得下一个人重踩）

1. **块注释状态机的判断顺序**：license header 的结束行 ` */` 长得像 KDoc 行。若先判 KDoc，`inBlock` 会永久卡在 true，实测导致解析出的声明数从 13,045 掉到 887。`extract_ext.mjs` 第一版就栽过这个坑（只有 14 行结果）。
2. **"slice 里查不到"有两种完全不同的原因**：API 真不存在，或解析器漏了。必须双向复核——正向 `grep -rn "<名字>" src/`，反向查字节码：
   ```bash
   J=$(ls ~/.gradle/caches/modules-2/files-2.1/org.jetbrains.kotlin/kotlin-stdlib/2.2.10/*/kotlin-stdlib-2.2.10.jar | head -1)
   mkdir -p /tmp/jpx && cd /tmp/jpx && unzip -o -q "$J" "kotlin/sequences/*.class"
   javap -classpath . kotlin.sequences.SequencesKt___SequencesKt | grep -i whileTake   # 无输出 = 真不存在
   ```
   正面例子：`Path.pathString` 源码命中 + `kotlin.io.path.PathsKt__PathUtilsKt` 字节码命中，只是主切片漏了；反面例子：`whileTake`/`whileDrop`/`consume`/`distinctUntilChanged`/`cached` 两道全空，是从 RxJava / kotlinx.coroutines Flow 串过来的误记。
3. **函数体内的局部 `val`/`var` 会被当条目收录**（例如 `val iterator = iterator()`），所以 `kotlin_sequences.tsv` 里 `val=178`、`var=130` 远大于真实公开属性数；统计公开属性要结合 `sig` 列。
4. **接口体内不带修饰符的成员声明不会被收录**（解析器只认 `public/internal/expect/actual/override` 开头的行），例如 `KCallable.returnType`、`KClass.simpleName`；`kotlin_reflect.tsv` grep `returnType` 为空即为证据。这类 API 靠直接读源文件补。
5. 少数条目的 `name` 仍会落在接收者类型上（`kotlin.collections.Int`、`kotlin.text.Char`、`kotlin.time.Boolean`），肉眼剔除即可，别当 API 名统计。
