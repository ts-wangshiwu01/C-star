# Wiki SCHEMA Quick Reference

Source: `$OBSIDIAN_VAULT_PATH/SCHEMA.md` — always re-read the original if SCHEMA may have changed.

## Wiki Location
`$OBSIDIAN_VAULT_PATH/`

## Directory Structure

```
wiki/
├── knowledge/          ← 通用知识点
│   ├── database/       ← TiDB, Cassandra, Redis...
│   ├── middleware/     ← Nginx, RabbitMQ, Docker...
│   ├── devops/         ← systemd, K8s, LVM...
│   ├── network/        ← OpenVPN, IPSec, DNS...
│   ├── ai/             ← AIOps, LLM...
│   └── ...
├── campaign/           ← Campaign 项目
│   └── {专项名}/       ← 专项子文件夹
├── onepoint/           ← Onepoint 项目
├── articles/           ← 大型交付文档（changelog 内置）
└── raw/                ← 原文存档
```

## Placement Rules

| Content | Where | Style |
|---------|-------|-------|
| 通用知识点 | `knowledge/<type>/` | Concise, 10-50 lines |
| 项目内容 | `<project>/` | As needed |
| 项目专项 | `<project>/<专项>/` | As needed |
| 大型交付文档 | `articles/` | Complete, with internal changelog |

## articles/ changelog rule
Every `articles/` doc must include:
```markdown
## 文档变更记录
| 日期 | 变更 |
|------|------|
| 2026-06-10 | 初版：xxx |
```

## Frontmatter Template

```yaml
---
title: Page Title
created: YYYY-MM-DD
updated: YYYY-MM-DD
type: entity | concept | comparison | query | summary
tags: [from taxonomy below]
sources: [raw/articles/source-name.md]
confidence: high | medium | low
contested: true
contradictions: [other-page-slug]
---
```

`confidence` and `contested` are optional. `contradictions` lists pages that conflict.

## Tag Taxonomy

### 技术
tech, ai-ml, programming, architecture, tool, benchmark, training

### 投资
investment, fund, stock, macro, strategy, risk

### 人物与组织
person, company, lab, open-source, onepoint, campaign

### 方法论
methodology, framework, principle, comparison, timeline

### 元信息
summary, controversy, prediction, opinion

Rule: every tag must appear in this taxonomy. Add new tags to SCHEMA.md first.

## Page Thresholds

- **Create** when entity/concept appears in 2+ sources OR is central to one source
- **Add to existing** when a source mentions something already covered
- **DON'T create** for passing mentions or minor details
- **Split** when page exceeds ~200 lines
- **Archive** when fully superseded — move to `_archive/`, remove from index
- **Consolidate** — prefer 1-2 files over 5+ scattered mini-docs

## Update Checklist

When creating a new page:
1. Determine correct directory (knowledge/ vs project/ vs articles/)
2. Write page with frontmatter + min 2 wikilinks
3. Add entry to `index.md` under correct section
4. Append action to `log.md`

When updating an existing page:
1. Bump `updated` date in frontmatter
2. Append action to `log.md`

When deleting a page:
1. Remove from `index.md` + adjust page count
2. Append delete action to `log.md`
