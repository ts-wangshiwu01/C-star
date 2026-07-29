---
name: hld-designer
description: |
  High Level Design 产出器 — 4 入口:
  A. 从 PRD 生成项目级 HLD(整体设计)
  B. 从项目级 HLD 生成服务级 HLD(每个微服务一份)
  C. 从服务级 HLD + ticket 生成 feature 级 HLD(每个功能一份)
  D. feature review + 实现完成后,用户手动触发合并回服务级/项目级

  基于 doc-lifecycle 规范(8字段头部 + 概述 + 流程图),不绑定技术栈。
  混合模式:读上游文档自动填业务上下文,遇技术决策点暂停问用户。

  触发词:生成 HLD、写 high level design、做 HLD、项目级 HLD、服务级 HLD、
  feature 的 HLD、合并 HLD、从 PRD 出 HLD。

  不适用:detail design(用 write-detail-design)、API 文档(用 gen-api-doc)、
  完整架构文档(架构在 architecture/ 独立定义)。
---

# HLD Designer — High Level Design 产出器

## Overview

基于 doc-lifecycle 规范,产出"概述 + 处理流程图 + 数据模型 + 接口设计"为核心的 HLD。
不写完整架构(架构在 `architecture/` 独立定义),只聚焦"这个功能/需求怎么实现的流转链路"。

## 三级 HLD 层级

```
① 项目级 HLD  ── 整体设计,基于 PRD
        │
        │  入口 B 触发:为某个微服务出 HLD
        ↓
② 服务级 HLD  ── 每个微服务一份,基于项目级 HLD
        │
        │  入口 C 触发:为某个 feature 出 HLD
        ↓
③ Feature 级 HLD ── 每个功能一份,基于服务级 HLD + ticket
        │
        │  feature review + 实现 + 测试 + 上线后
        │  ↓
        │  用户手动触发入口 D
        ↓
④ 合并回服务级 / 项目级(不自动,间隔可能数周到数月)
```

**关键纪律**:合并是用户事后主动触发,不是 feature 写完自动连跑。中间可能经过 review、修改、开发、测试、上线,间隔数周至数月。

## 路由决策树

```
用户说什么?
    │
    ├─ "从 PRD 生成 HLD" / "项目级 HLD" / "整体设计"
    │       ↓
    │   入口 A — entry-init-project
    │   读 PRD + ADR → 采访技术决策点 → 生成项目级 HLD 初稿
    │
    ├─ "服务级 HLD" / "X 服务的 HLD" / "微服务 HLD"
    │       ↓
    │   入口 B — entry-create-service
    │   读项目级 HLD → 采访服务边界决策 → 生成服务级 HLD
    │
    ├─ "feature 的 HLD" / "功能的 HLD" / ticket + HLD
    │       ↓
    │   入口 C — entry-create-feature
    │   读服务级 HLD + ticket → 采访 feature 决策点 → 生成 feature HLD
    │
    └─ "合并 HLD" / "feature 完成了,更新服务级 HLD" / "把 feature 合并回项目级"
            ↓
        入口 D — entry-merge-feature
        读 feature HLD + 上级 HLD → diff → 生成 patch → 人审确认
        (用户手动触发,间隔可能数周到数月)
```

## 通用纪律

