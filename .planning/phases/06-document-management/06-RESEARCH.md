# Phase 6: Document Management - Research

**Researched:** 2026-03-20
**Domain:** File storage (S3-compatible), document versioning, multipart upload, MIME validation
**Confidence:** HIGH

## Summary

Phase 6 adds document management: upload PDF/DOCX files, version them, categorize them, link them to job applications, and download them. Files are stored via S3 API (AWS SDK v2 for Java) with MinIO as the local dev backend. The data model involves three new tables: documents (metadata), document_versions (file versions), and document_application_links (many-to-many join linking a specific version to an application).

The implementation follows established project patterns exactly -- UUID PKs, soft delete, user isolation, plain UUID FKs (no @ManyToOne), JPQL queries with CAST for nullable params, Spring Pageable pagination. The new elements are: S3Client configuration via @ConfigurationProperties, a StorageService abstraction, multipart file upload handling, and content-type validation via Apache Tika.

**Primary recommendation:** Use AWS SDK v2 BOM + S3 client for storage, Apache Tika Core for MIME validation, MinIO in Docker Compose for local dev. Follow existing entity/service/controller patterns verbatim.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Documents exist independently in a user's library -- upload without linking to any application
- One document can be linked to multiple applications (many-to-many via join table)
- Links point to a specific version (not just the document) -- preserves history of exactly what was sent
- Multiple documents per application allowed
- Explicit versioning -- user uploads a file and explicitly creates "a new version of document X"
- One version is marked "current/active" -- new versions auto-become current, user can set any old version as current
- Each version has an optional description/note
- Old versions are deletable -- if a deleted version was linked to an application, the link shows "version removed"
- Version metadata (title, category) inherited from parent document -- only file and optional note are new per version
- Fixed categories as enum: CV, COVER_LETTER, PORTFOLIO, OTHER
- User-defined title on upload, original filename stored separately for download
- Optional free-text description on the document
- Library filterable by category + text search on title/description -- same pattern as other entities
- Pagination with Spring Pageable
- Client: AWS SDK v2 for Java (`software.amazon.awssdk:s3`)
- Local dev: MinIO container in Docker Compose
- No Spring Cloud AWS (milestone-only with Spring Boot 4.x breakage)
- No auto-config for S3/MinIO -- manual @ConfigurationProperties
- StorageService interface abstracts all S3 operations
- File types: PDF and DOCX only (strict whitelist)
- Content validation: check MIME type from file content (magic bytes), not just extension
- Maximum file size: 25 MB
- Single-request upload: multipart form with file + metadata in one POST
- Download via API proxy: /api/documents/{id}/download
- UUID filenames in storage -- path traversal prevention
- Soft delete: archived boolean + archivedAt timestamp
- User isolation: all queries filter by userId, 404 for unauthorized access
- File size and content type stored in DocumentVersionEntity
- Archiving a document makes all its versions inaccessible via API
- Application links to archived documents show the document as archived
- Archiving does NOT delete files from S3

### Claude's Discretion
- S3 bucket naming and key structure
- Exact multipart config values in application.yml
- StorageService interface method signatures and error handling
- @ConfigurationProperties class design for storage config
- S3Client bean configuration (endpoint override, credentials provider, region)
- DTO field naming and response structure
- Repository query implementation
- Join table structure for document-application links
- Index strategy for filter/search columns
- Error handling for storage failures
- Bucket auto-creation strategy

