---
phase: 10
slug: gap-closure
status: approved
nyquist_compliant: true
wave_0_complete: true
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
| 10-01-01 | 01 | 1 | GAP-02 | unit | `cd frontend && pnpm vitest run __tests__/hooks/use-documents.test.ts` | ✅ | ✅ green |
| 10-01-02 | 01 | 1 | GAP-02 | manual (UI) | N/A — visual verification | N/A | ✅ green |
| 10-02-01 | 02 | 1 | GAP-01 | unit | `cd frontend && pnpm vitest run __tests__/hooks/use-interviews.test.ts` | ✅ | ✅ green |
| 10-02-02 | 02 | 1 | GAP-01 | manual (UI) | N/A — visual verification | N/A | ✅ green |
| 10-03-01 | 03 | 2 | GAP-03 | unit | `./gradlew :backend:test --tests "*EmailService*"` | ✅ | ✅ green |
| 10-03-02 | 03 | 2 | GAP-03 | integration | `./gradlew :backend:test --tests "*PasswordResetService*"` | ✅ | ✅ green |
| 10-03-03 | 03 | 2 | GAP-03 | unit | `cd frontend && npx tsc --noEmit` | ✅ | ✅ green |
| 10-03-04 | 03 | 2 | GAP-03 | manual (UI) | N/A — visual verification | N/A | ✅ green |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [x] `frontend/__tests__/hooks/use-interviews.test.ts` — 5 tests: create with noteType, update content-only, update excludes noteType, delete, list (all green)
- [x] `backend/src/test/kotlin/.../service/EmailServiceTests.kt` — 7 tests: send email, SMTP fallback, createMimeMessage, HTML template content + structure (all green)
- [x] `backend/src/test/kotlin/.../service/PasswordResetServiceTests.kt` — 12 tests: send email on reset, URL format, no email for unknown user, safe message, rate limiting, token validation, password update (all green)

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

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references
- [x] No watch-mode flags
- [x] Feedback latency < 45s
- [x] `nyquist_compliant: true` set in frontmatter (Wave 0 tests written and green)

**Approval:** APPROVED (2026-03-22 — planning audit verified task coverage and sampling rate)
