# Project Structure — <PROJECT_NAME>

> **产出方式**：`project-structure-rule` skill（<EXTRACT | DESIGN> 模式）
> **主语言/框架**：<e.g. Kotlin + Vert.x / Java + Micronaut / Go + Gin / TypeScript + NestJS>
> **版本**：v1.0 · **日期**：YYYY-MM-DD
> **配套机读版**：`.github/instructions/project-structure.instructions.md`

---

## 1. 骨架图

<用 ASCII 或 mermaid 画出主目录结构。示例：>

```
<repo-root>/
├── src/main/kotlin/com/example/<project>/
│   ├── api/                  # 对外入口（Controller/Handler/gRPC service impl）
│   │   ├── controller/       #   REST controller
│   │   └── grpc/             #   gRPC service impl
│   ├── application/          # 应用层（编排、事务边界）
│   │   ├── service/          #   业务用例编排
│   │   └── command/          #   Command/Query DTO
│   ├── domain/               # 领域层（实体、值对象、领域服务）
│   │   ├── model/            #   entity + value object
│   │   └── event/            #   domain event
│   ├── infrastructure/       # 基础设施层
│   │   ├── repository/       #   Repository 实现（JDBC/JPA）
│   │   ├── client/           #   外部服务客户端
│   │   └── config/           #   配置类
│   └── shared/               # 跨层共享（异常、错误码、通用工具）
└── src/test/kotlin/...
```

## 2. 每层职责 + 命名后缀

| 层 | 目录 | 职责（一句话） | 命名后缀 | 依赖方向 |
|---|---|---|---|---|
| API | `api/controller` | 接收 HTTP 请求，参数绑定，委托 Service | `*Controller` | 只依赖 `application` |
| API | `api/grpc` | 实现 gRPC service | `*GrpcService` | 只依赖 `application` |
| Application | `application/service` | 用例编排，事务边界 | `*Service` / `*UseCase` | 依赖 `domain` + `infrastructure` 接口 |
| Application | `application/command` | 用例入参 DTO | `*Command` / `*Query` | 无向下依赖 |
| Domain | `domain/model` | 实体、值对象、领域不变量 | `*Entity` / `*Vo` | 不依赖其他层 |
| Domain | `domain/event` | 领域事件 | `*Event` | 不依赖其他层 |
| Infrastructure | `infrastructure/repository` | Repository 实现，SQL/存储访问 | `*Repository` | 依赖 `domain` |
| Infrastructure | `infrastructure/client` | 外部服务 HTTP/gRPC 客户端 | `*Client` | 依赖 `application` 端口接口 |
| Shared | `shared/exception` | 业务异常 + 错误码 | `*Exception` / `ErrorCode` | 谁都可以依赖 |

<按项目实际情况调整。每条职责必须一句话讲清，不写抽象口号。>

## 3. 禁止事项清单

> 至少 5 条，越具体越好。这些是 AI 最容易踩、review 最容易漏的地方。

1. ❌ `Controller` 不能直接调 `Repository`——必须走 `Service`
2. ❌ `Domain` 层不能 import `infrastructure`、`api`、`application` 中任何包
3. ❌ `Repository` 只暴露持久化能力，不写业务逻辑（组合查询也不行）
4. ❌ `Dto/Command/Query` 不能持有可变状态或业务方法，只做数据搬运
5. ❌ `Service` 方法返回值不能是 `Entity`——必须转成 `Dto`，防止实体逃逸出事务边界
6. ❌ 同层内类之间不允许循环依赖
7. ❌ 一个 `Service` 类超过 <N> 个 public 方法必须拆——单一职责

<根据项目实际增补。>

## 4. 示例落点（真实功能对应到具体文件）

**功能**：<e.g. 发送赞赏 send-appreciation>

| 文件 | 层 | 承担的职责 |
|---|---|---|
| `api/grpc/AppreciationGrpcService.kt` | API | gRPC 入口，转 `SendAppreciationCommand` |
| `application/command/SendAppreciationCommand.kt` | Application | 用例入参 |
| `application/service/AppreciationService.kt` | Application | 编排：校验 → 事务 → 双榜更新 |
| `application/service/AppreciationValidator.kt` | Application | 6 条业务规则校验 |
| `domain/model/Appreciation.kt` | Domain | 赞赏实体 + 领域不变量 |
| `infrastructure/repository/AppreciationRepository.kt` | Infrastructure | JDBC 实现 |
| `shared/exception/AppreciationErrorCode.kt` | Shared | 错误码枚举 |

<给 1~2 个真实功能的完整落点，就是 AI 最好的 few-shot。>

## 5. 常见问答

- **Q：新增一个功能，如何决定放哪个包？**  
  A：先看它的输入源——外部请求进 `api`，内部编排进 `application`，纯领域逻辑进 `domain`，访问外部资源进 `infrastructure`。

- **Q：一个类横跨两层怎么办？**  
  A：说明职责不单一，必须拆。允许一个 `Service` 用多个 `Repository`，但不允许一个类同时含 controller 和 repository 逻辑。

- **Q：test 目录如何镜像？**  
  A：`src/test/<lang>/<same-package>/*Test.<ext>`，测试类命名同源类 + `Test` 后缀。

<按需增补。>

---

**变更记录**

| 版本 | 日期 | 变更 | 作者 |
|---|---|---|---|
| v1.0 | YYYY-MM-DD | 初版 | <name> |