### Deferred Ideas (OUT OF SCOPE)
- User profile with skills/experience (future AI phase)
- Rich text document preview (frontend Phase 8)
- Document templates
- Bulk upload
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| DOCS-01 | User can upload PDF and DOCX files (CVs, cover letters, other documents) | S3 storage via AWS SDK v2, multipart upload, MIME validation via Tika, 25MB limit |
| DOCS-02 | User can link uploaded documents to specific job applications | Many-to-many join table (document_application_links) linking version ID to application ID |
| DOCS-03 | User can keep multiple versions of the same document | document_versions table with parent document FK, current flag, version ordering |
| DOCS-04 | User can download previously uploaded documents | S3 GetObject streaming through API proxy with proper Content-Type and Content-Disposition headers |
| DOCS-05 | User can categorize documents by type (CV, cover letter, portfolio, other) | DocumentCategory enum (CV, COVER_LETTER, PORTFOLIO, OTHER) on DocumentEntity |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| software.amazon.awssdk:bom | 2.42.16 | AWS SDK v2 dependency management | BOM ensures consistent versions across AWS modules |
| software.amazon.awssdk:s3 | (managed by BOM) | S3 client for upload/download/delete | Universal S3-compatible client; works with MinIO, AWS, R2, etc. |
| org.apache.tika:tika-core | 3.2.3 | MIME type detection from file content | Industry standard for magic-byte detection; ~1.5MB, no transitive deps |
| minio/minio (Docker) | latest | S3-compatible object storage for local dev | Free, S3 API compatible, zero cloud account needed |

### Supporting
| Library | Version | Purpose | When to Use |
|---------|---------|---------|-------------|
| Spring Boot Validation | (managed) | @Valid on upload DTOs | Already in project; used for request validation |
| Spring Multipart | (built-in) | File upload parsing | spring.servlet.multipart.* config in application.yml |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| AWS SDK v2 | MinIO Java SDK | Locks you into MinIO-specific API; no portability to real S3 |
| AWS SDK v2 | Spring Cloud AWS | 4.0.0-M1 milestone-only, known breakage on Spring Boot 4.x |
| Apache Tika | Files.probeContentType | JDK built-in but unreliable; depends on OS MIME database, misses many types |
| Apache Tika | URLConnection.guessContentType | Extension-based only, trivially spoofable |

**Installation (build.gradle.kts additions):**
```kotlin
// AWS SDK v2 BOM + S3 client
implementation(platform("software.amazon.awssdk:bom:2.42.16"))
implementation("software.amazon.awssdk:s3")

// MIME type detection
implementation("org.apache.tika:tika-core:3.2.3")
```

## Architecture Patterns

### Recommended Project Structure
```
src/main/kotlin/com/alex/job/hunt/jobhunt/
  config/
    StorageProperties.kt          # @ConfigurationProperties("storage")
    StorageConfig.kt              # S3Client bean creation
  entity/
    DocumentEntity.kt             # Document metadata (title, category, description)
    DocumentVersionEntity.kt      # Version (file ref, note, current flag)
    DocumentApplicationLinkEntity.kt  # Join table
    enums.kt                      # Add DocumentCategory enum
  repository/
    DocumentRepository.kt
    DocumentVersionRepository.kt
    DocumentApplicationLinkRepository.kt
  service/
    StorageService.kt             # Interface: upload, download, delete
    S3StorageService.kt           # S3Client implementation
    DocumentService.kt            # Business logic
    DomainExceptions.kt           # Add StorageException, InvalidFileTypeException
  controller/
    DocumentController.kt         # REST endpoints
  dto/
    DocumentDtos.kt               # Request/response DTOs

src/main/resources/
  db/migration/
    V13__phase06_create_documents.sql
    V14__phase06_create_document_versions.sql
    V15__phase06_create_document_application_links.sql
```

### Pattern 1: S3Client Configuration via @ConfigurationProperties
**What:** Externalized storage config with type-safe binding
**When to use:** Always -- this is the standard Spring Boot pattern for non-auto-configured services

```kotlin
@ConfigurationProperties("storage")
data class StorageProperties(
    val endpoint: String,
    val accessKey: String,
    val secretKey: String,
    val bucket: String,
    val region: String = "us-east-1"
)
```

