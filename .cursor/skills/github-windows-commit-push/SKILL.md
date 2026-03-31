---
name: github-windows-commit-push
description: Commits and pushes to GitHub from this repo on Windows with English commit messages, staging review, and dev/ artifact rules. Use when the user asks to commit, push, sync with GitHub or origin, or when preparing repository updates from PowerShell.
---

# GitHub 提交与推送（Windows）

## 术语

用户口语里的「**面试日志**」在本流程中指 **Git 提交在 GitHub 上可见的说明**（即 **commit message / git log**），与代码审查或面试备忘无关。此类说明**一律使用英文**，便于协作并避免乱码。

## 必须遵守

1. **Commit message**：主题与正文均为 **English**（推荐 [Conventional Commits](https://www.conventionalcommits.org/)，例如 `chore:`, `feat:`, `fix:`）。
2. **勿在 PowerShell 里用中文写 `git commit -m "..."`**：默认编码易导致仓库里出现乱码提交说明。若必须用中文，改用 **Git Bash** 并确保 UTF-8，或把说明写入 UTF-8 文件后 `git commit -F path/to/msg.txt`。
3. **`dev/`**：目录已在 `.gitignore` 中；`git add -A` 前确认不会把反编译产物、`.class` 等本地实验文件加入版本库。

## 推荐流程（代理执行）

1. `git status` — 看清已修改、未跟踪、已删除项。
2. `git add` — 只加入应提交的路径；发现误选的 `dev/**` 或 `*.class` 时 `git restore --staged` 并必要时补全 `.gitignore`。
3. `git diff --staged`（可选）— 确认 diff 符合预期。
4. `git commit -m "type: short English subject"` — 需要多行时再用 `-m` 第二条或 `-F`。
5. `git push origin <branch>` — 通常为 `main`。

## Commit 主题示例

- `docs: add Tailscale and RDP guide`
- `chore: ignore dev/ local artifacts`
- `fix(vite): correct proxy target for API`

## 若用户明确要求中文提交说明

优先建议英文；若坚持中文：使用 **Git Bash** 或 `git commit -F msg.txt`（`msg.txt` 为 UTF-8），并在推送前 `git log -1` 确认无乱码。
