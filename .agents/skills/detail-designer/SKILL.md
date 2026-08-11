---
name: detail-designer
description: |
   为具体代码改动需求产出 4 段 detail design（名字 / 改动点 / As-Is & To-Be / 测试）。
   设计文档不含实现代码 / 伪代码——只写约束、行为、契约。
   跨边界的契约（DDL / proto / 错误码 / DTO 映射）必须字段级完整。

   前置：ticket 号 + system-design/ 必读。
   源码存在则读代码填 As-Is；greenfield 则 As-Is 标 N/A。
   适用：显式要求"写详细设计 / 做 detail design / 设计一下这个需求"。
   不适用：随口修 bug、问代码怎么工作、HLD（用 hld-designer）。
---

# Detail Designer — 需求级详细设计产出器

## Overview

给一个具体的代码改动需求（一个 ticket）产出一份 4 段 detail design，落到 `feature/detail-design/<ticket>-<slug>.md`。文档只写**约束、行为、跨边界契约**，**不含实现代码或伪代码**——实现是开发的活。

## 核心原则

- **设计不含代码**：§3 As-Is/To-Be 只写行为、调用链、不变量，**禁贴 Java/Kotlin/SQL 语句、伪代码、代码片段**
- **契约必须字段级**：DDL/proto/错误码/DTO 映射跨越团队或进程边界，必须白纸黑字，不留白
- **约束密度补偿**：为了让开发（人和 AI）能准确落地，不变量、错误码、测试用例三者必须齐全且互相印证
- **只 4 段**：严格 4 段（名字 / 改动点 / As-Is & To-Be / 测试）；决策点/风险/开发步骤等**不开新段**，有歧义就行内注记

## 硬前置（不满足即 abort）

1. **ticket 号**——用户不给不开工
2. **`system-design/` 存在且可读**——detail design 的上下文基础
3. **改动位置线索**——不清楚 → skill 先读 system-design 推断候选 → 用户确认后再继续

## 模式判定

skill 检查涉及源码是否存在：

| 情况 | 分支 | As-Is 处理 |
|---|---|---|
| 源码存在（改动型） | EXTRACT | 读代码 + 追一层调用链，写现状 |
| 源码不存在（greenfield） | GREENFIELD | 标 `N/A — greenfield, no prior code` |

一层调用链的定义：谁调用它（grep 引用）+ 它调用谁（读函数体调用点）。**不追第二层**——防止全链路深挖。

## 执行流程

```
0. 收集 ticket + 需求描述 + 改动位置线索
   └─ 位置不明 → 读 system-design/ 推断候选 → 用户确认

1. 读 system-design/ 全部文件
   ├─ 组件关系与职责划分
   └─ mermaid 图（As-Is/To-Be 调用链的权威来源）

2. 读 project-structure 规则（若存在）
   ├─ docs/project-structure.md
   └─ .github/instructions/project-structure.instructions.md
   目的：§2 改动点表的"文件路径"必须落到规则定义的包上

3. 判定模式（EXTRACT vs GREENFIELD）

── EXTRACT 分支 ────────────────
4E. 读用户指出的代码文件
5E. 追一层调用链（grep + 读函数体）
6E. 填 §3.1 As-Is：调用链 + 当前行为（分支 / 错误码 / 约束）+ file:line 引用
   （【禁】贴改前代码片段）

── GREENFIELD 分支 ────────────────
4G. 读 HLD 文档作为设计输入
5G. 按 system-design/ + HLD + project-structure 推断新建位置
6G. §3.1 As-Is 标 "N/A — greenfield, no prior code"

── 汇合 ──────────────────────
7. 契约确认（跨边界契约必须字段级）
   ├─ Proto（若架构用 gRPC）：本次是否新增/修改 proto？
   │  ├─ 是 → §3.5 写完整 Request/Response message + 错误码 enum
   │  └─ 否 → §3.5 标 "N/A — 本次不涉及 proto"
   ├─ DDL / 表结构变更：
   │  ├─ Greenfield → §3.4 写完整 CREATE TABLE
   │  ├─ 改动型有变更 → §3.4 写 ALTER / 新增表
   │  └─ 改动型无变更 → §3.4 标 "N/A — 无表结构变更"
   └─ DTO ↔ 表字段映射：
      └─ §3.6 列字段级映射表，字段名/类型/可空性一一对齐；不允许 DTO 有字段而表里没有

8. §3.2 To-Be：新行为 + 保留的不变量清单（每条不变量在 §4 有对应测试）
   （【禁】贴改后代码或伪代码）

9. §4 测试：按 repo 分组，5 类维度全覆盖
   （不变量维度对照 §3.2 的不变量清单）

10. 写入 feature/detail-design/<ticket>-<slug>.md

11. 报告路径，请用户审阅
```