```kotlin
@Configuration
@EnableConfigurationProperties(StorageProperties::class)
class StorageConfig(private val props: StorageProperties) {

    @Bean
    fun s3Client(): S3Client = S3Client.builder()
        .endpointOverride(URI.create(props.endpoint))
        .region(Region.of(props.region))
        .credentialsProvider(
            StaticCredentialsProvider.create(
                AwsBasicCredentials.create(props.accessKey, props.secretKey)
            )
        )
        .forcePathStyle(true)  // REQUIRED for MinIO
        .build()
}
```

### Pattern 2: StorageService Interface
**What:** Abstraction over S3 operations for testability and potential future provider swaps
**When to use:** All file storage operations go through this interface

```kotlin
data class StorageDownload(
    val content: InputStream,
    val contentType: String,
    val contentLength: Long
)

interface StorageService {
    fun upload(key: String, content: InputStream, contentLength: Long, contentType: String)
    fun download(key: String): StorageDownload
    fun delete(key: String)
    fun exists(key: String): Boolean
}
```

### Pattern 3: Multipart Upload with Metadata
**What:** Single POST with file + JSON metadata fields as @RequestPart
**When to use:** Document creation -- file and metadata in one request

```kotlin
@PostMapping(consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
fun createDocument(
    @RequestPart("file") file: MultipartFile,
    @RequestPart("title") title: String,
    @RequestPart("category") category: String,
    @RequestPart("description", required = false) description: String?
): ResponseEntity<DocumentResponse> { ... }
```

### Pattern 4: Streaming Download with Proper Headers
**What:** Proxy download from S3 with Content-Type and Content-Disposition
**When to use:** File download endpoint

```kotlin
@GetMapping("/{id}/versions/{versionId}/download")
fun downloadVersion(
    @PathVariable id: UUID,
    @PathVariable versionId: UUID
): ResponseEntity<InputStreamResource> {
    val (stream, contentType, filename) = documentService.downloadVersion(id, versionId, userId)
    return ResponseEntity.ok()
        .contentType(MediaType.parseMediaType(contentType))
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$filename\"")
        .body(InputStreamResource(stream))
}
```

### Pattern 5: MIME Type Validation via Tika
**What:** Validate actual file content, not just extension
**When to use:** Every file upload

```kotlin
private val tika = Tika()
private val allowedMimeTypes = setOf(
    "application/pdf",
    "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
)

fun validateFileType(file: MultipartFile) {
    val detectedType = tika.detect(file.inputStream)
    if (detectedType !in allowedMimeTypes) {
        throw InvalidFileTypeException("File type '$detectedType' not allowed. Allowed: PDF, DOCX")
    }
}
```

### Pattern 6: UUID Storage Keys (Path Traversal Prevention)
**What:** Store files with UUID-based keys, never user-supplied filenames
**When to use:** All S3 key generation

```kotlin
fun generateStorageKey(documentId: UUID, versionId: UUID, extension: String): String =
    "documents/$documentId/$versionId.$extension"
```

### Anti-Patterns to Avoid
- **Using original filenames as S3 keys:** Path traversal risk; use UUID-based keys only
- **Checking file extension instead of content:** Trivially spoofable; always use magic bytes via Tika
- **Storing files in the database:** BLOBs in PostgreSQL scale poorly; use S3
- **Eager-loading all versions when listing documents:** N+1; only load current version info in list queries
- **Returning S3 presigned URLs:** Adds complexity; proxy through API for auth consistency

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| MIME type detection | Byte-prefix matching | Apache Tika `tika.detect()` | Handles 1000+ formats, maintained, edge cases covered |
| S3 API client | HTTP client with S3 signing | AWS SDK v2 S3Client | Auth signing (SigV4), retries, streaming, multipart -- huge surface area |
| Multipart parsing | Manual request body parsing | Spring MultipartFile | Built-in, handles temp files, size limits, streaming |
| File size enforcement | Manual byte counting | spring.servlet.multipart.max-file-size | Framework-level; rejects before reading full body |

**Key insight:** File handling has many security edge cases (path traversal, MIME spoofing, partial uploads, concurrent access). Use battle-tested libraries for every layer.

## Common Pitfalls

