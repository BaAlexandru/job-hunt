---
phase: 5
slug: interview-management
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 5 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + MockMvc |
| **Config file** | build.gradle.kts (test dependencies already present) |
| **Quick run command** | `./gradlew :backend:test --tests "*Interview*"` |
| **Full suite command** | `./gradlew :backend:test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :backend:test --tests "*Interview*"`
- **After every plan wave:** Run `./gradlew :backend:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 05-01-01 | 01 | 1 | INTV-01, INTV-02 | integration | `./gradlew :backend:test --tests "*InterviewControllerIntegration*"` | ❌ W0 | ⬜ pending |
| 05-01-02 | 01 | 1 | INTV-03 | integration | `./gradlew :backend:test --tests "*InterviewNoteControllerIntegration*"` | ❌ W0 | ⬜ pending |
| 05-02-01 | 02 | 2 | INTV-04 | integration | `./gradlew :backend:test --tests "*TimelineControllerIntegration*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] `InterviewControllerIntegrationTests.kt` — stubs for INTV-01, INTV-02
- [ ] `InterviewNoteControllerIntegrationTests.kt` — stubs for INTV-03
- [ ] `TimelineControllerIntegrationTests.kt` — stubs for INTV-04
- [ ] TestHelper needs new methods: `createInterview()`, `createInterviewNote()`

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
