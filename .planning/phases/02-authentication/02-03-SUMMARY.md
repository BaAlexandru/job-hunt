---
phase: 02-authentication
plan: 03
subsystem: auth
tags: [email-verification, password-reset, redis, rate-limiting, integration-tests, spring-security]

requires:
  - phase: 02-authentication/02-01
    provides: "JPA entities, repositories, DTOs, Flyway migrations"
  - phase: 02-authentication/02-02
    provides: "SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter, AuthService, AuthController, AuthExceptionHandler"
provides:
  - "EmailVerificationService with token validation and user activation"
  - "PasswordResetService with Redis rate-limited reset flow"
  - "RateLimiter service using Redis INCR+EXPIRE sliding window"
  - "TokenBlocklistCleanupService with @Scheduled daily cleanup"
  - "Full auth API: register, verify, login, refresh, logout, password-reset, password-reset/confirm"
  - "9 integration tests proving end-to-end auth lifecycle"
affects: [03-company-crud, 07-frontend]

tech-stack:
  added: [spring-boot-webmvc-test]
  patterns: ["@Transactional on services accessing lazy-loaded JPA relationships", "@Scheduled + @EnableScheduling for cron jobs", "Redis INCR+EXPIRE for rate limiting", "AuthenticationEntryPoint returning 401 for REST APIs"]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/EmailVerificationService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/PasswordResetService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/RateLimiter.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/TokenBlocklistCleanupService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/RedisConfig.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/auth/AuthControllerIntegrationTest.kt
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/AuthController.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/AuthExceptionHandler.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/SecurityConfig.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/JobHuntApplication.kt
    - backend/build.gradle.kts

key-decisions:
  - "Added @Transactional to service methods accessing lazy-loaded JPA relationships to prevent LazyInitializationException"
  - "Added AuthenticationEntryPoint to SecurityConfig returning 401 (not default 403) for unauthenticated REST API requests"
  - "Added spring-boot-webmvc-test dependency for Spring Boot 4 MockMvc support (package moved from spring-boot-test-autoconfigure)"

patterns-established:
  - "@Transactional on service methods that traverse lazy JPA relationships"
  - "Redis rate limiting via INCR+EXPIRE atomic pattern"
  - "@Scheduled cron tasks with @EnableScheduling on application class"
  - "Spring Boot 4 test: use org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04]

duration: 10min
completed: 2026-03-20
---

# Phase 02 Plan 03: Auth Verification, Reset, Rate Limiting, and Integration Tests Summary

**Email verification, password reset with Redis rate limiting, blocklist cleanup, and 9 integration tests proving full auth lifecycle end-to-end**

## Performance

- **Duration:** 10 min
- **Started:** 2026-03-20T00:40:00Z
- **Completed:** 2026-03-20T00:50:00Z
- **Tasks:** 2
- **Files modified:** 11

## Accomplishments
- Email verification flow: register -> get token -> verify -> account enabled -> login works
- Password reset flow with Redis-based rate limiting (3 requests/hour per email)
- Scheduled token blocklist cleanup running daily at 3 AM
- 9 integration tests covering: register, duplicate email, weak password, unverified login, email verify + login, token refresh, logout invalidation, password reset, protected endpoints, CORS headers

## Task Commits

Each task was committed atomically:

1. **Task 1: Create EmailVerificationService, PasswordResetService, RateLimiter, RedisConfig, and TokenBlocklistCleanupService** - `99aa0f0` (feat)
2. **Task 2: Add verify and password-reset endpoints to AuthController, update exception handler, and create integration tests** - `632370c` (feat)

