# Phase 6: Document Management - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

Users can upload, download, version, categorize, and link documents (CVs, cover letters, portfolios) to job applications via the REST API. Documents live in an independent library and can be linked to multiple applications. Files are stored in MinIO (S3-compatible object storage). No frontend UI — API tested via Swagger/Postman.

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

### Upload behavior
- File types: PDF and DOCX only (strict whitelist)
- Content validation: check MIME type from file content (magic bytes), not just extension
- Maximum file size: 25 MB
- Single-request upload: multipart form with file + metadata (title, category, description) in one POST
- Files stored in MinIO (S3-compatible) — added to Docker Compose stack
- Download via API proxy: /api/documents/{id}/download — API checks auth, then streams from MinIO
- UUID filenames in storage — path traversal prevention
- Soft delete: archived boolean + archivedAt timestamp (same pattern as all entities)
- User isolation: all queries filter by userId, 404 for unauthorized access

### Claude's Discretion
- MinIO bucket naming and key structure
- Exact multipart config values in application.yml
- Storage service abstraction design (interface for future S3 swap)
- DTO field naming and response structure
- Repository query implementation
- Join table structure for document-application links
- Index strategy for filter/search columns
- Error handling for storage failures (MinIO down, upload interrupted)
- Whether to store file size and content type in the database for quick access

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
- `compose.yaml` — Docker Compose (add MinIO service here)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- Entity pattern: UUID PK, userId FK, timestamps, soft delete — replicate for DocumentEntity and DocumentVersionEntity
- `enums.kt`: Add DocumentCategory enum (CV, COVER_LETTER, PORTFOLIO, OTHER)
- `DomainExceptions.kt`: Add storage-related exceptions if needed
- `GlobalExceptionHandler.kt`: Extend for file upload errors (size exceeded, invalid type)
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
- compose.yaml: Add MinIO service with health check
- application.yml: Add multipart config and MinIO connection properties

</code_context>

<specifics>
## Specific Ideas

- Documents are a central library — not scoped to applications. User uploads CVs/cover letters independently and links them to applications later
- Version-specific linking is critical — "Application X used CV v2" gives accurate history of what was actually sent
- MinIO chosen over local filesystem for production-readiness and S3 API compatibility
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
