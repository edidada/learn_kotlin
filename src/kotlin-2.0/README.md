# kotlin-2.0：version/2.0-k2

2.0（2024-05）：K2 成为默认编译器。目录按原方案保留 `language/`、`stdlib/`、`compiler/{k1-vs-k2,fir,ir,diagnostics}` 的分层，`stdlib/` 下再按实测的 2.0 戳补齐具体主题。

## 本档主题目录

- [language](./language/README.md) — [文] 2.0
- [type-inference](./type-inference/README.md) — [文] 2.0
- [smart-cast](./smart-cast/README.md) — [文] 2.0
- [migration](./migration/README.md) — [文] 2.0
- [compiler/k1-vs-k2](./compiler/k1-vs-k2/README.md) — [文]
- [compiler/fir](./compiler/fir/README.md) — [文]
- [compiler/ir](./compiler/ir/README.md) — [文]
- [compiler/diagnostics](./compiler/diagnostics/README.md) — [文]
- [stdlib/uuid](./stdlib/uuid/README.md) — [实] 2.0
- [stdlib/auto-closeable](./stdlib/auto-closeable/README.md) — [实] 2.0
- [stdlib/enum-entries-fn](./stdlib/enum-entries-fn/README.md) — [实] 2.0
- [stdlib/copy-visibility](./stdlib/copy-visibility/README.md) — [实] 2.0
- [stdlib/padding-option](./stdlib/padding-option/README.md) — [实] 2.0
- [stdlib/remove-range](./stdlib/remove-range/README.md) — [实] 2.0

版本边界实验（本档可钉）：`./gradlew compileKotlin -Plv=2.0 -Pav=2.0` 能把这一档的代码约束在该语言/API 版本上——2.1.10 编译器接受的最低取值是 1.6，实测三档对照见 `docs/git-branch-strategy.md` 第 9 节。

下一步：`git switch version/2.0-k2`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
