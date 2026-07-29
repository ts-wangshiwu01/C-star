# C-Star — Product Requirements Document (PRD)

> Company-wide employee appreciation platform. Employees formally recognize each other's work; the recognized employee earns Red Flowers; rankings and records are visible across the whole company.
>
> This document is written **from the user's perspective**: what users can do, what they see, and what they experience. It is the single source of truth for **what to build**. Domain terminology is defined in [`CONTEXT.md`](../CONTEXT.md); architectural decisions and their rationale are in [`docs/adr/`](adr/).

---

## 1. What is C-Star (from the user's seat)

C-Star is a place where any employee can, in a few seconds, tell a colleague *"I saw what you did, and it mattered."* That act — an **Appreciation** — gives the colleague **Red Flowers**, which accumulate and feed two company-wide rankings: *who was most appreciated this period* and *who has been most appreciated of all time.* Every Appreciation is public to the whole company, so recognition carries real social weight rather than disappearing into a private message.

The product is intentionally small: no teams, no hierarchy, no notifications, no private modes. The simplicity is the value — recognition becomes a daily habit, not a quarterly ceremony.

### 1.1 Who is it for

| Role | Their relationship to C-Star |
|---|---|
| **Employee** | The heart of the product. Logs in via SSO, gives Appreciations, receives Red Flowers, browses records, checks rankings. Every Employee can be a Giver from day one; an Employee can be a Receiver only after their first login has created a local record. |
| **Admin** | A privileged Employee who, in a later phase, can configure the system and curate the Category set. Phase 1 has no Admin UI — settings live in config files. |

### 1.2 What C-Star is **not** (explicitly out of scope, from the user's perspective)

- It is **not** a private feedback tool — every Appreciation is visible to all employees.
- It is **not** a notification system — you find out you were appreciated by opening your Received List.
- It is **not** team-scoped — there is no "my team's ranking," only the whole company.
- It is **not** multi-language — the UI and content are English only.
- It does **not** police fraud beyond the daily quota — no anomaly detection, no auto-blocking.
- It does **not** manage departures — if a colleague leaves, their past records and ranking entry simply stay.

---

## 2. User Stories

Organized by the journeys a user actually takes. Each story follows **As a [role], I want to [action], so that [value]**. The rule the system must enforce to make each story work is tagged inline as **[BR-x]** (consolidated in §4).

### 2.1 Onboarding — "I'm a new employee, this is my first visit"

- **US-O1**: As a new employee, I want to log in with my usual company credentials, so that I don't have to remember yet another password.
  - *System enforces*: SSO is the only way in; no separate registration. On first successful login, a local employee record is created so I become eligible as a Receiver. **[BR-3]**
- **US-O2**: As a new employee, I want to land on something useful immediately after login, so that I understand what C-Star is without a tutorial.
  - *Expected experience*: I see recent company-wide Appreciations and/or the current ranking — proof that the product is alive.
- **US-O3**: As a new employee, I want my name to appear correctly on records and rankings, so that I am recognizable to colleagues.
  - *Expected experience*: My display name comes from SSO; if SSO later changes it, the local record updates on next login.

### 2.2 Giving an Appreciation — "I want to recognize a colleague"

- **US-G1**: As an employee, I want to pick exactly one colleague to appreciate, so that my recognition is specific and personal.
  - *System enforces*: One Receiver per Appreciation. I cannot pick myself **[BR-2]**. I can only pick colleagues who have logged in at least once **[BR-3]**.
- **US-G2**: As an employee, I want to choose how many Red Flowers to give, so that I can express how much the recognition means.
  - *System enforces*: I may give between 1 and my remaining quota, inclusive **[BR-4]**. The UI shows my remaining quota and stops me from entering more than that.
- **US-G3**: As an employee, I want to pick a Category that describes what I'm recognizing, so that the Appreciation is more than a generic "thanks."
  - *System enforces*: Category is mandatory and must come from the current active set **[BR-6]**.
- **US-G4**: As an employee, I want to write a short reason for the Appreciation, so that the Receiver (and the company) knows *what* they did well.
  - *System enforces*: Message is mandatory and non-empty **[BR-7]**.
