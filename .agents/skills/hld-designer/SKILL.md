---
name: hld-designer
description: |
  产出 HLD（项目级高层设计）+ Design（按服务入口类型的详细设计）两层文档。

  HLD：项目级一份文档，描述整个工程全貌——有什么服务、怎么工作、每个服务什么定位、
  整体架构图 + 业务流程概览 flow 图。让用户和 reviewer 一眼看清项目结构。

  Design：按服务+入口类型（api/batch/worker）产出，每服务每类型一份。
  api=同步请求/响应；batch=定时批处理；worker=异步消息消费者。

  分类是硬门：生成 Design 时首问类型，用户未明确回答则立即 abort，
  不读任何上游文档，不出任何文件。

  PRD（docs/REQUIREMENTS.md）必需，无则 abort。
  技术栈从 architecture/ 自动读取，不存在时通过采访补齐。不硬绑技术栈。

  HLD 是 feature 级文档：只写本次涉及的任务/接口/订阅，不是服务全量清单。
  后续新 feature 走独立 HLD/Design，不追加到已有文档。

  Architecture 不在本 skill 范围——由独立 skill 产出
  （技术选型 + 服务依赖图 + 代码仓库地址 + Helm 配置 + 数据库划分）。

  触发词: 生成 HLD、出高层设计、从 PRD 出 HLD、出 design、生成设计文档。

  不适用: detail design（As-Is/To-Be/改动点/测试）用 write-detail-design skill；
  API 文档用 gen-api-doc skill；Architecture 由独立 skill 产出。
---

# HLD Designer — HLD + Design 两层文档产出器

## Overview

从 PRD + ADR + architecture（若存在）产出两层文档：

- **HLD**（项目级）：一份文档覆盖整个项目，描述工程全貌
- **Design**（服务级）：按服务+入口类型（api/batch/worker）产出，每服务每类型一份

分类是硬门——生成 Design 时首问类型，用户未明确回答则立即 abort。

## 两层产出

### 第一层：HLD（项目级）

| 文件 | 必需 | 模板 | 定位 |
|---|---|---|---|
| `hld.md` | ✅ 必出 | `templates/hld.md` | 项目级高层设计：整体架构图 + 业务流程概览 + 服务清单与定位 |

**HLD 结构**（4 节）：
1. 项目概述：业务背景 + 关键约束（ADR 摘要）
2. 整体架构图：mermaid graph 画所有服务 + 调用关系 + 数据流
3. 业务流程概览：mermaid flowchart 画项目核心业务主流程（跨服务串联）
4. 服务清单与定位：每个服务一节，说明核心职责 + 上游 + 下游 + 入口类型（api/batch/worker）

### 第二层：Design（服务级，按入口类型）

| 文件 | 必需 | 模板 | 定位 |
|---|---|---|---|
| `design-api.md` | 按需 | `templates/design-api.md` | API 类 Design：接口设计 + 时序图 + 校验与错误码 |
| `design-batch.md` | 按需 | `templates/design-batch.md` | Batch 类 Design：任务时序图 + 调度计划 + 幂等与重试 |
| `design-worker.md` | 按需 | `templates/design-worker.md` | Worker 类 Design：消费时序图 + 消息模型 + 幂等与重试 |
| `glossary.md` | 可选 | `templates/glossary.md` | 术语表（独立于类型） |

**一份 Design 只能是一种类型**——混合服务（如同时含 API 和定时任务）出多份 Design，每份只记该类型内容。

## 执行流程

