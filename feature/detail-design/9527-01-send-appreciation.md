# 9527-01-send-appreciation — core-service 新增"发送赞赏"接口

> **Ticket**: 9527
> **Scope repos**: core-service（greenfield）

## 1. 名字

9527-01-send-appreciation — 在 core-service 新增"发送赞赏"API：Giver 给 Receiver 送 1~N 朵红花，原子完成"校验 → 扣配额 → 写记录 → 更双榜"，记录立即全公司公开。

## 2. 改动点

> **GREENFIELD 场景**：core-service 源码不存在，以下文件路径按 system-design 服务划分 + Micronaut 惯用分层推断（`project-structure.md` 规则不存在，若后续补充需回填对齐）。
> 所有"改动"均为新建。

| # | 仓库 | 文件（推断，新建） | 类/函数 | 改动 |
|---|------|------|--------|------|
| 1 | core-service | `application/service/AppreciationService.kt`（新建） | `send(appreciation)` | 发送赞赏应用服务：编排校验→扣配额→写记录→更双榜，单事务 |
| 2 | core-service | `application/service/PeriodQuotaService.kt`（新建） | `reserveAndConsume(giverId, flowerCount)` | 校验剩余配额并扣减，返回扣减后剩余 |
| 3 | core-service | `application/service/RankingService.kt`（新建） | `upsert(giverId, receiverId, flowers, periodDate)` | 更新周期榜与全时期榜累计红花数 |
| 4 | core-service | `adapter/grpc/SendAppreciationGrpcHandler.java`（新建） | `handle(request)` | 暴露 gRPC endpoint，DTO↔域模型转换，返回错误码 |
| 5 | core-service | `domain/Appreciation.java`（新建） | 实体 | 赞赏记录领域模型，不可变 |
| 6 | core-service | `domain/PeriodQuota.java`（新建） | 实体 | 周期配额领域模型 |
| 7 | core-service | `infrastructure/repository/JdbcAppreciationRepository.java`（新建） | `insert()/findList()` | 写入/查询赞赏记录 |
| 8 | core-service | `infrastructure/repository/JdbcPeriodQuotaRepository.java`（新建） | `find/consume/reset` | 配额读写 |
| 9 | core-service | `infrastructure/repository/JdbcRankingRepository.java`（新建） | `upsert()` | 双榜累计 upsert |

## 3. As-Is 和 To-Be

### 3.1 As-Is（现状）

`N/A — greenfield, no prior code`（core-service 不存在，无既有实现可比）。

### 3.2 To-Be（改后）

**新行为**（自然语言步骤，核心在 core-service 内单事务完成）：

0. 依次执行下列校验（Check 表），任一失败即拒绝，不产生任何写：
1. 原子扣减：Giver 的周期配额 `period_quota` 扣 flowerCount，若配额不足则拒绝（见 CK-06）。
2. 写入不可变记录 `appreciation`（ADR-0003）：不可编辑、不可撤销、全公开。
3. 更新双榜 `ranking`：周期榜（period_date=当天）与全时期榜（period_date=NULL）各加 flowerCount。
4. 返回确认；记录立即全员可见（无通知）。

**校验规则（Check）** — 发送赞赏的全部校验，按序执行：

