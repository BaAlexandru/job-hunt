---
phase: 1
slug: foundation-infrastructure
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-19
---

# Phase 1 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + Spring Boot Test |
| **Config file** | backend/build.gradle.kts |
| **Quick run command** | `cd backend && ../gradlew test` |
| **Full suite command** | `cd backend && ../gradlew test` |
| **Estimated runtime** | ~15 seconds |

---

## Sampling Rate

- **After every task commit:** Run `cd backend && ../gradlew test`
- **After every plan wave:** Run `cd backend && ../gradlew test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 15 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 01-01-01 | 01 | 1 | INFR-03 | manual | `ls backend/src frontend/ infra/` | ❌ W0 | pending |
| 01-01-02 | 01 | 1 | INFR-01 | integration | `docker compose up -d && docker compose ps` | ❌ W0 | pending |
| 01-01-03 | 01 | 1 | INFR-02 | integration | `docker compose exec postgres psql -U jobhunt -c '\l'` | ❌ W0 | pending |
| 01-01-04 | 01 | 1 | INFR-04 | integration | `cd backend && ../gradlew test` | ❌ W0 | pending |
| 01-02-01 | 02 | 2 | DEVX-01 | manual | `ls backend/CLAUDE.md frontend/CLAUDE.md infra/CLAUDE.md` | ❌ W0 | pending |
| 01-02-02 | 02 | 2 | DEVX-02 | manual | `ls .claude/skills/` | ❌ W0 | pending |

*Status: pending / green / red / flaky*

---

## Wave 0 Requirements

- [ ] Existing `JobHuntApplicationTests.kt` — move to backend/src/test/ and verify passes
- [ ] Spring Boot Test starter already in dependencies — no new framework install needed

*Existing infrastructure covers test framework requirements.*

---

## Notes

- **Test database access**: Tests use `testAndDevelopmentOnly` spring-boot-docker-compose to auto-start PostgreSQL. No test-specific Spring profile is needed for Phase 1, but Phase 2 (Security) will likely require an `application-test.yml` to configure security contexts for tests.
- **pgcrypto extension**: Technically unnecessary on PostgreSQL 13+ (gen_random_uuid() is built-in), but kept intentionally for additional cryptographic functions that may be useful later.

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| Monorepo directory structure | INFR-03 | Filesystem layout, not logic | Verify /backend, /frontend, /infra dirs exist with correct contents |
| CLAUDE.md files present | DEVX-01 | Documentation files, not logic | Verify each module has CLAUDE.md with relevant guidance |
| Project skills configured | DEVX-02 | Config files, not logic | Verify .claude/skills/ directory has skill files |

---

## Validation Sign-Off

- [ ] All tasks have automated verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 15s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
