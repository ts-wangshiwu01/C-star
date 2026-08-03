---
name: obsidian
description: Wiki authoring compliance — SCHEMA frontmatter, index.md updates, git commits, consolidation rules. MUST load before any wiki write. For multi-page topic updates, first audit related pages, reconcile conflicts, and delete superseded drafts by default; see references/topic-reconciliation.md.
platforms: [linux, macos, windows]
---

# Obsidian Vault

Use this skill for filesystem-first Obsidian vault work: reading notes, listing notes, searching note files, creating notes, appending content, and adding wikilinks.

## Vault path

Use a known or resolved vault path before calling file tools.

The documented vault-path convention is the `OBSIDIAN_VAULT_PATH` environment variable, for example from `~/.config/agent/.env`. If it is unset, use `~/Documents/Obsidian Vault`.

File tools do not expand shell variables. Do not pass paths containing `$OBSIDIAN_VAULT_PATH` to `read_file`, `write_file`, `patch`, or `search_files`; resolve the vault path first and pass a concrete absolute path. Vault paths may contain spaces, which is another reason to prefer file tools over shell commands.

If the vault path is unknown, `terminal` is acceptable for resolving `OBSIDIAN_VAULT_PATH` or checking whether the fallback path exists. Once the path is known, switch back to file tools.

## Read a note

Use `read_file` with the resolved absolute path to the note. Prefer this over `cat` because it provides line numbers and pagination.

## List notes

Use `search_files` with `target: "files"` and the resolved vault path. Prefer this over `find` or `ls`.

- To list all markdown notes, use `pattern: "*.md"` under the vault path.
- To list a subfolder, search under that subfolder's absolute path.

## Search

Use `search_files` for both filename and content searches. Prefer this over `grep`, `find`, or `ls`.

- For filenames, use `search_files` with `target: "files"` and a filename `pattern`.
- For note contents, use `search_files` with `target: "content"`, the content regex as `pattern`, and `file_glob: "*.md"` when you want to restrict matches to markdown notes.

## Create a note

Use `write_file` with the resolved absolute path and the full markdown content. Prefer this over shell heredocs or `echo` because it avoids shell quoting issues and returns structured results.

## Append to a note

Prefer a native file-tool workflow when it is not awkward:

- Read the target note with `read_file`.
- Use `patch` for an anchored append when there is stable context, such as adding a section after an existing heading or appending before a known trailing block.
- Use `write_file` when rewriting the whole note is clearer than constructing a fragile patch.

For an anchored append with `patch`, replace the anchor with the anchor plus the new content.

For a simple append with no stable context, `terminal` is acceptable if it is the clearest safe option.

## Targeted edits

Use `patch` for focused note changes when the current content gives you stable context. Prefer this over shell text rewriting.

## Wikilinks

Obsidian links notes with `[[Note Name]]` syntax. When creating notes, use these to link related content.

## ⚠️ Structured Wiki (User-Specific)

This user has a **structured wiki** at `$OBSIDIAN_VAULT_PATH/`. NOT the default `~/Documents/Obsidian Vault`. When the user says "文档库" (document library) or asks to organize information into notes, **this wiki is the target**.

### Workflow: Load obsidian skill BEFORE every wiki operation

**This is mandatory.** Every write/create/update/delete to the wiki MUST:
1. Load this skill (`skill_view(name='obsidian')`) to refresh the vault path and SCHEMA rules
2. Optionally also load `wiki-ingestion` skill for bulk ingest tasks

### Content consolidation rule: ONE topic = ONE or TWO documents

When planning a set of wiki pages, prefer consolidation over scattering:
- **Phase 1 + Phase 2** as 2 documents for a multi-phase project — NOT 5+ scattered files
- Each document should be 200-400 lines; split only when exceeding ~400 lines
- Use `##` sections within a file for different sub-topics, not separate files
- Ask yourself: "can the reader get the full picture from 2 files?" before creating a 3rd
- Once the content is cleanly split into **general knowledge** vs **project-specific information**, stop reorganizing for taxonomy purity. Do not keep moving pages just to make the tree prettier if that split is already satisfied.

Rationale: The user explicitly corrected "文档太分散了 你不如整合成两个" — scattered docs are harder to navigate. They also explicitly said that separating general knowledge from project information is sufficient; extra restructuring beyond that is noise.

### Superseded drafts: delete by default

When a new authoritative page replaces older draft pages, phase plans, or temporary reconciliation stubs, **delete the superseded pages by default** instead of keeping archive placeholders. Do not preserve stale wiki pages just because git history already exists — git commit history and the old `log.md` archive are sufficient audit trails. Keep an old page only when:
- the user explicitly asks to retain it, or
- it still contains distinct operational value not absorbed into the surviving page.