- **US-G5**: As an employee, I want my Appreciation to take effect the moment I submit it, so that I don't have to wait for the Receiver to "accept."
  - *System enforces*: No acceptance step — the Appreciation is immediate, public, and immutable **[BR-8], [BR-9], [BR-10]**.
- **US-G6**: As an employee, I want a clear confirmation after submitting, so that I trust the Appreciation went through.
  - *Expected experience*: Confirmation screen + the record appears immediately in the company feed and in both parties' lists.
- **US-G7**: As an employee, I want to be reminded that an Appreciation is final before I submit, so that I'm not anxious about making a mistake I can't undo.
  - *System enforces*: Once submitted, an Appreciation cannot be edited or revoked — by me, by the Receiver, or by an Admin **[BR-8]**. The UI should make this finality clear before submission.

### 2.3 Receiving an Appreciation — "Someone appreciated me"

- **US-R1**: As an employee, I want a list of every Appreciation I've ever received, so that I can see who recognized me and why.
  - *Expected experience*: A "Received" view, newest first, showing Giver, Category, Message, flower count, and timestamp.
- **US-R2**: As an employee, I want to discover appreciations on my own terms, so that I'm not interrupted by pings.
  - *System enforces*: No push notifications, no emails. Discovery is solely by visiting the Received List **[glossary: Received List]**.
- **US-R3**: As an employee, I want my received Red Flowers to count toward my ranking standing, so that recognition accumulates over time.
  - *System enforces*: Every Red Flower I receive adds to both my period-window total and my all-time total.

### 2.4 Tracking what I've given — "What have I sent, and how much can I still give?"

- **US-S1**: As an employee, I want a list of every Appreciation I've sent, so that I can recall who I've recognized and why.
  - *Expected experience*: A "Sent" view, newest first, showing Receiver, Category, Message, flower count, and timestamp.
- **US-S2**: As an employee, I want to see how much quota I have left in the current period, so that I know whether I can still give today.
  - *Expected experience*: Remaining quota is shown alongside my Sent List. When it hits zero, I'm told when it resets.
- **US-S3**: As an employee, I want my quota to refresh on a predictable schedule, so that I can plan my recognition rhythm.
  - *System enforces*: Quota resets to the configured default at the period boundary (default: every day at 00:00 China time). Unused quota does not carry over **[BR-13]**.

### 2.5 Browsing the company's recognition — "What's happening across the company?"

- **US-B1**: As an employee, I want to browse all Appreciations across the company, so that I can see what kinds of work are being recognized.
  - *Expected experience*: A company-wide record view, newest first, showing Giver, Receiver, Category, Message, flower count, and timestamp for every Appreciation **[BR-9]**.
- **US-B2**: As an employee, I want every Appreciation I see to be honest and unedited, so that I can trust what I read.
  - *System enforces*: Records are immutable — no edits, no deletions, by anyone **[BR-8]**.

### 2.6 Checking the ranking — "Who's being appreciated?"

- **US-K1**: As an employee, I want to see who was most appreciated *this period*, so that I can celebrate recent wins.
  - *Expected experience*: A period-window ranking, ordered by Red Flowers received this period.
- **US-K2**: As an employee, I want to see who has been most appreciated *of all time*, so that I can recognize sustained contributors.
  - *Expected experience*: An all-time cumulative ranking, ordered by total Red Flowers ever received.
- **US-K3**: As an employee, I want departed colleagues to still appear in the all-time ranking, so that historical recognition isn't erased when someone leaves.
  - *System enforces*: Departed employees' records persist and remain in the all-time ranking **[BR-15]**.

### 2.7 Administering C-Star — "I curate the recognition vocabulary" (Phase 2 only)

- **US-AD1**: As an Admin, I want to add new Categories, so that the recognition vocabulary can evolve with the company's culture.
  - *Expected experience*: A Category management screen where I can add new active Categories.
