# send-appreciation — 实现发送赞赏主流程(校验 + 原子事务 + 更新榜单)

> **Ticket**: 9527-01
> **Scope repos**: c-star-core-service + c-star-proto + c-star-database

## 1. 名字
send-appreciation — 实现发送赞赏主流程:接收 Giver 提交,校验 6 条业务规则,原子事务内扣配额+写记录,更新双榜,返回确认。

## 2. 改动点

| # | 仓库 | 文件 | 类/函数 | 改动 |
|---|------|------|--------|------|
| 1 | c-star-proto | .../cstar/appreciation.proto | SendAppreciation RPC | （新建）gRPC 契约:Request/Response message + 错误码枚举,字段级定义见 §3.5 |
| 2 | c-star-database | .../migrations/V1__init_core.sql | DDL | （新建）4 张表(employee/appreciation/period_quota/ranking)的 CREATE TABLE,见 §3.4 |
| 3 | c-star-core-service | .../appreciation/AppreciationGrpcService.java | sendAppreciation() | （新建）gRPC 入口,接收 SendAppreciationRequest,委托给 AppreciationService |
| 4 | c-star-core-service | .../appreciation/AppreciationService.java | send() | （新建）主编排:校验 → 原子事务 → 更新榜单 → 返回结果 |
| 5 | c-star-core-service | .../appreciation/AppreciationValidator.java | validate() | （新建）校验 6 条业务规则(BR-2/3/4/6/7),任一失败抛业务异常带错误码 |
| 6 | c-star-core-service | .../appreciation/AppreciationRepository.java | insert() | （新建）Micronaut Data JDBC Repository,只暴露 insert,不暴露 update/delete(BR-8) |
| 7 | c-star-core-service | .../quota/PeriodQuotaRepository.java | deductCas() / getRemaining() | （新建）原子扣减 Giver 配额(CAS 防超扣)+ 查剩余配额 |
| 8 | c-star-core-service | .../ranking/RankingRepository.java | incrementBoth() | （新建）原子递增周期榜 + 全时期榜的累计红花数 |
| 9 | c-star-core-service | .../employee/EmployeeRepository.java | existsBySsoId() | （新建）查 Receiver 是否有本地记录(BR-3) |
| 10 | c-star-core-service | .../category/CategoryService.java | getActiveSet() | （新建）查 Category 活跃集(BR-6),Phase 1 读配置文件 |

> Greenfield 场景:所有位置为新建,按 HLD §4.2 推断 core-service 仓库结构。

## 3. As-Is 和 To-Be

### 3.1 As-Is（现状）
N/A — greenfield, no prior code

### 3.2 To-Be（改后）

**新行为**:

Giver 通过 API Gateway 提交发送赞赏请求(经 gRPC 到 core-service),系统按以下顺序处理:

1. **解析入参**:Giver ssoId、Receiver ssoId、flowerCount、categoryCode、message
2. **校验**(AppreciationValidator.validate,6 条业务规则,短路求值,任一失败立即抛异常):
   - BR-2: Giver.ssoId ≠ Receiver.ssoId
   - BR-3: Receiver 在 employee 表存在本地记录(existsBySsoId)
   - BR-4: 1 ≤ flowerCount ≤ Giver 剩余配额(查 period_quota.remaining)
   - BR-6: categoryCode 在活跃集(Phase 1 读配置文件)
   - BR-7: message 非空且 trim 后非空
   - BR-8 隐含:写入后不可变,本步不校验(是后续不变量)
3. **原子事务**(AppreciationService.send,单 @Transactional):
   - 扣配额:`UPDATE period_quota SET used = used + ?, remaining = remaining - ? WHERE sso_id = ? AND period_date = ? AND remaining >= ?`（CAS 防超扣,affected rows = 0 则回滚并抛 QUOTA_INSUFFICIENT）
   - 写记录:`INSERT INTO appreciation (giver_sso_id, receiver_sso_id, flower_count, category_code, message, created_at) VALUES (...)`
   - 更新榜单:周期榜 `INSERT ... ON DUPLICATE KEY UPDATE total_flowers = total_flowers + ?`;全时期榜同理(period_date = NULL)
