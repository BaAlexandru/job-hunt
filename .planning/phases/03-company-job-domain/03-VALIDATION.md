---
phase: 3
slug: company-job-domain
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 3 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + MockK 1.14.2 + SpringMockK 4.0.2 |
| **Config file** | build.gradle.kts (test dependencies already present) |
| **Quick run command** | `./gradlew :backend:test --tests "com.alex.job.hunt.jobhunt.company.*"` |
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
| 03-01-01 | 01 | 1 | COMP-01 | integration | `./gradlew :backend:test --tests "*CompanyController*create*"` | ❌ W0 | ⬜ pending |
| 03-01-02 | 01 | 1 | COMP-02 | integration | `./gradlew :backend:test --tests "*CompanyController*update*"` | ❌ W0 | ⬜ pending |
| 03-01-03 | 01 | 1 | COMP-03 | integration | `./gradlew :backend:test --tests "*CompanyController*list*"` | ❌ W0 | ⬜ pending |
| 03-02-01 | 02 | 1 | JOBS-01 | integration | `./gradlew :backend:test --tests "*JobController*create*"` | ❌ W0 | ⬜ pending |
| 03-02-02 | 02 | 1 | JOBS-02 | integration | `./gradlew :backend:test --tests "*JobController*company*"` | ❌ W0 | ⬜ pending |
| 03-02-03 | 02 | 1 | JOBS-03 | integration | `./gradlew :backend:test --tests "*JobController*update*"` | ❌ W0 | ⬜ pending |
| 03-02-04 | 02 | 1 | JOBS-04 | integration | `./gradlew :backend:test --tests "*JobController*description*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `backend/src/test/kotlin/com/alex/job/hunt/jobhunt/company/CompanyControllerIntegrationTests.kt` — stubs for COMP-01, COMP-02, COMP-03
- [ ] `backend/src/test/kotlin/com/alex/job/hunt/jobhunt/job/JobControllerIntegrationTests.kt` — stubs for JOBS-01, JOBS-02, JOBS-03, JOBS-04
- [ ] Test helper: `registerAndVerifyAndLogin()` utility for authenticated endpoint tests

*Existing infrastructure covers test framework — no new dependencies needed.*

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
