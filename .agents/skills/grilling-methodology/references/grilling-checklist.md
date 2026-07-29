# Grilling Pre-Output Self-Check

Before presenting your grilling results, verify:

## Evidence Quality
- [ ] Every file mentioned in contradictions was actually read (not just filename-skimmed)
- [ ] External skills referenced as best-practice baselines were actually loaded via skill_view
- [ ] No contradiction is based on assumption — each has a cited source

## Contradiction Quality
- [ ] Each contradiction has: Claim (cited) + Reality (cited) + Impact (explained)
- [ ] "Missing feature" gaps are separated from true "claim vs reality" contradictions
- [ ] No duplicate contradictions (same gap stated twice in different words)

## Action Quality
- [ ] Every action has a concrete file path (not "add a skill" but "create `.opencode/skills/X/SKILL.md`")
- [ ] Every action is prioritized P0/P1/P2
- [ ] Priority assignment is consistent (P0 = blocks core function, not just "important")

## Questions
- [ ] Clarifying questions are batched at the end, not scattered
- [ ] Questions are decision-shaped (scope / format / ownership), not open-ended
- [ ] No question that could be answered by reading more files

## Common Failure Modes
- **Skim failure**: Read file headers but not body → missed nuances → shallow contradictions
- **External bias failure**: Imposed external patterns without checking if they fit the project's tech stack
- **Brainstorm failure**: Listed nice-to-haves without grounding them in contradictions
- **Drip failure**: Asked one question, waited, asked another → wasted user time