### Pitfall 1: forcePathStyle(true) Missing for MinIO
**What goes wrong:** S3Client uses virtual-hosted-style URLs by default (bucket.endpoint), which MinIO does not support
**Why it happens:** AWS SDK v2 defaults to virtual-hosted style since v2.18.x
**How to avoid:** Always set `.forcePathStyle(true)` on S3Client builder when using MinIO or other S3-compatible services
**Warning signs:** DNS resolution errors, "bucket not found" errors in local dev

### Pitfall 2: MaxUploadSizeExceededException Not Handled
**What goes wrong:** Spring throws MaxUploadSizeExceededException before reaching the controller, bypassing @ExceptionHandler
**Why it happens:** Multipart parsing happens at the servlet level, before Spring MVC dispatching
**How to avoid:** Add an @ExceptionHandler for MaxUploadSizeExceededException in GlobalExceptionHandler. Note: in some Spring Boot versions this needs to be handled via a custom ErrorController or by catching the Tomcat-level exception wrapper
**Warning signs:** Users get a generic 500 instead of a clear "file too large" error

### Pitfall 3: Forgetting max-request-size Along with max-file-size
**What goes wrong:** Setting only max-file-size still fails if the entire multipart request (file + metadata) exceeds max-request-size (default 10MB)
**Why it happens:** Two separate limits: per-file and per-request
**How to avoid:** Set both: `spring.servlet.multipart.max-file-size=25MB` and `spring.servlet.multipart.max-request-size=30MB` (slightly larger to accommodate metadata)

### Pitfall 4: S3 Bucket Not Created on Startup
**What goes wrong:** First upload fails with "bucket does not exist"
**Why it happens:** MinIO starts empty; unlike PostgreSQL tables, S3 buckets are not auto-created
**How to avoid:** Add bucket creation check in @PostConstruct or an ApplicationRunner. Use headBucket() to check existence, createBucket() if missing
**Warning signs:** NoSuchBucketException on first upload

### Pitfall 5: InputStream Already Consumed
**What goes wrong:** Reading MultipartFile.inputStream twice (once for Tika detection, once for S3 upload) fails silently or uploads empty file
**Why it happens:** InputStream is single-read by default
**How to avoid:** Read bytes once into a byte array, or use MultipartFile.getBytes(). For large files, use Tika.detect(inputStream) which only reads the first few KB, then reset or get a fresh stream
**Warning signs:** Zero-byte files in S3, or Tika detecting "application/octet-stream"

### Pitfall 6: Docker Compose MinIO Health Check
**What goes wrong:** Spring Boot starts before MinIO is ready, first S3 operation fails
**Why it happens:** curl was removed from MinIO Docker image; old health checks break silently
**How to avoid:** Use `mc ready local` health check (current official recommendation). Spring Boot's docker-compose integration respects health checks via `depends_on` with `condition: service_healthy`
**Warning signs:** Intermittent "connection refused" on startup

### Pitfall 7: Test Isolation with S3
**What goes wrong:** Tests interfere with each other via shared S3 bucket
**Why it happens:** Unlike DB tables, S3 objects are not rolled back by @Transactional
**How to avoid:** Clear S3 bucket in @BeforeEach, or use unique key prefixes per test. The StorageService interface also enables mocking in unit tests
**Warning signs:** Tests pass individually but fail when run together

## Code Examples

### MinIO Docker Compose Service
```yaml
# Add to compose.yaml
minio:
  image: 'minio/minio:latest'
  command: server /data --console-address ":9001"
  environment:
    MINIO_ROOT_USER: minioadmin
    MINIO_ROOT_PASSWORD: minioadmin
  ports:
    - '9000:9000'
    - '9001:9001'
  volumes:
    - miniodata:/data
  healthcheck:
    test: ["CMD", "mc", "ready", "local"]
    interval: 5s
    timeout: 5s
    retries: 5

# Add to volumes:
miniodata:
```

