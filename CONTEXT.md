# C-Star

C-Star is a company-wide employee appreciation platform. Every employee can formally recognize another employee's work or behavior; the recognized employee earns a Red Flower. Rankings and records are visible across the whole company.

## Language

**Employee**:
A person who works for the company and can log into C-Star via SSO. Every Employee can be a Giver; an Employee can only be a Receiver after their first login has created a local record (see ADR-0004).
_Avoid_: user, member, staff

**Admin**:
A privileged Employee who can configure deployment-wide settings (period length, default quota, mail templates) and manage the Category set. Introduced in a later phase; Phase 1 ships configuration-file-driven settings only.
_Avoid_: superuser, operator

**Appreciation**:
A single act of one employee formally recognizing another employee's work or behavior. Self-appreciation is forbidden.
_Avoid_: praise, thanks, compliment, 讚美, 感謝

**Red Flower**:
The recognition token awarded to the Receiver of an Appreciation. A single Appreciation carries between 1 and N Red Flowers, where N is bounded only by the Giver's remaining Period Quota. Accumulates over time and feeds the ranking.
_Avoid_: point, coin, badge, star, 積分

**Giver**:
The employee who initiates an Appreciation. Subject to a Period Quota.
_Avoid_: sender, 赞赏者(口语), 讚赏者

**Receiver**:
The employee who is the target of an Appreciation and who earns a Red Flower. Cannot be the same person as the Giver, and must have logged into C-Star at least once (so that a local employee record exists).
_Avoid_: recipient, 被赞赏者(口语)

**Appreciation Message**:
The mandatory free-text reason attached to every Appreciation, explaining what the Receiver did that deserved recognition. Visible company-wide alongside the Appreciation record.
_Avoid_: comment, note, 留言

**Period Quota**:
The finite number of Red Flowers a Giver may award within one accounting period. Each Appreciation consumes 1–N Red Flowers from the Giver's remaining quota. Resets at period boundary. Period length is configurable per deployment; default configuration is daily, resetting at 00:00 China time (UTC+8).
_Avoid_: budget, limit, 额度(口语)

**Category**:
A mandatory classification attached to every Appreciation. The preset seed set is: Teamwork, Excellence, Innovation, Customer Focus, Going Above & Beyond. Admins may extend or retire categories at runtime; retired categories become a placeholder reference on historical records (the Appreciation record itself is never rewritten).
_Avoid_: type, tag, 类别(口语)

**Received List**:
A per-employee view of all Appreciations in which they are the Receiver. There is no push notification; Receivers discover appreciations through this list.
_Avoid_: inbox, notifications

**Sent List**:
A per-employee view of all Appreciations in which they are the Giver, together with their remaining Period Quota.
_Avoid_: outbox, sent items

**Ranking**:
The company-wide ordering of employees by accumulated Red Flowers, presented in two views: a period-window view (who was most appreciated this period) and an all-time cumulative view. Departed employees' historical records are retained, remain publicly visible, and continue to count in the all-time ranking.
_Avoid_: leaderboard, 排行榜(口语)

**Company-Wide Visibility**:
Every Appreciation record — Giver, Receiver, timestamp, Message, and Category — is visible to all employees. No private or team-scoped appreciations.
_Avoid_: feed, timeline
