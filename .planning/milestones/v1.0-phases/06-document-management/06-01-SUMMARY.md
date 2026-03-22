---
phase: 06-document-management
plan: 01
subsystem: infra, database, api
tags: [minio, s3, aws-sdk-v2, tika, flyway, jpa, docker-compose]

# Dependency graph
requires:
  - phase: 04-application-tracking
    provides: applications table for document_application_links FK
provides:
  - MinIO object storage in Docker Compose
  - Documents, document_versions, document_application_links schema
  - JPA entities for all three document tables
  - Spring Data JPA repositories with query methods
  - StorageService interface and S3StorageService implementation
  - DocumentCategory enum and DTOs for request/response
affects: [06-document-management]

# Tech tracking
tech-stack:
  added: [aws-sdk-v2-s3, apache-tika-core, minio]
  patterns: [S3 storage abstraction, ConfigurationProperties binding, PostConstruct bucket creation]

key-files:
  created:
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/StorageConfig.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/StorageProperties.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/StorageService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/S3StorageService.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/DocumentEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/DocumentVersionEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/DocumentApplicationLinkEntity.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/DocumentRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/DocumentVersionRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/repository/DocumentApplicationLinkRepository.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/DocumentDtos.kt
    - backend/src/main/resources/db/migration/V13__phase06_create_documents.sql
    - backend/src/main/resources/db/migration/V14__phase06_create_document_versions.sql
    - backend/src/main/resources/db/migration/V15__phase06_create_document_application_links.sql
  modified:
    - compose.yaml
    - backend/build.gradle.kts
    - backend/src/main/resources/application.yml
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/DomainExceptions.kt
    - backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/GlobalExceptionHandler.kt

key-decisions:
  - "forcePathStyle(true) on S3Client for MinIO compatibility"
  - "PostConstruct bucket auto-creation for zero-config local dev"
  - "StorageService interface abstracts S3 for testability and future provider swap"

patterns-established:
  - "Storage abstraction: interface StorageService with S3 implementation"
  - "ConfigurationProperties binding for custom storage config block"
  - "Document versioning with is_current flag and unique partial index"

requirements-completed: [DOCS-01, DOCS-03, DOCS-05]

# Metrics
duration: 4min
completed: 2026-03-20
---

# Phase 6 Plan 1: Document Management Foundation Summary

**MinIO S3 storage with AWS SDK v2, Flyway schema for documents/versions/links, JPA entities, repositories, and StorageService abstraction**

## Performance

- **Duration:** 4 min
- **Started:** 2026-03-20T20:27:34Z
- **Completed:** 2026-03-20T20:31:07Z
- **Tasks:** 2
- **Files modified:** 20

## Accomplishments
- MinIO added to Docker Compose with health check and persistent volume
- Three Flyway migrations creating documents, document_versions, and document_application_links tables with indexes
- JPA entities, repositories with JPQL queries, and DTOs for all document domain objects
- S3Client bean with ConfigurationProperties binding and auto bucket creation
- StorageService interface with full S3StorageService implementation (upload, download, delete, exists)
- Exception handlers for storage errors, invalid file types, and upload size limits

## Task Commits

Each task was committed atomically:

1. **Task 1: Infrastructure and data layer setup** - `edf8630` (feat)
2. **Task 2: Storage configuration and service abstraction** - `7af1321` (feat)

## Files Created/Modified
- `compose.yaml` - Added MinIO service with health check and miniodata volume
- `backend/build.gradle.kts` - Added AWS SDK v2 BOM, S3, and Tika dependencies
- `backend/src/main/resources/application.yml` - Added storage config block and multipart limits
- `backend/src/main/resources/db/migration/V13-V15` - Three Flyway migrations for document tables
- `backend/src/main/kotlin/.../entity/enums.kt` - Added DocumentCategory enum
- `backend/src/main/kotlin/.../entity/Document*.kt` - Three JPA entities
- `backend/src/main/kotlin/.../repository/Document*.kt` - Three repositories with query methods
- `backend/src/main/kotlin/.../dto/DocumentDtos.kt` - Response and request DTOs
- `backend/src/main/kotlin/.../config/StorageProperties.kt` - Configuration properties binding
- `backend/src/main/kotlin/.../config/StorageConfig.kt` - S3Client bean with bucket auto-creation
- `backend/src/main/kotlin/.../service/StorageService.kt` - Storage abstraction interface
- `backend/src/main/kotlin/.../service/S3StorageService.kt` - S3 implementation
- `backend/src/main/kotlin/.../service/DomainExceptions.kt` - Added StorageException, InvalidFileTypeException
- `backend/src/main/kotlin/.../config/GlobalExceptionHandler.kt` - Added 3 new exception handlers

## Decisions Made
- Used forcePathStyle(true) on S3Client for MinIO compatibility (required for non-AWS S3 endpoints)
- PostConstruct bucket auto-creation ensures zero-config local development
- StorageService as interface abstracts S3 for easy testing and future provider swap
- Used SLF4J parameterized logging (not string interpolation) in StorageConfig for security

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered

None.

## User Setup Required

None - MinIO starts automatically via Docker Compose alongside PostgreSQL and Redis.

## Next Phase Readiness
- All infrastructure and data layer ready for Plan 02 (DocumentService + DocumentController)
- StorageService injectable for file upload/download operations
- Repositories provide all query methods needed by service layer

---
*Phase: 06-document-management*
*Completed: 2026-03-20*
