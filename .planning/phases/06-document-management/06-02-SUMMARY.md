---
phase: 06-document-management
plan: 02
subsystem: api, service
tags: [tika, multipart, s3, rest-api, versioning, document-management]

# Dependency graph
requires:
  - phase: 06-document-management
    plan: 01
    provides: MinIO storage, document entities, repositories, DTOs, StorageService
  - phase: 04-application-tracking
    provides: ApplicationRepository.findByIdAndUserId for linking verification
provides:
  - DocumentService with full business logic (CRUD, versioning, linking, MIME validation)
  - DocumentController with 13 REST endpoints under /api/documents
  - Integration tests covering all DOCS-01 through DOCS-05 requirements
affects: [06-document-management]

# Tech tracking
tech-stack:
  added: []
  patterns: [Tika MIME detection with filename hint, multipart @RequestPart handling, InputStreamResource streaming download]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/DocumentService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/DocumentController.kt
    - backend/src/test/kotlin/com/alex/job/hunt/jobhunt/document/DocumentControllerIntegrationTests.kt
    - backend/src/test/resources/fixtures/test.pdf
    - backend/src/test/resources/fixtures/test.docx
  modified:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/StorageConfig.kt

key-decisions:
  - "Tika detect(bytes, filename) for accurate OOXML MIME detection -- bytes-only detection returns application/zip for DOCX"
  - "CommandLineRunner instead of PostConstruct for bucket creation to avoid circular bean dependency"
  - "DocumentDownload data class for clean download triple (content, contentType, filename)"

patterns-established:
  - "Multipart upload with @RequestPart for file and metadata fields"
  - "Streaming download with InputStreamResource and Content-Disposition header"
  - "Version management with clearCurrentFlag batch update before setting new current"

requirements-completed: [DOCS-01, DOCS-02, DOCS-03, DOCS-04, DOCS-05]

# Metrics
duration: 13min
completed: 2026-03-20
---

# Phase 6 Plan 2: Document Service, Controller, and Integration Tests Summary

**DocumentService with Tika MIME validation, 13 REST endpoints for upload/download/versioning/linking, and 19 integration tests covering all DOCS requirements**

## Performance

- **Duration:** 13 min
- **Started:** 2026-03-20T20:34:51Z
- **Completed:** 2026-03-20T20:48:38Z
- **Tasks:** 2
- **Files modified:** 6

## Accomplishments
- DocumentService implementing full business logic: CRUD, versioning with current flag, application linking, Tika MIME validation, S3 upload/download
- DocumentController exposing 13 REST endpoints with multipart upload, streaming download, pagination, and proper HTTP status codes
- 19 integration tests proving all DOCS-01 through DOCS-05 requirements pass end-to-end
- Fixed circular bean dependency in StorageConfig and Tika MIME detection accuracy

## Task Commits

Each task was committed atomically:

1. **Task 1: DocumentService and DocumentController implementation** - `b0aa373` (feat)
2. **Task 2: Integration tests for all DOCS requirements** - `79a6405` (test)

## Files Created/Modified
- `backend/src/main/kotlin/.../service/DocumentService.kt` - Business logic: upload, download, versioning, linking, MIME validation, archive
- `backend/src/main/kotlin/.../controller/DocumentController.kt` - 13 REST endpoints under /api/documents
- `backend/src/test/kotlin/.../document/DocumentControllerIntegrationTests.kt` - 19 integration tests
- `backend/src/test/resources/fixtures/test.pdf` - Minimal valid PDF fixture for testing
- `backend/src/test/resources/fixtures/test.docx` - Minimal valid DOCX fixture for testing
- `backend/src/main/kotlin/.../config/StorageConfig.kt` - Fixed PostConstruct -> CommandLineRunner

## Decisions Made
- Used `tika.detect(bytes, filename)` instead of `tika.detect(bytes)` because byte-only detection returns `application/zip` for DOCX files (OOXML is ZIP-based)
- Replaced `@PostConstruct ensureBucketExists()` with `@Bean CommandLineRunner` to avoid circular bean creation exception when StorageService depends on S3Client
- Created `DocumentDownload` data class to cleanly return (InputStream, contentType, filename) from service to controller

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Fixed circular bean dependency in StorageConfig**
- **Found during:** Task 2 (running integration tests)
- **Issue:** `@PostConstruct` calling `s3Client()` bean method caused `BeanCurrentlyInCreationException` when Spring tried to resolve the dependency chain: DocumentService -> StorageService -> S3Client -> StorageConfig PostConstruct
- **Fix:** Changed `@PostConstruct ensureBucketExists()` to a `@Bean CommandLineRunner` that receives `S3Client` as a parameter, running after all beans are created
- **Files modified:** `backend/src/main/kotlin/.../config/StorageConfig.kt`
- **Verification:** Full test suite passes (all 19 document tests + all existing tests)
- **Committed in:** `79a6405` (Task 2 commit)

**2. [Rule 1 - Bug] Fixed Tika MIME detection for DOCX files**
- **Found during:** Task 2 (DOCX upload test failing)
- **Issue:** `tika.detect(bytes)` returns `application/zip` for DOCX files since OOXML is ZIP-based. Without filename hint, Tika cannot distinguish DOCX from plain ZIP.
- **Fix:** Changed to `tika.detect(bytes, file.originalFilename)` which uses the `.docx` extension to correctly identify `application/vnd.openxmlformats-officedocument.wordprocessingml.document`
- **Files modified:** `backend/src/main/kotlin/.../service/DocumentService.kt`
- **Verification:** DOCX upload test passes, invalid file rejection still works
- **Committed in:** `79a6405` (Task 2 commit)

---

**Total deviations:** 2 auto-fixed (2 bugs)
**Impact on plan:** Both fixes necessary for correct operation. No scope creep.

## Issues Encountered
- MinIO container was not running during initial test execution. Started it with `docker compose up -d minio`. Spring Boot's Docker Compose integration should auto-start it, but it was not yet running from a previous session.

## User Setup Required

None - MinIO starts automatically via Docker Compose alongside PostgreSQL and Redis.

## Next Phase Readiness
- Document management API fully functional and tested
- Phase 6 complete -- all infrastructure, data layer, service logic, and endpoints delivered
- Ready for PR to merge into master

---
*Phase: 06-document-management*
*Completed: 2026-03-20*