4. **返回**:AppreciationDto（id / giverSsoId / receiverSsoId / flowerCount / categoryCode / message / createdAt）

**调用链**:

```
API Gateway (REST) 
  → core-service AppreciationGrpcService.sendAppreciation (gRPC handler)
    → AppreciationService.send (主编排, @Transactional)
      ├─ AppreciationValidator.validate (6 条规则)
      │    ├─ EmployeeRepository.existsBySsoId (BR-3)
      │    ├─ PeriodQuotaRepository.getRemaining (BR-4)
      │    └─ CategoryService.getActiveSet (BR-6)
      ├─ PeriodQuotaRepository.deductCas (CAS 扣配额)
      ├─ AppreciationRepository.insert (写记录)
      └─ RankingRepository.incrementBoth (更新双榜)
```

**保留的不变量**（什么不变,§4 不变量测试要对照）:

- **INV-1（配额-记录一致性,AC-G6）**:扣配额与写记录在同一事务内,要么都成功要么都回滚。不存在"扣了配额没记录"或"有记录没扣配额"的可观察状态
- **INV-2（不可变性,BR-8/AC-G7）**:appreciation 表只 INSERT,无 UPDATE/DELETE 路径（Repository 不暴露 update/delete 方法）
- **INV-3（即时可见,BR-9/10/AC-G5）**:事务提交后,记录立即可被公司级浏览/收发列表/双榜查询读到,无 accept 步骤
- **INV-4（双榜同步,AC-R3）**:同一事务内同时更新周期榜 + 全时期榜,不存在"周期榜更新了全时期榜没更新"的中间态
- **INV-5（配额非负,BR-4/AC-E2）**:remaining 永远 ≥ 0,CAS WHERE remaining >= flowerCount 保证不会扣成负数

**改后代码片段**（关键伪代码）:

```java
// AppreciationService.java
@Transactional
public AppreciationDto send(SendAppreciationCommand cmd) {
    validator.validate(cmd);  // 抛业务异常带错误码
    
    int affected = periodQuotaRepository.deductCas(
        cmd.giverSsoId(), cmd.flowerCount(), clock.todayBeijing());
    if (affected == 0) {
        throw new BusinessException(ErrorCode.QUOTA_INSUFFICIENT);
    }
    
    var appreciation = Appreciation.create(cmd, clock.nowBeijing());
    appreciationRepository.insert(appreciation);
    
    rankingRepository.incrementBoth(
        cmd.receiverSsoId(), cmd.flowerCount(), clock.todayBeijing());
    
    return AppreciationDto.from(appreciation);
}

// PeriodQuotaRepository.java — CAS 扣减
@Query("UPDATE period_quota SET used = used + :n, remaining = remaining - :n " +
       "WHERE sso_id = :ssoId AND period_date = :periodDate AND remaining >= :n")
int deductCas(@Param("ssoId") String ssoId, @Param("n") int flowerCount, 
              @Param("periodDate") LocalDate periodDate);
```

### 3.3 改前 vs 改后对比
N/A — greenfield,无改前状态可对比。

### 3.4 DDL / 表结构变更

Greenfield 新建型,本次涉及 4 张表的完整 CREATE TABLE DDL:

