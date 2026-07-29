# Entry D — Feature 合并回上级(用户手动触发)

**触发词**:合并 HLD / feature 完成了,更新服务级 HLD / 把 feature 合并回项目级

## 触发条件

用户明确说"合并 HLD",并指定合并目标(服务级或项目级)。间隔可能数周到数月(feature review + 实现 + 测试 + 上线后)。

**关键纪律**:合并是用户事后主动触发,不是 feature 写完自动连跑。

## 输入

- Feature 级 HLD:`docs/*_HLD_FEATURE_*.md`
- 用户指定的目标上级 HLD(服务级或项目级):`docs/*_HLD_SERVICE_*.md` 或 `docs/*_HLD_PROJECT_*.md`

## 流程(4 步)

### Step 1 — 读取两份 HLD

- Feature HLD:提取 4 章内容
- 目标上级 HLD:提取对应章节

### Step 2 — Diff 分析

逐章对比,识别:

- **新增**:feature 引入的表/接口/流程步骤
- **修改**:feature 改了原有接口签名或字段
- **删除**:feature 移除了某个流程分支

详细方法见 `references/patch-generation.md`。

### Step 3 — 生成 patch

生成结构化 patch(不是覆盖),分 4 章列出变更点:

```
## Patch — [feature HLD] → [目标 HLD]

### 概述
- [无变更 / 新增一句:...]

### 处理流程图
- 在 step N 后新增 step N+1:...
- 修改 step M 的描述:...

### 数据模型
- 新增表:xxx(字段:y, z)
- 修改表:yyy,新增字段 zzz
- 无删除

### 接口设计
- 新增接口:POST /api/xxx
- 修改接口:GET /api/yyy 增加 query param zzz
```

### Step 4 — 人审确认

把 patch 呈现给用户:

- **用户确认** → 用 patch 工具写入目标 HLD,版本号 +0.1,修订记录加一条
- **用户修改** → 调整 patch 后重新确认
- **用户拒绝** → 不写入,记录原因

## 产物

- 目标 HLD 更新(版本号 +0.1,修订记录加一条)
- patch 记录(可选,存到 `docs/patches/` 或直接在对话里)

## 合并目标判断

用户明确指定合并到哪一级,skill 不自动判断。常见场景:

- feature 实现完 → 合并回服务级(更新服务级 HLD)
- 多个 feature 合并到服务级后 → 用户再触发合并回项目级(更新项目级 HLD)

## 完成标准

- [ ] patch 已生成(分 4 章)
- [ ] 用户已审阅(确认 / 修改 / 拒绝)
- [ ] 若确认:目标 HLD 版本号 +0.1,修订记录已更新
- [ ] 若确认:重跑校验脚本退出码 = 0
