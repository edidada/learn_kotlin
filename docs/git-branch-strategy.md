# Git 分支策略 —— 让提交历史本身成为 Kotlin 语言演进时间轴

> 前提：本仓库已经 `git init` 并完成首次提交，所以原文档"第二步：初始化仓库"整节跳过，直接从分支结构开始。
> 本页是落地版：结构 + 已建好的骨架 + 操作规则 + **和 `docs/_data` 实测数据核对过的版本归属表**（原方案里有几处版本归错，见第 4 节）。

## 1. 核心原则（一句话）

**每一个高版本 branch 都从它的直接前驱 branch 创建，而不是每次从 `main` 重新开。**

于是历史是一条串行链，后一档天然继承之前所有学习代码：

```text
main
 └─ version/1.0-foundation
      └─ version/1.1-1.2
           └─ version/1.3
                └─ version/1.4-1.6
                     └─ version/1.7-1.9
                          └─ version/2.0-k2
                               └─ version/2.1
                                    └─ version/2.2
                                         └─ version/2.3
                                              └─ version/2.4
```

不是这种并行扇出（语义完全不同，学 1.9 时看不到 1.3 的代码）：

```text
main ──┬── version/1.0
       ├── version/1.3
       └── version/1.9 ...
```

## 2. 本仓库当前骨架（实测）

10 个 `version/*` 分支已按上面的顺序串行创建完毕，创建时每一档都停在"前一档"分支上执行 `git switch -c`，所以谱系可以在 reflog 里查出来：

```bash
$ git reflog -11 --format='%gd %gs' | head -11     # HEAD reflog：逐档切换的痕迹
HEAD@{10} checkout: moving from main to version/1.0-foundation
HEAD@{9}  checkout: moving from version/1.0-foundation to version/1.1-1.2
HEAD@{8}  checkout: moving from version/1.1-1.2 to version/1.3
HEAD@{7}  checkout: moving from version/1.3 to version/1.4-1.6
...
```

注意 git 的措辞：单条分支自己的 reflog 只写 `branch: Created from HEAD`，它记的是"当时所在的 ref"而不是分支名，所以**谱系要看 HEAD reflog 的切换序列**（或用第 7 节的 `git log --graph --all` 看拓扑）。等后面各档真的提交了不同内容，前驱关系就会直接显形在提交图里。

```bash
$ git branch --format='%(refname:short) -> %(objectname:short)'
main                       -> f0bf5cd
version/1.0-foundation     -> f0bf5cd
version/1.1-1.2            -> f0bf5cd
...
version/2.4                -> f0bf5cd
```

所有 ref 现在都指向同一个 commit，这是**故意的**：分支是"学习轨道"，内容靠后续 commit 填。

这里有一次真实的传播要记录：链是在 `39e49ed` 上切出来的，而策略文档本身（`b557bfd`）和随后两处口径修订（`9588781`、`f0bf5cd`）落在 `main`，所以刚建好链时 `version/*` 全档都看不到本文。修法是第 6 节规则三的一次标准应用——低版本向后快进，且因为链上还没有任何分叉内容，每一跳都是 pure fast-forward，零 merge commit：

```bash
git switch version/1.0-foundation && git merge --ff-only main
git switch version/1.1-1.2        && git merge --ff-only version/1.0-foundation
# …逐档传到 version/2.4
```

用 `--ff-only` 而不是裸 `merge` 是有意为之：它能传播文档，又绝不在本该线性的骨架上悄悄生成 merge 节点；一旦哪一跳不能快进，说明分叉已经产生，那时才该停下来看拓扑。复查传播结果：

```bash
$ git rev-list --merges --count main..version/2.4
0                                                  # 没有引入任何 merge commit
$ git merge-base --is-ancestor version/2.3 version/2.4 && echo YES
YES                                                # 前驱确实被后继包含，逐档同理
```

开始学习时第一步：

```bash
git switch version/1.0-foundation
```

里程碑 tag **一个都没打**。理由：tag 的语义是"我学完那一档时的历史快照"（见第 5 节），现在打等于给空气盖章。等你在某个分支上真正提交完该档内容，再打：

```bash
git tag -a kotlin-1.3-learning -m "Kotlin 1.3 learning milestone"
```

## 3. 六条操作规则

1. 高版本永远从**直接前驱** branch 创建。
2. 某个版本的知识只在**对应 branch 首次加入**。
3. 低版本后补内容 → 向高版本逐级 merge（第 6 节）。
4. 高版本内容**绝不反向 merge** 给低版本，否则"1.9 学习快照"被 2.0 的东西倒灌，历史意义消失。
5. tag 表示历史快照，**不随 branch 后续移动**。
6. 纵向演进放 `version/*`，横向深入放 `topic/*`（`topic/coroutines`、`topic/flow`、`topic/compiler-k2`、`topic/jvm-internals`、`topic/java-interop`、`topic/gradle`、`topic/reflection`、`topic/serialization`、`topic/dsl`、`topic/multiplatform`，都从 `version/2.4` 开）。