| # | Check | 校验内容 / 条件 | 验证通过才继续 | 失败 → 错误码 | 对应不变量 | 对应测试 |
|---|---|---|---|---|---|---|
| CK-01 | 参数完整 | `request` 字段齐全、非空 | ✅ | APPR_INTERNAL | I7 | §4 Happy |
| CK-02 | Giver 存在 | giver_sso_id 在 employee 表有本地记录 | ✅ | APPR_INVALID_RECEIVER (1) | I2 | §4 Error |
| CK-03 | Receiver ≠ Giver | receiver_sso_id ≠ giver_sso_id | 拒绝自赠 | APPR_INVALID_RECEIVER (1) | I1 | §4 Error / Invariant I1 |
| CK-04 | Receiver 已存在 | receiver_sso_id 在 employee 有本地记录（已登录过） | ✅ | APPR_INVALID_RECEIVER (1) | I2 | §4 Error / Invariant I2 |
| CK-05 | Message 合法 | message 非空、非纯空白、长度≤500 | ✅ | APPR_EMPTY_MESSAGE (2) | I5 | §4 Error |
| CK-06 | flowerCount 下界 | `1 ≤ flowerCount` | ✅ | APPR_INVALID_FLOWER_COUNT (7) | I3 | §4 Boundary |
| CK-07 | flowerCount ≤ 剩余配额 | `flowerCount ≤ (default_quota - consumed)` | 允许 | APPR_INSUFFICIENT_QUOTA (4) | I3, I4 | §4 Boundary / Invariant I3,I4 |
| CK-08 | Category 活跃 | category_id 是 admin 活跃分类之一 | ✅ | APPR_INVALID_CATEGORY (3) | I8 | §4 Error / Invariant I8 |
| CK-09 | 周期日期归属 | 按北京时间 UTC+8 计算 period_date，落在当日周期 | ✅ | —— | I9 | §4 Boundary / Invariant I9 |

> **跨服务契约说明**：发送接口在 core-service 内完成；Category 活跃集来自 admin-service（Phase 2 前为配置文件里 5 个预设）。

**保留的不变量（Invariant）**（§4 不变量测试逐条对应）：

| # | 不变量 | 依据 |
|---|---|---|
| I1 | Giver ≠ Receiver | Giver 不能给自己送红花 |
| I2 | Receiver 本地记录必须已存在 | ADR-0004 |
| I3 | 单次红花数 ∈ [1, Giver 剩余配额] | ADR-0002 / ADR-0001 |
| I4 | 剩余配额不为负；扣减后剩余 = 扣减前 - flowerCount | ADR-0001 |
| I5 | appreciation 记录单调不可变、不可撤销、全公开 | ADR-0003 |
| I6 | 周期榜 period_date=当天；全时期榜 period_date=NULL | system-design §5.1 |
| I7 | 同一发送器完成后：配额 + 记录 + 双榜三者一致（原子） | system-design 架构 |
| I8 | Category 必须为活跃分类 | CONTEXT / system-design §5.2 |
| I9 | 用时区：当日周期按北京时间 UTC+8 结算 | ADR-0005 |

**跨服务契约说明**：发送接口在 core-service 内完成；Category 活跃集来自 admin-service（Phase 2 前为配置文件里 5 个预设）。

### 3.3 改前 vs 改后对比

（§3.3 省略：GREENFIELD 无改前实现可比。）

### 3.4 DDL / 表结构变更

**Greenfield 新建型** — core schema 三张表 + employee（9527 涉及校验 receiver 存在，若已有则不改）：

> 完整表结构（字段级契约，locale 中文注释保留原文语义）

#### 表 `employee`
```sql
CREATE TABLE employee (
  id            BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  sso_id        VARCHAR(64)  NOT NULL COMMENT 'SSO 账号（ALDP），全局唯一',
  name          VARCHAR(128) NOT NULL COMMENT '员工姓名',
  created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间（首次登录懒同步）',
  updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  UNIQUE KEY uq_employee_sso_id (sso_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='员工记录，首次 SSO 登录懒同步创建（ADR-0004）';
```

