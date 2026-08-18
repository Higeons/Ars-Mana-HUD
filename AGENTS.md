# AGENTS.md — Ars Mana HUD 项目维护约定

本文档供 AI Agent（如 ZCode）在维护本项目时遵循。**一切以本文档为准，与通用约定冲突时以本文档为准。**

## 项目概况

- 本项目实际上维护 **两个相互独立** 的 Minecraft 模组版本，共享同一功能设计，但代码与构建体系完全不同：
  - **Forge 1.20.1 版**（`main` 分支，功能最全，是后续功能的"源头"）
  - **NeoForge 1.21.1 版**（`neoforge-1.21.1` 分支，由 main 人工移植而来，可能滞后于新功能）
- **GitHub 仓库**：`https://github.com/Higeons/Ars-Mana-HUD`
- **两分支为"无关联历史"**（各自有独立的 root commit），**禁止使用 `git merge` / `cherry-pick` 同步**；功能同步一律采用"人工移植"（见下文）。
- 构建：Forge 版用 ForgeGradle（Java 17），NeoForge 版用 NeoGradle。两版前置均为 Ars Nouveau 对应版本，仅在 `build.gradle` 中 compileOnly/runtimeOnly 引入，**不得打包发布**。

## 两个版本：目录 ↔ 分支 ↔ 推送关系

| 目录（独立 git 仓库） | 所属分支 | 该目录内的推送命令 |
| --- | --- | --- |
| `E:\Minecraft_Tools\arsmanahud-1.20.1` | `main` | `git push` |
| `E:\Minecraft_Tools\arsmanahud-1.21.1` | `neoforge-1.21.1` | `git push` |

维护哪个版本，就在哪个目录下工作；两目录互不干涉。

## 关键路径（Forge 版，main 分支）

| 用途 | 路径 |
| --- | --- |
| 主模组类 | `src/main/java/com/arsmanahud/ArsManaHud.java` |
| HUD 渲染 | `src/main/java/com/arsmanahud/client/ManaHudRenderer.java` |
| 事件处理 | `src/main/java/com/arsmanahud/client/ArsManaHudEvents.java` |
| 覆盖层注册 | `src/main/java/com/arsmanahud/client/ManaHudClient.java` |
| 版本号 | `gradle.properties` 中的 `mod_version` |
| 构建产物 | `build/libs/arsmanahud-1.20.1-<mod_version>.jar` |

> NeoForge 版的源码结构类似，但模组 API 与装配方式不同；涉及该版本时以该目录实际源码为准，不要照搬 Forge 版代码。

## 例行变更流程（默认模式：Agent 本地提交，用户负责推送）

1. **Agent 做**：分析/修改代码 → 本地验证（尽量执行 `./gradlew build`）→ `git add` → `git commit` → 向用户汇报 commit hash、改动摘要与应执行的推送命令。
2. **用户做（默认）**：在对应目录的终端执行推送（见"用户终端命令框架"）。
3. 除非用户明确要求直接推送并授予网络/非沙箱权限，否则 **Agent 不得擅自 `git push`**（环境沙箱默认无法连接 GitHub）。

### Agent 本地提交规范