## 4. 版本归属核对表（这是重点：原方案有 4 处需要改）

标注方式：`[实]` = 用本仓库 `docs/_data/slices/*.tsv`（解析自 `kotlin-stdlib-2.2.10-sources.jar` 的 `@SinceKotlin`）跑 awk 查到的；`[文]` = 语言/编译器特性，stdlib 源码里没有戳，来源是官方 What's New，本机无法实测，学到那一档时以 IDE + 编译器报错为准。

| 原方案放的目录 | 核对结果 | 说明 |
|---|---|---|
| `kotlin-1.1-1.2/type-alias` | ✅ `[文]` 1.1 | 同批还有 `takeIf`/`takeUnless`/`also` —— `[实]` 这三个在 stdlib 里戳都是 `1.1`，放对了 |
| `kotlin-1.1-1.2/multiplatform-intro` | ⚠️ 建议挪到 `kotlin-1.3/` | 1.1/1.2 是"common 模块 + JS 试验"阶段，真正能拿来入门的 MPP（common stdlib + Native）是 1.3 才成型的；放 1.3 更贴合"能跑起来" |
| `kotlin-1.3/result` | ✅ `[实]` `Result` = `@SinceKotlin("1.3")` | |
| `kotlin-1.3/random` | ✅ `[实]` `Random` = `1.3` | |
| `kotlin-1.3/contracts` | ✅ `[实]` `contract` = `1.3`（当时 `@ExperimentalContracts`） | |
| `kotlin-1.3/unsigned-types` | ⚠️ 拆两半 | `[实]` 无符号**数组**（`UIntArray`/`UByteArray`…）= `1.3`；无符号**标量类型本体**（`UInt`/`UShort`/`UByte`/`ULong`）= `1.5`。所以 1.3 只写数组与 `rangeTo`/`step`，标量放 `kotlin-1.4-1.6/unsigned-scalars/` |
| **`kotlin-1.3/duration`** | ❌ 错档，必须挪 | `[实]` `Duration`/`DurationUnit` = `@SinceKotlin("1.6")`，`toDuration` = `1.6`。1.3 那批时间 API 是实验形态的 `ExperimentalTime`(`1.3`) + `MonotonicTimeSource`(`1.3`)；`TimeSource`/`measureTime` 是 `1.9` 重构后的东西。建议：`duration/` 移到 `kotlin-1.4-1.6/`，`kotlin-1.3/` 里只留一句"1.3 有实验版时间测量"的 README 注记 |
| `kotlin-1.3/coroutines-intro` | ✅ `[实]` stdlib 里的 `kotlin.coroutines` 基础设施（`Continuation`/`CoroutineContext`/`suspendCoroutine`）整批 = `1.3`；但 `launch`/`async`/`withContext` **不在 stdlib**，属 `kotlinx-coroutines`（已在 `07-contracts-coroutines-reflect-jvm.md` grep 证实），示例要么加依赖要么只写 `suspend` + `Continuation` |
| `kotlin-1.4-1.6/fun-interface`、`sam-conversion`、`trailing-comma`、`suspend-conversion` | ✅ `[文]` 均 1.4 | |
| `kotlin-1.4-1.6/sealed-interface`、`value-class`、`jvm-record` | ✅ `[文]` 均 1.5（`value class` 关键字是 1.9 由 `inline class` 改名而来，1.4–1.6 档先按 `inline class` 学，1.9 档补改名） |
| `kotlin-1.4-1.6/jvm-record` | ✅ `[文]` `@JvmRecord` 1.5，需要 JDK 14+ 才有效果 —— 本仓库 CI 是 JDK 17，可以直接跑 |
| `kotlin-1.4-1.6/builder-inference` 与 `kotlin-1.7-1.9/builder-inference` | ⚠️ 重复 | `[实]` `buildList`/`buildMap`/`buildSet` = `1.6`（`BuilderInference` 注解本身 1.3 就有）。只保留 `kotlin-1.4-1.6/builder-inference/` 一处 |
| `kotlin-1.7-1.9/definitely-non-nullable` | ✅ `[文]` 1.7（`T!!` 类型标注） |
| `kotlin-1.7-1.9/data-object`、`range-until` | ✅ `rangeUntil` `[实]` = `1.9`；`data object` `[文]` = 1.9 |
| `kotlin-1.7-1.9/enum-entries` | ⚠️ 跨档 | `[实]` `EnumEntries` 类型 = `1.9`，`enumEntries()` 顶层函数 = `2.0`（1.8 里那条 1.8 戳的是 internal 预研）。建议 1.9 档写 `EnumEntries`/`entries`，把 `enumEntries()` 留给 `version/2.0-k2` |
| `kotlin-1.7-1.9/jvm-default` | ✅ `[文]` 1.4 引入、1.4 起 `-jvm-default` 可选，2.0 后成为默认方向之一；放 1.7–1.9 学习没问题，但记一句"最早是 1.4" |
| `kotlin-1.7-1.9/k2-preview` | ✅ `[文]` K2 在 1.7.0 成为 JVM 侧 alpha、1.9.20 beta、2.0.0 默认（时间线见 `kotlin-versioning.md`） |

