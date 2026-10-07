# kotlin-1.1-1.2：version/1.1-1.2

1.1（2017-01）与 1.2（2017-12）两档合并：主线是函数类型/库函数补齐 + common 模块多平台雏形。原方案里的 `multiplatform-intro` 我挪到了 `src/kotlin-1.3/`，理由见 `docs/git-branch-strategy.md` 第 4 节。

## 本档主题目录

- [type-alias](./type-alias/README.md) — [文] 1.1
- [take-if](./take-if/README.md) — [实] 1.1
- [take-unless](./take-unless/README.md) — [实] 1.1
- [also](./also/README.md) — [实] 1.1
- [bound-callable-reference](./bound-callable-reference/README.md) — [文] 1.1
- [grouping-collectors](./grouping-collectors/README.md) — [实] 1.1
- [content-deep-equality](./content-deep-equality/README.md) — [实] 1.1
- [chunked-windowed](./chunked-windowed/README.md) — [实] 1.2
- [math-batch](./math-batch/README.md) — [实] 1.2
- [streams-1.2](./streams-1.2/README.md) — [实] 1.2
- [jvm-default-annotation](./jvm-default-annotation/README.md) — [实] 1.2

这一档**钉不了 `languageVersion`**：2.1.10 编译器对 1.6 以下的取值直接报 `error: language version X is no longer supported; please, use version 1.6 or greater`。所以早期档只能靠"自觉不用后出的 API"来约束（`-api-version` 同样有下界）。真要验证某条 API 属于哪一档，跑本目录各主题 README 里的 awk 复核命令。

下一步：`git switch version/1.1-1.2`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
