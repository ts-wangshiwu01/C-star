# SSO login with lazy employee sync to local DB on first login

Employees authenticate via the company SSO. C-Star does not preload the entire employee directory; instead, the first time an employee logs in, C-Star writes a local employee record to its own DB. Only employees with such a local record can be a Receiver (see glossary).

We chose lazy sync over a full HR-system integration because it avoids a sustained sync pipeline and org-chart ownership questions, and cross-hierarchy appreciation is unrestricted (so C-Star does not need a team/reporting model). The accepted cost is that an employee cannot be appreciated until they have logged in at least once.