```sql
-- ============================================================
-- V1__init_core.sql — C-Star core schema 初始化
-- ============================================================

-- 1. 员工记录(ADR-0004 懒同步,ADR-0006 不维护离职)
CREATE TABLE employee (
  sso_id        VARCHAR(128) NOT NULL COMMENT '公司 SSO 唯一标识',
  display_name  VARCHAR(128) NOT NULL COMMENT '展示名,来自 SSO claims',
  created_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '首次登录创建时间',
  updated_at    DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近一次登录更新时间(SSO name 变更时更新)',
  PRIMARY KEY (sso_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='员工记录';

-- 2. 赞赏记录(BR-8 不可变,ADR-0003 全公开)
CREATE TABLE appreciation (
  id              BIGINT       NOT NULL AUTO_INCREMENT COMMENT '自增主键',
  giver_sso_id    VARCHAR(128) NOT NULL COMMENT '发送者 SSO ID',
  receiver_sso_id VARCHAR(128) NOT NULL COMMENT '接收者 SSO ID',
  flower_count    INT          NOT NULL COMMENT '本次赞赏的红花数(1~N)',
  category_code   VARCHAR(64)  NOT NULL COMMENT '分类代码(引用 category.code,退役后保留原值)',
  message         TEXT         NOT NULL COMMENT '赞赏理由(BR-7 非空)',
  created_at      DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) COMMENT '创建时间,不可变',
  PRIMARY KEY (id),
  INDEX idx_appreciation_giver_created (giver_sso_id, created_at DESC) COMMENT 'Sent List 查询',
  INDEX idx_appreciation_receiver_created (receiver_sso_id, created_at DESC) COMMENT 'Received List 查询',
  INDEX idx_appreciation_created (created_at DESC) COMMENT '公司级浏览查询'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='赞赏记录,写入后不可变';

-- 3. 周期配额(ADR-0005 每日 00:00 北京时间重置,BR-13 不结转)
CREATE TABLE period_quota (
  sso_id     VARCHAR(128) NOT NULL COMMENT '员工 SSO ID',
  period_date DATE        NOT NULL COMMENT '周期日期(默认当天,北京时间)',
  used       INT          NOT NULL DEFAULT 0 COMMENT '本周期已用红花数',
  remaining  INT          NOT NULL COMMENT '本周期剩余红花数(重置时设为默认配额)',
  updated_at DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近扣减时间',
  PRIMARY KEY (sso_id, period_date),
  CONSTRAINT chk_quota_nonneg CHECK (used >= 0 AND remaining >= 0) COMMENT 'BR-4 配额非负'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='周期配额';

-- 4. 榜单(周期榜 period_date=当天,全时期榜 period_date=NULL)
CREATE TABLE ranking (
  sso_id         VARCHAR(128) NOT NULL COMMENT '员工 SSO ID(Receiver)',
  period_date    DATE         NULL COMMENT '周期日期(NULL=全时期榜,非NULL=当日周期榜)',
  total_flowers  BIGINT       NOT NULL DEFAULT 0 COMMENT '累计红花数',
  updated_at     DATETIME(3)  NOT NULL DEFAULT CURRENT_TIMESTAMP(3) ON UPDATE CURRENT_TIMESTAMP(3) COMMENT '最近更新时间',
  PRIMARY KEY (sso_id, period_date) COMMENT 'period_date NULL 与具体日期是不同的行,聚簇索引区分'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='排行榜(周期+全时期)';
```

> 注:CHECK 约束在 MariaDB 10.2.1+ 支持;若部署到不支持 CHECK 的版本,需在应用层补校验。

### 3.5 Proto 契约

本次需要定义 SendAppreciation RPC 的完整 proto 契约:

```proto
syntax = "proto3";

package cstar.appreciation.v1;

option java_package = "com.rakuten.cstar.proto.appreciation.v1";
option java_multiple_files = true;

// ============================================================
// SendAppreciation RPC
// ============================================================
service AppreciationService {
  // 发送赞赏(主流程)
  rpc SendAppreciation(SendAppreciationRequest) returns (SendAppreciationResponse);
}

// ============================================================
// Request
// ============================================================
message SendAppreciationRequest {
  // Giver 的 SSO ID(从 JWT 提取,服务端校验与请求体一致)
  string giver_sso_id = 1;
  // Receiver 的 SSO ID
  string receiver_sso_id = 2;
  // 红花数(1~N,N 受剩余配额限制)
  int32 flower_count = 3;
  // 分类代码(必须来自活跃集)
  string category_code = 4;
  // 赞赏理由(非空)
  string message = 5;
}

// ============================================================
// Response
// ============================================================
message SendAppreciationResponse {
  // 创建的赞赏记录
  AppreciationDto appreciation = 1;
}

message AppreciationDto {
  int64 id = 1;                  // 记录自增 ID
  string giver_sso_id = 2;
  string receiver_sso_id = 3;
  int32 flower_count = 4;
  string category_code = 5;
  string message = 6;
  // 创建时间(ISO 8601 字符串,UTC)
  string created_at = 7;
}

// ============================================================
// 错误码枚举(业务校验失败用,gRPC status code = FAILED_PRECONDITION / INVALID_ARGUMENT)
// ============================================================
enum AppreciationErrorCode {
  APPRECIATION_ERROR_UNSPECIFIED = 0;
  // BR-2: Giver = Receiver
  SELF_APPRECIATION_FORBIDDEN = 1;
  // BR-3: Receiver 无本地记录(未登录过)
  RECEIVER_NOT_FOUND = 2;
  // BR-4: 花数超剩余配额
  QUOTA_INSUFFICIENT = 3;
  // BR-4: 花数 < 1
  INVALID_FLOWER_COUNT = 4;
  // BR-6: Category 不在活跃集
  CATEGORY_NOT_ACTIVE = 5;
  // BR-7: Message 为空
  MESSAGE_REQUIRED = 6;
}
```

