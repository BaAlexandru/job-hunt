---
phase: 6
slug: document-management
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 6 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test + Testcontainers |
| **Config file** | backend/build.gradle.kts |
| **Quick run command** | `./gradlew :backend:test --tests "*Document*"` |
| **Full suite command** | `./gradlew :backend:test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :backend:test --tests "*Document*"`
- **After every plan wave:** Run `./gradlew :backend:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 06-01-01 | 01 | 1 | DOCS-01 (infra) | compile | `./gradlew :backend:classes` | ❌ W0 | ⬜ pending |
| 06-01-02 | 01 | 1 | DOCS-05 (infra) | compile | `./gradlew :backend:classes` | ❌ W0 | ⬜ pending |
| 06-02-01 | 02 | 2 | DOCS-01, DOCS-04 | integration | `./gradlew :backend:test --tests "*Document*"` | ❌ W0 | ⬜ pending |
| 06-02-02 | 02 | 2 | DOCS-02, DOCS-03 | integration | `./gradlew :backend:test --tests "*Document*"` | ❌ W0 | ⬜ pending |
| 06-02-03 | 02 | 2 | DOCS-05 | integration | `./gradlew :backend:test --tests "*Document*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Test stubs for document upload/download (DOCS-01, DOCS-04)
- [ ] Test stubs for document versioning (DOCS-02)
- [ ] Test stubs for document-application linking (DOCS-03)
- [ ] Test stubs for document categorization (DOCS-05)
- [ ] MinIO Testcontainers setup for S3-compatible storage testing

*Existing JUnit 5 + Spring Boot Test infrastructure covers framework needs.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| File content renders correctly after download | DOCS-04 | Binary content visual verification | Upload PDF, download, open in viewer |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
