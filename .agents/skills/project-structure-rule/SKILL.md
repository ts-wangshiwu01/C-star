---
name: project-structure-rule
description: |
   从已有代码提取，或从 architecture/需求文档设计，产出项目目录/包结构规则。
   两种模式（EXTRACT 提取 / DESIGN 设计）由 skill 扫仓库自动判断，不问用户。

   同时产出两份文件：
   - docs/project-structure.md（人读）
   - .github/instructions/project-structure.instructions.md（Copilot 按 applyTo 自动加载）

   适用：0→1 项目定包结构、已有项目沉淀现有约定、给 AI 补"该放哪个包"的规则。
   不适用：单个类改名、包重构 refactor（用 IDE）、写具体代码（用 tdd 或 detail-designer）。
---

# Project Structure Rule — 项目结构规则产出器

## Overview

给项目产出一份**"这个类放哪个包"的权威规则**，让人和 AI 后续开发都按同一套约定走。

两种模式：

- **EXTRACT**：仓库已有可辨识的分层结构（如 controller/service 分包）→ 从代码提取
- **DESIGN**：仓库为空或结构混乱 → 从 `architecture/` + 官方推荐做设计

模式**由 skill 扫仓库自动判断**，用户只要说"帮我出项目结构规则"。

## 产出物（两份，同源）

| 文件 | 定位 | 谁读 |
|---|---|---|
| `docs/project-structure.md` | 人读版：骨架图 + 每层职责 + 命名 + 禁令 + 示例，可含说明性文字 | 团队成员、reviewer |
| `.github/instructions/project-structure.instructions.md` | 机读版：命令式短句 + frontmatter `applyTo` glob | Copilot 编辑源码时自动加载 |

两份文件同一次会话生成，内容同源，格式不同。

## Rule 必备四段（缺一段视作不合格）

任何一份 rule（不论 EXTRACT 还是 DESIGN 产出）都必须包含：

1. **目录/包骨架图**（ASCII 或 mermaid）— 让人一眼看到全貌
2. **每层职责 + 命名后缀表** — `Controller / Service / Repository / Dto / Validator ...` 各干什么、命名后缀是什么
3. **禁止事项清单** — 至少 5 条，如 `Controller 不能直接调 Repository`、`Dto 不能含业务逻辑`、`Service 不能返回实体类`
4. **示例落点** — 挑一个具体功能（如"发送赞赏"），列出它落到哪几个具体文件路径

四段缺一段就不落盘，回去补齐。

## 模式判定（skill 自动执行，用户不选）

skill 启动后先扫仓库：

```
扫描目标：src/main/ 或 src/ 或 pkg/ 或 app/ 下的源码目录
   ↓
统计每层文件数量（controller/service/repository/handler/model/dto/...）
   ↓
── 满足以下任一条件 → EXTRACT 模式 ──
   - 检测到 ≥ 2 种分层包（如同时有 service/ 和 repository/）
   - 检测到 ≥ 5 个源码文件遵循同一命名后缀（如 5 个 *Service.kt）
   ↓
── 否则 → DESIGN 模式 ──
```

判定后，明确告知用户走哪种模式再继续（透明，不隐式）。

## 执行流程

```
0. 扫仓库判定模式（EXTRACT vs DESIGN）
   └─ 判定结果告知用户

── EXTRACT 分支 ────────────────────────
1E. 提取分层包
    ├─ 找主源码根（src/main/kotlin, src/main/java, pkg/, src/）
    ├─ 列出所有二级包及文件数
    └─ 识别命名后缀模式（*Controller / *Service / *Repository ...）

2E. 提取禁令
    ├─ grep 反向导入（controller import repository? service import controller?）
    ├─ 已经被代码遵守的约束 → 明确列为禁令
    └─ 存在少量违反的 → 标注为「大部分遵守，X 处违反待整改」

3E. 采访补缺（跳过 skill 无法推断的部分）
    ├─ 有没有对外契约层（proto/openapi）单独放？
    └─ Dto/Entity/Domain 三者当前是不是同一个包？团队意图是分还是不分？

── DESIGN 分支 ────────────────────────
1D. 读上游
    ├─ architecture/*.md（必读，提取语言 + 框架）
    ├─ 无 architecture/ → 采访「主语言/框架/构建工具」
    └─ 读 references/default-structures.md 对应官方推荐

2D. 呈现候选结构给用户确认
    ├─ 至少 2 种候选（如 Micronaut 官方 vs DDD 分层）
    └─ 用户选一个作为基线，或提出微调

3D. 采访细化
    ├─ 是否有对外契约层单独包？
    ├─ 单模块 vs 多模块（Gradle subprojects）？
    └─ 前端/后端是否同仓？

── 汇合 ──────────────────────────────
4. 生成两份产出草稿（从同一份内部数据模型渲染）
   ├─ 展示 diff / 预览给用户
   └─ 用户确认后落盘

5. 落盘
   ├─ docs/project-structure.md
   └─ .github/instructions/project-structure.instructions.md
       └─ frontmatter applyTo 按项目主语言设置，如 "src/**/*.{kt,java}"

6. 若已有同名文件 → 【硬门】必须先展示 diff，用户确认覆盖才写；未确认即 abort
```

## 硬门

| 情况 | 处理 |
|---|---|
| EXTRACT 模式扫不到任何分层信号 | 自动降级为 DESIGN 模式，告知用户 |
| DESIGN 模式无 `architecture/` 且用户拒绝回答技术栈 | abort，不猜栈 |
| 目标输出文件已存在 | 必须 diff 预览 + 显式确认覆盖 |
| Rule 四段缺任意一段 | 不落盘，补齐后再落 |

## 依赖关系

### 上游

| 输入 | EXTRACT 必需 | DESIGN 必需 |
|---|---|---|
| 仓库源码 | ✅ | — |
| `architecture/*.md` | 可选（补技术栈信息） | ✅（缺则采访） |
| `docs/REQUIREMENTS.md` | — | 可选 |

### 下游（谁消费本 skill 产出）

| 消费者 | 用途 |
|---|---|
| `detail-designer` skill | detail design §2 改动点表的文件路径合规依据 |
| `hld-designer` skill | 服务清单与包名对齐 |
| Copilot / AI 编码 | 通过 `applyTo` 自动加载，写新文件时按 rule 落包 |
| 团队成员 | 读 `docs/project-structure.md` 建立心智模型 |

## 不适用

- **单个类改名/包重构**：用 IDE refactor 更快
- **写具体代码**：用 `tdd` 或 `detail-designer`
- **架构技术选型**：那是 Architecture skill 的活，本 skill 只管"选完之后代码往哪放"
- **依赖版本管理**：不属于结构规则
