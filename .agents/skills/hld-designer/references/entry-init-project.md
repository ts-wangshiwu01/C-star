# Entry A — 项目级 HLD 初始化

**触发词**:从 PRD 生成 HLD / 项目级 HLD / 整体设计

## 输入

- PRD:`docs/REQUIREMENTS.md`(或 PRD 目录)
- ADR:`docs/adr/*.md`
- CONTEXT.md(领域术语,可选)
- `architecture/`(若存在,作为已定技术约束)

## 流程(5 步)

### Step 1 — 读取上游文档

- 读 PRD 全文,提取:产品定位、用户故事、业务规则、验收标准
- 读 ADR 全部,提取:已锁定的技术决策(不可改)
- 读 CONTEXT.md(若存在),提取:领域术语(避免 HLD 里用错词)
- 读 `architecture/`(若存在),提取:已定的技术栈、服务划分、通信协议、数据库划分

### Step 2 — 自动填充业务上下文

基于 PRD 自动填入 HLD 的前两章:

- **概述**:从 PRD §1 产品概述提炼一句话(不超过 3 行)
- **处理流程图**:从 PRD §2 用户故事提炼主流程(谁触发谁),用 mermaid sequenceDiagram

如果 `architecture/` 存在,自动提取:
- 技术栈填入 HLD 概述的"技术约束"部分
- 服务划分作为接口设计的起点

### Step 3 — 采访技术决策点

自动识别 PRD 里未定的技术决策(即 `architecture/` 没覆盖的),逐个问用户:

- 微服务怎么拆?(若 architecture 未定)
- 服务间通信?(若未定)
- 数据库选型 + 划分?(若未定)
- 认证方式?(若未定)
- 部署环境?(若未定)

**纪律**:每个决策用户回答后,填入 HLD 对应章节(数据模型/接口设计)。不假设默认值。

### Step 4 — 生成项目级 HLD

按 `templates/hld-project.md` 填入 4 章:

1. 概述(Step 2 自动 + Step 3 技术约束)
2. 处理流程图(Step 2 自动,mermaid)
3. 数据模型(Step 3 采访后的整体数据模型,实体关系层)
4. 接口设计(Step 3 采访后的服务边界接口分类)

文件名:`docs/[项目缩写]_HLD_PROJECT_v1.0_[YYYYMMDD].md`

### Step 5 — 校验

```bash
python3 .agents/skills/hld-designer/scripts/check_hld_structure.py docs/[项目缩写]_HLD_PROJECT_v1.0_*.md
```

退出码 = 0 才算完成。若有违规,修复后重跑。

## 产物

- `docs/[项目缩写]_HLD_PROJECT_v1.0_[YYYYMMDD].md`
- 校验通过

## 完成标准

- [ ] 8 字段头部齐全
- [ ] 4 章必备(概述 + 流程图 + 数据模型 + 接口设计)
- [ ] 流程图是 mermaid 语法
- [ ] 文件名符合命名规范
- [ ] 校验脚本退出码 = 0
