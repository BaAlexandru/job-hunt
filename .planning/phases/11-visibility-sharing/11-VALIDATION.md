---
phase: 11
slug: visibility-sharing
status: draft
nyquist_compliant: false
wave_0_complete: false
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
| 11-01-01 | 01 | 1 | VISI-01 | unit | `./gradlew :backend:test --tests "*.VisibilityEnumTest"` | ❌ W0 | ⬜ pending |
| 11-01-02 | 01 | 1 | VISI-01 | integration | `./gradlew :backend:test --tests "*.VisibilityMigrationTest"` | ❌ W0 | ⬜ pending |
| 11-02-01 | 02 | 1 | VISI-01 | integration | `./gradlew :backend:test --tests "*.CompanyVisibilityServiceTest"` | ❌ W0 | ⬜ pending |
| 11-02-02 | 02 | 1 | VISI-01 | integration | `./gradlew :backend:test --tests "*.JobVisibilityServiceTest"` | ❌ W0 | ⬜ pending |
| 11-03-01 | 03 | 2 | VISI-02 | integration | `./gradlew :backend:test --tests "*.ResourceShareServiceTest"` | ❌ W0 | ⬜ pending |
| 11-03-02 | 03 | 2 | VISI-04 | integration | `./gradlew :backend:test --tests "*.SharedWithMeControllerTest"` | ❌ W0 | ⬜ pending |
| 11-04-01 | 04 | 2 | VISI-03 | integration | `./gradlew :backend:test --tests "*.BrowsePublicControllerTest"` | ❌ W0 | ⬜ pending |
| 11-05-01 | 05 | 3 | VISI-05 | integration | `./gradlew :backend:test --tests "*.VisibilityDefaultTest"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Test stubs for visibility enum, migration, services, and controllers
- [ ] Shared test fixtures for multi-user scenarios (owner + recipient)

*Existing test infrastructure (JUnit 5, Spring Boot Test, Testcontainers) covers framework needs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Default visibility on existing data | VISI-05 | Requires checking production-like migration | Run migration on test DB with existing data, verify all rows have PRIVATE |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
