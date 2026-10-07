# kotlin-2.3：version/2.3

本仓库的实测数据止于 `kotlin-stdlib-2.2.10-sources.jar`，所以 2.3/2.4 这两档**无法本机验证**——先把工具链升上去，再按同样方法抽 `@SinceKotlin`，别照抄任何 AI 给的清单。

## 本档主题目录

- [toolchain-upgrade](./toolchain-upgrade/README.md) — 待实测
- [stdlib-batch](./stdlib-batch/README.md) — 待实测

版本边界实验（本档可钉）：`./gradlew compileKotlin -Plv=2.2 -Pav=2.2` 能把这一档的代码约束在该语言/API 版本上——2.1.10 编译器接受的最低取值是 1.6，实测三档对照见 `docs/git-branch-strategy.md` 第 9 节。

下一步：`git switch version/2.3`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
