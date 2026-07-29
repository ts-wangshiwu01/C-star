# No active departure handling — departed employees' records persist by default

C-Star does not maintain an "active" or "departed" flag on employees, does not sync departure status from HR/SSO, and does not scrub or anonymize records when an employee leaves the company. Once an employee record is created on first login (see ADR-0004), it persists indefinitely along with all Appreciations they participated in as Giver or Receiver.

We chose passive retention over active departure handling because C-Star has no employee-lifecycle ownership (Admins do not manage employees) and the all-time ranking is meaningless if historical participants disappear. The accepted consequences are: (a) departed employees remain visible in records and in the all-time ranking, and (b) the edge case of appreciating someone who has already left the company is not prevented and is treated as out-of-scope.
