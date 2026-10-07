# kotlin-2.1：version/2.1

2.1（2024-11）实测新增 41 个 API 名 / 76 条声明。原方案没写这一档的内容，下面这份是按 `@SinceKotlin("2.1")` 实测出来的。

## 本档主题目录

- [atomics](./atomics/README.md) — [实] 2.1
- [instant-clock](./instant-clock/README.md) — [实] 2.1
- [path-walk](./path-walk/README.md) — [实] 2.1
- [subclass-opt-in](./subclass-opt-in/README.md) — [实] 2.1
- [uuid-bytes](./uuid-bytes/README.md) — [实] 2.1

版本边界实验（本档可钉）：`./gradlew compileKotlin -Plv=2.1 -Pav=2.1` 能把这一档的代码约束在该语言/API 版本上——2.1.10 编译器接受的最低取值是 1.6，实测三档对照见 `docs/git-branch-strategy.md` 第 9 节。

下一步：`git switch version/2.1`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
