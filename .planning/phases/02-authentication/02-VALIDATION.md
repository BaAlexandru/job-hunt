---
phase: 2
slug: authentication
status: draft
nyquist_compliant: false
wave_0_complete: false
created: 2026-03-20
---

# Phase 2 — Validation Strategy

> Per-phase validation contract for feedback sampling during execution.

---

## Test Infrastructure

| Property | Value |
|----------|-------|
| **Framework** | JUnit 5 + MockK + Testcontainers |
| **Config file** | `backend/build.gradle.kts` |
| **Quick run command** | `./gradlew :backend:test --tests "com.alex.job.hunt.jobhunt.auth.*"` |
| **Full suite command** | `./gradlew :backend:test` |
| **Estimated runtime** | ~30 seconds |

---

## Sampling Rate

- **After every task commit:** Run `./gradlew :backend:test --tests "com.alex.job.hunt.jobhunt.auth.*"`
- **After every plan wave:** Run `./gradlew :backend:test`
- **Before `/gsd:verify-work`:** Full suite must be green
- **Max feedback latency:** 30 seconds

---

## Per-Task Verification Map

| Task ID | Plan | Wave | Requirement | Test Type | Automated Command | File Exists | Status |
|---------|------|------|-------------|-----------|-------------------|-------------|--------|
| 02-01-01 | 01 | 1 | AUTH-01 | integration | `./gradlew :backend:test --tests "*RegistrationTest*"` | ❌ W0 | ⬜ pending |
| 02-01-02 | 01 | 1 | AUTH-02 | integration | `./gradlew :backend:test --tests "*LoginTest*"` | ❌ W0 | ⬜ pending |
| 02-02-01 | 02 | 2 | AUTH-02 | integration | `./gradlew :backend:test --tests "*JwtTokenTest*"` | ❌ W0 | ⬜ pending |
| 02-02-02 | 02 | 2 | AUTH-03 | integration | `./gradlew :backend:test --tests "*LogoutTest*"` | ❌ W0 | ⬜ pending |
| 02-02-03 | 02 | 2 | AUTH-04 | integration | `./gradlew :backend:test --tests "*PasswordResetTest*"` | ❌ W0 | ⬜ pending |

*Status: ⬜ pending · ✅ green · ❌ red · ⚠️ flaky*

---

## Wave 0 Requirements

- [ ] Add test dependencies: MockK, SpringMockK (if compatible), Testcontainers PostgreSQL, Testcontainers Redis
- [ ] Verify JJWT `jjwt-jackson` compatibility with Spring Boot 4 Jackson 3
- [ ] Create shared test fixtures for auth integration tests (test user factory, JWT helper)
- [ ] Validate SpringMockK 4.x works with Spring Boot 4 — fallback to `@MockitoBean` if not

*Wave 0 validates toolchain compatibility before feature work begins.*

---

## Manual-Only Verifications

| Behavior | Requirement | Why Manual | Test Instructions |
|----------|-------------|------------|-------------------|
| CORS OPTIONS preflight | AUTH-01 | Browser-specific preflight behavior | Send OPTIONS request to /api/auth/login from Postman with Origin: http://localhost:3000, verify 200 + correct headers |
| Password reset console log | AUTH-04 | Console output verification | Trigger reset, check application log for reset link URL |

---

## Validation Sign-Off

- [ ] All tasks have `<automated>` verify or Wave 0 dependencies
- [ ] Sampling continuity: no 3 consecutive tasks without automated verify
- [ ] Wave 0 covers all MISSING references
- [ ] No watch-mode flags
- [ ] Feedback latency < 30s
- [ ] `nyquist_compliant: true` set in frontmatter

**Approval:** pending
