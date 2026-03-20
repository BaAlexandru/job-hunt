# Phase 6: Document Management - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can upload, download, version, categorize, and link documents (CVs, cover letters, portfolios) to job applications via the REST API. Documents live in an independent library and can be linked to multiple applications. Files are stored via the S3 API (AWS SDK v2 for Java) backed by MinIO in local dev. No frontend UI — API tested via Swagger/Postman.

</domain>

<decisions>
## Implementation Decisions

### Document-application linking
- Documents exist independently in a user's library — upload without linking to any application
- One document can be linked to multiple applications (many-to-many via join table)
- Links point to a specific version (not just the document) — preserves history of exactly what was sent
- Multiple documents per application allowed — e.g., one CV + one cover letter + one portfolio for the same application
- No limit on documents per application or applications per document

### Versioning behavior
- Explicit versioning — user uploads a file and explicitly creates "a new version of document X"
- One version is marked "current/active" — new versions auto-become current, user can set any old version as current
- Each version has an optional description/note (e.g., "Tailored for fintech roles", "Added AWS certifications")
- Old versions are deletable — if a deleted version was linked to an application, the link shows "version removed"
- Version metadata (title, category) inherited from parent document — only the file and optional note are new per version

### Document organization
- Fixed categories as enum: CV, COVER_LETTER, PORTFOLIO, OTHER
- User-defined title on upload (e.g., "Backend Developer CV") — original filename stored separately for download
- Optional free-text description on the document (e.g., "My general-purpose CV for backend roles")
- Library filterable by category + text search on title/description — same pattern as other entities
- Pagination with Spring Pageable (same as Company/Job/Application)

### File storage strategy
- **Client**: AWS SDK v2 for Java (`software.amazon.awssdk:s3`) — the universal S3 client
- **Local dev**: MinIO container in Docker Compose as S3-compatible backend
- **Production**: Same code targets any S3-compatible provider (AWS S3, DigitalOcean Spaces, Cloudflare R2, MinIO in K8s) — just change endpoint URL and credentials
- **Why AWS SDK v2 over MinIO Java SDK**: AWS SDK v2 works with MinIO AND real S3 providers. MinIO SDK locks you into MinIO-specific API. Zero code changes between environments.
- **Why NOT Spring Cloud AWS**: Spring Cloud AWS 4.0.0-M1 is milestone-only with known breakage on Spring Boot 4.0.x. Too risky.
- **No auto-config**: Spring Boot Docker Compose does NOT auto-configure S3/MinIO (unlike PostgreSQL/Redis). Storage config uses manual @ConfigurationProperties — standard practice for S3 in any environment.
- StorageService interface abstracts all S3 operations — single implementation via S3Client

### Upload behavior
- File types: PDF and DOCX only (strict whitelist)
- Content validation: check MIME type from file content (magic bytes), not just extension
- Maximum file size: 25 MB
- Single-request upload: multipart form with file + metadata (title, category, description) in one POST
- Files stored via S3 API (MinIO locally, any S3-compatible provider in production) — MinIO added to Docker Compose stack
- Download via API proxy: /api/documents/{id}/versions/{versionId}/download — API checks auth, then streams from S3
- UUID filenames in storage — path traversal prevention
- Soft delete: archived boolean + archivedAt timestamp (same pattern as all entities)
- User isolation: all queries filter by userId, 404 for unauthorized access
- File size and content type stored in DocumentVersionEntity for download headers and UI display

### Document archive cascading
- Archiving a document makes all its versions inaccessible via API
- Application links to archived documents show the document as archived
- Archiving does NOT delete files from S3 — cleanup is a separate concern