### application.yml Storage Config
```yaml
storage:
  endpoint: http://localhost:9000
  access-key: minioadmin
  secret-key: minioadmin
  bucket: jobhunt-documents
  region: us-east-1

spring:
  servlet:
    multipart:
      max-file-size: 25MB
      max-request-size: 30MB
```

### Flyway Migration: documents table
```sql
-- V13__phase06_create_documents.sql
CREATE TABLE documents (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL,
    title       VARCHAR(255) NOT NULL,
    description TEXT,
    category    VARCHAR(50) NOT NULL,
    archived    BOOLEAN NOT NULL DEFAULT FALSE,
    archived_at TIMESTAMPTZ,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documents_user_id ON documents(user_id);
CREATE INDEX idx_documents_category ON documents(user_id, category);
CREATE INDEX idx_documents_title ON documents(user_id, lower(title));
```

### Flyway Migration: document_versions table
```sql
-- V14__phase06_create_document_versions.sql
CREATE TABLE document_versions (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_id     UUID NOT NULL REFERENCES documents(id),
    version_number  INT NOT NULL,
    storage_key     VARCHAR(500) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    content_type    VARCHAR(100) NOT NULL,
    file_size       BIGINT NOT NULL,
    note            TEXT,
    is_current      BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_document_versions_document_id ON document_versions(document_id);
CREATE UNIQUE INDEX idx_document_versions_current ON document_versions(document_id) WHERE is_current = TRUE;
```

### Flyway Migration: document_application_links table
```sql
-- V15__phase06_create_document_application_links.sql
CREATE TABLE document_application_links (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    document_version_id UUID NOT NULL REFERENCES document_versions(id),
    application_id      UUID NOT NULL REFERENCES applications(id),
    linked_at           TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_doc_app_links_version ON document_application_links(document_version_id);
CREATE INDEX idx_doc_app_links_application ON document_application_links(application_id);
CREATE UNIQUE INDEX idx_doc_app_links_unique ON document_application_links(document_version_id, application_id);
```

### Bucket Auto-Creation on Startup
```kotlin
@PostConstruct
fun ensureBucketExists() {
    try {
        s3Client.headBucket(HeadBucketRequest.builder().bucket(props.bucket).build())
    } catch (e: NoSuchBucketException) {
        s3Client.createBucket(CreateBucketRequest.builder().bucket(props.bucket).build())
        logger.info("Created S3 bucket: ${props.bucket}")
    }
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| AWS SDK v1 (com.amazonaws) | AWS SDK v2 (software.amazon.awssdk) | EOL Dec 2025 | v1 no longer maintained; v2 is required |
| Spring Cloud AWS 3.x | Direct AWS SDK v2 (no Spring wrapper) | Spring Boot 4.x | Spring Cloud AWS 4.0.0-M1 is unstable on SB4 |
| curl-based MinIO health check | `mc ready local` | 2024 | curl removed from MinIO Docker image |
| Virtual-hosted S3 URLs | forcePathStyle(true) for MinIO | SDK v2.18.x | Default changed; must explicitly set for non-AWS S3 |

**Deprecated/outdated:**
- AWS SDK v1 (com.amazonaws): End-of-life December 2025; use v2
- Spring Cloud AWS for Spring Boot 4: Milestone-only, known breakage

## Open Questions

1. **Spring Boot 4 multipart property namespace**
   - What we know: Spring Boot 3.x uses `spring.servlet.multipart.*`. Spring Boot 4 renamed starters (webmvc instead of web).
   - What's unclear: Whether the property path changed to `spring.webmvc.multipart.*` or remains `spring.servlet.multipart.*`
   - Recommendation: Start with `spring.servlet.multipart.*` (documented standard); if it doesn't work, check Spring Boot 4 release notes for property renames. LOW risk -- multipart config is servlet-level, likely unchanged.

2. **Docker Compose auto-discovery of MinIO**
   - What we know: Spring Boot docker-compose auto-configures PostgreSQL and Redis but NOT S3/MinIO
   - What's unclear: Whether spring-boot-docker-compose recognizes MinIO at all (for health check waiting)
   - Recommendation: Configure storage properties manually via @ConfigurationProperties. Use `depends_on` with health check in compose.yaml so MinIO is ready before Spring starts.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | JUnit 5 + Spring Boot Test |
| Config file | backend/build.gradle.kts (test dependencies) |
| Quick run command | `./gradlew :backend:test --tests "com.alex.job.hunt.jobhunt.document.*"` |
| Full suite command | `./gradlew :backend:test` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| DOCS-01 | Upload PDF and DOCX files with MIME validation | integration | `./gradlew :backend:test --tests "*.document.DocumentControllerIntegrationTests.uploadDocument*"` | Wave 0 |
| DOCS-01 | Reject non-PDF/DOCX files | integration | `./gradlew :backend:test --tests "*.document.DocumentControllerIntegrationTests.rejectInvalid*"` | Wave 0 |
| DOCS-02 | Link document version to application | integration | `./gradlew :backend:test --tests "*.document.DocumentControllerIntegrationTests.linkDocument*"` | Wave 0 |
| DOCS-03 | Upload new version of existing document | integration | `./gradlew :backend:test --tests "*.document.DocumentControllerIntegrationTests.createVersion*"` | Wave 0 |
| DOCS-04 | Download uploaded document | integration | `./gradlew :backend:test --tests "*.document.DocumentControllerIntegrationTests.downloadDocument*"` | Wave 0 |
| DOCS-05 | Filter documents by category | integration | `./gradlew :backend:test --tests "*.document.DocumentControllerIntegrationTests.filterByCategory*"` | Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test --tests "com.alex.job.hunt.jobhunt.document.*"`
- **Per wave merge:** `./gradlew :backend:test`
- **Phase gate:** Full suite green before /gsd:verify-work

