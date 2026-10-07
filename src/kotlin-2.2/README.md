# kotlin-2.2：version/2.2

2.2（2025-06）实测新增 22 个 API 名 / 55 条声明，按包分布：kotlin 25、kotlin.text 24、kotlin.time 2、kotlin.sequences 2、kotlin.io.encoding 1、kotlin.experimental 1（条数口径含重载展开）。

## 本档主题目录

- [context-parameters](./context-parameters/README.md) — [实] 2.2（API）/ [文]（语言）
- [must-use-return-value](./must-use-return-value/README.md) — [实] 2.2
- [hex-format](./hex-format/README.md) — [实] 2.2
- [base64-stable](./base64-stable/README.md) — [实] 2.2
- [expect-refinement](./expect-refinement/README.md) — [实] 2.2
- [sequence-of-overload](./sequence-of-overload/README.md) — [实] 2.2

版本边界实验（本档可钉）：`./gradlew compileKotlin -Plv=2.2 -Pav=2.2` 能把这一档的代码约束在该语言/API 版本上——2.1.10 编译器接受的最低取值是 1.6，实测三档对照见 `docs/git-branch-strategy.md` 第 9 节。

下一步：`git switch version/2.2`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