## Files Created/Modified
- `backend/src/main/kotlin/.../service/EmailVerificationService.kt` - Token validation, user activation, InvalidTokenException
- `backend/src/main/kotlin/.../service/PasswordResetService.kt` - Rate-limited reset request, token confirmation, RateLimitException
- `backend/src/main/kotlin/.../service/RateLimiter.kt` - Redis INCR+EXPIRE sliding window rate limiter
- `backend/src/main/kotlin/.../service/TokenBlocklistCleanupService.kt` - @Scheduled daily cleanup of expired blocklist entries
- `backend/src/main/kotlin/.../config/RedisConfig.kt` - Placeholder for future Redis customization
- `backend/src/main/kotlin/.../controller/AuthController.kt` - Added verify, password-reset, password-reset/confirm endpoints
- `backend/src/main/kotlin/.../config/AuthExceptionHandler.kt` - Added InvalidTokenException (400) and RateLimitException (429) handlers
- `backend/src/main/kotlin/.../config/SecurityConfig.kt` - Added 401 AuthenticationEntryPoint
- `backend/src/main/kotlin/.../JobHuntApplication.kt` - Added @EnableScheduling
- `backend/build.gradle.kts` - Added spring-boot-webmvc-test dependency
- `backend/src/test/.../auth/AuthControllerIntegrationTest.kt` - 9 integration tests for full auth lifecycle

## Decisions Made
- Added `@Transactional` to EmailVerificationService.verify() and PasswordResetService methods because lazy-loaded JPA relationships (EmailVerificationToken.user, PasswordResetToken.user) require an open session
- Added `AuthenticationEntryPoint` to SecurityConfig to return 401 instead of Spring Security's default 403 for unauthenticated requests -- correct REST API behavior
- Spring Boot 4 moved `AutoConfigureMockMvc` to `org.springframework.boot.webmvc.test.autoconfigure` package (from `org.springframework.boot.test.autoconfigure.web.servlet`), required adding `spring-boot-webmvc-test` dependency
- Used `tools.jackson.databind.json.JsonMapper` instead of `ObjectMapper` for Spring Boot 4 Jackson 3 compatibility

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Added @Transactional to prevent LazyInitializationException**
- **Found during:** Task 2 (integration tests)
- **Issue:** EmailVerificationService.verify() and PasswordResetService methods access lazy-loaded `user` relationship on token entities, causing LazyInitializationException outside a transaction
- **Fix:** Added `@Transactional` annotation to verify(), requestReset(), and confirmReset() methods
- **Files modified:** EmailVerificationService.kt, PasswordResetService.kt
- **Verification:** All 9 integration tests pass
- **Committed in:** 632370c (Task 2 commit)

**2. [Rule 2 - Missing Critical] Added 401 AuthenticationEntryPoint to SecurityConfig**
- **Found during:** Task 2 (protectedEndpointRequiresAuth test)
- **Issue:** Spring Security defaults to 403 Forbidden for unauthenticated requests when no entry point is configured; REST APIs should return 401 Unauthorized
- **Fix:** Added `exceptionHandling { authenticationEntryPoint = ... }` to SecurityConfig returning 401
- **Files modified:** SecurityConfig.kt
- **Verification:** protectedEndpointRequiresAuth test passes with correct 401 status
- **Committed in:** 632370c (Task 2 commit)

**3. [Rule 3 - Blocking] Added spring-boot-webmvc-test dependency and fixed import**
- **Found during:** Task 2 (test compilation)
- **Issue:** Spring Boot 4 moved `AutoConfigureMockMvc` to separate `spring-boot-webmvc-test` module; old import path `org.springframework.boot.test.autoconfigure.web.servlet` no longer exists
- **Fix:** Added `testImplementation("org.springframework.boot:spring-boot-webmvc-test")` to build.gradle.kts; updated import to `org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc`
- **Files modified:** build.gradle.kts, AuthControllerIntegrationTest.kt
- **Verification:** Test compilation succeeds, all tests pass
- **Committed in:** 632370c (Task 2 commit)

---

**Total deviations:** 3 auto-fixed (1 bug, 1 missing critical, 1 blocking)
**Impact on plan:** All auto-fixes necessary for correctness and Spring Boot 4 compatibility. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations above.

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Full authentication system complete: register, verify, login, refresh, logout, password-reset
- All AUTH-01 through AUTH-04 requirements satisfied and tested
- Ready for Phase 3 (Company CRUD) which will use authenticated endpoints
- Protected endpoints return 401 for unauthenticated requests

## Self-Check: PASSED

- All 6 created files verified present on disk
- Commit 99aa0f0 (Task 1) verified in git log
- Commit 632370c (Task 2) verified in git log
- All 11 tests pass (9 integration + 2 existing)

---
*Phase: 02-authentication*
*Completed: 2026-03-20*
