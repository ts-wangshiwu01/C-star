# Wiki Link Audit Methodology

Three-layer traceability audit for the structured wiki at `$OBSIDIAN_VAULT_PATH/`.

## The Three Layers

```
wiki 页面 ──sources:──→ raw/ 本地副本 ──source_url:──→ 外部在线原文 URL
```

All three links must be intact for a reader to trace any wiki claim back to its online original.

## Diagnostic Phase

### 1. Broken wikilinks

Scan wiki .md files (excluding `raw/`, `.obsidian/`, `.git/`) for `[[wikilinks]]` and check each target exists as a .md file.

**False positive filter**: Skip targets containing `$`, `=`, `!`, or `~` — these are bash `[[ ]]` conditionals, not Obsidian links.

```python
wikilink_pattern = re.compile(r'\[\[([^\]|]+)(?:\|[^\]]+)?\]\]')
# Filter: if any(c in target for c in ['$', '=', '!', '~']): skip
```

Common causes:
- File was renamed but links not updated (e.g., `[[segment-cluster-migration-phase1]]` → should be `[[phase1]]`)
- Page never created

### 2. Broken `sources:` references

Parse frontmatter `sources:` list from wiki pages. Check each path:
- Must exist under `raw/` (or be a valid GitHub/online URL)
- Repository paths like `incentive-platform-deployments/docs/...` are NOT valid raw/ paths — convert to `https://ghe.rakuten-it.com/incentive-platform/<repo>/blob/main/<path>`
- Missing raw files: mark with `# ⚠️ file not archived` comment

### 3. raw/ `source_url` coverage

Check every raw/ .md file has `source_url:` in frontmatter:
- External source → actual URL
- Team-authored → `source_url: internal`
- Truly no URL → `source_url: n/a`

**Extraction shortcut for Pulsar files**: Pulsar raw files embed `🔗 原文链接：[URL](URL)` in body text. Extract the first such URL programmatically.

### 4. `## 来源` section coverage

Every wiki page with non-empty `sources:` frontmatter must have a `## 来源` section at the bottom (before `## 关联` if both exist). Format:

```markdown
## 来源

- [在线原文标题](https://example.com/article) — 在线原文
- raw/articles/handbook/TiDB/... — 团队原创
- raw/articles/confluence/... — ⚠️ 原文未存档
```

### 5. Orphan pages

Pages with 0 inbound wikilinks (excluding `SCHEMA`, `index`, `log` which are metadata). Add inbound links from related pages.

## Repair Phase (Parallel Sub-Agents)

Split repairs into parallel workstreams:

| Sub-agent | Task |
|-----------|------|
| 1 | Fix broken wikilinks + add inbound links to orphans |
| 2 | Add `source_url` to Pulsar raw/ files (extract from `🔗` markers) |
| 3 | Add `source_url` to TiDB/Cassandra/other raw/ files (mostly `internal`) |
| 4 | Add `## 来源` sections to wiki pages + fix broken `sources:` references |
| 5 | Update SCHEMA.md rules (if needed) |

### sources: repair rules

- **Repository path references** → Convert to GitHub URL: `https://ghe.rakuten-it.com/incentive-platform/<repo>/blob/main/<path>`
- **Missing raw/ files** → Keep reference with `# ⚠️ file not archived` comment
- **Invalid paths** (e.g., `kubectl线上确认`) → Mark with `# ⚠️ source path invalid`

### ## 来源 section format

For each source in frontmatter:
1. Read the raw/ file's `source_url`
2. If URL exists → `- [标题](URL) — 在线原文`
3. If `source_url: internal` → `filename — 团队原创`
4. If raw file missing → `filename — ⚠️ 原文未存档`
5. If source path invalid → `path — ⚠️ 原文路径无效`

Place `## 来源` before `## 关联` / `## 关联页面` if they exist, otherwise at page end.

## Verification

After all repairs, re-run diagnostics:
- Broken wikilinks: 0 (excluding bash)
- raw/ source_url coverage: 100%
- Wiki pages with ## 来源: matches pages with sources:
- Broken sources references: 0

## SCHEMA.md Rules (Added 2026-06-17)

- `source_url` in raw/ frontmatter is **REQUIRED** (not optional)
- Wiki pages must include `## 来源` section for source traceability
- `sources:` paths must reference existing raw/ files; broken paths marked with `# ⚠️ file not archived`
- New `source_urls:` frontmatter field for direct online-original links
