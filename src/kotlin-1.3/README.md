# kotlin-1.3：version/1.3

1.3（2018-10）是标准库最大的一次扩容：实测 261 个 API 名 / 761 条声明带 `@SinceKotlin("1.3")`，同时协程基础设施（`kotlin.coroutines`）也进 stdlib。原方案把 `duration` 放在这档是错的——`Duration` 的戳是 1.6，已挪到 `src/kotlin-1.4-1.6/duration/`；1.3 的时间 API 是实验形态的 `ExperimentalTime` + `MonotonicTimeSource`（戳确为 1.3）。

## 本档主题目录

- [result](./result/README.md) — [实] 1.3
- [random](./random/README.md) — [实] 1.3
- [contracts](./contracts/README.md) — [实] 1.3
- [unsigned-arrays](./unsigned-arrays/README.md) — [实] 1.3
- [coroutines-infra](./coroutines-infra/README.md) — [实] 1.3
- [multiplatform-intro](./multiplatform-intro/README.md) — [文] 1.3
- [stdlib-batch](./stdlib-batch/README.md) — [实] 1.3

这一档**钉不了 `languageVersion`**：2.1.10 编译器对 1.6 以下的取值直接报 `error: language version X is no longer supported; please, use version 1.6 or greater`。所以早期档只能靠"自觉不用后出的 API"来约束（`-api-version` 同样有下界）。真要验证某条 API 属于哪一档，跑本目录各主题 README 里的 awk 复核命令。

下一步：`git switch version/1.3`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
