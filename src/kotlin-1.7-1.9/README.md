# kotlin-1.7-1.9：version/1.7-1.9

1.7（2022-07，K2 在 JVM 侧进 alpha、`definitelyNonNull`）、1.8（2022-12，K2 服务端、`Base64` 实验版）、1.9（2023-10，K2 beta、`data object`、时间源重写）。这是 1.x 时代的收尾档。

## 本档主题目录

- [definitely-non-nullable](./definitely-non-nullable/README.md) — [文] 1.7
- [min-max-batch](./min-max-batch/README.md) — [实] 1.7
- [deep-recursive](./deep-recursive/README.md) — [实] 1.7
- [data-object](./data-object/README.md) — [文] 1.9
- [enum-entries](./enum-entries/README.md) — [实] 1.9
- [range-until](./range-until/README.md) — [实] 1.9
- [time-source](./time-source/README.md) — [实] 1.9
- [volatile](./volatile/README.md) — [实] 1.9
- [jvm-default](./jvm-default/README.md) — [文] 1.4 起
- [k2-preview](./k2-preview/README.md) — [文] 1.7.0 alpha / 1.9.20 beta

版本边界实验（本档可钉）：`./gradlew compileKotlin -Plv=1.9 -Pav=1.9` 能把这一档的代码约束在该语言/API 版本上——2.1.10 编译器接受的最低取值是 1.6，实测三档对照见 `docs/git-branch-strategy.md` 第 9 节。

下一步：`git switch version/1.7-1.9`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