### Index update is MANDATORY, not optional

**Every** write/create/update/delete operation on wiki files MUST be immediately followed by:
1. `index.md` update — add/remove/modify entries under correct section + bump page count + update date
2. `git add + git commit` — commit message follows the log format: `action: subject — detail` (e.g., `create: example-page`, `update: pulsar-architecture — 补充组件间服务发现机制`). This replaces the former `log.md` update step.

This is the single most common recurring mistake — forgetting index.md. The user has explicitly corrected this pattern.

### Git commit workflow (replaces log.md)

The wiki repo (resolve path from $OBSIDIAN_VAULT_PATH) is a git repo. After every wiki write operation:

1. `git add` all changed files (the page itself + index.md + any other touched files)
2. `git commit -m "action: subject — detail"` — use concise Chinese or English matching the old log.md style
3. Do **NOT** push — the user pushes manually. **⚠️ THIS IS NON-NEGOTIABLE. NEVER run `git push`, even if the commit succeeds. Even if the user seems impatient. Even if you think it's just one step away. The user pushes manually, always. Violating this breaks trust.**

**⚠️ Auto-commit rule (wiki repo ONLY):** Wiki changes are committed automatically without asking for confirmation. This is an explicit exception to the general safety boundary that requires confirmation before mutating operations. For all OTHER git repos, you MUST still ask for confirmation before committing.

Commit message format examples:
- `create: example-page`
- `update: pulsar-architecture — 补充组件间服务发现机制`
- `ingest: Linux-operationbook 文档集（通用运维知识）`
- `delete: outdated-page-name`

The old `log.md` is retained as a historical archive but is no longer written to. More importantly: do not preserve superseded wiki pages just to keep history — git commit history and log already provide that audit trail, so stale drafts should normally be deleted.

### Wiki SCHEMA Compliance (CRITICAL)

This wiki has a `SCHEMA.md` at its root that defines strict conventions. Every page written to this wiki **must** comply:

1. **YAML frontmatter** — `title`, `created` (ISO 8601 with seconds: `YYYY-MM-DDTHH:MM:SS`), `updated` (same format), `type` (entity|concept|comparison|query|summary), `tags` (from SCHEMA taxonomy), `sources`, optional `confidence`/`contested`/`contradictions`
2. **File naming** — lowercase, hyphens, no spaces (e.g., `pulsar-cluster-migration.md`)
3. **Minimum 2 `[[wikilinks]]`** per page
4. **Update `index.md`** — add new page under correct section with one-line summary; bump page count and date
5. **Git commit** — `git add` + `git commit -m "action: subject — detail"` (replaces former log.md step)
6. **Bump `updated` date** in frontmatter when modifying existing pages
7. **Tags must come from SCHEMA.md taxonomy** — add new tags there first if needed

### Two scenarios for wiki content

| Scenario | Reader | Style | Where | Notification |
|----------|--------|-------|-------|-------------|
| **日常积累** — knowledge, project notes | Agent + user (Obsidian browsing) | Concise, 10-50 lines | `knowledge/` or `{project}/` | User discovers in Obsidian, no push |
| **交付文档** — research reports, migration guides | User (reading/executing) | Complete, detailed, self-contained | `articles/` | Agent actively tells user "写好了" |

**Rule of thumb**: If the user asked me to research something and write it up → `articles/`. If I'm accumulating knowledge from daily work → `knowledge/` or `{project}/`.

### Global task-intake / investigation logs belong at wiki scope

Do **not** automatically hang task-intake, reconnaissance, or early-stage investigation logs under a project directory just because the current task happens to touch one project. Some tasks are cross-project, and the logging system itself is a **wiki-global workflow**, not a project artifact.

Default pattern for this class of content:
- global task log / queue page → `articles/task-log.md`
- one page per investigated task → `articles/tasks/<task-slug>.md`

Use a project directory instead only when the task documentation is clearly part of a project's durable domain knowledge rather than a reusable task-tracking workflow.

This distinction matters because:
- `campaign/` or `onepoint/` are project knowledge spaces
- `task-log` is an operational intake mechanism spanning many possible domains
- putting the mechanism under one project makes later retrieval and reuse worse

### Page directory structure

```
wiki/
├── knowledge/          ← 通用知识点（与项目无关）
│   ├── database/       ← TiDB, Cassandra, Redis, MySQL, Oracle...
│   ├── middleware/     ← Nginx, RabbitMQ, RocketMQ, Docker...
│   ├── devops/         ← systemd, K8s, LVM, firewalld...
│   ├── network/        ← OpenVPN, IPSec, DNS, Keepalived...
│   ├── ai/             ← AIOps, LLM...
│   └── ...             ← 按需新增子文件夹（按类型分）
│
├── campaign/           ← Campaign 项目所有内容
│   ├── *.md            ← 项目级文档
│   └── {专项名}/       ← 专项子文件夹（migration/, refactor/...）
│
├── onepoint/           ← Onepoint 项目所有内容
│   └── *.md
│
├── articles/           ← 大型交付文档（给用户看的研究报告/操作指南）
│   └── *.md            ← changelog 写在文档内部（见下方规则）
│
└── raw/                ← 原文存档（不可变）
```