### 3.6 DTO ↔ 表字段映射

DTO/Command/Response 字段与表字段(§3.4)一一对应关系:

**SendAppreciationCommand**(入参,来自 Request):

| DTO 字段 | 类型 | 对应表.字段 | 类型 | 可空 | 备注 |
|---|---|---|---|---|---|
| giverSsoId | String | employee.sso_id | VARCHAR(128) | NOT NULL | Giver 必须有本地记录 |
| receiverSsoId | String | employee.sso_id | VARCHAR(128) | NOT NULL | BR-3 校验存在 |
| flowerCount | Integer | appreciation.flower_count | INT | NOT NULL | BR-4: 1 ≤ N ≤ remaining |
| categoryCode | String | appreciation.category_code | VARCHAR(64) | NOT NULL | BR-6 校验在活跃集 |
| message | String | appreciation.message | TEXT | NOT NULL | BR-7 非空 |

**AppreciationDto**(出参,写入 appreciation 表后返回):

| DTO 字段 | 类型 | 对应表.字段 | 类型 | 可空 | 备注 |
|---|---|---|---|---|---|
| id | Long | appreciation.id | BIGINT | NOT NULL | 自增主键 |
| giverSsoId | String | appreciation.giver_sso_id | VARCHAR(128) | NOT NULL | |
| receiverSsoId | String | appreciation.receiver_sso_id | VARCHAR(128) | NOT NULL | |
| flowerCount | Integer | appreciation.flower_count | INT | NOT NULL | |
| categoryCode | String | appreciation.category_code | VARCHAR(64) | NOT NULL | |
| message | String | appreciation.message | TEXT | NOT NULL | |
| createdAt | String(ISO 8601) | appreciation.created_at | DATETIME(3) | NOT NULL | 序列化为 UTC 字符串 |

**PeriodQuota 内部字段**(事务内使用,不对外):

| 字段 | 类型 | 对应表.字段 | 类型 | 备注 |
|---|---|---|---|---|
| used | Integer | period_quota.used | INT | 扣减时 used + N |
| remaining | Integer | period_quota.remaining | INT | 扣减时 remaining - N,CAS WHERE remaining >= N |
| periodDate | LocalDate | period_quota.period_date | DATE | 北京时间当天 |

**Ranking 内部字段**(事务内使用,不对外):

| 字段 | 类型 | 对应表.字段 | 类型 | 备注 |
|---|---|---|---|---|
| ssoId | String | ranking.sso_id | VARCHAR(128) | Receiver |
| periodDate | LocalDate | ranking.period_date | DATE(NULL) | 周期榜=当天,全时期榜=NULL |
| totalFlowers | Long | ranking.total_flowers | BIGINT | 原子递增 N |

> 一致性确认:DTO 所有字段在表里都有对应列,无悬空字段。proto Request 字段与 Command 一一对应,proto Response.AppreciationDto 字段与内部 AppreciationDto 一一对应。

## 4. 测试

按 repo 分组的用例表。每行: 用例 / 期望（✅通过 或 ❌报<error code>）。

### c-star-core-service

