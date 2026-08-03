# hld-designer Skill — 设计文档

> **Spec date**: 2026-08-03
> **Status**: 待用户审查
> **Replaces**: `hld-designer-v3`（完全替换，旧 skill 整目录删除）

---

## 0. 决策总览

| 决策点 | 结果 | 来由 |
|---|---|---|
| 产出层级 | 两层：HLD（项目级）+ Design（服务级按类型）| 用户要求分层，HLD 描述工程全貌，Design 聚焦具体入口设计 |
| HLD 定位 | 项目级一份文档 | 描述整个项目：有什么服务、怎么工作、每个服务定位、整体架构图 + 业务流程概览 flow |
| Design 定位 | 按服务+入口类型，每服务每类型一份 | api/batch/worker 三类，每份只记该类型内容 |
| Architecture | 不在本 skill 范围 | 由独立 skill 产出（技术选型 + 服务依赖图 + 仓库地址 + Helm + 数据库划分）|
| 与 v3 关系 | 完全替换 | 删除 v3 skill 和三套模板 |
| 分类维度 | api / batch / worker 三类 | 同步 API / 定时批 / 异步消费者 |
| 分类粒度 | 一份 Design = 一种类型 | 混合服务出多份 Design |
| 分类门控 | 首问硬门，未答则 abort | 用户要求：未明确分类不能进行下一步 |
| 技术栈 | 不硬绑，看 `architecture/` 是否存在 | 存在就读，不存在才通过采访补齐 |
| 上游要求 | PRD 必需（`docs/REQUIREMENTS.md`）| 没 PRD 直接 abort |
| skill 名 | `hld-designer` | 去 v3/v4 版本号 |
| 运行方式 | 交互式一次完成 | 读上游 → 生成 HLD → 生成 Design → 可选 glossary |
| 模板组织 | 扁平多模板（5 个文件）| 类型已在文件名体现 |
| Worker MQ 类型 | 通用 MQ，不绑 Kafka | 用户最终选择 |
| glossary | 可选，独立于类型 | 初版基于 CONTEXT.md，后续追加 |

---

## 1. Skill 身份

```yaml
name: hld-designer
description: |
  产出 HLD（项目级高层设计）+ Design（按服务入口类型的详细设计）两层文档。

  HLD：项目级一份文档，描述整个工程全貌——有什么服务、怎么工作、每个服务什么定位、
  整体架构图 + 业务流程概览 flow 图。

  Design：按服务+入口类型（api/batch/worker）产出，每服务每类型一份。

  分类是硬门：生成 Design 时首问类型，用户未明确回答则立即 abort。

  PRD（docs/REQUIREMENTS.md）必需，无则 abort。
  技术栈从 architecture/ 自动读取，不存在时通过采访补齐。

  Architecture 不在本 skill 范围——由独立 skill 产出。
```

**位置**：`.agents/skills/hld-designer/`
**触发**：用户明确说要生成 HLD 或 Design 时

---

## 2. 两层产出

### 第一层：HLD（项目级）

| 文件 | 必需 | 模板 |
|---|---|---|
| `hld.md` | ✅ 必出 | `templates/hld.md` |

**HLD 结构**（4 节）：
1. **项目概述**：业务背景 + 关键约束（ADR 摘要）
2. **整体架构图**：mermaid graph 画所有服务 + 调用关系 + 数据流（项目的"地图"）
3. **业务流程概览**：mermaid flowchart 画项目核心业务主流程（跨服务串联）
4. **服务清单与定位**：每个服务一节，说明核心职责 + 上游 + 下游 + 入口类型（api/batch/worker）

### 第二层：Design（服务级，按入口类型）

| 文件 | 必需 | 模板 | 定位 |
|---|---|---|---|
| `design-api.md` | 按需 | `templates/design-api.md` | API 类：接口设计 + 时序图 + 校验与错误码 |
| `design-batch.md` | 按需 | `templates/design-batch.md` | Batch 类：任务时序图 + 调度计划 + 幂等与重试 |
| `design-worker.md` | 按需 | `templates/design-worker.md` | Worker 类：消费时序图 + 消息模型 + 幂等与重试 |
| `glossary.md` | 可选 | `templates/glossary.md` | 术语表（独立于类型）|

**一份 Design 只能是一种类型**——混合服务出多份 Design。

---

## 3. 执行流程