```
0. 前置检查
   ├─ docs/REQUIREMENTS.md 存在?
   │  ├─ 否 → abort,提示用户先写需求文档
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

## 硬门执行细则（Design 层）

| 情况 | 处理 |
|---|---|
| 用户回答 "api" / "batch" / "worker" | 锁定类型，生成对应 Design |
| 用户回答 "API" / "Api" 等大小写变体 | 大小写归一化后锁定 |
| 用户回答 "全部" / "三种都要" | 提示「一份 Design 只能一种类型，请指定本次先生成哪一种」再问一次 |
| 用户回答 "你看着办" / "随便" / "都行" | abort，明确告知「分类是硬门，未明确不能继续」 |
| 用户回答 "混合" / "既有 API 又有定时" | 提示「混合服务需出多份 Design，本次先做哪一份？请指定主类型」再问一次 |
| 用户回答无关内容 | abort |
| 用户不答 / 跳过 / 沉默 | abort |

**门控设计要点**：
- **首问即门**——生成 Design 时第一个动作就是问类型，不读上游、不分析、不预处理
- **未答即停**——任何非明确回答都触发 abort，不重试、不引导、不猜测
- **混合服务显式提示**——告诉用户"一份 Design 一种类型，混合服务出多份"
- **门控不可绕过**——后续任何步骤都依赖此处的类型锁定

## Templates

| 模板 | 文件 | 产出 |
|---|---|---|
| HLD（项目级）| `templates/hld.md` | 项目级高层设计 |
| API Design | `templates/design/design-api.md` | 同步请求/响应类详细设计 |
| Batch Design | `templates/design/design-batch.md` | 定时批处理类详细设计 |
| Worker Design | `templates/design/design-worker.md` | 异步消息消费类详细设计 |
| Glossary | `templates/glossary.md` | 术语表（独立于类型）|

## 通用纪律

1. **硬门不可绕过**——生成 Design 时分类未答，整个 skill 不启动 Design 生成
2. **一份 Design 只能是一种类型**——混合服务出多份 Design，每份只记该类型内容
3. **PRD 必需**——没 PRD 直接 abort，不替用户编造需求
4. **技术栈不硬绑**——`architecture/` 存在就读，不存在才问
5. **glossary 独立于类型**——一份 glossary 覆盖整个项目领域术语；不强制每次出 Design 都生成
6. **流程图必须**——HLD 必有整体架构图 + 业务流程概览；Design 必有处理时序图（mermaid）
7. **数据模型不写 DDL**——Design 只列实体 + 关键字段（ER 图），DDL 是 write-detail-design 阶段的事
8. **不写接口 5 要素契约**——Design 接口设计只列方法名 + 说明；但**错误码在 Design §5 关键校验与错误码中定义**（校验规则 + 错误码清单），detail design 引用本表不重定义。Request/Response 字段级定义仍是 detail design 的活
9. **§4 和 §5 按接口/任务/订阅分小节**——Design 的"接口处理时序图"（§4）和"关键校验与错误码"（§5.1）都按接口分小节；Batch/Worker 同理按任务/订阅分小节。简单接口/任务也写，不省略
10. **HLD 和 Design 是 feature 级文档**——只写本次涉及的服务/任务/接口/订阅，不是全量清单。后续新 feature 走独立文档，不追加
11. **Architecture 不在本 skill 范围**——由独立 skill 产出（技术选型 + 服务依赖图 + 仓库地址 + Helm + 数据库划分）

## 命名规范

```
[PROJECT]_HLD_v[版本号]_[日期].md                          — 项目级高层设计
[PROJECT]_design_[type]_[service]_v[版本号]_[日期].md       — 服务级详细设计
[PROJECT]_GLOSSARY_v[版本号]_[日期].md                      — 术语表
```

- **PROJECT**：项目缩写大写（如 `CS` = C-Star），从用户询问得到
- **type**：`api` / `batch` / `worker`
- **service**：服务名小写（如 `core-service`）
- **版本号**：`vX.Y`，初版默认 `v1.0`
- **日期**：`YYYYMMDD`

示例：
- `CS_HLD_v1.0_20260803.md`
- `CS_design_api_core-service_v1.0_20260803.md`
- `CS_design_batch_core-service_v1.0_20260803.md`
- `CS_GLOSSARY_v1.0_20260803.md`

## 类型定义

三类服务入口的精确定义：

- **API**：同步请求/响应。对外暴露 REST/gRPC handler，请求进来立即处理返回。如发送赞赏、查询列表、查排行榜
- **Batch**：定时批处理。由调度器（如 xxl-job/Quartz）按 cron 触发，处理一批数据。如每日配额重置、周期性数据清理
- **Worker**：异步消息消费者。从 MQ（如 Kafka/RabbitMQ/RocketMQ）消费消息，有重试/幂等/DLQ 考量。如事件驱动型业务处理

**batch 和 worker 的区别**：
- batch 是"时间驱动"——到点就跑，处理存量数据
- worker 是"事件驱动"——来消息就处理，处理增量事件

## 文档层级关系

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

## 不适用场景

- **detail design**——用 `write-detail-design` skill（As-Is/To-Be/改动点/测试 4 段）
- **API 文档**——用 `gen-api-doc` skill
- **Architecture**——由独立 skill 产出（技术选型 + 服务依赖图 + 仓库地址 + Helm 配置 + 数据库划分）
- **单文件改 typo / 一句话**——直接 patch
- **ADR**——用 `domain-modeling` skill 的 ADR 流程

## 依赖关系

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
