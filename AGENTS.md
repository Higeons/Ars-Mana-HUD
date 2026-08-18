# AGENTS.md — Arsenal Mana HUD 项目维护约定

本文档供 AI Agent（如 ZCode）在维护本项目时遵循。**一切以本文档为准，与通用约定冲突时以本文档为准。**

## 项目概况

- **项目**：Ars Mana HUD（`arsmanahud`）—— Minecraft 1.20.1 / Forge 47+ 的纯客户端附属模组，为 Ars Nouveau 提供魔力 HUD 增强显示。
- **GitHub 仓库**：`https://github.com/Higeons/Ars-Mana-HUD`（分支 `main`）
- **构建**：MinecraftForge Gradle（ForgeGradle 6），Java 17 编译目标
- **前置依赖**：Ars Nouveau ≥ 4.12.0（仅在 `build.gradle` 中以 compileOnly/runtimeOnly 引入，**不得打包发布**）

## 关键路径

| 用途 | 路径 |
| --- | --- |
| 主模组类 | `src/main/java/com/arsmanahud/ArsManaHud.java` |
| HUD 渲染 | `src/main/java/com/arsmanahud/client/ManaHudRenderer.java` |
| 事件处理 | `src/main/java/com/arsmanahud/client/ArsManaHudEvents.java` |
| 覆盖层注册 | `src/main/java/com/arsmanahud/client/ManaHudClient.java` |
| 版本号 | `gradle.properties` 中的 `mod_version` |
| 构建产物 | `build/libs/arsmanahud-1.20.1-<mod_version>.jar` |

## 例行变更流程（默认模式：Agent 本地提交，用户负责推送）

Agent 的职责边界：

1. **Agent 做**：分析/修改代码 → 本地验证（尽量执行 `./gradlew build`）→ `git add` → `git commit` → 向用户汇报 commit hash 与改动摘要、以及应执行的推送命令。
2. **用户做（默认）**：执行推送与发布（见下方"用户终端命令框架"）。
3. 除非用户明确要求直接推送并授予网络/非沙箱权限，否则 **Agent 不得擅自 `git push`**（环境沙箱默认无法连接 GitHub）。

### Agent 本地提交规范

- 提交信息用英文，短横线风格，前缀标识改动类型：
  `feat:`, `fix:`, `docs:`, `refactor:`, `perf:`, `chore:`（例如 `fix: regen text offset on small screens`）。
- 一次提交只做一件事，便于回滚。
- 发新版本性质的功能完成后，同步在 `gradle.properties` 更新 `mod_version`（遵循 [SemVer](https://semver.org/lang/zh-CN/)）。
- 工作区有未提交改动时，**先 `git status` 确认现状再动手**，不要丢弃用户已有改动。

### 变更前的准备

会话开始时先确认代码是最新的：

```bash
git status     # 确认工作区状态
```

若用户在别处有未推送提交，先让用户推送，或由 Agent 告知需要先处理，避免在过时基线（如 `8d3b942`）上改代码。

## 用户终端命令框架（复制到 Git Bash 使用，替换占位符）

### 场景一：只推送代码改动（最常见）

```bash
cd "E:\Minecraft_Tools\arsmanahud-1.20.1"
git status                       # 先看有哪些待推送内容
git push                         # 推送 main 到 GitHub
```

### 场景二：发布一个新版本（推代码 + 打 tag + 网页传 jar）

```bash
cd "E:\Minecraft_Tools\arsmanahud-1.20.1"
git push
git tag v0.2.1                   # 版本号与 gradle.properties 的 mod_version 保持一致
git push origin v0.2.1
```

之后打开 <https://github.com/Higeons/Ars-Mana-HUD/releases> 点击 **Draft a new release**：

1. **Choose a tag** 选择刚推送的 `v0.2.1`；
2. **Release title** 填 `Ars Mana HUD v0.2.1`；
3. 描述可粘贴 Agent 提供的发布说明草稿；
4. 把 jar 拖入 **Attach binaries**：`build\libs\arsmanahud-1.20.1-0.2.1.jar`；
5. 点击 **Publish release**。

> 提示：GitHub 上传 `.jar` 偶尔会提示"可能是恶意软件"，属常见误报，确认是自行构建的文件即可继续。

### 场景三：回滚 / 标签修正

```bash
# 回滚到上一个提交（本地）
git reset --hard HEAD~1
# 删除已推送的错误标签（远程与本地）
git push origin --delete v0.2.0
git tag -d v0.2.0
```

## 环境与权限说明

- Agent 的 Bash 沙箱默认 **无法连接 github.com**，因此 `git push` 默认由用户执行。
- 若用户希望 Agent 直接推送：当 Agent 请求"非沙箱/网络权限"时在权限弹窗选择允许；且需先在环境中完成一次 GitHub 凭据认证（如 Credential Manager），否则推送会要求输入凭据。
- 构建依赖网络（Maven / MinecraftForge 仓库），沙箱内可能失败；必要时由用户在自己终端执行 `./gradlew build`。

## 版本与分支约定

- 分支：`main`（唯一长期分支），不建议在本地开长期副分支。
- Tag 格式：`vX.Y.Z`，与 `mod_version` 一致，用附注标签（`git tag -a`）。
- 版本号只通过 `gradle.properties` 的 `mod_version` 维护，构建产物文件名会自动跟随。