```
0. 前置检查
   ├─ docs/REQUIREMENTS.md 存在?
   │  ├─ 否 → abort
   │  └─ 是 → 继续

1. 读上游
   ├─ docs/REQUIREMENTS.md (必读)
   ├─ docs/adr/*.md (若存在)
   └─ architecture/*.md (若存在,提取技术栈)

2. 生成 HLD（项目级）
   ├─ 填 hld.md 模板：项目概述 + 整体架构图 + 业务流程概览 + 服务清单与定位
   └─ 遇决策点暂停问用户（混合模式）

3. 问「需要生成哪些 Design?」
   ├─ 用户指定服务+类型（如"core-service 的 api + batch"）
   ├─ 【硬门】对每个 Design 首问类型,未答则 abort
   └─ 按类型走对应模板生成 design-*.md

4. 问「是否需要 glossary.md?」
   ├─ 项目已有 CONTEXT.md 或 glossary 且本次无新术语 → 建议跳过
   ├─ 是 → 走 glossary 模板生成
   └─ 否 → 跳过

5. 命名落盘到 docs/
   ├─ [PROJECT]_HLD_v1.0_YYYYMMDD.md
   ├─ [PROJECT]_design_[type]_[service]_v1.0_YYYYMMDD.md
   └─ [PROJECT]_GLOSSARY_v1.0_YYYYMMDD.md (若出)

6. 报告路径,提示用户审阅
```

### 硬门执行细则（Design 层）

| 情况 | 处理 |
|---|---|
| 用户回答 "api" / "batch" / "worker" | 锁定类型，生成对应 Design |
| 用户回答 "API" / "Api" 等大小写变体 | 大小写归一化后锁定 |
| 用户回答 "全部" / "三种都要" | 提示「一份 Design 只能一种类型，请指定本次先生成哪一种」再问一次 |
| 用户回答 "你看着办" / "随便" / "都行" | abort |
| 用户回答 "混合" / "既有 API 又有定时" | 提示「混合服务需出多份 Design，本次先做哪一份？请指定主类型」再问一次 |
| 用户回答无关内容 | abort |
| 用户不答 / 跳过 / 沉默 | abort |

---

## 4. Design 模板结构（三类统一）

三类 Design 模板共享统一结构（5 节），公共节在前，专属节在后：

| 节 | API | Batch | Worker |
|---|---|---|---|
| §1 概述（公共）| 关键约束 + 技术栈 + 文档边界 | 同左 | 同左 |
| §2 数据模型（公共）| 实体说明 + ER 图 | 同左 | 同左 |
| §3 本次范围（专属）| 接口概览表 | 任务概览表 | 订阅概览表 |
| §4 处理时序图（专属）| 按接口分小节 + sequenceDiagram | 按任务分小节 + sequenceDiagram | 按订阅分小节 + sequenceDiagram |
| §5 类型细节（专属）| 关键校验与错误码（按接口分小节 + 菱形决策树）| 调度计划（按任务分小节）| 消息模型与消费语义（按订阅分小节）|
| §6 幂等与重试（专属）| — | 按任务分小节 + 菱形决策树 | 按订阅分小节 + 菱形决策树 |

> API 模板只有 5 节（§6 留空）；Batch 和 Worker 有 §6。

---

## 5. 模板文件结构

```
.agents/skills/hld-designer/
├── SKILL.md
└── templates/
    ├── hld.md                    ← 项目级 HLD 模板 (4 节)
    ├── design/
    │   ├── design-api.md         ← API Design 模板 (5 节)
    │   ├── design-batch.md       ← Batch Design 模板 (6 节)
    │   └── design-worker.md      ← Worker Design 模板 (6 节)
    └── glossary.md               ← glossary 模板 (独立于类型)
```

### glossary.md 说明

- **风格**：参考 `CONTEXT.md` — 每个术语三段：术语名（加粗）+ 定义段落 + `_Avoid_`（斜体）近义词列表
- **不强制每次出 Design 都生成**：
  - 初版可基于项目根目录的 `CONTEXT.md`（若存在）复制整理为基线
  - 后续新 feature 引入新术语时，追加到已有 glossary 末尾，递增版本号
  - 若本次 feature 未引入新术语且已有 glossary/CONTEXT.md，则无需产出
- **独立于类型**：一份 glossary 覆盖整个项目领域术语

---

## 6. 通用纪律

