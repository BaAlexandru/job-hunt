---
phase: 8
slug: frontend-core-views
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-21
---

# Phase 8 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | vitest + React Testing Library + Playwright |
| **Config file** | `frontend/vitest.config.ts` (Wave 0 creates if missing) |
| **Quick run command** | `cd frontend && npm test` |
| **Full suite command** | `cd frontend && npm test -- --run` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd frontend && npm test`
- **After every plan wave:** Run `cd frontend && npm test -- --run`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 08-01-01 | 01 | 1 | APPL-03 | unit | `npm test` | ❌ W0 | ⬜ pending |
| 08-01-02 | 01 | 1 | APPL-04 | unit | `npm test` | ❌ W0 | ⬜ pending |
| 08-02-01 | 02 | 1 | APPL-03 | integration | `npm test` | ❌ W0 | ⬜ pending |
| 08-03-01 | 03 | 2 | APPL-03, APPL-04 | e2e | `npx playwright test` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `frontend/vitest.config.ts` — vitest configuration if missing
- [ ] `frontend/src/test/setup.ts` — test setup with React Testing Library
- [ ] `frontend/src/test/mocks/handlers.ts` — MSW handlers for API mocking

*If none: "Existing infrastructure covers all phase requirements."*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Kanban drag-and-drop visual feedback | APPL-03 | Visual animation quality | Drag card between columns, verify smooth animation |
| Responsive layout at breakpoints | APPL-03, APPL-04 | Visual layout verification | Resize browser to mobile/tablet/desktop breakpoints |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