**Placement rules:**

| Content type | Where | Example |
|-------------|-------|---------|
| 通用知识点 | `knowledge/<type>/` | "TiDB MVCC 原理" → `knowledge/database/tidb.md` |
| 项目相关内容 | `<project>/` | "Pulsar 架构" → `campaign/pulsar-architecture.md` |
| 项目专项 | `<project>/<专项>/` | 迁移 → `campaign/migration/phase1.md` |
| 大型交付文档 | `articles/` | "上下游依赖分析" → `articles/incentive-platform-dependencies.md` |

### articles/ changelog rule

Every document under `articles/` MUST include a changelog section inside the document:

```markdown
## 文档变更记录

| 日期 | 变更 |
|------|------|
| 2026-06-10 | 初版：xxx |
| 2026-06-12 | 补充：xxx |
```

Other wiki pages (knowledge/, campaign/, onepoint/) do NOT need internal changelogs — git commit history serves that purpose (formerly `log.md`, archived 2026-06-12).

For `articles/task-log.md`, keep the page itself concise: it is an index/status board, not the full investigation narrative. Put the full content in `articles/tasks/<task-slug>.md` and link to it from the log.

### ⚠️ Verify infrastructure facts before writing

When writing about deployment topology (replica counts, resource limits, cluster configs), **always verify against the live cluster first** — never rely on subagent summaries or overlay config guesses. The user has corrected this: stating "3 proxy + 5 broker" without verification turned out to be wrong (actual: 2 proxy + 3 broker).

### ⚠️ Migration docs: separate cutover mechanism from steady-state target

For migration / DR / topology documents, explicitly split these two concerns instead of blending them:

1. **Cutover mechanism** — the exact operational sequence used during migration day (for example: no-balance validation → revert to normal config at 0 replica → cut GSLB → shut old → start new).
2. **Post-migration steady state** — the architecture that exists after cutover settles (for example: later cross-DC topology, standby placement, geo-replication, or paper failover).

Do not infer that the steady-state topology is also the migration path. If a source doc describes the target Option A/B/C architecture, verify separately whether the actual cutover is still "shut old / start new" or something else. When facts evolve mid-session, rewrite the wiki page so these two sections stay distinct instead of leaving contradictory explanations.

### ⚠️ Geo-location GSLB migrations: record the routing model precisely

When documenting cutovers behind GSLB, do **not** casually rewrite the access model as "two domains", "two endpoints", or "all clients are affected together" unless the source explicitly says that.

For this user, a recurring pitfall is: **one primary GSLB domain with geo-location routing and per-region backends**. In that model, the migration impact is usually **regional per cutover**, not global all-at-once.

For each migration page, explicitly write down all four facts when known:
1. whether clients use **one domain or two**;
2. whether GSLB routing is **geo-location, failover, round-robin, or other**;
3. which **regional backend** is being changed in this cutover;
4. whether client restart is a **hard prerequisite**, a **recommended recovery action**, or merely a **risk note**.

If the conversation reveals a correction like "same GSLB domain, geo routing 100/100" or "JPC1 also cuts to seg12", update the page immediately instead of leaving the old broader wording in place.
**Mandatory verification workflow:**
1. `roc login -c<cluster> -n<namespace>` → `kubectl get pods/sts/deploy`
2. Compare against code overlay `.env` files for diffs
3. If you cannot access the cluster, explicitly mark the fact as "unverified" in the wiki page

**Code vs Live diff technique** (for CaaS/K8s deployments):
- Active ConfigMap names appear in `envFrom:` of the running StatefulSet/Deployment
- Compare the active ConfigMap values against the overlay `.env` files in the repo
- This surfaced a real config bug (ZK port `21810` typo in `overlays/dev/jpe2/.env`)

### Ingestion style: 概述+链接，不搬原文

When ingesting documentation from code repos or external sources into the wiki, the user's explicit preference is:

- ✅ **概述** — 1-sentence summary of what each document covers
- ✅ **链接** — source file path so the reader can find the original
- ❌ **不搬原文** — do NOT copy raw content into wiki pages
- ❌ **不逐段翻译** — do NOT reproduce the source structure paragraph-by-paragraph

Rationale: The wiki is an **index and navigation layer**, not a mirror. Readers go to the source repo for full detail. The wiki page tells them WHERE to look and WHAT they'll find.

