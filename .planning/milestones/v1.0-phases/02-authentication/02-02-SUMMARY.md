---
phase: 02-authentication
plan: 02
subsystem: auth
tags: [spring-security, jwt, jjwt, cors, kotlin, spring-boot]

# Dependency graph
requires:
  - phase: 02-authentication
    plan: 01
    provides: "JPA entities, repositories, DTOs, Flyway migrations, Spring Security dependency"
provides:
  - "Spring Security filter chain with JWT validation and CORS"
  - "JWT token provider (access + refresh tokens via JJWT)"
  - "Authentication filter checking blocklist on each request"
  - "Register, login, refresh, logout REST endpoints"
  - "Global exception handler for auth and validation errors"
affects: [02-03-PLAN, frontend-auth, api-endpoints]

# Tech tracking
tech-stack:
  added: []
  patterns: [security-filter-chain-kotlin-dsl, jwt-access-refresh-pair, http-only-cookie-refresh, token-blocklist-on-logout, delegating-password-encoder]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/SecurityConfig.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/JwtTokenProvider.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/JwtAuthenticationFilter.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/UserDetailsServiceImpl.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/AuthService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/AuthController.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/AuthExceptionHandler.kt
  modified: []

key-decisions:
  - "Used PasswordEncoderFactories.createDelegatingPasswordEncoder() for future-proof password hashing"
  - "Refresh token scoped to /api/auth/refresh path in cookie for security"
  - "Custom AuthenticationException (not Spring's) to avoid classpath conflict with Spring Security's AuthenticationException"

patterns-established:
  - "Controller pattern: thin controllers delegating to service, cookie management in controller"
  - "Service pattern: business logic with custom exceptions, service returns Pair for multi-value responses"
  - "Exception handler pattern: @RestControllerAdvice with per-exception-type handlers"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03]

# Metrics
duration: 3min
completed: 2026-03-20
---

# Phase 02 Plan 02: Auth Security Layer Summary

**Spring Security filter chain with JWT access/refresh tokens, register/login/refresh/logout endpoints, CORS for localhost:3000, and token blocklist on logout**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T00:32:30Z
- **Completed:** 2026-03-20T00:35:55Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Spring Security configured with stateless JWT sessions, CORS for frontend, and public auth/actuator endpoints
- JWT token provider creates signed access tokens (15min, with userId/role/type claims) and refresh tokens (7 days) using JJWT
- Auth filter validates JWT on every request, checks blocklist, and only authenticates access tokens (not refresh)
- Four auth endpoints: register (201), login (200 + cookie), refresh (200 + rotated cookie), logout (200 + cleared cookie)
- Global exception handler returns proper HTTP status codes for registration, auth, validation, and missing cookie errors

## Task Commits

Each task was committed atomically:

1. **Task 1: SecurityConfig, JwtTokenProvider, JwtAuthenticationFilter, UserDetailsServiceImpl** - `4b712d3` (feat)
2. **Task 2: AuthService, AuthController, AuthExceptionHandler** - `ecc580b` (feat)

## Files Created/Modified
- `SecurityConfig.kt` - Filter chain with CORS, CSRF disabled, stateless sessions, JWT filter before UsernamePasswordAuthenticationFilter
- `JwtTokenProvider.kt` - Token creation/validation with JJWT, access and refresh tokens with jti for blocklist
- `JwtAuthenticationFilter.kt` - OncePerRequestFilter extracting JWT from Authorization header, checking blocklist, setting SecurityContext
- `UserDetailsServiceImpl.kt` - Loads user from DB, maps to Spring Security UserDetails with role and disabled flag
- `AuthService.kt` - Register (with email verification token), login, refresh (with token rotation), logout (blocklists both tokens)
- `AuthController.kt` - REST endpoints with HTTP-only refresh cookie management
- `AuthExceptionHandler.kt` - Global exception handling for auth flow errors

## Decisions Made
- Used `PasswordEncoderFactories.createDelegatingPasswordEncoder()` instead of raw BCryptPasswordEncoder for algorithm upgradeability
- Scoped refresh cookie path to `/api/auth/refresh` so the cookie is only sent on refresh requests (not all auth requests)
- Created custom `AuthenticationException` and `RegistrationException` in the service package to avoid name collision with Spring Security's `AuthenticationException`

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed nullable PasswordEncoder.encode() return in Spring Security 7**
- **Found during:** Task 2 (AuthService implementation)
- **Issue:** `PasswordEncoder.encode()` in Spring Security 7 returns `String?` (nullable) in Kotlin due to updated nullability annotations, but `UserEntity.password` expects `String`
- **Fix:** Added `!!` non-null assertion on `passwordEncoder.encode(request.password)`
- **Files modified:** AuthService.kt
- **Verification:** BUILD SUCCESSFUL after fix
- **Committed in:** ecc580b (Task 2 commit)

---

**Total deviations:** 1 auto-fixed (1 bug)
**Impact on plan:** Trivial fix for Kotlin nullability. No scope creep.

## Issues Encountered

None beyond the auto-fixed deviation above.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Security layer complete: filter chain, JWT, and all auth endpoints operational
- Plan 03 can build email verification (verify endpoint), password reset flow, and integration tests
- Note: Registered users have `enabled = false` -- email verification (Plan 03) required before login works

## Self-Check: PASSED

All 7 files verified present. Both task commits (4b712d3, ecc580b) confirmed in git log.

---
*Phase: 02-authentication*
*Completed: 2026-03-20*