- **US-AD2**: As an Admin, I want to retire Categories that no longer fit, so that Givers can't pick stale ones — without destroying history.
  - *System enforces*: Retired Categories disappear from the Giver's dropdown. Historical Appreciations referencing a retired Category display a placeholder (e.g., "Retired Category"). The Appreciation records themselves are never rewritten **[BR-14]**.
- **US-AD3**: As an Admin, I want to change deployment-wide settings like the period length or the default quota, so that I can tune C-Star to our company rhythm.
  - *Expected experience*: A settings screen (Phase 2). In Phase 1, the same levers exist but are set via config files at deploy time.
- **US-AD4**: As an Admin, I want to see usage stats across the whole company, so that I can understand adoption and health.
  - *Expected experience*: Audit-style aggregates — quota consumption, ranking details, usage over time — not exposed to regular employees.
- **US-AD5**: As an Admin, I want my powers to stop at the boundary of historical truth, so that I can't be asked to "fix" a record.
  - *System enforces*: Admins cannot edit, revoke, or rewrite any Appreciation; cannot manage employees or their lifecycle **[BR-8], Non-Goals]**.

---

## 3. The Five Preset Categories (what users see on day one)

When a Giver opens the Category dropdown for the first time, they see:

1. **Teamwork**
2. **Excellence**
3. **Innovation**
4. **Customer Focus**
5. **Going Above & Beyond**

These signal what the company chooses to celebrate. Admins can extend this set later; they cannot silently rewrite history when they retire one.

---

## 4. Business Rules (the invariants users can rely on)

The non-negotiable rules the system must enforce. Every story above references one or more of these.

| # | Rule | Why it matters to users |
|---|---|---|
| BR-1 | An Appreciation has exactly one Giver and exactly one Receiver. | Recognition is personal — one-to-one, not broadcast. |
| BR-2 | Giver ≠ Receiver. | You can't appreciate yourself. |
| BR-3 | Receiver must have a local employee record (must have logged in once). | You can only appreciate someone who is actually "in" C-Star. |
| BR-4 | An Appreciation carries 1 to N Red Flowers, where N ≤ Giver's remaining quota. | Strength of recognition is expressible, but bounded by what you have. |
| BR-5 | Quota is consumed by flower count, not by act count. | You can't bypass the quota by dumping many flowers in one act. |
| BR-6 | Category is mandatory and must be from the active set. | Every Appreciation says *what kind* of work was recognized. |
| BR-7 | Message is mandatory and non-empty. | "Thanks" alone is not enough — the Receiver deserves a reason. |
| BR-8 | Appreciations are immutable — no edit, no revoke, by anyone. | What you read in the feed is true and final. |
| BR-9 | All Appreciation records are visible to all employees. | Recognition carries social weight because it's public. |
| BR-10 | No acceptance step — Appreciation is effective immediately. | Recognition shouldn't wait for the Receiver to click "accept." |
| BR-11 | No cross-hierarchy restriction — any Employee may appreciate any other eligible Employee. | Peers, managers, reports — all fair game. |
| BR-12 | No anonymous Appreciations — real name only. | You stand behind what you recognize. |
| BR-13 | Period resets to configured default at the configured boundary; unused quota does not carry over. | Quota stays a fresh daily resource, not a hoardable currency. |
| BR-14 | Retired Categories become placeholders on historical records; records are never rewritten. | History stays honest even as the vocabulary evolves. |
| BR-15 | Departed employees' records persist and remain in the all-time ranking. | Past recognition isn't erased when someone leaves. |

---

## 5. What users expect to see (information the system must hold)

This describes what the user-facing experience requires the system to *know and remember* — not how it stores anything. Table design, fields, caching, and computation strategy are the dev team's design freedom.

### 5.1 For each Employee, the user expects to see
- That they exist in C-Star (created on first SSO login).
- A display name that identifies them on records and rankings.
- That they became Receiver-eligible from the moment they first logged in.
- The user does **not** see (and the system does not need) their team, manager, employment status, or any org-chart attribute.

### 5.2 For each Category, the user expects to see
- Its display label.
- Whether it is currently selectable by Givers (active) or retired.
- A stable placeholder on historical Appreciations after the Category is retired (so an old record still makes sense to a reader).

