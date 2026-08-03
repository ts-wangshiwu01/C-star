# 前端选型：Vue 3 + Element Plus

## 状态

Accepted

## 背景

C-Star 是公司级员工赞赏平台，前端功能以简单 CRUD 为主：登录、表单（发送赞赏）、列表（收发列表、全公司记录）、排行榜、配额展示。

后端已选定为 Micronaut + gRPC + MariaDB，前端只需 SPA，不需要 SSR。`architecture/c-star-architecture.md` 中前端行原标记为"待定"，倾向 React + shadcn/ui 或 Vue + Element Plus。

## 决策

前端选用 **Vue 3 + Element Plus**。

## 理由

- **场景匹配**：C-Star 是简单 CRUD 应用，Element Plus 自带表格、表单、分页、对话框等组件，开箱即用，列表/表单场景直接套用，无需自行组装
- **上手快**：Vue 3 单文件组件（SFC）模板语法直观，学习曲线平缓
- **省配置**：Element Plus 是完整的组件库，不需要像 shadcn/ui 那样"复制代码 + 自行组装样式"

## 备选方案与取舍

| 方案 | 取舍 |
|---|---|
| React + shadcn/ui | 灵活但上手成本高。shadcn/ui 是"可复制代码"不是组件库，需要自行组装和样式。对简单 CRUD 项目来说大材小用 |
| React + Ant Design | 与 Element Plus 定位类似，开箱即用。如果团队更熟 React 可作为备选 |
| Next.js / Nuxt 3（全栈框架）| C-Star 后端是 Micronaut + gRPC，前端只需 SPA，不需要 SSR。全栈框架对简单项目来说配置复杂度偏高 |

## 影响

- 前端代码仓库使用 Vue 3 + Element Plus + Vite
- `architecture/c-star-architecture.md` 的前端行从"待定"更新为 Vue 3 + Element Plus
