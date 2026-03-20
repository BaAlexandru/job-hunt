---
phase: 02-authentication
plan: 01
subsystem: auth
tags: [spring-security, jjwt, redis, flyway, jpa, kotlin]

# Dependency graph
requires:
  - phase: 01-foundation-infrastructure
    provides: "Spring Boot project, PostgreSQL, Flyway baseline, Docker Compose"
provides:
  - "Spring Security, JJWT, validation, Redis dependencies"
  - "Users table with email/password/role/enabled"
  - "Token blocklist table for JWT revocation"
  - "Email verification and password reset token tables"
  - "JPA entities validated against Flyway schema"
  - "Auth DTOs with Jakarta Bean Validation"
  - "Repository interfaces for all auth tables"
affects: [02-02-PLAN, 02-03-PLAN, security-config, jwt-service, auth-controller]

# Tech tracking
tech-stack:
  added: [spring-boot-starter-security, spring-boot-starter-validation, jjwt-api-0.12.6, jjwt-gson-0.12.6, spring-boot-starter-data-redis, mockk-1.14.2, springmockk-4.0.2, spring-security-test]
  patterns: [entity-patterns-uuid-keys, id-based-equals-hashcode, flyway-then-validate, lazy-fetch-manytoone]

key-files:
  created:
    - backend/src/main/resources/db/migration/V2__phase02_create_users.sql
    - backend/src/main/resources/db/migration/V3__phase02_token_blocklist.sql
    - backend/src/main/resources/db/migration/V4__phase02_verification_reset_tokens.sql
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/UserEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/Role.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/TokenBlocklistEntry.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/EmailVerificationToken.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/PasswordResetToken.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/UserRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/TokenBlocklistRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/EmailVerificationTokenRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/PasswordResetTokenRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/RegisterRequest.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/AuthRequest.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/AuthResponse.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/PasswordResetRequest.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/PasswordResetConfirmRequest.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/MessageResponse.kt
  modified:
    - backend/build.gradle.kts
    - compose.yaml
    - backend/src/main/resources/application.yml

key-decisions:
  - "Used jjwt-gson instead of jjwt-jackson to avoid Jackson 2/3 classpath conflict with Spring Boot 4"
  - "JWT config as custom properties (jwt.secret, jwt.access-expiration-ms) not under spring namespace"
  - "Redis auto-configured via Docker Compose integration, no explicit host/port properties"

patterns-established:
  - "Entity pattern: UUID PK, audit columns (createdAt/updatedAt), id-based equals/hashCode"
  - "DTO pattern: Kotlin data classes with @field: Jakarta validation annotations"
  - "Repository pattern: JpaRepository<Entity, UUID> with custom query methods"

requirements-completed: [AUTH-01, AUTH-02, AUTH-03, AUTH-04]

# Metrics
duration: 3min
completed: 2026-03-20
---

# Phase 02 Plan 01: Auth Data Layer Summary

**Flyway migrations for 4 auth tables, JPA entities with UUID keys, Spring Security + JJWT + Redis dependencies, and validated DTOs with Jakarta Bean Validation**

## Performance

- **Duration:** 3 min
- **Started:** 2026-03-20T00:26:13Z
- **Completed:** 2026-03-20T00:28:47Z
- **Tasks:** 2
- **Files modified:** 21

## Accomplishments
- Added all Phase 2 dependencies (Spring Security, JJWT with Gson serializer, Redis, validation, MockK)
- Created 3 Flyway migrations producing 4 tables: users, token_blocklist, email_verification_tokens, password_reset_tokens
- Built 5 JPA entities following entity-patterns (UUID keys, audit columns, lazy fetch, id-based equals/hashCode)
- Created 4 repository interfaces with domain-specific query methods
- Created 6 DTOs with Jakarta Bean Validation annotations (password complexity regex, email format)

## Task Commits

Each task was committed atomically:

1. **Task 1: Add Phase 2 dependencies, Redis, JWT config** - `cfc2936` (feat)
2. **Task 2: Create Flyway migrations, JPA entities, repositories, DTOs** - `878fcc3` (feat)

## Files Created/Modified
- `backend/build.gradle.kts` - Added security, validation, JJWT, Redis, testing dependencies
- `compose.yaml` - Added Redis 7 Alpine service on port 6379
- `backend/src/main/resources/application.yml` - Added JWT secret/expiration config
- `V2__phase02_create_users.sql` - Users table with email uniqueness, role enum, enabled flag
- `V3__phase02_token_blocklist.sql` - Token blocklist with token_id index and expiry index
- `V4__phase02_verification_reset_tokens.sql` - Email verification and password reset token tables
- `UserEntity.kt` - User JPA entity with Role enum, email/password/enabled
- `TokenBlocklistEntry.kt` - Blocklist entry for JWT revocation
- `EmailVerificationToken.kt` - Verification token with lazy user relationship
- `PasswordResetToken.kt` - Password reset token with lazy user relationship
- `Role.kt` - USER/ADMIN enum
- `UserRepository.kt` - findByEmail, existsByEmail
- `TokenBlocklistRepository.kt` - existsByTokenId, deleteByExpiresAtBefore
- `EmailVerificationTokenRepository.kt` - findByToken
- `PasswordResetTokenRepository.kt` - findByToken
- `RegisterRequest.kt` - Email + password with complexity validation
- `AuthRequest.kt` - Login credentials DTO
- `AuthResponse.kt` - Access token + expiry + token type
- `PasswordResetRequest.kt` - Email for reset initiation
- `PasswordResetConfirmRequest.kt` - Token + new password with validation
- `MessageResponse.kt` - Generic message wrapper

## Decisions Made
- Used jjwt-gson instead of jjwt-jackson to avoid Jackson 2/3 classpath conflict (Spring Boot 4 uses Jackson 3 via tools.jackson, JJWT's jjwt-jackson depends on Jackson 2)
- JWT configuration placed under custom `jwt:` namespace (not under `spring:`) since these are application-specific properties
- Redis auto-configured via Docker Compose integration -- no explicit spring.data.redis.host/port needed

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - no external service configuration required.

## Next Phase Readiness
- Data layer complete: all auth tables, entities, repositories, and DTOs ready
- Plan 02 can build SecurityFilterChain, JwtService, and UserDetailsService on this foundation
- Plan 03 can build auth controllers using these DTOs and repositories
- Note: `./gradlew :backend:test` will fail until Plan 02 adds SecurityFilterChain (Spring Security auto-secures all endpoints with no config)

## Self-Check: PASSED

All 21 files verified present. Both task commits (cfc2936, 878fcc3) confirmed in git log.

---
*Phase: 02-authentication*
*Completed: 2026-03-20*