### Wave 0 Gaps
- [ ] `backend/src/test/kotlin/.../document/DocumentControllerIntegrationTests.kt` -- covers DOCS-01 through DOCS-05
- [ ] Test PDF and DOCX fixture files in test resources
- [ ] TestHelper.createDocument() and TestHelper.createDocumentVersion() helper methods
- [ ] S3/MinIO cleanup in @BeforeEach (clear bucket or mock StorageService)

## Sources

### Primary (HIGH confidence)
- Existing project codebase -- entity patterns, service patterns, test patterns, migration naming
- [AWS SDK v2 S3 documentation](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/examples-s3.html) -- S3Client API, builder configuration
- [MinIO Docker health check](https://github.com/minio/minio/issues/18389) -- `mc ready local` replaces curl

### Secondary (MEDIUM confidence)
- [AWS SDK v2 BOM on Maven Central](https://central.sonatype.com/artifact/software.amazon.awssdk/bom) -- version 2.42.16 (latest as of 2026-03-18)
- [Apache Tika Core](https://central.sonatype.com/artifact/org.apache.tika/tika-core) -- version 3.2.3 (latest as of 2025-09-09)
- [Spring Boot multipart configuration](https://spring.io/guides/gs/uploading-files/) -- spring.servlet.multipart.* properties
- [S3Client forcePathStyle for MinIO](https://github.com/aws/aws-sdk-java-v2/discussions/3611) -- required since SDK v2.18.x

### Tertiary (LOW confidence)
- Spring Boot 4 multipart property namespace -- assumed unchanged from 3.x; needs runtime validation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- AWS SDK v2, Tika, MinIO are well-documented and widely used
- Architecture: HIGH -- follows existing project patterns exactly (entities, services, controllers, tests)
- Pitfalls: HIGH -- documented from official sources (MinIO health check, forcePathStyle, InputStream consumption)
- S3 integration details: MEDIUM -- AWS SDK v2 Java with Kotlin is well-supported but Spring Boot 4 specifics less documented

**Research date:** 2026-03-20
**Valid until:** 2026-04-20 (stable libraries, low churn)
