---
phase: 10-gap-closure
plan: 03
subsystem: auth
tags: [smtp, email, spring-mail, gmail, password-reset, javamailsender]

# Dependency graph
requires:
  - phase: 02-auth
    provides: PasswordResetService, UserEntity, PasswordResetToken, rate limiter
provides:
  - EmailService with SMTP email sending and HTML template
  - Password reset email delivery via Gmail SMTP
  - Unit tests for EmailService and PasswordResetService
affects: [11-deployment, infra]

# Tech tracking
tech-stack:
  added: [spring-boot-starter-mail, JavaMailSender, MimeMessageHelper]
  patterns: [SMTP fallback to logging on failure, HTML email template as Kotlin string]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/EmailService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/SendResetEmailRequest.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/service/EmailServiceTests.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/service/PasswordResetServiceTests.kt
  modified:
    - backend/build.gradle.kts
    - backend/src/main/resources/application.yml
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/PasswordResetService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/AuthController.kt
    - frontend/lib/auth.ts

key-decisions:
  - "Gmail SMTP with App Passwords for email delivery (simple, no third-party service)"
  - "HTML email template inline in Kotlin (no external template engine needed for single template)"
  - "Fallback to logging on SMTP failure so reset flow never breaks"
  - "Backend /api/auth/send-reset-email endpoint for Better Auth callback to call"

patterns-established:
  - "EmailService pattern: try/catch around mailSender.send with fallback logging"
  - "SMTP config via env vars (SMTP_USERNAME, SMTP_PASSWORD) for deployment flexibility"

requirements-completed: [GAP-03]

# Metrics
duration: 7min
completed: 2026-03-22
---

# Phase 10 Plan 03: Password Reset Email Summary

**SMTP email delivery for password reset via Gmail with HTML template, fallback logging, and unit tests**

## Performance

- **Duration:** 7 min (continuation session, tasks 1-3 completed in prior session)
- **Started:** 2026-03-22T16:19:14Z
- **Completed:** 2026-03-22T16:26:04Z
- **Tasks:** 4 (3 auto + 1 human-verify checkpoint)
- **Files modified:** 9

## Accomplishments
- EmailService sends HTML password reset emails via Gmail SMTP with JavaMailSender
- PasswordResetService calls EmailService instead of logging the reset URL
- Better Auth sendResetPassword callback now sends email via backend API endpoint
- SMTP failure falls back to logging without breaking the reset flow
- User verified full end-to-end flow: email received, password reset, login with new password
- Unit tests for EmailService (7 tests) and PasswordResetService (12 tests) added

## Task Commits

Each task was committed atomically:

1. **Task 1: Add mail dependency, SMTP config, and EmailService** - `2a574cf` (feat)
2. **Task 2: Wire EmailService into PasswordResetService** - `f497b1f` (feat)
3. **Task 3: Wire email sending into Better Auth sendResetPassword callback** - `4b17e49` (feat)
4. **Task 4: Verify password reset email flow** - checkpoint approved by user
5. **Tests: Unit tests for EmailService and PasswordResetService** - `fb50d2d` (test)

## Files Created/Modified
- `backend/build.gradle.kts` - Added spring-boot-starter-mail dependency
- `backend/src/main/resources/application.yml` - SMTP config (Gmail), app.frontend-base-url, app.mail-from
- `backend/src/main/kotlin/.../service/EmailService.kt` - SMTP email sending with HTML template
- `backend/src/main/kotlin/.../service/PasswordResetService.kt` - Wired EmailService, frontend reset URL
- `backend/src/main/kotlin/.../controller/AuthController.kt` - Added /api/auth/send-reset-email endpoint
- `backend/src/main/kotlin/.../dto/SendResetEmailRequest.kt` - Request DTO for send-reset-email
- `frontend/lib/auth.ts` - sendResetPassword callback calls backend instead of console.log
- `backend/src/test/kotlin/.../service/EmailServiceTests.kt` - 7 unit tests for EmailService
- `backend/src/test/kotlin/.../service/PasswordResetServiceTests.kt` - 12 unit tests for PasswordResetService

## Decisions Made
- Gmail SMTP with App Passwords chosen over third-party services (Resend, SendGrid) for simplicity
- HTML email template stored inline in Kotlin rather than using Thymeleaf/Freemarker (only one template needed)
- Backend endpoint /api/auth/send-reset-email added so Better Auth callback can trigger email via existing EmailService
- Real entity instances used in unit tests instead of mocking JPA entities (avoids MockK ClassCastException with generic save methods)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing Critical] Added unit tests for EmailService and PasswordResetService**
- **Found during:** Post-checkpoint (user request for Nyquist compliance)
- **Issue:** Plan did not include unit tests; validation plan required them
- **Fix:** Created EmailServiceTests (7 tests) and PasswordResetServiceTests (12 tests) with MockK
- **Files created:** EmailServiceTests.kt, PasswordResetServiceTests.kt
- **Verification:** `./gradlew :backend:test` passes (all tests green)
- **Committed in:** fb50d2d

---

**Total deviations:** 1 auto-fixed (1 missing critical)
**Impact on plan:** Test addition required for completeness. No scope creep.

## Issues Encountered
- MockK relaxed mocks on JPA repositories return Object instead of entity type for generic save() methods, causing ClassCastException. Resolved by explicitly stubbing `save()` with `answers { firstArg() }` and using real entity instances instead of mocking JPA entities.

## User Setup Required

Password reset email requires Gmail SMTP credentials:
- `SMTP_USERNAME` - Gmail address
- `SMTP_PASSWORD` - Google App Password (16-char, from myaccount.google.com/apppasswords)
- `FRONTEND_BASE_URL` - Frontend URL (defaults to http://localhost:3000)
- `MAIL_FROM` - Sender address (defaults to noreply@job-hunt.dev)

## Next Phase Readiness
- All 3 plans in Phase 10 complete
- Password reset email delivery working end-to-end
- Ready for Phase 11 (deployment/infrastructure)

## Self-Check: PASSED

All files verified present. All commits verified in git log.

---
*Phase: 10-gap-closure*
*Completed: 2026-03-22*
