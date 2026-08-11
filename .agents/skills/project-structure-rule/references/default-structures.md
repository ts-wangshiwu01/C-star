# Default Structures — 官方推荐/社区惯例参考

> DESIGN 模式用这份速查表给用户提候选。skill 会把匹配栈的骨架作为**初稿**呈现，让用户选并微调。
> 这里只是模板起点，不是硬绑——用户可以拒绝并要求另换结构。

## Java / Kotlin

### Spring Boot（官方 guide 推荐 + 社区惯例）

```
src/main/java/com/example/<project>/
├── controller/       # REST controller
├── service/          # 业务服务
├── repository/       # Spring Data Repository
├── model/            # entity / dto（多数项目不分）
├── config/           # 配置类
└── exception/        # 异常 + handler
```

- 优点：Spring 官方 tutorial 通用，团队新人上手快
- 缺点：分层浅，大项目容易 model 层膨胀

### Micronaut（官方 guide + Kotlin 项目常见）

```
src/main/kotlin/com/example/<project>/
├── api/              # HTTP/gRPC 入口
├── application/      # 用例编排
├── domain/           # 领域模型
├── infrastructure/   # 持久化 + 外部调用
└── shared/           # 异常、错误码
```

- 优点：分层清晰，适合 domain-heavy 项目
- 缺点：小项目略重

### DDD 严格四层（Alistair Cockburn / Vaughn Vernon 风格）

```
├── interfaces/       # 用户接口层（Controller / REST / gRPC）
├── application/      # 应用服务（用例）
├── domain/           # 领域层（entity, VO, domain service, repo interface）
└── infrastructure/   # 基础设施（repo impl, MQ client, external HTTP）
```

- 优点：领域驱动纯正，端口/适配器边界清楚
- 缺点：小团队/CRUD 型项目 overkill

### Vert.x（响应式项目）

```
src/main/kotlin/com/example/<project>/
├── verticle/         # Verticle 类
├── handler/          # Route handler
├── service/          # 业务服务
├── repository/       # 数据访问
├── model/            # DTO / entity
└── config/           # 配置类
```

## Go

### 官方 project-layout（github.com/golang-standards/project-layout，非官方但事实标准）

```
<repo-root>/
├── cmd/<app-name>/   # main 包
├── internal/         # 私有代码（不允许外部 import）
│   ├── handler/
│   ├── service/
│   ├── repository/
│   └── model/
├── pkg/              # 公共库
├── api/              # OpenAPI/proto 契约
├── configs/
└── deployments/
```

- 注意：Go 官方从未认可此 layout，但社区采用率极高
- 小项目可以直接扁平（不分 internal），单文件也没问题

### Domain-Driven Go（大项目推荐）

```
internal/
├── api/          # gin/echo handler
├── application/  # usecase
├── domain/       # entity + interfaces
├── infrastructure/  # DB, external API impl
└── shared/
```

## TypeScript / Node.js

### NestJS（官方 CLI 生成）

```
src/
├── <feature>/                    # 按 feature 分模块（推荐）
│   ├── <feature>.controller.ts
│   ├── <feature>.service.ts
│   ├── <feature>.module.ts
│   ├── <feature>.repository.ts
│   ├── dto/
│   └── entities/
├── common/                       # 跨模块共享
│   ├── filters/
│   ├── interceptors/
│   └── decorators/
└── main.ts
```

- NestJS 官方**强烈推荐 feature-first**，而不是按 layer 分包
- 命令：`nest g resource <name>` 会自动生成上述结构

### Express（无官方规范，社区惯例）

```
src/
├── routes/       # Express routes
├── controllers/
├── services/
├── models/
├── middlewares/
└── utils/
```

## Vue 3 前端（配合 Element Plus）

### 官方 create-vue 生成 + 常见组织

```
src/
├── views/            # 路由页面
├── components/       # 可复用组件
│   ├── base/         #   通用原子组件
│   └── business/     #   业务组合组件
├── composables/      # 组合式函数（Vue 3 惯例）
├── stores/           # Pinia store（官方推荐）
├── router/
├── api/              # HTTP 客户端封装
├── utils/
└── assets/
```

- Vue 3 官方文档推荐用 `composables/` 而不是 mixins
- 状态管理用 Pinia（官方推荐），不用 Vuex

---

## skill 用法

DESIGN 模式启动后，`project-structure-rule` skill 应：

1. 从 `architecture/*.md` 提取语言 + 框架
2. 在本文件里找匹配栈的 1~2 个候选（首选官方，次选社区流行）
3. 呈现骨架给用户
4. 用户选中后进入采访细化（是否分模块、契约层是否独立、test 镜像结构等）
5. 生成最终 `docs/project-structure.md` + `.github/instructions/project-structure.instructions.md`
