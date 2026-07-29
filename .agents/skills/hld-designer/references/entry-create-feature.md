# Entry C — Feature 级 HLD 创建

**触发词**:feature 的 HLD / 功能的 HLD / ticket + HLD

## 输入

- 服务级 HLD:`docs/*_HLD_SERVICE_[服务名]_*.md`
- Ticket 编号 + 需求描述(用户给)
- PRD 对应章节(可选,定位业务上下文)
- `architecture/`(若存在,补充依赖关系)

## 流程(4 步)

### Step 1 — 读取服务级 HLD

提取:

- 服务处理流程图(定位本 feature 在服务内的位置)
- 服务数据模型(看 feature 会动哪些表)
- 服务接口设计(看 feature 会加/改哪些接口)

### Step 2 — 采访 feature 决策点

逐个问用户:

- 这个 feature 涉及哪些数据变更?(新增表?改字段?)
- 暴露/修改哪些接口?
- 流程图里这个 feature 在哪一步介入?
- 异常处理方向?(可选,只在有明显失败模式时问)

### Step 3 — 生成 feature 级 HLD

按 `templates/hld-feature.md` 填入 4 章,详细度最高:

1. 概述:本 feature 做什么(一句话)+ ticket 号 + 所属服务级 HLD 文件名
2. 处理流程图:feature 的完整请求流转(最细,体现从入口到落库的每一步)
3. 数据模型:具体表/字段变更(新增/修改,逻辑层接近 DDL 但不到 DDL)
4. 接口设计:具体接口签名 + 请求/响应字段(精确到 proto 字段级)

文件名:`docs/[项目缩写]_HLD_FEATURE_[服务名]_[ticket-slug]_v1.0_[YYYYMMDD].md`

### Step 4 — 校验

```bash
python3 .agents/skills/hld-designer/scripts/check_hld_structure.py docs/[项目缩写]_HLD_FEATURE_[服务名]_[ticket-slug]_v1.0_*.md
```

退出码 = 0 才算完成。

## 产物

- `docs/[项目缩写]_HLD_FEATURE_[服务名]_[ticket-slug]_v1.0_[YYYYMMDD].md`
- 校验通过

## 完成标准

- [ ] 8 字段头部齐全(文档类型为 "High Level Design — Feature 级")
- [ ] 4 章必备
- [ ] 流程图是 mermaid 语法
- [ ] 文件名符合命名规范
- [ ] 校验脚本退出码 = 0
