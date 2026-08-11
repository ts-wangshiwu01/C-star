---
name: system-designer
description: |
   从 PRD + ADR 产出系统级架构文档（system-design/<project>-system-design.md）：技术选型、整体架构图、服务划分与职责、数据库 schema 划分。
   这是三层设计流水线的第一层（系统设计 → 概要设计 HLD → 详细设计）。

   产出物是 hld-designer 的强制上游：HLD 必须基于本 skill 产出的 system-design/ 文档。
   PRD（docs/REQUIREMENTS.md）必需；至少 1 个 ADR 必需（不能凭空做技术选型）。

   适用：生成 system-design 文档、做系统设计、技术选型、服务划分、数据库 schema 划分、从 PRD 补架构层。
   不适用：HLD、detail design、API 文档、ADR 本身（ADR 由人决策，本 skill 只读不写）。
   触发短语：「系统设计」「架构设计」「技术选型」「服务划分」「做 system-design」「补架构层」「system design」。
---

# System Designer — 系统级架构文档产出器

## Overview

从 PRD + ADR 产出一份系统级架构文档，落到 `system-design/<project>-system-design.md`。这是三层设计流水线的入口：

```
system-designer(系统设计) → hld-designer(概要设计 HLD) → detail-designer(详细设计)
```

文档回答四个问题：
1. **用什么技术栈**——每个选型都要能追溯到某个 ADR 或 PRD 约束
2. **系统有哪些服务**——服务清单 + 每个服务的职责边界
3. **服务怎么协作**——整体架构图（调用关系 + 数据流 + 协议）
4. **数据怎么分**——Database-per-service 原则下的 schema 划分

## 核心约束

- **PRD 必需**：缺 `docs/REQUIREMENTS.md` 直接 abort，不替用户编造需求
- **ADR 必需**：至少 1 个 `docs/adr/*.md`；技术选型**必须**追溯到具体 ADR 编号，凭空选型 abort
- **不写 ADR**：ADR 是人的决策记录，本 skill 只读不写。技术选型以 PRD 约束 + ADR 为准，缺失选型不臆造（按既定公司技术栈基线补全）
- **不写 HLD/detail**：那些归 hld-designer / detail-designer。本 skill 只到"系统全景"层
- **不写 DDL**：schema 划分只列"哪个库存哪些表 + 表用途一句话"，字段级定义属 detail design
- **不写代码/伪代码**：架构文档是蓝图不是实现
- **不越权做业务规则**：业务校验、错误码归 detail design；本 skill 只画服务边界和调用关系

## 三层文档分层（与 hld-designer / detail-designer 的边界）

| 层 | skill | 产出 | 粒度 |
|---|---|---|---|
| 系统设计 | system-designer | `system-design/<project>-system-design.md` | 技术栈 + 服务全景 + DB schema 划分 |
| 概要设计 | hld-designer | `<PROJECT>_HLD_*.md` | 模块划分 + 业务流程概览 + 服务清单 |
| 详细设计 | detail-designer | `feature/detail-design/<ticket>-*.md` | 字段级 DDL/proto/DTO/错误码 + 测试 |

**衔接规则**：hld-designer 流程第 1 步"读上游"必读本 skill 产出的 `system-design/` 文档提取技术栈和服务边界。如果 `system-design/` 不存在 → hld-designer 应提示用户先跑 system-designer。

## 执行流程

```
0. 前置检查
   ├─ docs/REQUIREMENTS.md 不存在 → abort
   └─ docs/adr/*.md 不存在或为空 → abort

1. 读上游
   ├─ docs/REQUIREMENTS.md（必读，提取功能边界 + 非功能需求）
   ├─ docs/adr/*.md（必读，提取已决策的技术约束）
   └─ docs/CONTEXT.md（若存在，提取术语对齐）

2. 提取技术底座
   ├─ 从 PRD 功能列表推断"需要哪些服务"
   └─ 从 ADR 提取已锁定的技术选型；PRD/ADR 未覆盖的 → 按既定公司技术栈基线补全

3. 填 system-design.md 模板
   ├─ §1 概述（一段话 + 业务背景）
   ├─ §2 技术选型表（每行追溯到 ADR 或公司基线）
   ├─ §3 架构图（mermaid graph，服务全景 + 调用关系 + 数据流）
   ├─ §4 服务职责表（服务名 + 职责 + 对外/对内协议）
   └─ §5 数据库划分（Database-per-service + 每服务 schema 表清单）

4. 命名落盘到 system-design/
   └─ system-design/<project>-system-design.md（首次）/ system-design/<project>-system-design_v<版本>_<日期>.md（迭代）

5. 报告路径，提示用户审阅；提醒下一步用 hld-designer 产出 HLD
```

## 硬门响应表

| 用户回答 | 处理 |
|---|---|
| PRD 或 ADR 缺失 | abort，明确说明「系统设计不能凭空做，需要 PRD + 至少 1 个 ADR」 |
| 技术选型无 ADR 也无公司基线可依 | 按既定公司技术栈基线补全选型；不虚造 ADR 引用 |
| 用户说"你看着办" / "随便选" | abort，明确说明「技术选型必须追溯到 ADR 或公司基线，不能凭感觉」 |

## 命名规范

```
system-design/<project>-system-design.md            （首次/当前版本）
system-design/<project>-system-design_v<版本>_<日期>.md （历史归档，迭代时）
```

- **project**：项目缩写小写（如 `c-star`）
- **版本号**：`vX.Y`，初版 `v1.0`
- **日期**：`YYYYMMDD`

示例：`system-design/c-star-system-design.md`、`system-design/c-star-system-design_v2.0_20260811.md`

## 内容纪律

- **技术选型表每行必有 ADR 追溯**：`| 类别 | 选型 | 说明 + [ADR-000X](../docs/adr/000X-*.md) |`
- **架构图用 mermaid graph**：不画 ASCII art（难维护、难演进）
- **服务职责表只写边界**：不写接口签名、不写校验规则（那些归 Design）
- **schema 划分只列"哪个库存哪些表 + 一句话用途"**：字段/索引/约束属 detail design
- **不画业务流程图**：业务流程归 HLD §3；本 skill 只画服务全景和调用关系
- **决策点显式化**：所有"为什么这么选"都要能在 ADR 找到答案；找不到 → 是决策缺失，不是文档缺失

## 与现有 system-design/ 文档的关系

若 `system-design/<project>-system-design.md` 已存在：
- 用户未明确要求重写 → 提示「已有架构文档，是否要 review/迭代？」
- 用户要求迭代 → 读旧版 → 识别哪些决策点变了（新增 ADR / PRD 变更）→ 增量更新，旧版归档为 `*_v<旧版本>_<日期>.md`
- 不做"全量重写"除非用户明确要求

## 不做什么（边界声明）

- 不写 ADR（人决策，skill 只读）
- 不写 HLD（hld-designer 的活）
- 不写 detail design（detail-designer 的活）
- 不写 DDL / proto / DTO 字段定义（detail design 的活）
- 不写业务校验规则 / 错误码（detail design 的活）
- 不画业务流程图 / 时序图（HLD §3 / Design §4 的活）
- 不替用户做技术选型决策（必须追溯到 ADR）
