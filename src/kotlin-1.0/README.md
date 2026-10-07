# kotlin-1.0：version/1.0-foundation

Kotlin 1.0 定下的语言底座。stdlib 2.2.10 里 13045 条声明中有 9732 条不带 `@SinceKotlin` 戳，那批就是 1.0 就存在的东西（判读规则见 `docs/kotlin-stdlib/00-overview.md` 第 5 节）。

## 本档主题目录

- [01-basic-syntax](./01-basic-syntax/README.md) — [文] 1.0
- [02-functions](./02-functions/README.md) — [文] 1.0
- [03-null-safety](./03-null-safety/README.md) — [文] 1.0
- [04-classes](./04-classes/README.md) — [文] 1.0
- [05-data-class](./05-data-class/README.md) — [文] 1.0
- [06-extension-functions](./06-extension-functions/README.md) — [文] 1.0
- [07-lambdas](./07-lambdas/README.md) — [文] 1.0
- [08-higher-order-functions](./08-higher-order-functions/README.md) — [文] 1.0
- [09-smart-cast](./09-smart-cast/README.md) — [文] 1.0
- [10-when](./10-when/README.md) — [文] 1.0
- [11-generics](./11-generics/README.md) — [文] 1.0
- [12-object](./12-object/README.md) — [文] 1.0
- [13-companion-object](./13-companion-object/README.md) — [文] 1.0
- [14-delegation](./14-delegation/README.md) — [文] 1.0

这一档**钉不了 `languageVersion`**：2.1.10 编译器对 1.6 以下的取值直接报 `error: language version X is no longer supported; please, use version 1.6 or greater`。所以早期档只能靠"自觉不用后出的 API"来约束（`-api-version` 同样有下界）。真要验证某条 API 属于哪一档，跑本目录各主题 README 里的 awk 复核命令。

下一步：`git switch version/1.0-foundation`，学完本档内容后 `git tag -a kotlin-...` （tag 名见 `docs/git-branch-strategy.md` 第 5 节）。
