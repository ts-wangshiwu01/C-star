---
name: write-detail-design
description: Use when the user explicitly asks to write a detailed design / detail design
  for a code change requirement — phrases like "写详细设计", "做 detail design",
  "设计一下这个需求", "write detail design for X". Produces a 4-section design
  doc (name / change points / As-Is & To-Be / tests) saved to
  feature/detail-design/. Reads architecture/ and actual source code
  to ground As-Is in reality. Do NOT trigger for casual bug fixes or quick edits —
  only for formal design work the user explicitly requests.
---

# Write Detail Design

## Overview

This is the **Design-stage skill** for code change requirements. It produces a 4-section design doc (name / change points / As-Is & To-Be / tests) grounded in `architecture/` and real source code, saved to `feature/detail-design/`. The skill reads actual source code to verify every claimed change point and traces one-level call chains to ensure the As-Is description reflects reality.

## When to Use

**Trigger explicitly** when the user asks to write a detailed design for a code change requirement. Trigger phrases include:

- "写详细设计"
- "做 detail design"
- "设计一下这个需求"
- "write detail design for X"

**Do NOT trigger for:**
- Casual bug fixes or quick edits
- Asking how something works (that's understanding, not design)

## How It Works

1. Collect ticket number (hard prerequisite), requirement description, and change-location hints from the user
2. Read all files under `architecture/` (mandatory)
3. Verify the involved source code exists — if not, ask the user to confirm the code location
4. Read the code files the user identified (or inferred from architecture/ + confirmed with user)
5. Trace one-level call chains: who calls the target code, and what it calls
6. Fill the 4-section output template with verified information
7. Write the design doc to `feature/detail-design/<ticket>-<slug>.md`
8. Report the path and ask the user to review

## Workflow

```
0. 向用户索取 ticket 号（硬前置，无 ticket 不开工）
   - 同时收集：需求描述、改动位置线索（哪个 service / 哪个类或文件）
   - 若用户不知道改动位置 → 先读 architecture/ 基于需求关键词推断候选位置 → 向用户确认后再继续
1. 读 architecture/ 全部文件 —— detail design 的上下文基础：
   - **high level 架构设计**：`architecture/*.md` 中的组件关系与职责划分
   - **涉及服务的流程图与调用关系**：同文件的 mermaid 图，是 As-Is 调用链的权威来源
   - 依赖以上两者 + 用户需求三者共同作为设计输入，确保设计文档准确
2. 确认涉及源码存在
   - 不存在 → 提示用户确认代码位置或先完成源码搭建，停止
3. 读用户指出的代码文件
4. 追踪直接调用链（只一层）：
   - 谁调用它（grep 符号引用）
   - 它调用谁（读函数体里的调用）
5. 填 4 段：
   - §1 名字: slug 从需求提炼，文档头部写 <ticket>-<slug>
   - §2 改动点: 读代码核实每个位置真实存在
   - §3 As-Is: 基于代码 + 调用链写现状; To-Be: 目标行为 + 不变量
   - §4 测试: 每个改动点配 5 类用例
6. 写入 feature/detail-design/<ticket>-<slug>.md
7. 报告路径，提示用户审阅
```

## Output Document Template

The skill produces exactly this structure — no more, no less:

```markdown
# <SLUG> — <一句话概括这次修改了什么>

> **Ticket**: <ticket>
> **Scope repos**: <repo1> + <repo2>

## 1. 名字
<SLUG> — <一句话概括这次修改了什么>
例: wl-9527-remove-item-check — CANCEL 时跳过 item 校验

## 2. 改动点
| # | 仓库 | 文件 | 类/函数 | 改动 |
|---|------|------|--------|------|
| 1 | paas-purchase-service | .../PurchaseInputValidator.kt | validate() | CANCEL 时跳过 3 个 item 校验 |

> 列出的文件/类/函数必须实际存在于源码中（已读代码验证）

## 3. As-Is 和 To-Be
### 3.1 As-Is（现状）
- 调用链（直接 caller → 本点 → 直接 callee）
- 当前行为（分支/错误码/约束）
- 改前代码片段（含 file:line）

### 3.2 To-Be（改后）
- 新行为
- 保留的不变量（什么不变） ← 必填，§4 不变量测试要对照它
- 改后代码片段

### 3.3 改前 vs 改后对比（可选子段，表格式）

## 4. 测试
按 repo 分组的用例表。每行: 用例 / 期望（✅通过 或 ❌报<error code>）。
必须覆盖 5 类（缺一类就标 ⚠️ 待补）：

| 维度 | 用例 | 期望 |
|------|------|------|
| 正向 (Happy) | <改后应通过的场景> | ✅ |
| 异常 (Error) | <改后仍应被拒的场景> | ❌ <code> |
| 边界 (Boundary) | <边界值/极端值> | ✅/❌ |
| 回归 (Regression) | <不该被改动的路径> | ❌ <code>（行为不变） |
| 不变量 (Invariant) | <跨改动必须保持的约束> | ✅ 或 ❌ <code> |

> 不变量来源: §3.2 To-Be 中列出的"保留的不变量"，每条都要有对应测试
```

**严格只有 4 段**。As-Is/To-Be 是同一段下的两个子段。决策点/风险/开发步骤等都不要 —— 若有真实歧义，以行内注记写在对应段内，不开新段。

## What NOT to Do

- ❌ 不读 `architecture/` 就动手
- ❌ 在源码不存在时不提示、自己偷偷创建（应让用户确认代码位置或先完成源码搭建）
- ❌ 调用链追超过一层（hybrid 模式不要变全链路深挖）
- ❌ 在 4 段之外加段（决策点/风险/开发步骤等 —— 用户明确要极简）
- ❌ 改动点里写没读过的文件/类（每条必须经代码验证存在）
- ❌ 把设计文档写到源码目录里（设计是仓库元数据，代码是工作区）
- ❌ 没 ticket 就开工（ticket 是硬前置）