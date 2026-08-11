---
name: hld-designer
description: |
   从 PRD + ADR + system-design 产出概要设计文档（HLD）：模块/服务划分、业务流程概览、服务清单与定位。
   这是三层设计流水线的第二层（系统设计 → 概要设计 HLD → 详细设计）。

   PRD（docs/REQUIREMENTS.md）必需；system-design/ 文档必需（由 system-designer 产出）。
   只出一份 HLD 文档，不产出服务级 Design（那是历史中间层，已废弃）。

   适用：生成 HLD、做概要设计、从 PRD 补 HLD。
   不适用：系统设计/架构文档（用 system-designer）、detail design（用 detail-designer）、API 文档、ADR。
   触发短语：「概要设计」「HLD」「做 HLD」「生成 HLD」「high level design」。
---

# HLD Designer — 概要设计（HLD）产出器

## Overview

从 PRD + ADR + system-design 产出一份概要设计文档（HLD），落到 `feature/hld-design/<PROJECT>_HLD_v<版本>_<日期>.md`。这是三层设计流水线的第二层：

```
system-designer(系统设计) → hld-designer(概要设计 HLD) → detail-designer(详细设计)
```

HLD 回答三个问题：
1. **系统有哪些模块/服务**——服务清单 + 每个服务的职责边界（从 system-design/ 继承，聚焦本次 feature 涉及的）
2. **业务流程怎么走**——本次涉及的接口/任务清单，每个一句话说干啥
3. **服务间怎么协作**——跨服务编排图（极简，不画校验分支）

## 核心约束

- **PRD 必需**：缺 `docs/REQUIREMENTS.md` 直接 abort，不替用户编造需求
- **system-design/ 必需**：缺 `system-design/*.md`（system-designer 产出）→ 提示用户先跑 system-designer，不替用户做架构决策
- **feature 级产出**：HLD 只写本次涉及的服务/接口/任务；后续 feature 出独立文档，不追加
- **不越权**：系统设计/架构文档归 system-designer；detail design 归 detail-designer；ADR 由人决策
- **不画校验分支 / 不列业务规则清单**：那些属 detail design；HLD 只句"处处需要业务校验"

## 一层产出

| 文件 | 必需 | 模板 |
|---|---|---|
| `<PROJECT>_HLD_v<版本>_<日期>.md` | ✅ | `templates/hld.md` |
| `glossary.md`（可选） | 可选 | `templates/glossary.md` |

**HLD 结构**（4 节）：

1. **项目概述**：业务背景 + 关键约束（ADR 摘要）
2. **整体架构图**：mermaid graph 画所有服务 + 调用关系 + 数据流（从 system-design/ 继承，聚焦本次 feature）
3. **业务流程概览**：本次涉及的接口/任务清单，每个一句话说干啥；可附一张极简 mermaid flowchart 画跨服务编排（**禁画校验分支 / 禁列业务规则清单** —— 那些属 detail design）
4. **服务清单与定位**：每个服务一节 —— 核心职责 + 上游 + 下游 + 入口类型

**入口类型定义**（用于 HLD §3.1 清单表的"类型"列）：

- **api**：同步请求/响应，REST/gRPC handler 直接返回（如发送赞赏、查排行榜）
- **batch**：时间驱动，调度器按 cron 触发处理存量数据（如每日配额重置）
- **worker**：事件驱动，从 MQ 消费消息，有重试/幂等/DLQ（如事件驱动业务处理）

## 执行流程

```
0. 前置检查
   ├─ docs/REQUIREMENTS.md 不存在 → abort
   └─ system-design/*.md 不存在 → 提示用户先跑 system-designer，abort

1. 读上游
   ├─ docs/REQUIREMENTS.md（必读，提取本次 feature 涉及的功能）
   ├─ docs/adr/*.md（若存在，提取关键约束）
   ├─ system-design/*.md（必读，提取技术栈 + 服务划分 + DB schema）
   └─ docs/CONTEXT.md（若存在，术语对齐）

2. 生成 HLD
   ├─ 填 hld.md 模板
   ├─ §2 整体架构图：从 system-design/ 继承，聚焦本次 feature 涉及的服务
   ├─ §3 业务流程概览：列本次涉及的接口/任务，每个一句话；禁画校验分支
   ├─ §4 服务清单：每个服务一节，含入口类型标注
   └─ 遇决策点暂停问用户

3. 问「是否需要 glossary.md?」
   ├─ 已有 CONTEXT.md/glossary 且本次无新术语 → 建议跳过
   └─ 需要 → 走 glossary 模板

4. 命名落盘到 feature/hld-design/（见 §命名规范）

5. 报告路径，提示用户审阅；提醒下一步用 detail-designer 产出详细设计
```

## 内容纪律

- **§2 整体架构图**：从 system-design/ 继承服务全景，可裁剪到本次 feature 涉及的子集；用 mermaid graph
- **§3 业务流程概览**：只列"接口/任务 + 一句话说明"，**禁画校验分支 / 禁列业务规则 / 禁写错误码**——那些属 detail design
- **§3.2 跨服务编排图**（可选）：只在有跨服务调用时画，只体现"谁调谁、走什么协议"，禁画判断节点
- **§4 服务清单**：每个服务列核心职责 + 上游 + 下游 + 入口类型；不写接口签名、不写校验规则
- **不写 DDL / proto / DTO 字段**：那些属 detail design
- **不写业务校验规则 / 错误码**：那些属 detail design

## 命名规范

```
[PROJECT]_HLD_v[版本号]_[日期].md
[PROJECT]_GLOSSARY_v[版本号]_[日期].md
```

- **PROJECT**：项目缩写大写（如 `CS` = C-Star）
- **版本号**：`vX.Y`，初版 `v1.0`
- **日期**：`YYYYMMDD`

示例：`CS_HLD_v1.0_20260811.md`、`CS_GLOSSARY_v1.0_20260811.md`

## 三层文档分层（与 system-designer / detail-designer 的边界）

| 层 | skill | 产出 | 粒度 |
|---|---|---|---|
| 系统设计 | system-designer | `system-design/<project>-system-design.md` | 技术栈 + 服务全景 + DB schema 划分 |
| 概要设计 | hld-designer | `<PROJECT>_HLD_*.md` | 模块划分 + 业务流程概览 + 服务清单 |
| 详细设计 | detail-designer | `feature/detail-design/<ticket>-*.md` | 字段级 DDL/proto/DTO/错误码 + 测试 |

**衔接规则**：
- 上游：必读 system-designer 产出的 `system-design/` 文档，提取技术栈和服务边界
- 下游：detail-designer 读 HLD 获取业务流程上下文 + 服务定位

## 不做什么（边界声明）

- 不做系统设计/架构文档（system-designer 的活）
- 不做服务级 Design（已废弃的中间层，不再产出）
- 不做 detail design（detail-designer 的活）
- 不写 DDL / proto / DTO 字段定义（detail design 的活）
- 不写业务校验规则 / 错误码（detail design 的活）
- 不画业务校验流程图 / 时序图（detail design 的活）
- 不写 ADR（人决策，skill 只读）