| 维度 | 用例 | 期望 |
|------|------|------|
| 正向 (Happy) | Giver 配额足够,Receiver 存在,Category 活跃,Message 非空 → 提交 | ✅ 返回 AppreciationDto,配额扣 N,记录可见,双榜+N |
| 异常 (Error) | Giver = Receiver | ❌ SELF_APPRECIATION_FORBIDDEN (BR-2) |
| 异常 (Error) | Receiver 无本地记录(未登录过) | ❌ RECEIVER_NOT_FOUND (BR-3) |
| 异常 (Error) | flowerCount > 剩余配额 | ❌ QUOTA_INSUFFICIENT (BR-4) |
| 异常 (Error) | flowerCount = 0 | ❌ INVALID_FLOWER_COUNT (BR-4) |
| 异常 (Error) | categoryCode 不在活跃集 | ❌ CATEGORY_NOT_ACTIVE (BR-6) |
| 异常 (Error) | message 为空 / 纯空格 | ❌ MESSAGE_REQUIRED (BR-7) |
| 边界 (Boundary) | flowerCount = 1(最小值) | ✅ |
| 边界 (Boundary) | flowerCount = 剩余配额(恰好用完) | ✅ remaining → 0 |
| 边界 (Boundary) | 剩余配额 = 0,flowerCount = 1 | ❌ QUOTA_INSUFFICIENT (AC-E2) |
| 边界 (Boundary) | 23:59:59 北京时间提交 → 时间戳归当前周期 | ✅ 计入当天配额与周期榜 (AC-E3) |
| 边界 (Boundary) | 同一 Giver 同一 Receiver 同周期提交两次,配额够 | ✅ 两条记录都创建,配额扣两次 (AC-E1) |
| 不变量 (Invariant) | INV-1: 扣配额后模拟 insert 失败 → 事务回滚,配额恢复 | ✅ 无"扣了没写"状态 (AC-G6) |
| 不变量 (Invariant) | INV-2: 调用后尝试 UPDATE/DELETE appreciation → 无 API 路径可走 | ✅ 不可变 (BR-8/AC-G7) |
| 不变量 (Invariant) | INV-3: 事务提交后立即查公司级浏览/收发列表/双榜 → 都能读到 | ✅ 即时可见 (BR-9/10) |
| 不变量 (Invariant) | INV-4: 事务提交后查周期榜 + 全时期榜 → 两者都+N | ✅ 双榜同步 (AC-R3) |
| 不变量 (Invariant) | INV-5: 并发提交导致剩余配额刚好不够 → CAS affected=0 → 回滚 | ✅ remaining 永不 < 0 (BR-4) |
| 回归 (Regression) | ⚠️ 待补 — greenfield 无改前路径可回归 | N/A |

### c-star-proto

| 维度 | 用例 | 期望 |
|------|------|------|
| 正向 (Happy) | SendAppreciationRequest 字段全部合法,序列化/反序列化 round-trip | ✅ |
| 异常 (Error) | 缺必填字段(giver_sso_id/receiver_sso_id/flower_count/category_code/message) | ❌ INVALID_ARGUMENT (gRPC 层) |
| 边界 (Boundary) | flower_count = Int.MAX | ✅ proto 接受(core-service 校验层拒绝) |
| 回归 (Regression) | ⚠️ 待补 — greenfield | N/A |
| 不变量 (Invariant) | 错误码枚举完整覆盖 §3.5 的 7 个业务错误(UNSPECIFIED + 6 个业务码) | ✅ |

### c-star-database

| 维度 | 用例 | 期望 |
|------|------|------|
| 正向 (Happy) | V1__init_core.sql 在 MySQL 8.0 执行 → 4 张表创建成功 | ✅ |
| 不变量 (Invariant) | appreciation 表无 UPDATE/DELETE 权限给应用账号(只 INSERT/SELECT) | ✅ INV-2 DB 层兜底 |
| 不变量 (Invariant) | period_quota CHECK used >= 0 AND remaining >= 0 约束生效 | ✅ INV-5 DB 层兜底 |
| 边界 (Boundary) | ranking 表 period_date = NULL 与 period_date = '2026-08-05' 是不同行 | ✅ 聚簇索引区分 |

> 不变量来源: §3.2 To-Be 中列出的 INV-1~INV-5,每条都有对应测试
> 回归类标 ⚠️ 待补: greenfield 场景无改前路径可回归
