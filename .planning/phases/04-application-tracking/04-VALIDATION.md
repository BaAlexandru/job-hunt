---
phase: 4
slug: application-tracking
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 4 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + MockK |
| **Config file** | build.gradle.kts (already configured) |
| **Quick run command** | `./gradlew :backend:test --tests "*.application.*"` |
| **Full suite command** | `./gradlew :backend:test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :backend:test`
- **After every plan wave:** Run `./gradlew :backend:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 04-01-01 | 01 | 1 | APPL-01 | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 | ⬜ pending |
| 04-01-02 | 01 | 1 | APPL-02 | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 | ⬜ pending |
| 04-01-03 | 01 | 1 | APPL-05 | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 | ⬜ pending |
| 04-02-01 | 02 | 1 | APPL-06 | integration | `./gradlew :backend:test --tests "*.application.ApplicationNoteControllerIntegrationTests"` | No -- Wave 0 | ⬜ pending |
| 04-02-02 | 02 | 2 | APPL-07 | integration | `./gradlew :backend:test --tests "*.application.ApplicationControllerIntegrationTests"` | No -- Wave 0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/kotlin/.../application/ApplicationControllerIntegrationTests.kt` -- stubs for APPL-01, APPL-02, APPL-05, APPL-07
- [ ] `backend/src/test/kotlin/.../application/ApplicationNoteControllerIntegrationTests.kt` -- stubs for APPL-06
- [ ] TestHelper needs `createJob` and `createApplication` helper methods added

*Existing test infrastructure (JUnit 5, Spring Boot Test, MockK, Testcontainers) covers framework needs.*

---

## Manual-Only Verifications

*All phase behaviors have automated verification.*

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
