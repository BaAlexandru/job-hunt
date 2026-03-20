---
phase: 7
slug: frontend-shell-auth-ui
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 7 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | Vitest + React Testing Library |
| **Config file** | None — Wave 0 installs |
| **Quick run command** | `cd frontend && pnpm test` |
| **Full suite command** | `cd frontend && pnpm test:ci` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd frontend && pnpm test`
- **After every plan wave:** Run `cd frontend && pnpm test && pnpm build`
- **Before `/gsd:verify-work`:** Full suite must be green + `pnpm build` succeeds
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 07-01-01 | 01 | 1 | INFR-05 | smoke | `pnpm build` | No — W0 | ⬜ pending |
| 07-01-02 | 01 | 1 | SC-4 | unit | `pnpm test -- api-client` | No — W0 | ⬜ pending |
| 07-02-01 | 02 | 2 | SC-1 | smoke | `pnpm build` (AuthView renders) | No — W0 | ⬜ pending |
| 07-03-01 | 03 | 3 | SC-2/SC-3 | manual | Human verification (Plan 03 Task 2) | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

**Note:** Auth UI is provided by Better Auth UI pre-built components (AuthView, UserButton). Unit testing third-party components has low value. Testing focuses on custom code (api-client wrapper) and manual verification of auth flow + responsive layout.

---

## Wave 0 Requirements

- [ ] Test framework setup (Vitest + React Testing Library + jsdom)
- [ ] `__tests__/lib/api-client.test.ts` — covers SC-4 (fetch wrapper, error handling, 204 response)

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Responsive layout at mobile/tablet/desktop | SC-3 / INFR-05 | Visual layout check requires browser rendering | Open app in Chrome DevTools, test at 375px, 768px, 1280px widths |
| Sidebar collapse to hamburger on mobile | SC-3 | CSS transition/overlay behavior | Resize below 768px, verify hamburger menu appears and slides over |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