1. **硬门不可绕过**——生成 Design 时分类未答，整个 skill 不启动 Design 生成
2. **一份 Design 只能是一种类型**——混合服务出多份 Design，每份只记该类型内容
3. **PRD 必需**——没 PRD 直接 abort，不替用户编造需求
4. **技术栈不硬绑**——`architecture/` 存在就读，不存在才问
5. **glossary 独立于类型**——一份 glossary 覆盖整个项目领域术语；不强制每次出 Design 都生成
6. **流程图必须**——HLD 必有整体架构图 + 业务流程概览；Design 必有处理时序图（mermaid）
7. **数据模型不写 DDL**——Design 只列实体 + 关键字段（ER 图），DDL 是 write-detail-design 阶段的事
8. **不写接口 5 要素契约**——Design 接口设计只列方法名 + 说明；但**错误码在 Design §5 关键校验与错误码中定义**，detail design 引用本表不重定义。Request/Response 字段级定义仍是 detail design 的活
9. **§4 和 §5 按接口/任务/订阅分小节**——Design 的"接口处理时序图"（§4）和"关键校验与错误码"（§5.1）都按接口分小节；Batch/Worker 同理按任务/订阅分小节。简单接口/任务也写，不省略
10. **HLD 和 Design 是 feature 级文档**——只写本次涉及的服务/任务/接口/订阅，不是全量清单。后续新 feature 走独立文档，不追加
11. **Architecture 不在本 skill 范围**——由独立 skill 产出（技术选型 + 服务依赖图 + 仓库地址 + Helm + 数据库划分）

---

## 7. 命名规范

```
[PROJECT]_HLD_v[版本号]_[日期].md                          — 项目级高层设计
[PROJECT]_design_[type]_[service]_v[版本号]_[日期].md       — 服务级详细设计
[PROJECT]_GLOSSARY_v[版本号]_[日期].md                      — 术语表
```

- **PROJECT**：项目缩写大写（如 `CS` = C-Star）
- **type**：`api` / `batch` / `worker`
- **service**：服务名小写（如 `core-service`）
- **版本号**：`vX.Y`，初版默认 `v1.0`
- **日期**：`YYYYMMDD`

---

## 8. 类型定义

- **API**：同步请求/响应。对外暴露 REST/gRPC handler，请求进来立即处理返回
- **Batch**：定时批处理。由调度器（如 xxl-job/Quartz）按 cron 触发，处理一批数据
- **Worker**：异步消息消费者。从 MQ（如 Kafka/RabbitMQ/RocketMQ）消费消息，有重试/幂等/DLQ 考量

**batch 和 worker 的区别**：batch 是"时间驱动"（到点就跑，处理存量数据）；worker 是"事件驱动"（来消息就处理，处理增量事件）

---

## 9. 文档层级关系

```
PRD（需求）
  ↓
HLD（项目级全局设计）← 本 skill 产出
  ↓
Design（服务级按类型详细设计）← 本 skill 产出
  ↓
Detail Design（字段级 As-Is/To-Be 改动设计）← write-detail-design skill 产出
  ↓
Architecture（技术选型 + 服务依赖 + 仓库地址 + Helm）← 独立 skill 产出
```

---

## 10. 不适用场景

- **detail design**——用 `write-detail-design` skill
- **API 文档**——用 `gen-api-doc` skill
- **Architecture**——由独立 skill 产出
- **单文件改 typo / 一句话**——直接 patch
- **ADR**——用 `domain-modeling` skill 的 ADR 流程

---

## 11. 依赖关系

### 上游（本 skill 的输入）

| 文档 | 必需 | 来源 |
|---|---|---|
| `docs/REQUIREMENTS.md` | ✅ 必需 | 用户编写 |
| `docs/adr/*.md` | 可选 | 用户编写 / `domain-modeling` skill 产出 |
| `architecture/*.md` | 可选 | 用户编写 / 独立 Architecture skill 产出 |
| `CONTEXT.md` | 可选 | `domain-modeling` skill 产出 |

### 下游（消费本 skill 产出）

| 文档 | 消费者 |
|---|---|
| `*_HLD_*.md` | 全项目（项目级全局视角）+ Architecture skill（作为技术选型依据）|
| `*_design_*.md` | `write-detail-design` skill（作为 detail design 的上游）|
| `*_GLOSSARY_*.md` | 全项目（术语统一参照）|

---

## 12. 旧 skill 清理

- `.agents/skills/hld-designer-v3/` 整目录已删除
- `docs/superpowers/specs/2026-07-28-hld-designer-skill-design.md`（v3 spec）保留作历史记录

---

## 13. 风险与缓解

| 风险 | 缓解 |
|---|---|
| HLD 过于宏观，缺少操作指引 | HLD §4 服务清单与定位 + Design 两层互补，HLD 看全景，Design 看细节 |
| Design 分类门控生硬 | 这是设计意图——分类是硬门。abort 时明确告知原因和如何继续 |
| 混合服务需要出多份 Design | 流程第 3 步明确提示"混合服务出多份，本次先做哪一份"|
| 技术栈来源分散 | 优先读 `architecture/`，缺失时才采访；ADR 作为约束来源 |
| Architecture 不在本 skill 范围，用户可能混淆 | SKILL.md 明确说明，并在文档层级关系图里标出 Architecture 由独立 skill 产出 |