### Claude's Discretion
- S3 bucket naming and key structure
- Exact multipart config values in application.yml
- StorageService interface method signatures and error handling
- @ConfigurationProperties class design for storage config (endpoint, access-key, secret-key, bucket, region)
- S3Client bean configuration (endpoint override, credentials provider, region)
- DTO field naming and response structure
- Repository query implementation
- Join table structure for document-application links
- Index strategy for filter/search columns
- Error handling for storage failures (S3/MinIO down, upload interrupted)
- Bucket auto-creation strategy (e.g., @PostConstruct or startup check)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` — Tech stack constraints (Kotlin, Spring Boot 4.x, Spring Data JPA, PostgreSQL)
- `.planning/REQUIREMENTS.md` — DOCS-01 through DOCS-05
- `.planning/ROADMAP.md` — Phase 6 success criteria (5 criteria)

### Prior phase context
- `.planning/phases/03-company-job-domain/03-CONTEXT.md` — Entity patterns, soft delete, user isolation, pagination, error handling
- `.planning/phases/04-application-tracking/04-CONTEXT.md` — Application entity structure (link target for document-application joins)

### Backend conventions
- `backend/CLAUDE.md` — Package structure, coding conventions, Flyway naming, testing approach

### Existing code to build on
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/ApplicationEntity.kt` — Application entity (FK target for document links)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt` — Enum pattern (add DocumentCategory here)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/DomainExceptions.kt` — Custom exceptions to extend
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/GlobalExceptionHandler.kt` — Error handling to extend
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/SecurityContextUtil.kt` — userId extraction
- `backend/build.gradle.kts` — Add AWS SDK v2 S3 dependency (platform BOM + s3 client)
- `backend/src/main/resources/application.yml` — Add multipart config and storage properties (@ConfigurationProperties)
- `compose.yaml` — Docker Compose (add MinIO service here)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Entity pattern: UUID PK, userId FK, timestamps, soft delete — replicate for DocumentEntity and DocumentVersionEntity
- `enums.kt`: Add DocumentCategory enum (CV, COVER_LETTER, PORTFOLIO, OTHER)
- `DomainExceptions.kt`: Add storage-related exceptions (e.g., StorageException, InvalidFileTypeException)
- `GlobalExceptionHandler.kt`: Extend for file upload errors (MaxUploadSizeExceededException, invalid type, storage failures)
- `SecurityContextUtil.kt`: Reuse for userId extraction in DocumentController

### Established Patterns
- Plain UUID foreign keys (no @ManyToOne) — use for applicationId and documentId references
- Batch resolution via findAllByIdInAndUserId for N+1 prevention in list queries
- JPQL @Query with CAST(:param AS string) for nullable search parameters
- Flyway: next migration is V13__phase06_{description}.sql
- Soft delete with archived boolean + archivedAt timestamp

### Integration Points
- SecurityConfig: /api/documents/** endpoints protected by default (require JWT)
- Flyway: New migration files for documents, document_versions, and document_application_links tables
- ApplicationEntity: FK target for document-application link join table
- compose.yaml: Add MinIO service (minio/minio image, ports 9000+9001, health check, volume)
- application.yml: Add multipart config (max-file-size: 25MB) and storage properties (endpoint, access-key, secret-key, bucket)
- build.gradle.kts: Add AWS SDK v2 BOM (`software.amazon.awssdk:bom`) and S3 client (`software.amazon.awssdk:s3`)
- New @ConfigurationProperties("storage") class for S3 connection config
- New StorageService interface + S3StorageService implementation using AWS SDK v2 S3Client

</code_context>

<specifics>
## Specific Ideas

- Documents are a central library — not scoped to applications. User uploads CVs/cover letters independently and links them to applications later
- Version-specific linking is critical — "Application X used CV v2" gives accurate history of what was actually sent
- AWS SDK v2 chosen as S3 client (over MinIO Java SDK) for provider-agnostic compatibility — works with MinIO locally and any S3-compatible provider in production (AWS S3, DigitalOcean Spaces, Cloudflare R2, MinIO in K8s). No cloud account needed during development.
- 25 MB limit is generous to accommodate portfolios with embedded images
- Content type validation (magic bytes) prevents malicious file uploads disguised with wrong extensions

</specifics>

<deferred>
## Deferred Ideas

- **User profile with skills/experience** — User suggested a profile containing skills, experience, and relevant information for LLM-powered CV improvement. This is a new capability that feeds into AI features (AI-01, AI-02) and deserves its own phase with a purpose-built data model
- **Rich text document preview** — Rendering PDFs/DOCX in-browser is a frontend concern (Phase 8)
- **Document templates** — Pre-built CV/cover letter templates for quick starts
- **Bulk upload** — Upload multiple files at once

</deferred>

---

*Phase: 06-document-management*
*Context gathered: 2026-03-20*