复核任一条，直接跑：

```bash
awk -F'\t' '$3=="Duration"{print $1, $2, $3, $7}' docs/_data/slices/kotlin_time.tsv
awk -F'\t' '$3=="rangeUntil"{print $1, $3}' docs/_data/slices/kotlin_ranges.tsv
```

列序 `since kind name receiver arity sig loc`，`since` 为 `-` 表示源码里没写 `@SinceKotlin`（按惯例即 1.0 就有）。

## 5. 每档的标准操作节奏

```bash
git switch version/1.0-foundation           # 从前一档切过来
#   写代码 → 一个知识点一个 commit
git add src/kotlin-1.0/03-null-safety && git commit -m "learn: add null safety examples"
#   该档学完，就地打 tag（不要回头改它）
git tag -a kotlin-1.0-learning -m "Kotlin 1.0 learning milestone"
#   下一档从这一档开
git switch -c version/1.1-1.2
```

commit message 沿用统一前缀，`git log --oneline` 自己就变成学习台账：`learn:` / `experiment:` / `docs:` / `fix:` / `build:`。

## 6. 低版本发现遗漏怎么补（唯一有技术含量的地方）

已经学到 `version/2.4`，发现 `version/1.3` 的 `Result` 示例漏了 `runCatching`：

```bash
git switch version/1.3
git add -A && git commit -m "learn: complete Kotlin 1.3 Result examples"
```

此刻历史真分叉（1.3 分支多出一个 commit，2.x 各线看不到它）。要把它传播下去，就**从低往高逐级 merge**：

```bash
git switch version/1.4-1.6 && git merge version/1.3
git switch version/1.7-1.9 && git merge version/1.4-1.6
git switch version/2.0-k2  && git merge version/1.7-1.9
git switch version/2.1     && git merge version/2.0-k2
git switch version/2.2     && git merge version/2.1
git switch version/2.3     && git merge version/2.2
git switch version/2.4     && git merge version/2.3
```

方向永远是低→高。反向 merge（`git switch version/1.3 && git merge version/2.4`）会把 2.x 的代码灌进 1.3 快照，这一档就废了。

如果某次补漏**不该**出现在高版本（例如你只想修正 1.3 的表述，不想让后面各档继承），那就不 merge，留着分叉，tag 已经把当时状态钉住了：

```bash
git switch --detach kotlin-1.3-learning    # 看当时快照
git switch version/2.4                     # 看完回来
```

## 7. 常用查看命令

```bash
git branch                                  # 看版本线
git log --graph --oneline --decorate --all  # 看整棵演进树（本仓库最常用）
git diff version/1.3..version/1.7-1.9       # 比较两档
git diff --name-status version/1.7-1.9..version/2.0-k2   # 只看文件增删
```

## 8. 当前待推清单（尚未 push）

远端是 `origin git@github.com:edidada/learn_kotlin.git`。`main` 领先 `origin/main` **4 个 commit**，且 10 个 `version/*` 分支只存在于本地：

```text
f0bf5cd docs: distinguish dedup and raw slice scopes for kotlin package counts
9588781 docs: repoint io-path-encoding evidence at in-repo slices
b557bfd docs: add version-branch strategy and verified feature-to-version table
39e49ed add docs
```

```bash
git push origin main                                  # 上面 4 个 commit
git push origin --tags                                # git tag --list 实测为 0，暂无 tag 可推
git push origin 'refs/heads/version/*:refs/heads/version/*'   # 10 个分支骨架，当前全指向 f0bf5cd
```

因为骨架阶段 10 条分支与 `main` 同指一个 commit，推上去只是 10 个 ref 指针，不产生新对象；等各档提交了自己的学习内容，分支才在远端真正分叉。按你的习惯，push 等你单独下指令再执行。