## Output Document Template

严格按此结构产出，不多不少：

````markdown
# <SLUG> — <一句话概括这次修改了什么>

> **Ticket**: <ticket>
> **Scope repos**: <repo1> + <repo2>

## 1. 名字
<SLUG> — <一句话概括这次修改了什么>
例：wl-9527-remove-item-check — CANCEL 时跳过 item 校验

## 2. 改动点
| # | 仓库 | 文件（必须符合 project-structure 规则） | 类/函数 | 改动 |
|---|------|------|--------|------|
| 1 | paas-purchase-service | .../application/service/PurchaseInputValidator.kt | validate() | CANCEL 时跳过 3 个 item 校验 |

> 列出的文件/类/函数：
> - EXTRACT 场景：必须实际存在于源码中（已读代码验证）
> - GREENFIELD 场景：按 system-design/ + HLD + project-structure 推断，改动单元格标注 "（新建）"
>
> 文件路径必须符合 `docs/project-structure.md` 的分层与命名后缀；找不到规则时先跑 `project-structure-rule` skill

## 3. As-Is 和 To-Be

### 3.1 As-Is（现状）
- **EXTRACT 场景**：
  - 调用链：直接 caller → 本点 → 直接 callee（只一层）
  - 当前行为：分支 / 错误码 / 约束（自然语言描述，禁贴代码）
  - 相关代码定位：`file:line` 引用，如 `PurchaseInputValidator.kt:47-56`
- **GREENFIELD 场景**：`N/A — greenfield, no prior code`

### 3.2 To-Be（改后）
- **新行为**：改后期望的处理逻辑（自然语言 + 决策表，禁贴代码/伪代码）
- **保留的不变量**：什么不能变（必填，§4 不变量测试对照本清单）

> 【禁】任何 Java/Kotlin/Go/TS 代码片段、伪代码、SQL 语句
> 表达意图用：自然语言步骤列表 + 判断/循环用决策表 + 状态转移用 mermaid stateDiagram

### 3.3 改前 vs 改后对比（条件段，非必出）
仅当**存在真实改动**时输出——即存在改前实现（EXTRACT），且行为、契约或接口确实发生变化。以下情况**整体省略 §3.3**，不写占位 N/A：
- **GREENFIELD / 新功能**：无改前实现可比（As-Is 为 N/A）
- **无行为差异**：改动未改变既有行为、契约、接口（纯内部重构、重命名、格式调整）→ 无可对比内容

> 评判标准：若 §3.1 是 N/A 或调用链/行为无变化，则 §3.3 省略。只有你能写出"改前是 A、改后是 B"且 A≠B 时才保留。

### 3.4 DDL / 表结构变更
- **Greenfield 新建型**：完整 CREATE TABLE DDL（字段/类型/索引/约束/字符集），每张表一块
- **改动型有变更**：ALTER TABLE 或新增表的 DDL
- **改动型无变更**：`N/A — 无表结构变更`

> DDL 是数据库契约，必须字段级完整；这不算"代码片段"

