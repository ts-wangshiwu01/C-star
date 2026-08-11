---
applyTo: "src/**/*.{kt,java,go,ts,vue}"
description: 项目结构规则 — 决定新代码放哪个包 / 类命名后缀 / 层间依赖方向
---

# Project Structure Rule (auto-loaded)

> 本文件由 `project-structure-rule` skill 生成。人读版见 `docs/project-structure.md`。
> **修改本文件前请先改人读版，两份保持同源。**

## 决策规则（AI 编码时严格遵守）

**新增一个类前，先按这两步定位置：**

1. **看输入源**
   - 外部 HTTP/gRPC 请求触发 → `api/`
   - 定时任务/消息消费触发 → `api/`（子包 `scheduler/` 或 `worker/`）
   - 内部业务用例编排 → `application/service/`
   - 纯领域概念（实体/不变量/领域事件）→ `domain/`
   - 数据库/外部 API/文件系统 → `infrastructure/`
   - 跨层通用（异常、错误码、时间工具）→ `shared/`

2. **看命名后缀**（后缀决定去向）：

   | 类名后缀 | 必须落在 | 
   |---|---|
   | `*Controller` / `*GrpcService` / `*Handler` | `api/` |
   | `*Service` / `*UseCase` | `application/service/` |
   | `*Command` / `*Query` / `*Dto` | `application/command/` 或 `application/dto/` |
   | `*Entity` / `*Vo` / `*Event` | `domain/` |
   | `*Repository` / `*Client` | `infrastructure/` |
   | `*Exception` / `ErrorCode` | `shared/exception/` |
   | `*Config` | `infrastructure/config/` |
   | `*Test` | `src/test/**` 镜像被测类包路径 |

## 依赖方向（编译期约束）

```
api  ──►  application  ──►  domain
                │
                └──►  infrastructure  ──►  domain
```

**允许**：上层依赖下层、同层之间。  
**禁止**：

- ❌ `domain` 依赖任何其他层
- ❌ `api` 直接依赖 `infrastructure`（必须经 `application`）
- ❌ `infrastructure` 依赖 `api`
- ❌ 循环依赖（同层内也不允许）

## 硬性禁令

1. ❌ `Controller` 不能直接调 `Repository`——必须走 `Service`
2. ❌ `Service` 返回值必须是 `Dto`，**不能返回 `Entity`**（防止实体逃逸事务边界）
3. ❌ `Repository` 只暴露持久化操作，不写业务逻辑，不含条件组合查询
4. ❌ `Dto/Command/Query` 不能含业务方法，只做数据搬运
5. ❌ `Domain` 层禁止任何框架注解（如 `@Component / @Service / @Transactional`）
6. ❌ 一个 `Service` 类的 public 方法数 > <N> 必须拆

## 快速自检 checklist（AI 生成新代码后自查）

- [ ] 类名后缀是否与目标目录匹配？
- [ ] 有没有跨层反向依赖？（`grep import` 一遍）
- [ ] 是否有硬性禁令被违反？
- [ ] 测试类是否放在镜像的 `src/test/` 路径下？

<按项目实际情况增补。>
