# C-Star 技术选型

> **状态**:草稿(待 review)
> **日期**:2026-07-28
> **基于**:C-Star PRD + 6 个 ADR

## 1. 概述

C-Star 是公司级员工赞赏平台。为支撑 PRD 描述的功能,采用微服务架构,3 个核心服务 + 1 个 API 网关,服务间 gRPC 通信,每服务独立数据库 schema,部署在 Kubernetes。

## 2. 技术栈

| 类别 | 选型 | 说明 |
|---|---|---|
| **后端框架** | Micronaut | 编译期注入,启动快,内存小,适合容器化部署 |
| **构建工具** | Gradle | 多模块管理方便,Micronaut 官方推荐 |
| **数据访问** | Micronaut Data JDBC(@JdbcRepository) | 轻量 Repository,基于注解,编译期生成实现,比 JPA 简单 |
| **数据库** | MariaDB | 成熟稳定,运维成本低 |
| **服务间通信** | gRPC | 类型安全,Protobuf 契约,内部调用性能好 |
| **部署** | Kubernetes | 跟现有项目一致,复用 ArgoCD GitOps 流程 |
| **定时任务** | xxl-job | 可视化调度,内置失败重试,直接配 Cron + 时区不用换算 UTC |
| **SSO** | 公司 ALDP | 复用公司单点登录,不自己实现认证 |
| **前端** | Vue 3 + Element Plus | 简单 CRUD 场景开箱即用,上手快,详见 [ADR-0007](../docs/adr/0007-frontend-vue3-element-plus.md) |

## 3. 架构图

```
                    ┌───────────────────────────────────┐
                    │         用户 / 浏览器              │
                    │  (员工 / Admin / 全员浏览者)      │
                    └────────────────┬──────────────────┘
                                     │ HTTPS / REST + JWT (ALDP)
                                     ▼
                    ┌───────────────────────────────────┐
                    │          API Gateway               │
                    │       (api-gateway)                │
                    │  - SSO 验证(JWT)                  │
                    │  - 路由到后端 gRPC 服务            │
                    └───────┬───────────────┬───────────┘
                            │ gRPC          │ gRPC
                            ▼               ▼
              ┌─────────────────────┐   ┌─────────────────────┐
              │   Core Service      │   │   Admin Service     │
              │  (core-service)     │   │  (admin-service)    │
              │                     │   │                     │
              │  - 发送赞赏          │   │  - Category CRUD    │
              │  - 收发列表          │   │  - 配额配置         │
              │  - 双榜查询          │   │  - 审计统计         │
              │  - 配额扣减          │   │                     │
              └──────────┬──────────┘   └──────────┬──────────┘
                         │ JdbcRepository            │ JdbcRepository
                         ▼                          ▼
              ┌─────────────────────┐   ┌─────────────────────┐
              │  MariaDB            │   │  MariaDB            │
              │  (core schema)     │   │  (admin schema)     │
              └─────────────────────┘   └─────────────────────┘
```

## 4. 服务职责

| 服务 | 职责 | 对外协议 | 对内协议 |
|---|---|---|---|
| API Gateway | SSO 验证 + 请求路由 | REST(用户友好) | gRPC → core / admin |
| Core Service | 赞赏主流程(发送/列表/榜单/配额) | gRPC(只对 gateway) | Micronaut Data JDBC → MariaDB |
| Admin Service | 管理功能(Category/配额/审计) | gRPC(只对 gateway) | Micronaut Data JDBC → MariaDB |

## 5. 数据库划分

每服务独立 schema(Database-per-service),服务间不跨库查表,需要对方数据走 gRPC。

### 5.1 core schema

| 表 | 用途 |
|---|---|
| employee | 员工记录。首次 SSO 登录时懒同步创建(ADR-0004),只存 sso_id + name,不维护离职状态(ADR-0006) |
| appreciation | 赞赏记录。记录谁给谁送了多少 Red Flower + Message + Category,写入后不可变(ADR-0003),全公开(ADR-0003) |
| period_quota | 周期配额。记录每个员工每个周期的配额使用情况,每日 00:00 北京时间重置(ADR-0005),不结转 |
| ranking | 榜单。维护本周期榜(period_date = 当天)和全时期榜(period_date = NULL)的累计 Red Flower 数,读优化 |

### 5.2 admin schema

| 表 | 用途 |
|---|---|
| category | 赞赏分类。5 个预设(Teamwork / Excellence / Innovation / Customer Focus / Going Above & Beyond),Phase 2 Admin 可运行时增删,退役是软删除不删历史 |
| quota_config | 配额配置。存默认配额值和周期长度,Phase 2 Admin 可改 |
| audit_log | 审计日志。记录 Admin 的所有操作(谁、什么时候、改了什么),供 Admin 审计统计查询 |
