# HLD Designer Skill — Design Spec

> **Spec date**: 2026-07-28
> **Skill name**: hld-designer
> **Location**: `/Users/ts-shiwu.wang/Documents/workspace/c-start/.agents/skills/hld-designer/`

## 1. 概述

创建一个可复用的 HLD(High Level Design)产出 skill,基于 doc-lifecycle 规范,产出"概述 + 处理流程图 + 数据模型 + 接口设计"为核心的 HLD 文档。支持三级 HLD 层级(项目级 / 服务级 / feature 级),用户通过触发词路由到 4 个入口(A/B/C/D)。

## 2. 设计决策

| 决策点 | 选择 | 理由 |
|---|---|---|
| 工作模式 | 混合式(读上游自动填 + 决策点采访) | 平衡自动化与用户控制 |
| 技术栈范围 | 通用(不绑定栈) | 跨项目复用 |
| HLD 章节结构 | 4 章:概述 + 流程图 + 数据模型 + 接口设计 | 依赖归 architecture/,决策点/非功能不写 |
| HLD 层级 | 3 级:项目级 / 服务级 / feature 级 | 从整体到局部 |
| 合并触发 | 用户手动触发(入口 D),不自动连跑 | feature review 后间隔可能数周到数月 |
| 架构形式 | 单 skill + references 分入口(方案 C) | SKILL.md 精简,渐进式加载 |

## 3. 三级 HLD 层级

```
① 项目级 HLD(入口 A:从 PRD 生成,整体设计)
        │
        │  入口 B 触发:为某个微服务出 HLD
        ↓
② 服务级 HLD(每个微服务一份,基于项目级 HLD)
        │
        │  入口 C 触发:为某个 feature 出 HLD
        ↓
③ Feature 级 HLD(每个功能一份,基于服务级 HLD + ticket)
        │
        │  feature review + 实现 + 测试 + 上线后
        │  ↓
        │  用户手动触发入口 D
        ↓
④ 合并回服务级 / 项目级(不自动,间隔可能数周到数月)
```

**关键纪律**:合并是用户事后主动触发,不是 feature 写完自动连跑。中间可能经过 review、修改、开发、测试、上线,间隔数周至数月。

## 4. HLD 章节结构(固定 4 章)

```
1. 概述         — 一句话说明这个功能干啥
2. 处理流程图    — 谁流转给谁(mermaid)
3. 数据模型     — 涉及的核心实体/表(逻辑层)
4. 接口设计     — 暴露/消费的接口签名
```

**不放进 HLD 的**:
- 架构 / 依赖关系 → 在 `architecture/` 独立定义
- 决策点 / 非功能 → 不写
- 具体类设计 / DDL / 错误码 / 单测 → 属于 detail design

详细度按级别递增:
- **项目级**:粗(整体数据模型概览、接口分类)
- **服务级**:中(服务内实体、对外接口)
- **Feature 级**:细(具体表/字段、具体接口签名)

## 5. 路由决策树

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

## 6. 4 个入口核心流程

### 入口 A — 项目级 HLD 初始化

