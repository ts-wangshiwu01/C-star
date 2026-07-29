---
name: grilling-methodology
description: Structured adversarial review methodology for stress-testing plans, designs, and improvement initiatives against codebase evidence. Use when grilling a repo, challenging a plan, or evaluating a workspace for quality gaps.
---

# Grilling Methodology

A structured adversarial review that stress-tests a plan or design against evidence from the codebase, documentation, and external best practices.

## Core Principle

**A grilling session is not a brainstorm.** It is an interrogation: every claim must be grounded in evidence, every gap surfaced as a contradiction, and every recommendation traceable to a specific finding. The output is not "ideas" — it is a structured list of contradictions + actionable improvements with priority.

## When to Use

- User asks to "grill", "stress-test", "review this plan", "challenge this design"
- User wants to evaluate a repo or workspace for quality improvements
- User says "研究一下 X 然后给一个改进计划" or similar
- Before committing to a significant change initiative
- The `grill-with-docs` skill triggered but you need the actual methodology (the original skill is a thin pointer)

## The Grilling Process

### Phase 1: Evidence Gathering (Research)

Read **everything** before forming opinions:

1. **Entry documents**: README, AGENT.md, CLAUDE.md, init.md, INSTRUCTION files — any file that defines "how this project works"
2. **Architecture docs**: Every file under `architecture/` or equivalent — component relationships, data flows, design constraints
3. **Skills / operational patterns**: Every SKILL.md in the project's skill directory — understand what automations exist
4. **Existing examples**: Any detail-design docs, feature docs, ADRs — these are "success cases" that reveal the intended quality bar
5. **External best practices**: Load relevant skills from your own skill library (code-review, TDD, planning, domain-modeling patterns) to use as comparison baselines
6. **Code structure**: `git log`, directory tree, file inventory — understand what actually exists vs what docs claim exists

> **Key**: Read the full content of each file, not just filenames and headers. The contradictions live in the gap between what docs claim and what code/skills actually do.

### Phase 2: Cross-Referencing (Find Contradictions)

Compare claims against reality across three axes:

| Axis | What to check | Example |
|------|--------------|---------|
| **Claim vs Reality** | Doc says X exists, but code/skill doesn't | "Quality is enforced" but no quality-gate skill exists |
| **Role vs Support** | Doc claims to serve role Y, but no entry point for Y | "Helps PDM" but no PDM-facing docs or skills |
| **Workflow vs Enforcement** | Process says step A → step B, but no skill enforces the dependency | "Design before develop" but no prerequisite check |

For each contradiction found, articulate it precisely:
- **What is claimed** (quote the doc/source)
- **What actually exists** (cite the file/code/skill)
- **Why it matters** (what breaks or degrades because of this gap)

### Phase 3: Structured Output

Produce a contradiction list + improvement plan:

```
### Contradiction N: [precise title]

**Claim**: [what the docs/claims say]
**Reality**: [what the code/skills actually show]
**Impact**: [what breaks or degrades]

**Action needed**:
- [New file/skill/patch with exact path]
- [What it should contain — 1-2 sentences]
```

Then a summary table:

```
| # | Type | Action | Priority |
|---|------|--------|----------|
```

### Phase 4: Clarifying Questions

Before the grilling concludes, surface genuine ambiguities that need the user's decision:

- **Scope**: All items or P0 only?
- **Format**: Execute now or save as plan doc for team review?
- **Ownership**: Who owns each improvement?

Ask these as a batch — do not drip one question at a time. The user's answers shape the execution path, not the analysis itself.

## Domain Modeling Integration

During the grilling, if domain terms surface that don't have a canonical definition:

1. **Create `CONTEXT.md`** at the repo root — record each term with its definition and `_Avoid_` list (per `domain-modeling` skill format)
2. **Cross-service term mapping**: If different services use different names for the same concept (e.g. `eventType` in service A = `orderType` in service B), record the mapping explicitly
3. **Offer ADRs** when architectural decisions are found that are hard to reverse, surprising without context, and the result of a real trade-off

## Output Quality Standards

- **Every contradiction cites specific files** — "README.md line 30 claims X" not "the docs say X"
- **Every improvement action has a concrete path** — "create `.opencode/skills/quality-gate/SKILL.md`" not "add a quality skill"
- **Priority is assigned** — P0 (blocks core function), P1 (significant gap), P2 (nice to have)
- **External best practices are referenced** — "a typical code-review skill has a complete static-scan → baseline-test → independent-reviewer pipeline that this repo doesn't use"
- **No vague suggestions** — "improve documentation" is not an action; "add Quick Start section to README.md with PDM and Dev paths" is

## Pitfalls

- **Don't skip the research phase** — forming opinions before reading all files produces shallow contradictions the user has already thought of
- **Don't confuse "missing feature" with "contradiction"** — a contradiction is when a claim doesn't match reality; a missing feature is just a gap. Both go in the plan, but contradictions are higher signal
- **Don't externalize best practices without checking** — before saying "should use TDD like skill X", verify that skill X actually exists and is relevant to the project's tech stack
- **Don't produce improvement lists without priority** — an unprioritized list is a brainstorm, not a plan
- **Don't drip clarifying questions** — batch them at the end so the user can answer in one shot
- **Do not treat external skill ecosystems as the only valid pattern** — use them as reference baselines, not golden standards. The project may have valid reasons to deviate

## See Also

- `domain-modeling` skill — for CONTEXT.md and ADR creation
- `grill-with-docs` skill — the thin pointer that triggers a grilling session; this skill provides the actual methodology
- `references/grilling-checklist.md` — pre-output self-check checklist