#### 表 `appreciation`
```sql
CREATE TABLE appreciation (
  id             BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  giver_id       BIGINT      NOT NULL COMMENT '赠送者 employee.id，不可变',
  receiver_id    BIGINT      NOT NULL COMMENT '接收者 employee.id，不可变',
  category_id    BIGINT      NOT NULL COMMENT '分类 id，不可变（退役置位仍保留引用）',
  flower_count   INT         NOT NULL COMMENT '本次赠送的红花数 1~N（ADR-0002）',
  message        VARCHAR(500) NOT NULL COMMENT '附言，必填，全公司可见（ADR-0003）',
  sent_at        DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '赠送时间（北京时间语义，UTC 存储）',
  KEY idx_app_receiver (receiver_id),
  KEY idx_app_giver (giver_id),
  KEY idx_app_category (category_id),
  CONSTRAINT fk_app_giver FOREIGN KEY (giver_id) REFERENCES employee(id),
  CONSTRAINT fk_app_receiver FOREIGN KEY (receiver_id) REFERENCES employee(id),
  CONSTRAINT fk_app_category FOREIGN KEY (category_id) REFERENCES category(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='赞赏记录，写入后不可变、全公开（ADR-0003）';
```

> 注：`Category` 字段来源 admin schema（category 表），此处引用 category.id，具体 RETIRED 处理见各 DDL。category_id 外键在 Phase 1 预设 5 条。

#### 表 `period_quota`
```sql
CREATE TABLE period_quota (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  employee_id BIGINT      NOT NULL COMMENT '员工 id',
  period_date DATE        NOT NULL COMMENT '结算周期起始日（北京时间！），如 2026-08-11',
  period_len  INT         NOT NULL DEFAULT 1 COMMENT '周期长度（天），默认 1（ADR-0005）',
  default_quota   INT     NOT NULL COMMENT '本期默认配额数',
  consumed    INT         NOT NULL DEFAULT 0  COMMENT '本期已消耗红花数',
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_quota_emp_period (employee_id, period_date),
  CONSTRAINT fk_q_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='周期配额，每日 00:00 北京时间重置（ADR-0005）；发放数计量（ADR-0001）';
```

#### 表 `ranking`
```sql
CREATE TABLE ranking (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键',
  employee_id BIGINT      NOT NULL COMMENT '员工 id',
  period_date DATE        NULL COMMENT '周期榜=当天；全榜=NULL',
  total       BIGINT      NOT NULL DEFAULT 0 COMMENT '累计红花数',
  updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uq_rank_emp_period (employee_id, period_date),
  CONSTRAINT fk_rk_employee FOREIGN KEY (employee_id) REFERENCES employee(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='榜单读优化累计表（system-design §5.1）';
```

### 3.5 Proto 契约（gRPC / proto）

**新增 proto message**（core-service 对外 gRPC 契约，字段级，SendAppreciation 属 core）：

```proto
syntax = "proto3";
package cstar.core;

message SendAppreciationRequest {
  string giver_sso_id = 1;    // Giver 的 SSO 账号
  string receiver_sso_id = 2; // Receiver 的 SSO 账号
  int32  flower_count = 3;    // 红花数，1..N
  int64  category_id = 4;     // 分类 id
  string message = 5;         // 必填建议
}

message SendAppreciationResponse {
  int64  appreciation_id = 1;
  int32  remaining_quota = 2; // 发送后 Giver 剩余配额
}

// 错误：AppreciationErrorCode (enum)
enum AppreciationErrorCode {
  APPR_OK = 0;
  APPR_INVALID_RECEIVER = 1;        // Receiver 无效：本地无记录 / 自己送自己
  APPR_EMPTY_MESSAGE = 2;            // 消息为空或纯空白
  APPR_INVALID_CATEGORY = 3;         // 分类不存在或未启用
  APPR_INSUFFICIENT_QUOTA = 4;       // 剩余配额不足（含不可结转）
  APPR_INTERNAL = 5;                 // 事务失败 / 系统内部错误
  APPR_RECORD_IMMUTABLE = 6;         // 尝试修改不可变记录（保留）
  APPR_INVALID_FLOWER_COUNT = 7;     // 红花数越界（<1）
}
```

### 3.6 DTO ↔ 表字段映射

对应 `SendAppreciation` 入参 DTO 与域对象 → 表字段映射（本地 com.xxx.cstar.appreciation 域对象）：

