# Multi-page wiki topic reconciliation

Use this when a user asks to "整理/精细整理/统一" a topic that already spans several pages.

## Goal
Turn a drifting cluster of pages into a coherent mini-knowledge-base with one clear owner per concern.

## Required workflow
1. **Inventory first**
   - List all pages for the topic from the index and by filename/content search.
   - Separate: overview pages, execution/runbook pages, architecture pages, risk/limitations pages, historical notes/drafts.
2. **Define page responsibilities**
   - Choose one **authority page** for execution.
   - Keep architecture pages focused on steady-state design, not migration runbooks.
   - Keep project pages focused on scope/timeline/decision context, not low-level mechanics.
   - Mark stale planning drafts as historical/archive instead of letting them masquerade as current truth.
3. **Reconcile contradictions explicitly**
   - Fix stale dates, target clusters, routing assumptions, and client-impact statements.
   - If a statement was only an early hypothesis, either remove it or label it as historical context.
4. **Promote cross-links**
   - Every major page should point to the authority pages for adjacent concerns.
   - Add or fix `[[wikilinks]]` so readers can navigate overview → architecture → execution → limitations.
5. **Update index summaries**
   - The index line must reflect the current role of each page, especially if a page became archive-only.
6. **Commit the wiki**
   - For wiki changes, commit after reconciliation is complete.

## Durable lessons from the Pulsar migration documentation cleanup

### 1. Treat stale execution drafts as dangerous
If pages like `phase1.md`, `phase2.md`, or `solution.md` still contain old deadlines, old target clusters, or superseded execution flows, do **not** leave them looking live. Convert them into historical/archive pages with an explicit warning and a pointer to the current authority pages.

### 2. Separate these concerns or readers will get misled
For infra/middleware migrations, keep four concerns separate:
- **Overview**: what the system is and where to start
- **Architecture**: steady-state components and relationships
- **Execution**: cutover / runbook / step-by-step migration logic
- **Limitations/Risks**: DNS, routing, client behavior, known hazards

### 3. When the user asks for "精细整理", audit related pages before writing
Do not only update the page the user named. First inspect adjacent pages that could contradict it, then reconcile all of them in one pass.

### 4. Prefer archival over deletion when preserving project history matters
If an outdated page still provides useful historical context, reduce it to an archive/stub page that says:
- this page is historical
- do not execute from it
- current authority pages are X/Y/Z

## Pulsar-specific documentation lessons worth reusing
- Proxy/routing explanations are often oversimplified. Be precise: client may only know proxy, but bundle ownership metadata determines the owner broker.
- Migration docs often mix steady-state architecture with cutover mechanics. Split them.
- GSLB/DNS/client-restart behavior belongs in a dedicated limitations/risk page, not scattered across architecture and runbook pages.