1. **产物结构固定** 4 章:概述 + 处理流程图 + 数据模型 + 接口设计。不擅自加章节(架构在 `architecture/`)
2. **混合模式**:读上游文档时自动填业务上下文,遇技术决策点必须暂停问用户
3. **不绑定技术栈**:栈、协议、DB、部署都由用户在采访中确定,skill 不假设默认值
4. **流程图必须**:每个 HLD 必有处理流程图(mermaid sequence/flow),体现"谁流转给谁"
5. **入口 D 不自动**:合并是用户手动触发,不连跑;间隔可能数周到数月
6. **8 字段头部**:沿用 doc-lifecycle scene-create 规范
7. **服务级 API 契约**:服务级 HLD 的接口设计必须包含完整 API 契约,每个接口 5 要素缺一不可:
   - **Endpoint**:REST = HTTP 方法 + 路径(如 `POST /api/appreciations`);gRPC = 全限定方法名(如 `CoreService/SendAppreciation`)
   - **Request**:字段名 + 类型 + 是否必填 + 说明(表格或 protobuf 定义)
   - **Response**:成功响应字段 + 类型 + 说明(表格或 protobuf 定义)
   - **Error**:错误码 + 触发条件 + HTTP 状态码(REST)/ gRPC status code;至少覆盖校验失败 / 资源不存在 / 权限不足 / 业务规则违反
   - **说明**:一句话描述接口用途

   排列格式按服务分表,每个服务一个独立小节,示例:
   ```
   ### 4.1 API Gateway(对外 REST)
   #### POST /api/appreciations
   **说明**:发送赞赏
   **Request**:
   | 字段 | 类型 | 必填 | 说明 |
   |---|---|---|---|
   | giver_id | int64 | 是 | 发送者 ID |
   ...
   **Response(200)**:
   | 字段 | 类型 | 说明 |
   |---|---|---|
   | appreciation_id | int64 | 赞赏记录 ID |
   ...
   **Error**:
   | HTTP 状态码 | 错误码 | 触发条件 |
   |---|---|---|
   | 400 | INVALID_FLOWERS | flowers < 1 或为空 |
   ...
   ```
   gRPC 接口同样格式,把 HTTP 状态码换成 gRPC status code(如 `INVALID_ARGUMENT` / `FAILED_PRECONDITION` / `NOT_FOUND`)。

   **错误码命名规范**:
   - 全大写 + 下划线分隔,如 `QUOTA_EXCEEDED`
   - 业务错误用业务语义命名(如 `SELF_APPRECICIATION`,不用 `VALIDATION_ERROR`)
   - 通用错误用通用命名(如 `UNAUTHORIZED` / `FORBIDDEN` / `NOT_FOUND`)

   **反例**(不要这样写):只列方法名没有 Request/Response/Error —— reviewer 看不到接口契约,无法判断字段是否齐全、错误处理是否覆盖。

   **适用范围**:服务级 HLD 必须含完整 5 要素;项目级 HLD 只列接口分类和归属服务;Feature 级 HLD 只列变更部分。

## References 分区

| 入口 | 方法文件 | 说明 |
|---|---|---|
| A. 项目级初始化 | `references/entry-init-project.md` | 从 PRD 生成项目级 HLD |
| B. 服务级创建 | `references/entry-create-service.md` | 从项目级 HLD 生成服务级 HLD |
| C. Feature 级创建 | `references/entry-create-feature.md` | 从服务级 + ticket 生成 feature HLD |
| D. Feature 合并回上级 | `references/entry-merge-feature.md` | 用户手动触发,diff + patch + 人审 |
| 模板规范 | `references/template-rules.md` | 8 字段头部 + 4 章规范 + 接口排列规则 |
| Patch 生成方法 | `references/patch-generation.md` | 合并时生成 patch 的具体方法 |

## Templates

| 模板 | 文件 | 用途 |
|---|---|---|
| 项目级 HLD | `templates/hld-project.md` | 入口 A 产出 |
| 服务级 HLD | `templates/hld-service.md` | 入口 B 产出 |
| Feature 级 HLD | `templates/hld-feature.md` | 入口 C 产出 |

## 校验

每个 HLD 产出后必跑 `scripts/check_hld_structure.py`:
- 8 字段头部齐全
- 必备章节(概述 + 处理流程图 + 数据模型 + 接口设计)存在
- 流程图是 mermaid 语法
- 文件名符合 `[项目缩写]_HLD_[级别]_[模块]_[版本号]_[日期].md`

退出码 = 0 才算完成。

## 命名规范

```
[项目缩写]_HLD_[级别]_[模块]_[版本号]_[日期].md
```

- 项目缩写:大写首字母(如 `CS` = C-Star)
- 级别:`PROJECT` / `SERVICE_[服务名]` / `FEATURE_[服务名]_[ticket-slug]`
- 版本号:`vX.Y`
- 日期:`YYYYMMDD`

示例:
- `CS_HLD_PROJECT_v1.0_20260728.md`
- `CS_HLD_SERVICE_core-service_v1.0_20260728.md`
- `CS_HLD_FEATURE_core-service_US-001-send-appreciation_v1.0_20260728.md`

## 不适用场景

- detail design(用 write-detail-design skill)
- API 文档(用 gen-api-doc skill)
- 完整架构文档(架构在 `architecture/` 独立定义)
- 单文件改 typo / 一句话(直接 patch)
