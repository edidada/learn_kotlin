# kotlin-1.4-1.6：version/1.4-1.6

1.4（2020-08，含 1.4 的 Mixed 编译与 SAM 转换）、1.5（2021-05，无符号标量与 `sealed` 接口）、1.6（2021-11，`Duration` 与 builder 模式）。合并成一档是因为这三年的改动都还属于 K1 语义的完善期。

## 本档主题目录

- [fun-interface](./fun-interface/README.md) — [文] 1.4
- [sam-conversion](./sam-conversion/README.md) — [文] 1.4
- [trailing-comma](./trailing-comma/README.md) — [文] 1.4
- [suspend-conversion](./suspend-conversion/README.md) — [文] 1.4
- [sealed-interface](./sealed-interface/README.md) — [文] 1.5
- [inline-value-class](./inline-value-class/README.md) — [文] 1.5
- [jvm-record](./jvm-record/README.md) — [文] 1.5
- [unsigned-scalars](./unsigned-scalars/README.md) — [实] 1.5
- [io-path](./io-path/README.md) — [实] 1.5
- [duration](./duration/README.md) — [实] 1.6
- [builder-inference](./builder-inference/README.md) — [实] 1.6
- [readln-typeof](./readln-typeof/README.md) — [实] 1.6

版本边界实验（本档可钉）：`./gradlew compileKotlin -Plv=1.6 -Pav=1.6` 能把这一档的代码约束在该语言/API 版本上——2.1.10 编译器接受的最低取值是 1.6，实测三档对照见 `docs/git-branch-strategy.md` 第 9 节。

下一步：`git switch version/1.4-1.6`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