### 5.3 For each Appreciation, the user expects to see (and the system must remember, permanently and immutably)
- Who the Giver is.
- Who the Receiver is.
- When it happened, precisely enough to assign it to the correct accounting period and to order records newest-first.
- Which Category it was filed under, in a way that survives the Category later being retired.
- The Message text.
- The Red Flower count awarded.
- The user must **never** see an edit or deletion of this record after creation.

### 5.4 For each Giver, in the current period, the user expects to see
- How much quota they have already used.
- How much quota they have remaining.
- After a reset, that the prior period's usage no longer limits them. Whether prior-period usage is retained for audit is a dev decision, not a user-visible concern.

### 5.5 In the rankings, the user expects to be able to see
- For the current period: each Employee's total Red Flowers received in that window.
- For all time: each Employee's total Red Flowers ever received, including departed employees.
- How these totals are computed (on demand, cached, precomputed) is invisible to the user.

---

## 6. Edge cases — what happens when things go sideways

| Situation | What the user experiences |
|---|---|
| I try to appreciate myself | The system won't let me select myself as Receiver. **[BR-2]** |
| I try to appreciate a colleague who never logged in | They don't appear in the Receiver picker. **[BR-3]** |
| I try to give more flowers than I have quota for | The UI clamps my input to the remaining quota; the server rejects anything above it. **[BR-4]** |
| I try to submit with an empty message | Submission is blocked; I'm told a message is required. **[BR-7]** |
| I submit at 23:59:59, just before the daily reset | My Appreciation is valid — it counts against today's quota and is timestamped accordingly. |
| A Category I once used is retired later | My old Appreciation still shows up, with the Category rendered as "Retired Category." **[BR-14]** |
| A colleague leaves the company | Their past records and their all-time ranking entry stay. Whether they can still be appreciated is out of scope. **[BR-15]** |
| I appreciate the same colleague twice in one period | Allowed, as long as my quota covers each. There's no per-pair limit. |
| I run out of quota mid-period | I'm told I have no quota left and must wait for the next reset. |
| My display name changes in SSO | On my next login, C-Star updates my local name. Whether historical records show my current or past name is an open question (§8). |

---

## 7. Non-functional expectations (from the user's perspective)

- **Speed**: Giving an Appreciation should feel instant — I click submit, I see confirmation immediately.
- **Trust**: I should never catch the system in a state where my quota was spent but no record exists, or vice versa. **[BR-4, BR-5]**
- **Read availability**: Even if giving is briefly slow, I can always browse records, my lists, and rankings.
- **Timezone correctness**: The daily reset happens at 00:00 China time, no matter where the server physically runs. I shouldn't be surprised by a reset at the wrong local hour.
- **Security**: Only I can give Appreciations under my identity. Admin-only actions are gated to Admins. Authentication is SSO-only — no passwords for me to lose.
- **Language**: The UI and all content are in English.

---

## 8. Phasing — what ships when

| Phase | What users get |
|---|---|
| **Phase 1 (MVP)** | SSO login with lazy employee sync; give and receive Appreciations; Received and Sent Lists; company-wide record browse; both rankings (period + all-time); daily quota reset at 00:00 China time; the 5 preset Categories; all settings via config files. |
| **Phase 2** | Admin role: add and retire Categories at runtime; change period length and default quota at runtime; audit-style usage stats. |
| **Out of scope** | Push notifications, multi-language, team ranking, anti-fraud beyond quota, active departure handling. |

---

## 9. Open questions (need decisions before/during implementation)

1. **When an Admin renames a Category**, should historical records show the new name, or should rename be implemented as retire-old + create-new? (Current PRD assumes the latter; rename is not a first-class operation.)
2. **Admin "view all data"** — since records are already public to all employees, what extra does the Admin see? (PRD assumes audit-style aggregates like quota consumption and usage over time, not already-public records.)
3. **Display name on historical records when SSO name changes** — does an old Appreciation show the Receiver's current name or their name-at-the-time? (Impl. choice; PRD does not mandate.)
4. **Default quota value** — to be set per deployment; not mandated here.
5. **Ranking pagination and tie-breaking** — impl. detail; ties may be broken by earliest-first-received or left unordered.