**输入**:PRD(docs/REQUIREMENTS.md) + ADR(docs/adr/*.md) + CONTEXT.md

**流程**(5 步):
1. **读取上游文档**:PRD 全文 + ADR 全部 + CONTEXT.md
2. **自动填充业务上下文**:从 PRD §1 提炼概述;从 §2 用户故事提炼主流程图
3. **采访技术决策点**:逐个问微服务拆分 / 通信 / DB / 认证 / 部署
4. **生成项目级 HLD**:按 template 填 4 章
5. **校验**:跑 check_hld_structure.py,退出码 = 0

**产物**:`docs/[项目缩写]_HLD_PROJECT_v1.0_[YYYYMMDD].md`

### 入口 B — 服务级 HLD 创建

**输入**:项目级 HLD + 用户指定的服务名

**流程**(4 步):
1. **读取项目级 HLD**:提取整体流程图 / 整体数据模型 / 服务边界
2. **采访服务边界决策**:对外暴露接口 / 对内消费接口 / 数据模型细化
3. **生成服务级 HLD**:按 template 填 4 章,详细度比项目级高一级
4. **校验**:跑 check_hld_structure.py

**产物**:`docs/[项目缩写]_HLD_SERVICE_[服务名]_v1.0_[YYYYMMDD].md`

### 入口 C — Feature 级 HLD 创建

**输入**:服务级 HLD + ticket 编号 + 需求描述

**流程**(4 步):
1. **读取服务级 HLD**:提取服务流程图 / 服务数据模型 / 服务接口设计
2. **采访 feature 决策点**:涉及哪些数据变更 / 接口变更 / 流程图位置 / 异常处理方向
3. **生成 feature 级 HLD**:按 template 填 4 章,详细度最高
4. **校验**:跑 check_hld_structure.py

**产物**:`docs/[项目缩写]_HLD_FEATURE_[服务名]_[ticket-slug]_v1.0_[YYYYMMDD].md`

### 入口 D — Feature 合并回上级

**触发条件**:用户明确说"合并 HLD",并指定合并目标(服务级或项目级)。间隔可能数周到数月。

**输入**:Feature 级 HLD + 用户指定的目标上级 HLD(服务级或项目级)

**流程**(4 步):
1. **读取两份 HLD**:Feature HLD + 用户指定的目标上级 HLD
2. **Diff 分析**:逐章对比,识别新增 / 修改 / 删除
3. **生成 patch**:结构化 patch(不是覆盖),分 4 章列出变更点
4. **人审确认**:用户确认 → 写入,版本号 +0.1;用户修改 → 调整 patch 后重新确认;用户拒绝 → 不写入

**产物**:目标 HLD 更新(版本号 +0.1,修订记录加一条)+ patch 记录存档

**合并目标判断**:用户明确指定合并到哪一级,skill 不自动判断。常见场景:
- feature 实现完 → 合并回服务级(更新服务级 HLD)
- 多个 feature 合并到服务级后 → 用户再触发合并回项目级(更新项目级 HLD)

## 7. 通用纪律

1. **产物结构固定** 4 章:概述 + 处理流程图 + 数据模型 + 接口设计。不擅自加章节
2. **混合模式**:读上游文档时自动填业务上下文,遇技术决策点必须暂停问用户
3. **不绑定技术栈**:栈、协议、DB、部署都由用户在采访中确定,skill 不假设默认值
4. **流程图必须**:每个 HLD 必有处理流程图(mermaid sequence/flow)
5. **入口 D 不自动**:合并是用户手动触发,不连跑;间隔可能数周到数月
6. **8 字段头部**:沿用 doc-lifecycle scene-create 规范

## 8. 目录结构

```
.agents/skills/hld-designer/
├── SKILL.md                              # 路由 + 通用纪律(<200行)
├── references/
│   ├── entry-init-project.md             # 入口 A 方法
│   ├── entry-create-service.md           # 入口 B 方法
│   ├── entry-create-feature.md           # 入口 C 方法
│   ├── entry-merge-feature.md            # 入口 D 方法
│   ├── template-rules.md                 # 8字段头部 + 4章规范
│   └── patch-generation.md               # 合并时生成 patch 的方法
├── templates/
│   ├── hld-project.md                    # 项目级 HLD 模板
│   ├── hld-service.md                    # 服务级 HLD 模板
│   └── hld-feature.md                    # Feature 级 HLD 模板
└── scripts/
    └── check_hld_structure.py            # 校验脚本
```

## 9. 命名规范

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

## 10. 校验规则

`scripts/check_hld_structure.py` 检查:
- 8 字段头部齐全(项目名称 / 文档类型 / 创建日期 / 创建人 / 审核人 / 版本号 / 状态)
- 修订记录表紧跟头部
- 4 章必备(概述 / 处理流程图 / 数据模型 / 接口设计)
- 流程图必须用 mermaid 语法
- 文件名符合 `[项目缩写]_HLD_[级别]_[模块]_vX.Y_YYYYMMDD.md`

退出码 = 0 才算完成。

## 11. 不适用场景

- detail design(用 write-detail-design skill)
- API 文档(用 gen-api-doc skill)
- 完整架构文档(架构在 `architecture/` 独立定义)
- 单文件改 typo / 一句话(直接 patch)