**Exception**: Confluence-only documents (not in any repo) may need more content, since there's no local file to link to.

### Workflow for adding/removing wiki pages

1. Load this skill (`skill_view(name='obsidian')`) and `wiki-ingestion` if bulk ingest
2. Read `index.md` — check existing pages that might overlap. **Add to existing page** if topic already covered, don't create a new one.
3. Plan the document set — prefer 1-2 consolidated files over scattered mini-docs
4. Write the document with frontmatter + ≥2 wikilinks
5. **Delete superseded drafts by default** when a new authoritative page replaces them. Do not keep stale phase pages or archive stubs just because git already preserves history. Keep old pages only when the user explicitly wants them retained or they still carry distinct operational value.
6. **IMMEDIATELY update `index.md`** — add/remove entries + bump count + update date
7. **IMMEDIATELY git commit** — `git add` all changed files + `git commit -m "action: subject — detail"`
8. If deleting old documents, update index + commit in the same step

### ⚠️ Pitfall: Use jira-confluence-query skill for Confluence URLs

When a wiki update requires reading a Confluence page (e.g., user provides a `confluence.rakuten-it.com` URL as source), **DO NOT use browser_navigate**. SSO cannot auto-complete and browser will always fail at the login page. Instead, use the `jira-confluence-query` skill's CLI: `jc.py conf <page-id-or-url>`. This is the only reliable way to fetch Confluence content.

**Why agents keep making this mistake**: The description of this skill used to say "Read, search, create, and edit notes in the Obsidian vault" — which sounds like a file operation tool, not a compliance standard. Agents would decide "I'm writing a migration document" and not realize they need this skill. The description has been updated to emphasize **compliance** and **MUST load before any wiki write**, but if you're reading this, you've already loaded it correctly.

### ⚠️ Pitfall: Skill description quality affects recall

When a skill's description describes **what it does** (tool-level: "read, create, edit notes") instead of **when it must be used** (compliance-level: "MUST load before any wiki write"), agents won't recognize the trigger and will skip loading it. If you notice a skill not being recalled when it should be, check if its description contains:
- ❌ Tool-level phrasing ("Read, search, create...")
- ✅ Compliance/trigger phrasing ("MUST load before X", "compliance rules for Y")
- ✅ Domain-specific trigger words that match how agents think about the task

If you find a poorly-described skill, patch its description immediately.

**Never write cluster infrastructure details (replica counts, resource limits, IPs, ports) to wiki based on subagent output or past session memory alone.** Always verify from the live cluster first.

The user explicitly corrected this: unverified data ("3 proxy + 5 broker") was written as fact, turned out to be wrong (actual: 2 proxy + 3 broker in dev).

**Required verification steps for infrastructure wiki pages:**
1. `kubectl get pods/sts/deploy -n <namespace>` — confirm actual replica counts
2. `kubectl get configmap <active-cm> -o yaml` — confirm running config values
3. Compare against code repo overlay `.env` files for drift

If the live cluster is not accessible, mark the data as "unverified" in the wiki page and note what needs checking.

### Link integrity audit

When the user reports "链接困难" or "找不到原文" or asks to check wiki-to-external linking, run a **three-layer link audit**:

```
wiki 页面 → raw/ 本地副本 → 外部在线原文 URL
```

**Diagnostic script** (run via `execute_code`):
1. Scan all wiki .md for `[[wikilinks]]`, check each target exists as a file — report broken links
2. Scan all wiki .md for `sources:` frontmatter — check each path exists under `raw/` — report broken sources
3. Scan all raw/ .md for `source_url:` frontmatter — report files missing it
4. Scan all wiki pages with `sources:` — check which have a `## 来源` section linking to online originals
5. Report orphan pages (0 inbound wikilinks, excluding SCHEMA/index/log)

**Key metrics to report:**
- raw/ `source_url` coverage (target: 100%)
- wiki pages with `## 来源` section vs. pages with `sources:` (target: 1:1)
- Broken wikilinks (target: 0, excluding bash `[[ ]]` false positives)
- Broken `sources:` references (target: 0)

**Bash `[[ ]]` false positive filter**: When scanning for wikilinks, skip targets containing `$`, `=`, `!`, or `~` — these are bash conditional expressions, not Obsidian links.

See `references/wiki-link-audit-methodology.md` for the full audit + repair workflow.

### Quick reference

Condensed SCHEMA rules and tag taxonomy: `references/wiki-schema-quickref.md`. Always re-read the actual `SCHEMA.md` at the wiki root if it may have been updated.
- Bulk mem0 memory cleanup: `references/mem0-bulk-cleanup.md` — when memories exceed 100+ and need pruning
- Link audit methodology: `references/wiki-link-audit-methodology.md` — three-layer link audit + repair workflow