---

## 10. References

- [`CONTEXT.md`](../CONTEXT.md) — domain glossary (definitions of all capitalized terms used above).
- [`docs/adr/`](adr/) — architectural decision records explaining *why* key choices were made:
  - [ADR-0001](adr/0001-quota-measured-in-red-flowers.md) — Why quota counts flowers, not acts.
  - [ADR-0002](adr/0002-variable-red-flower-count-per-appreciation.md) — Why a single Appreciation can carry 1–N flowers.
  - [ADR-0003](adr/0003-records-public-no-opt-out.md) — Why records are public with no opt-out.
  - [ADR-0004](adr/0004-sso-lazy-employee-sync.md) — Why employee records are created on first login, not preloaded.
  - [ADR-0005](adr/0005-daily-reset-china-time.md) — Why the default period is daily at 00:00 China time.
  - [ADR-0006](adr/0006-no-active-departure-handling.md) — Why departed employees' records simply persist.

---

## 11. Acceptance Criteria

Acceptance criteria are written in **Given / When / Then** form. Each set is keyed to its parent User Story (§2) and the Business Rules it verifies (§4). An implementation satisfies the PRD only when every AC below passes.

### 11.1 Onboarding (US-O1 – US-O3)

**AC-O1 — SSO is the only entry**
- *Given* an unauthenticated visitor at the C-Star URL
- *When* they attempt to access any authenticated page
- *Then* they are redirected to the company SSO login
- *And* there is no registration form, no password reset flow, and no local-account creation path anywhere in the product.

**AC-O2 — First login creates a local employee record and unlocks Receiver eligibility**
- *Given* an employee has never logged into C-Star (no local record exists)
- *When* they authenticate successfully via SSO
- *Then* a local employee record is created with the display name from SSO claims
- *And* from that moment they are selectable as a Receiver by other employees **[BR-3]**

**AC-O3 — Returning login does not duplicate the employee record**
- *Given* an employee who already has a local record
- *When* they log in again via SSO
- *Then* no new employee record is created
- *And* their existing record is reused (display name updated if SSO claims changed)

**AC-O4 — First-time landing experience**
- *Given* a newly-onboarded employee
- *When* they land on the home view
- *Then* they see recent company-wide Appreciations and/or the current ranking (not an empty page, not a setup wizard)

### 11.2 Giving an Appreciation (US-G1 – US-G7)

**AC-G1 — Receiver selection excludes self and non-logged-in employees**
- *Given* an employee is composing an Appreciation
- *When* they open the Receiver picker
- *Then* they do not appear in the list themselves **[BR-2]**
- *And* employees who have never logged into C-Star do not appear in the list **[BR-3]**

**AC-G2 — Flower count is bounded by remaining quota**
- *Given* a Giver with 3 Red Flowers remaining in the current period
- *When* they attempt to submit an Appreciation with flower count = 5
- *Then* submission is rejected (server-side) **[BR-4]**
- *And* the UI prevents or clamps the input to 3 before submission
- *And* flower count = 0 is also rejected **[BR-4]**

**AC-G3 — Category is mandatory and from the active set**
- *Given* an employee is composing an Appreciation
- *When* they attempt to submit without selecting a Category
- *Then* submission is blocked and they are told a Category is required **[BR-6]**
- *And* the dropdown offered contains only Categories currently in the active set (retired Categories are not selectable)

**AC-G4 — Message is mandatory and non-empty**
- *Given* an employee is composing an Appreciation
- *When* they attempt to submit with an empty or whitespace-only Message
- *Then* submission is blocked and they are told a Message is required **[BR-7]**

**AC-G5 — Appreciation is effective immediately with no acceptance step**
- *Given* a Giver has submitted a valid Appreciation
- *When* the submission completes
- *Then* the Appreciation record exists and is visible company-wide **[BR-9], [BR-10]**
- *And* no "accept" action is required or available on the Receiver side **[BR-10]**