### 3.5 Proto 契约（若架构用 gRPC/proto）
- **需要定义/修改** → 字段级 proto message + 错误码 enum，不留空
- **不涉及** → `N/A — 本次不涉及 proto`

> proto 是跨服务契约，必须字段级完整；这不算"代码片段"

### 3.6 DTO ↔ 表字段映射
DTO/Command/Response 每个字段 ↔ 表字段（§3.4 或既有表）一一对应。字段名、类型、可空性必须对齐。

| DTO 字段 | 类型 | 对应表.字段 | 类型 | 可空 | 备注 |
|---|---|---|---|---|---|
| giverSsoId | String | appreciation.giver_sso_id | VARCHAR(128) | NOT NULL | |
| flowerCount | Integer | appreciation.flower_count | INT | NOT NULL | |

> DTO 有字段但表里没有对应列 → 回 §3.4 补 DDL 或从 DTO 删字段，不允许漂移

## 4. 测试
按 repo 分组的用例表。5 类维度必须全覆盖，缺一类标 ⚠️ 待补。

| 维度 | 用例 | 期望 |
|------|------|------|
| 正向 (Happy) | <改后应通过的场景> | ✅ |
| 异常 (Error) | <改后仍应被拒的场景> | ❌ <错误码> |
| 边界 (Boundary) | <边界值/极端值> | ✅/❌ |
| 回归 (Regression) | <不该被改动的路径> | ❌ <错误码>（行为不变） |
| 不变量 (Invariant) | <§3.2 每条不变量都要一行> | ✅ 或 ❌ <错误码> |

> Greenfield 场景下回归维度标 ⚠️ 待补（无"改前路径"可回归）
> 不变量维度**必须**与 §3.2 的不变量清单一一对应，一条不落
````

## What NOT to Do

- ❌ **贴任何实现代码或伪代码到 §3.1/§3.2**——设计不含实现，用不变量 + 决策表表达行为
- ❌ 不读 `system-design/` 就动手
- ❌ 源码不存在时不提示、自己偷偷创建（应让用户确认位置或先搭源码骨架）
- ❌ 调用链追超过一层
- ❌ 在 4 段之外加段（决策点/风险/开发步骤等）
- ❌ 改动点里写没读过的文件/类（EXTRACT 场景必须已读验证）
- ❌ 改动点的文件路径不符合 `project-structure` 规则
- ❌ DDL / proto / DTO 映射留空或标 "TODO"（这是契约，不能延迟到开发时定）
- ❌ 把设计文档写到源码目录（设计属仓库元数据，放 `feature/detail-design/`）
- ❌ 没 ticket 就开工

## 依赖关系

### 上游

| 输入 | 必需 | 用途 |
|---|---|---|
| ticket 号 | ✅ | 硬前置 |
| `system-design/*.md` | ✅ | 上下文 + 调用关系权威来源 |
| 用户指出的代码位置 | EXTRACT 必需 | 读代码填 As-Is |
| `docs/project-structure.md` + `.instructions.md` | 推荐 | §2 改动点文件路径的合规依据 |
| HLD 文档 | GREENFIELD 必需 | 推断新建位置的依据 |
| PRD `docs/REQUIREMENTS.md` | 可选 | 需求全貌参照 |

### 下游

| 消费者 | 用途 |
|---|---|
| 开发者（人） | 按 §2/§3.4/§3.5/§3.6 落代码；§4 是验收清单 |
| AI 编码 skill（tdd 等） | detail design 是开发的输入；配合 project-structure 规则 |
| Code Reviewer | 按 §3.2 不变量 + §4 测试用例逐条核对 PR |

## 不适用

- **HLD**：用 `hld-designer` skill
- **随口修 bug / 快速改动**：直接 patch，不必走 detail design
- **API 文档**：用 `gen-api-doc` skill
- **项目结构规则**：用 `project-structure-rule` skill
- **ADR**：用 `domain-modeling` skill 的 ADR 流程