- 提交信息用英文，短横线风格，前缀标识类型：`feat:`, `fix:`, `docs:`, `refactor:`, `perf:`, `chore:`（例如 `fix: regen text offset on small screens`）。
- 一次提交只做一件事，便于回滚。
- 发新版本性质的功能完成后，同步在对应版本的 `gradle.properties` 更新 `mod_version`（遵循 [SemVer](https://semver.org/lang/zh-CN/)）。
- 工作区有未提交改动时，**先 `git status` 确认现状再动手**，不要丢弃用户已有改动。

### 变更前的准备

会话开始时先确认代码最新：

```bash
git status
```

若用户在别处有未推送的提交，先让用户推送或处理，避免在过时基线上开发。

## 功能同步（main → neoforge-1.21.1）：人工移植

主分支（Forge 1.20.1）的新功能需要同步到 NeoForge 分支时，**不用 git merge / cherry-pick**，按以下流程人工移植：

1. 用户给出指令，指明要移植的功能（或源 commit）。
2. Agent 在 `E:\Minecraft_Tools\arsmanahud-1.21.1` 目录内工作：
   - 先阅读 Forge 版对应实现，理解功能逻辑；
   - 用 **NeoForge 的 API 重新实现等价功能**（NeoForge 与 Forge 的事件总线、mod 装载、注册、GUI 渲染等均有差异，需要适配，不是复制粘贴）；
   - 本地 `./gradlew build` 验证；
   - 本地提交，提交信息注明来源：`feat: port <功能名> from main (<源commit hash>)`。
3. 用户在 NeoForge 目录的终端执行 `git push`，推送至 `neoforge-1.21.1`。

## 用户终端命令框架（复制到 Git Bash 使用，替换占位符）

### 场景一：推送代码改动

```bash
# Forge 版（→ main）
cd "E:\Minecraft_Tools\arsmanahud-1.20.1"
git status              # 先看有哪些待推送内容
git push

# NeoForge 版（→ neoforge-1.21.1）
cd "E:\Minecraft_Tools\arsmanahud-1.21.1"
git status
git push
```

### 场景二：发布一个新版本（以 Forge 版为例，NeoForge 版同理）

```bash
cd "E:\Minecraft_Tools\arsmanahud-1.20.1"
git push
git tag v0.2.1          # 与 gradle.properties 的 mod_version 保持一致
git push origin v0.2.1
```

之后打开 <https://github.com/Higeons/Ars-Mana-HUD/releases> 点击 **Draft a new release**：

1. **Choose a tag** 选择刚推送的 `v0.2.1`；
2. **Release title** 填 `Ars Mana HUD v0.2.1`（NeoForge 版建议加注 `(NeoForge 1.21.1)`）；
3. 描述可粘贴 Agent 提供的发布说明草稿；
4. 把 jar 拖入 **Attach binaries**：`build\libs\arsmanahud-1.20.1-0.2.1.jar`（NeoForge 版路径同理）；
5. 点击 **Publish release**。

> 提示：GitHub 上传 `.jar` 偶尔会提示"可能是恶意软件"，属常见误报，确认是自行构建的文件即可继续。

### 场景三：首次把 NeoForge 目录推成分支（一次性，已完成则跳过）

```bash
cd "E:\Minecraft_Tools\arsmanahud-1.21.1"

# 先在其 .gitignore 末尾追加排除项（NeoGradle 解包产物，不应入库）：
#   com/
#   net/
#   patches/
#   META-INF/

git init
git config user.name "Higeons"
git config user.email "210358956+Higeons@users.noreply.github.com"
git add .
git status              # 核对没有 com/ net/ patches/ 混入
git commit -m "Initial commit: Ars Mana HUD for NeoForge 1.21.1"
git remote add origin https://github.com/Higeons/Ars-Mana-HUD.git
git branch -M neoforge-1.21.1
git push -u origin neoforge-1.21.1
```

### 场景四：回滚 / 标签修正

```bash
git reset --hard HEAD~1            # 回滚本地提交
git push origin --delete v0.2.0    # 删除已推送的错误标签（远程）
git tag -d v0.2.0                  # 删除错误标签（本地）
```

## 环境与权限说明

- Agent 的 Bash 沙箱默认 **无法连接 github.com**，因此 `git push` 默认由用户执行。
- 若用户希望 Agent 直接推送：当 Agent 请求"非沙箱/网络权限"时在权限弹窗选择允许；且需先在环境中完成一次 GitHub 凭据认证（如 Credential Manager），否则推送会要求输入凭据。
- 构建依赖网络（Maven / MinecraftForge 仓库），沙箱内可能失败；必要时由用户在自己终端执行 `./gradlew build`。

## 版本与分支约定

- 分支：`main`（Forge 1.20.1）与 `neoforge-1.21.1`（NeoForge 1.21.1）。**两分支无关联历史，禁止 merge / cherry-pick 互通**。
- Tag 格式：`vX.Y.Z`，与对应版本目录 `gradle.properties` 的 `mod_version` 一致，使用附注标签（`git tag -a`）。
- 两版本可各自独立发版、独立打 tag；NeoForge 版发布说明应注明"移植自 main 的哪个版本 / commit"。
- 版本号只通过各自 `gradle.properties` 的 `mod_version` 维护，构建产物文件名会自动跟随。