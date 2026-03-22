---
phase: 8
slug: frontend-core-views
status: draft
nyquist_compliant: true
wave_0_complete: false
created: 2026-03-21
---

# Phase 8 -- Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | TypeScript compiler (`pnpm build`) |
| **Config file** | `frontend/tsconfig.json` (existing) |
| **Quick run command** | `cd frontend && pnpm build` |
| **Full suite command** | `cd frontend && pnpm build` |
| **Estimated runtime** | ~20 seconds |

**Rationale for build-only verification:** Phase 8 is a UI-heavy frontend phase creating page components, hooks, and forms. The primary risk is type errors, broken imports, and invalid JSX -- all caught by `pnpm build`. Component behavior is verified through the human checkpoint in Plan 04 Task 2 (end-to-end manual verification). Unit tests for UI components would require MSW mocking infrastructure and provide low signal-to-noise for this phase's scope. The build check runs after every task and catches structural issues immediately.

---

## Sampling Rate

- **After every task commit:** Run `cd frontend && pnpm build`
- **After every plan wave:** Run `cd frontend && pnpm build`
- **Before `/gsd:verify-work`:** Build must pass + human checkpoint (Plan 04 Task 2) approved
- **Max feedback latency:** 20 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | Status |
|---------|------|------|-------------|-----------|-------------------|--------|
| 08-01-01 | 01 | 1 | APPL-03 | build | `pnpm build` | pending |
| 08-01-02 | 01 | 1 | APPL-04 | build | `pnpm build` | pending |
| 08-02-01 | 02 | 2 | APPL-03 | build | `pnpm build` | pending |
| 08-02-02 | 02 | 2 | APPL-04 | build | `pnpm build` | pending |
| 08-03-01 | 03 | 2 | APPL-03, APPL-04 | build | `pnpm build` | pending |
| 08-03-02 | 03 | 2 | APPL-03, APPL-04 | build | `pnpm build` | pending |
| 08-04-01 | 04 | 3 | APPL-03, APPL-04 | build | `pnpm build` | pending |
| 08-04-02 | 04 | 3 | APPL-03, APPL-04 | human-verify | Manual e2e test | pending |

---

## Wave 0 Requirements

Existing infrastructure covers all phase requirements. No test framework setup needed -- TypeScript compilation via `pnpm build` is the automated verification gate, supplemented by the human checkpoint for functional verification.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Kanban drag-and-drop visual feedback | APPL-03 | Visual animation quality | Drag card between columns, verify smooth animation |
| Invalid column dimming during drag | APPL-03 | Visual feedback verification | Drag card, verify invalid target columns dim |
| Responsive layout at breakpoints | APPL-03, APPL-04 | Visual layout verification | Resize browser to mobile/tablet/desktop breakpoints |
| End-to-end CRUD flows | All | Functional integration | Full test sequence in Plan 04 Task 2 |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify commands (`pnpm build`)
- [x] Sampling continuity: every task has build verification
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter
- [x] Human checkpoint (Plan 04 Task 2) covers functional verification

**Approval:** accepted (build-only + human checkpoint strategy)