| DTO 字段 | 类型 | 对应表.字段 | 类型 | 可空 | 备注 |
|---|---|---|---|---|---|
| giverSsoId | String | employee.sso_id | VARCHAR(64) | NOT NULL | 定位 giver_id |
| receiverSsoId | String | employee.sso_id | VARCHAR(64) | NOT NULL | 定位 receiver_id → appreciation.receiver_id |
| flowerCount | int | appreciation.flower_count | INT | NOT NULL | |
| categoryId | long | appreciation.category_id | BIGINT | NOT NULL | 校验有效 |
| message | String | appreciation.message | VARCHAR(500) | NOT NULL | |

---

## 4. 测试

**仓库**：`core-service`（greenfield）。按维度分组。522 Bad（GREENFIELD 无既有路径，回归标 ⚠️）。

| 维度 | 用例 | 期望 |
|------|------|------|
| 正向 (Happy) | 合规 Giver 给合法 Receiver 送 1 朵，配额、记录、双榜一致更新 | ✅ 剩余=DEFAULT-1，记录生成，两个 ranking 各 +1 |
| 正向 (Happy) | Giver 送 N 朵（N=剩余配额全部消耗） | ✅ 剩余 0，记录 flower_count=N |
| 正向 (Happy) | 今日首次为当天 + 全榜都加花 | ✅ period_date=今天 & NULL 行均 +N |
| 异常 (Error) | 发送给未登录用户（本地无 employee 记录） | ❌ APPR_INVALID_RECEIVER (1) |
| 异常 (Error) | Giver == Receiver（自己赠自己） | ❌ APPR_INVALID_RECEIVER (1) |
| 异常 (Error) | 消息为空 / 空白 | ❌ APPR_EMPTY_MESSAGE (2) |
| 异常 (Error) | 分类不存在 或 未激活 | ❌ APPR_INVALID_CATEGORY (3) |
| 异常 (Error) | 剩余配额 < flower_count | ❌ APPR_INSUFFICIENT_QUOTA (4)，不产生任何写 |
| 边界 (Boundary) | flower_count = 1（下界） | ✅ |
| 边界 (Boundary) | flower_count = 0 或负数（越下界） | ❌ APPR_INVALID_FLOWER_COUNT (7) |
| 边界 (Boundary) | flower_count = 剩余配额（上界） | ✅ 剩余 0 |
| 边界 (Boundary) | 跨日/时区：UTC 边界附近发送 → 归属正确北京时间日期 | ✅ 正确落到北京日期 period |
| 边界 (Boundary) | 配额恰好用完后再试送 1 次 | ❌ APPR_INSUFFICIENT_QUOTA |
| 回归 (Regression) | 旧有读取接口（ReceivedList / 双榜查询）不受影响 | ⚠️ 待补（greenfield） |
| 不变量 (Invariant) | I1: 自赠被拒 | ❌ APPR_INVALID_RECEIVER |
| 不变量 (Invariant) | I2: 无本地记录的 receiver 被拒 | ❌ APPR_INVALID_RECEIVER |
| 不变量 (Invariant) | I3: flower_count ∈ [1, remaining] | ✅ 域内成功；域外拒 |
| 不变量 (Invariant) | I4: 剩余 = 扣前 - flower_count，永不为负 | ✅/❌（不足拒） |
| 不变量 (Invariant) | I5: 记录生成后不可改/不可撤 | ❌（乘 AFTER/调用修改接口） |
| 不变量 (Invariant) | I6: 周期榜与全榜计数与记录一致 | ✅ 对比 count |
| 不变量 (Invariant) | I7: 配额+记录+双榜原子（失败回滚） | ✅ 事务失败时无部分写入 |
| 不变量 (Invariant) | I8: 分类有效性 | ❌ 无效分类拒 |
| 不变量 (Invariant) | I9: 北京时区结算 | ✅ 时区用例 |