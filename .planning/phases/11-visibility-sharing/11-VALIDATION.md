---
phase: 11
slug: visibility-sharing
status: draft
nyquist_compliant: true
wave_0_complete: true
created: 2026-03-22
---

# Phase 11 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test |
| **Config file** | `backend/build.gradle.kts` |
| **Quick run command** | `./gradlew :backend:test --tests "*.visibility.*"` |
| **Full suite command** | `./gradlew :backend:test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :backend:test --tests "*.visibility.*"`
- **After every plan wave:** Run `./gradlew :backend:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 11-01-00 | 01 | 1 | VISI-01 | stub | `./gradlew :backend:test --tests "*.visibility.*"` | Plan 01 Task 0 creates | W0 |
| 11-01-01 | 01 | 1 | VISI-01 | unit | `./gradlew :backend:test --tests "*.VisibilityEnumTest"` | Plan 01 Task 0 | W0 |
| 11-01-02 | 01 | 1 | VISI-01 | integration | `./gradlew :backend:test --tests "*.VisibilityMigrationTest"` | Plan 01 Task 0 | W0 |
| 11-02-01 | 02 | 2 | VISI-01 | integration | `./gradlew :backend:test --tests "*.CompanyVisibilityServiceTest"` | Plan 01 Task 0 | W0 |
| 11-02-02 | 02 | 2 | VISI-01 | integration | `./gradlew :backend:test --tests "*.JobVisibilityServiceTest"` | Plan 01 Task 0 | W0 |
| 11-02-03 | 02 | 2 | VISI-02 | integration | `./gradlew :backend:test --tests "*.ResourceShareServiceTest"` | Plan 01 Task 0 | W0 |
| 11-02-04 | 02 | 2 | VISI-04 | integration | `./gradlew :backend:test --tests "*.SharedWithMeControllerTest"` | Plan 01 Task 0 | W0 |
| 11-02-05 | 02 | 2 | VISI-03 | integration | `./gradlew :backend:test --tests "*.BrowsePublicControllerTest"` | Plan 01 Task 0 | W0 |
| 11-02-06 | 02 | 2 | VISI-05 | integration | `./gradlew :backend:test --tests "*.VisibilityDefaultTest"` | Plan 01 Task 0 | W0 |

*Status: W0 = wave 0 stub created by Plan 01 Task 0, pending implementation in Plan 02*

---

## Wave 0 Requirements

- [x] Test stubs for visibility enum, migration, services, and controllers — **Plan 01 Task 0**
- [x] Shared test fixtures for multi-user scenarios (owner + recipient) — created during Plan 02 TDD

*Plan 01 Task 0 creates all 8 test stub files with @Disabled tests. Plan 02 removes @Disabled and fills in real test bodies.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Default visibility on existing data | VISI-05 | Requires checking production-like migration | Run migration on test DB with existing data, verify all rows have PRIVATE |

---

## Validation Sign-Off

- [x] All tasks have `<automated>` verify or Wave 0 dependencies
- [x] Sampling continuity: no 3 consecutive tasks without automated verify
- [x] Wave 0 covers all MISSING references — Plan 01 Task 0 creates all 8 stubs
- [x] No watch-mode flags
- [x] Feedback latency < 30s
- [x] `nyquist_compliant: true` set in frontmatter

**Approval:** pending execution
