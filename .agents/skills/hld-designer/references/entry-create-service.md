# Entry B — 服务级 HLD 创建

**触发词**:服务级 HLD / X 服务的 HLD / 微服务 HLD

## 输入

- 项目级 HLD:`docs/*_HLD_PROJECT_*.md`
- 用户指定的服务名(如:`core-service`)
- `architecture/`(若存在,补充服务职责/通信细节)

## 流程(4 步)

### Step 1 — 读取项目级 HLD

提取:

- 整体处理流程图(定位本服务在流程中的位置)
- 整体数据模型(提取本服务负责的实体)
- 服务边界(项目级 HLD 里已定的服务划分)
- 技术约束(已定栈)

### Step 2 — 采访服务边界决策

逐个问用户(若项目级 HLD 未覆盖):

- 这个服务对外暴露哪些接口?(从项目级 HLD 的接口分类细化)
- 这个服务消费哪些其他服务接口?
- 数据模型细化:项目级是逻辑实体,服务级要明确表/字段(逻辑层,不到 DDL)

### Step 3 — 生成服务级 HLD

按 `templates/hld-service.md` 填入 4 章,详细度比项目级高一级:

1. 概述:本服务在整体流程中的角色 + 所属项目级 HLD 文件名
2. 处理流程图:服务内部的请求流转(比项目级更细,体现 handler/service/repository 层级)
3. 数据模型:本服务负责的表/字段(逻辑层,接近 DDL 但不到 DDL)
4. 接口设计:对外暴露 + 对内消费的接口签名(gRPC 方法 + proto 定义)

文件名:`docs/[项目缩写]_HLD_SERVICE_[服务名]_v1.0_[YYYYMMDD].md`

### Step 4 — 校验

```bash
python3 .agents/skills/hld-designer/scripts/check_hld_structure.py docs/[项目缩写]_HLD_SERVICE_[服务名]_v1.0_*.md
```

退出码 = 0 才算完成。

## 产物

- `docs/[项目缩写]_HLD_SERVICE_[服务名]_v1.0_[YYYYMMDD].md`
- 校验通过

## 完成标准

- [ ] 8 字段头部齐全(文档类型为 "High Level Design — 服务级")
- [ ] 4 章必备
- [ ] 流程图是 mermaid 语法
- [ ] 文件名符合命名规范
- [ ] 校验脚本退出码 = 0