**AC-G6 — Quota and record stay consistent**
- *Given* a Giver with remaining quota Q submits an Appreciation with flower count N (N ≤ Q)
- *When* the submission completes
- *Then* the Giver's remaining quota becomes Q − N **[BR-5]**
- *And* an Appreciation record with flower count = N exists
- *And* there is no observable state where quota was decremented without a record, or a record was created without quota being decremented **[BR-4, BR-5]**

**AC-G7 — Immutability: no edit, no revoke, by anyone**
- *Given* an Appreciation has been created
- *When* the Giver, the Receiver, or an Admin attempts to edit the Message, change the flower count, change the Category, or delete the record
- *Then* the attempt fails (no API path, no UI affordance succeeds) **[BR-8]**

**AC-G8 — Confirmation is shown to the Giver**
- *Given* a Giver has just submitted a valid Appreciation
- *When* the submission completes
- *Then* a confirmation is displayed
- *And* the new record is immediately visible in the company-wide feed and in both the Giver's Sent List and the Receiver's Received List

**AC-G9 — Finality is surfaced before submission**
- *Given* an employee is composing an Appreciation
- *When* they are about to submit
- *Then* the UI makes clear that the Appreciation cannot be edited or revoked after submission **[BR-8]**

### 11.3 Receiving an Appreciation (US-R1 – US-R3)

**AC-R1 — Received List shows all received Appreciations, newest first**
- *Given* an employee who has been the Receiver of multiple Appreciations
- *When* they open their Received List
- *Then* every Appreciation in which they are the Receiver is listed, newest first
- *And* each entry shows Giver, Category, Message, flower count, and timestamp

**AC-R2 — No notifications are sent on receipt**
- *Given* an Appreciation has just been awarded to a Receiver
- *When* the Appreciation is created
- *Then* no push notification, no email, and no in-app toast/ping is sent to the Receiver
- *And* the only way the Receiver can discover the Appreciation is by opening their Received List **[glossary: Received List]**

**AC-R3 — Received flowers feed both rankings**
- *Given* a Receiver has just received an Appreciation with N Red Flowers
- *When* the period-window and all-time rankings are next viewed
- *Then* the Receiver's totals in both rankings include those N flowers

### 11.4 Tracking what I've given (US-S1 – US-S3)

**AC-S1 — Sent List shows all sent Appreciations, newest first**
- *Given* an employee who has given multiple Appreciations
- *When* they open their Sent List
- *Then* every Appreciation in which they are the Giver is listed, newest first
- *And* each entry shows Receiver, Category, Message, flower count, and timestamp

**AC-S2 — Remaining quota is visible alongside the Sent List**
- *Given* an employee viewing their Sent List
- *When* the page loads
- *Then* their remaining Period Quota is displayed
- *And* when remaining quota = 0, they are told when the next reset occurs

**AC-S3 — Quota resets at the period boundary, no carryover**
- *Given* a Giver with remaining quota R (where R < default quota) at 23:59:59 China time
- *When* the clock crosses 00:00 China time
- *Then* the Giver's remaining quota resets to the configured default
- *And* the unused R is not added to the new quota **[BR-13]**

### 11.5 Browsing company-wide recognition (US-B1 – US-B2)

**AC-B1 — Company-wide record is browseable by any employee**
- *Given* any logged-in employee
- *When* they open the company-wide record view
- *Then* every Appreciation ever created is listed, newest first **[BR-9]**
- *And* each entry shows Giver, Receiver, Category, Message, flower count, and timestamp

**AC-B2 — Records cannot be altered after creation**
- *Given* any Appreciation in the company-wide record
- *When* any actor (Giver, Receiver, Admin, system process) attempts to edit or delete it
- *Then* the record remains unchanged **[BR-8]**
- *And* the record's content is identical to what was originally submitted

### 11.6 Ranking (US-K1 – US-K3)

