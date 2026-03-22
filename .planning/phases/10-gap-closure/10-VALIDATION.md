---
phase: 10
slug: gap-closure
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-22
---

# Phase 10 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework (Backend)** | JUnit 5 + MockK + SpringBootTest |
| **Framework (Frontend)** | Vitest 4.1.0 + Testing Library + jsdom |
| **Backend config** | JUnit5 via `useJUnitPlatform()` in build.gradle.kts |
| **Frontend config** | `frontend/vitest.config.ts` |
| **Backend quick run** | `./gradlew :backend:test` |
| **Frontend quick run** | `cd frontend && pnpm test` |
| **Full suite command** | `./gradlew :backend:test && cd frontend && pnpm test` |
| **Estimated runtime** | ~45 seconds |

---

## Sampling Rate

- **After every task commit:** `./gradlew :backend:test` (backend changes) or `cd frontend && pnpm test` (frontend changes)
- **After every plan wave:** `./gradlew :backend:test && cd frontend && pnpm test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 45 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 10-01-01 | 01 | 1 | GAP-02 | unit | `cd frontend && pnpm vitest run __tests__/hooks/use-documents.test.ts` | ✅ | ⬜ pending |
| 10-01-02 | 01 | 1 | GAP-02 | manual (UI) | N/A — visual verification | N/A | ⬜ pending |
| 10-02-01 | 02 | 1 | GAP-01 | unit | `cd frontend && pnpm vitest run __tests__/hooks/use-interviews.test.ts` | ❌ W0 | ⬜ pending |
| 10-02-02 | 02 | 1 | GAP-01 | manual (UI) | N/A — visual verification | N/A | ⬜ pending |
| 10-03-01 | 03 | 2 | GAP-03 | unit | `./gradlew :backend:test --tests "*EmailService*"` | ❌ W0 | ⬜ pending |
| 10-03-02 | 03 | 2 | GAP-03 | integration | `./gradlew :backend:test --tests "*PasswordResetService*"` | ❌ W0 | ⬜ pending |
| 10-03-03 | 03 | 2 | GAP-03 | manual (UI) | N/A — visual verification | N/A | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `frontend/__tests__/hooks/use-interviews.test.ts` — stubs for GAP-01 hook tests (useUpdateInterviewNote, useDeleteInterviewNote)
- [ ] `backend/src/test/kotlin/.../service/EmailServiceTests.kt` — covers GAP-03 email sending + SMTP fallback
- [ ] `backend/src/test/kotlin/.../service/PasswordResetServiceTests.kt` — update existing or create for GAP-03 integration

*Existing test infrastructure covers document hooks (GAP-02).*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Interview notes expand/collapse in InterviewsTab | GAP-01 | Visual interaction pattern | Click interview row → verify expandable section opens with notes list, note type selector, inline edit |
| Document version history in expanded row | GAP-02 | Visual interaction pattern | Click document row → verify version list, "Current" badge, "Set as Current" button, upload dropzone |
| Password reset email received | GAP-03 | End-to-end email delivery | Request reset on forgot-password page → check Gmail inbox → click link → verify reset completes |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 45s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