**AC-K1 — Period-window ranking orders employees by flowers received this period**
- *Given* Appreciations exist in the current accounting period
- *When* an employee opens the period-window ranking
- *Then* employees are listed in descending order of Red Flowers received within the current period
- *And* employees with zero flowers in the period are either not shown or shown at the bottom (impl. choice, documented)

**AC-K2 — All-time ranking orders employees by cumulative flowers ever received**
- *Given* Appreciations exist across all periods
- *When* an employee opens the all-time ranking
- *Then* employees are listed in descending order of total Red Flowers ever received

**AC-K3 — Departed employees remain in the all-time ranking**
- *Given* an employee who has departed the company (no active departure handling has occurred)
- *When* the all-time ranking is viewed
- *Then* that employee still appears with their accumulated total **[BR-15]**
- *And* their past Appreciation records (as Giver or Receiver) are still visible company-wide

### 11.7 Admin (Phase 2 only) (US-AD1 – US-AD5)

**AC-AD1 — Admin can add a new active Category**
- *Given* an Admin in the Category management view
- *When* they create a new Category
- *Then* it appears in the active set
- *And* Givers see it in their Category dropdown on the next composition

**AC-AD2 — Retiring a Category removes it from the dropdown but preserves history**
- *Given* an Admin retires an active Category that is referenced by existing Appreciations
- *When* a Giver next composes an Appreciation
- *Then* the retired Category is no longer selectable **[BR-6]**
- *And* historical Appreciations referencing it display a placeholder (e.g., "Retired Category") instead of the original label **[BR-14]**
- *And* the underlying Appreciation records are unchanged (no rewrite of Message, Giver, Receiver, timestamp, or flower count)

**AC-AD3 — Admin can change deployment-wide settings at runtime (Phase 2)**
- *Given* an Admin in the settings view
- *When* they change the period length or default quota value
- *Then* the change takes effect for subsequent periods (current period is not retroactively altered)

**AC-AD4 — Admin sees audit-style aggregates not visible to regular employees**
- *Given* an Admin in the audit view
- *When* they view usage data
- *Then* they see aggregates such as quota consumption, ranking details, and usage over time
- *And* a regular employee viewing the same area does not see these aggregates

**AC-AD5 — Admin cannot edit, revoke, or rewrite any Appreciation**
- *Given* an Admin with full Admin privileges
- *When* they attempt to edit a Message, change a flower count, change a Category on an existing Appreciation, or delete an Appreciation
- *Then* every attempt fails (no API path, no UI affordance succeeds) **[BR-8]**
- *And* the Admin UI exposes no such affordance

### 11.8 Edge cases (cross-cutting)

**AC-E1 — Same Giver can appreciate the same Receiver multiple times per period**
- *Given* a Giver with sufficient remaining quota
- *When* they submit two separate Appreciations to the same Receiver in one period
- *Then* both Appreciations are created and visible
- *And* both flower counts are deducted from the Giver's quota
- *And* there is no per-pair frequency limit

**AC-E2 — Quota exhaustion blocks further Appreciations until reset**
- *Given* a Giver whose remaining quota = 0
- *When* they attempt to submit any Appreciation with flower count ≥ 1
- *Then* submission is rejected **[BR-4]**
- *And* they are told to wait for the next period reset

**AC-E3 — Submission at the period boundary is timestamped correctly**
- *Given* a Giver submits an Appreciation at 23:59:59 China time
- *When* the record is created
- *Then* its timestamp places it in the current (ending) period for the period-window ranking
- *And* it consumes quota from the current period, not the next

**AC-E4 — Retired Category renders as a stable placeholder on old records**
- *Given* an Appreciation references a Category that was later retired
- *When* any employee views that Appreciation
- *Then* the Category field displays a placeholder (e.g., "Retired Category")
- *And* this behavior is stable across repeated views (no flicker, no missing label)

**AC-E5 — Non-logged-in employee cannot be appreciated even if their SSO identity is known**
- *Given* an employee who exists in SSO but has never logged into C-Star
- *When* a Giver searches for them in the Receiver picker
- *Then* they do not appear **[BR-3]**
- *And* if a request is crafted to target them directly, the server rejects it **[BR-3]**
