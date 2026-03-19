# Architecture Research

**Domain:** Job Application Tracker (Full-Stack Web Application)
**Researched:** 2026-03-19
**Confidence:** HIGH

## Current State Assessment

The project currently exists as a single Spring Boot 4.0.4 application with Kotlin 2.2.21, Java 24, and a basic Docker Compose for PostgreSQL. The PROJECT.md describes a monorepo with `/backend`, `/frontend`, `/infra` directories, but the actual structure is a Spring Initializr scaffold at the root. This architecture document recommends how to restructure.

**Key observation:** The build.gradle.kts already includes Spring AI (Anthropic), Spring Security with OAuth2, Flyway, JPA, and Spring REST Docs. These are solid choices. The architecture should organize around them, not fight them.

## System Overview

```
┌──────────────────────────────────────────────────────────────────┐
│                         Client Browser                            │
│  ┌──────────────────────────────────────────────────────────┐     │
│  │              Next.js Frontend (React/TS)                  │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐               │     │
│  │  │ Pages/   │  │ Components│  │ API Layer│               │     │
│  │  │ Routes   │  │ (shadcn) │  │ (TanStack│               │     │
│  │  │          │  │          │  │  Query)  │               │     │
│  │  └──────────┘  └──────────┘  └─────┬────┘               │     │
│  └─────────────────────────────────────┼────────────────────┘     │
└────────────────────────────────────────┼─────────────────────────┘
                                         │ REST API (JSON)
                                         │ JWT Bearer Token
┌────────────────────────────────────────┼─────────────────────────┐
│              Spring Boot Backend (Kotlin)                          │
│  ┌─────────────────────────────────────┼────────────────────┐     │
│  │                Security Filter Chain │                     │     │
│  │  ┌──────────┐  ┌──────────┐  ┌─────┴────┐               │     │
│  │  │ Auth     │  │ CORS     │  │ JWT      │               │     │
│  │  │ Provider │  │ Config   │  │ Filter   │               │     │
│  │  └──────────┘  └──────────┘  └──────────┘               │     │
│  ├──────────────────────────────────────────────────────────┤     │
│  │                  REST Controllers                         │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │     │
│  │  │ Auth     │  │ Job      │  │ Document │  │ AI       │ │     │
│  │  │ Controller│  │ Controller│  │ Controller│  │ Controller│ │     │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │     │
│  ├───────┴──────────────┴─────────────┴─────────────┴───────┤     │
│  │                  Service Layer                            │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐ │     │
│  │  │ Auth     │  │ Job      │  │ Document │  │ AI       │ │     │
│  │  │ Service  │  │ Service  │  │ Service  │  │ Service  │ │     │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘ │     │
│  ├───────┴──────────────┴─────────────┴─────────────┴───────┤     │
│  │                Repository / Data Layer                     │     │
│  │  ┌──────────┐  ┌──────────┐  ┌──────────┐               │     │
│  │  │ JPA      │  │ File     │  │ Spring AI│               │     │
│  │  │ Repos    │  │ Storage  │  │ Client   │               │     │
│  │  └────┬─────┘  └────┬─────┘  └────┬─────┘               │     │
│  └───────┴──────────────┴─────────────┴─────────────────────┘     │
└──────────┬──────────────┬─────────────┬──────────────────────────┘
           │              │             │
    ┌──────┴──────┐ ┌─────┴─────┐ ┌────┴──────────┐
    │ PostgreSQL  │ │ Local FS  │ │ Anthropic API │
    │ (Docker)    │ │ / S3      │ │ (Claude)      │
    └─────────────┘ └───────────┘ └───────────────┘
```

## Component Responsibilities

| Component | Responsibility | Communicates With |
|-----------|----------------|-------------------|
| **Next.js Frontend** | UI rendering, client-side routing, form handling, state management | Backend REST API via TanStack Query |
| **Security Filter Chain** | Authentication, authorization, CORS, JWT validation | All inbound requests |
| **Auth Controller/Service** | User registration, login, token refresh, OAuth2 | PostgreSQL (user table), JWT library |
| **Job Controller/Service** | CRUD for companies, jobs, applications, status transitions | PostgreSQL (core domain tables) |
| **Document Controller/Service** | File upload/download, document metadata, linking docs to applications | PostgreSQL (metadata), File Storage (binaries) |
| **AI Controller/Service** | CV analysis, cover letter generation, job description parsing | Spring AI ChatClient, Document Service, Job Service |
| **PostgreSQL** | Persistent storage for all structured data | Accessed via Spring Data JPA |
| **File Storage** | Binary document storage (PDFs, DOCX files) | Accessed via storage abstraction |
| **Anthropic API** | LLM inference for AI features | Accessed via Spring AI |

## Recommended Project Structure

### Monorepo Root

```
job-hunt/
├── backend/                     # Spring Boot application (Kotlin)
│   ├── build.gradle.kts
│   ├── src/
│   │   ├── main/
│   │   │   ├── kotlin/com/alex/jobhunt/
│   │   │   │   ├── JobHuntApplication.kt
│   │   │   │   ├── config/              # Spring configuration
│   │   │   │   │   ├── SecurityConfig.kt
│   │   │   │   │   ├── CorsConfig.kt
│   │   │   │   │   ├── JwtConfig.kt
│   │   │   │   │   ├── AiConfig.kt
│   │   │   │   │   └── StorageConfig.kt
│   │   │   │   ├── auth/                # Authentication module
│   │   │   │   │   ├── AuthController.kt
│   │   │   │   │   ├── AuthService.kt
│   │   │   │   │   ├── JwtService.kt
│   │   │   │   │   ├── User.kt          # Entity
│   │   │   │   │   ├── UserRepository.kt
│   │   │   │   │   └── dto/
│   │   │   │   ├── company/             # Company management
│   │   │   │   │   ├── CompanyController.kt
│   │   │   │   │   ├── CompanyService.kt
│   │   │   │   │   ├── Company.kt
│   │   │   │   │   ├── CompanyRepository.kt
│   │   │   │   │   └── dto/
│   │   │   │   ├── job/                 # Job postings
│   │   │   │   │   ├── JobController.kt
│   │   │   │   │   ├── JobService.kt
│   │   │   │   │   ├── Job.kt
│   │   │   │   │   ├── JobRepository.kt
│   │   │   │   │   └── dto/
│   │   │   │   ├── application/         # Application tracking (core)
│   │   │   │   │   ├── ApplicationController.kt
│   │   │   │   │   ├── ApplicationService.kt
│   │   │   │   │   ├── Application.kt
│   │   │   │   │   ├── ApplicationStatus.kt  # Enum
│   │   │   │   │   ├── ApplicationRepository.kt
│   │   │   │   │   └── dto/
│   │   │   │   ├── document/            # Document management
│   │   │   │   │   ├── DocumentController.kt
│   │   │   │   │   ├── DocumentService.kt
│   │   │   │   │   ├── Document.kt
│   │   │   │   │   ├── DocumentRepository.kt
│   │   │   │   │   ├── StorageService.kt     # Abstraction
│   │   │   │   │   ├── LocalStorageService.kt
│   │   │   │   │   └── dto/
│   │   │   │   ├── ai/                  # AI features (later phase)
│   │   │   │   │   ├── AiController.kt
│   │   │   │   │   ├── CvAnalysisService.kt
│   │   │   │   │   ├── CoverLetterService.kt
│   │   │   │   │   └── dto/
│   │   │   │   └── common/              # Shared utilities
│   │   │   │       ├── BaseEntity.kt
│   │   │   │       ├── exception/
│   │   │   │       │   ├── GlobalExceptionHandler.kt
│   │   │   │       │   └── Exceptions.kt
│   │   │   │       └── dto/
│   │   │   │           └── PageResponse.kt
│   │   │   └── resources/
│   │   │       ├── application.yml
│   │   │       ├── application-dev.yml
│   │   │       ├── application-prod.yml
│   │   │       └── db/migration/        # Flyway migrations
│   │   │           ├── V1__create_users.sql
│   │   │           ├── V2__create_companies.sql
│   │   │           ├── V3__create_jobs.sql
│   │   │           ├── V4__create_applications.sql
│   │   │           └── V5__create_documents.sql
│   │   └── test/kotlin/com/alex/jobhunt/
│   └── Dockerfile
├── frontend/                    # Next.js application
│   ├── package.json
│   ├── next.config.ts
│   ├── tsconfig.json
│   ├── tailwind.config.ts
│   ├── src/
│   │   ├── app/                 # Next.js App Router
│   │   │   ├── layout.tsx
│   │   │   ├── page.tsx         # Dashboard / landing
│   │   │   ├── login/
│   │   │   ├── register/
│   │   │   ├── companies/
│   │   │   ├── jobs/
│   │   │   ├── applications/
│   │   │   │   ├── page.tsx     # Table/list view
│   │   │   │   └── board/
│   │   │   │       └── page.tsx # Kanban view
│   │   │   ├── documents/
│   │   │   └── ai/              # AI features UI
│   │   ├── components/
│   │   │   ├── ui/              # shadcn/ui components
│   │   │   ├── layout/          # Header, sidebar, nav
│   │   │   ├── applications/    # Domain-specific components
│   │   │   ├── documents/
│   │   │   └── kanban/
│   │   ├── lib/
│   │   │   ├── api/             # API client functions
│   │   │   │   ├── client.ts    # Axios/fetch wrapper with JWT
│   │   │   │   ├── auth.ts
│   │   │   │   ├── companies.ts
│   │   │   │   ├── jobs.ts
│   │   │   │   ├── applications.ts
│   │   │   │   └── documents.ts
│   │   │   ├── hooks/           # TanStack Query hooks
│   │   │   │   ├── useAuth.ts
│   │   │   │   ├── useApplications.ts
│   │   │   │   └── ...
│   │   │   └── utils/
│   │   ├── types/               # TypeScript interfaces mirroring backend DTOs
│   │   └── providers/           # React context providers
│   │       ├── QueryProvider.tsx
│   │       └── AuthProvider.tsx
│   └── Dockerfile
├── infra/                       # Infrastructure configuration
│   ├── docker/
│   │   ├── docker-compose.yml   # Local dev (Postgres, backend, frontend)
│   │   └── docker-compose.prod.yml
│   └── k8s/                     # Kubernetes manifests (later)
│       ├── helm/
│       └── manifests/
├── .planning/                   # Project planning (GSD)
├── CLAUDE.md                    # Root-level AI assistant context
├── backend/CLAUDE.md            # Backend-specific AI context
├── frontend/CLAUDE.md           # Frontend-specific AI context
└── .gitignore
```

### Structure Rationale

- **Package-by-feature (not by layer):** Each domain module (`auth/`, `company/`, `job/`, `application/`, `document/`, `ai/`) contains its own controller, service, entity, repository, and DTOs. This is the modern Spring Boot convention and aligns with how features get built phase-by-phase. You never need to touch 5 different package trees to add one feature.

- **`common/` for cross-cutting concerns only:** Exception handling, base entities, shared DTOs. Keep this minimal -- if something is only used by two modules, put it in one and import from the other.

- **Frontend `lib/api/` + `lib/hooks/` separation:** API client functions are pure fetch wrappers. TanStack Query hooks wrap them with caching/invalidation. This separation means you can test API functions independently and swap query libraries if needed.

- **`infra/` directory:** Keeps Docker and K8s configs out of application code. The root `compose.yaml` from Spring Initializr should move here.

## Architectural Patterns

### Pattern 1: Package-by-Feature with Layered Internals

**What:** Each feature module contains all layers (controller, service, repository, entity, DTOs) but the layers within a module follow strict dependency direction: Controller -> Service -> Repository.

**When to use:** Always -- this is the recommended default for Spring Boot applications of this size.

**Trade-offs:** Slightly more files per feature, but vastly better discoverability. A developer working on "applications" only needs to look in one package.

**Example:**

```kotlin
// application/ApplicationController.kt
@RestController
@RequestMapping("/api/v1/applications")
class ApplicationController(
    private val applicationService: ApplicationService
) {
    @GetMapping
    fun list(@AuthenticationPrincipal user: UserDetails): ResponseEntity<List<ApplicationDto>> =
        ResponseEntity.ok(applicationService.findAllForUser(user.username))

    @PatchMapping("/{id}/status")
    fun updateStatus(
        @PathVariable id: Long,
        @RequestBody request: StatusUpdateRequest
    ): ResponseEntity<ApplicationDto> =
        ResponseEntity.ok(applicationService.updateStatus(id, request.status))
}
```

### Pattern 2: Storage Abstraction for Documents

**What:** A `StorageService` interface with `LocalStorageService` (dev) and `S3StorageService` (prod) implementations. Metadata in PostgreSQL, binaries on filesystem or object storage.

**When to use:** For the document upload feature. Never store binary files in PostgreSQL.

**Trade-offs:** Slightly more complexity than just saving to a known directory, but essential for future S3 migration and testability.

**Example:**

```kotlin
// document/StorageService.kt
interface StorageService {
    fun store(file: MultipartFile, userId: Long): StorageReference
    fun load(reference: StorageReference): Resource
    fun delete(reference: StorageReference)
}

// document/LocalStorageService.kt
@Service
@Profile("dev", "local")
class LocalStorageService(
    @Value("\${storage.local.path}") private val basePath: Path
) : StorageService {
    override fun store(file: MultipartFile, userId: Long): StorageReference {
        val filename = "${UUID.randomUUID()}_${file.originalFilename}"
        val target = basePath.resolve(userId.toString()).resolve(filename)
        Files.createDirectories(target.parent)
        file.transferTo(target)
        return StorageReference(path = target.toString(), type = StorageType.LOCAL)
    }
    // ...
}
```

### Pattern 3: Application Status as State Machine

**What:** Model application status transitions as an explicit state machine rather than a free-form string update. Define valid transitions (e.g., INTERESTED -> APPLIED, APPLIED -> INTERVIEW, but not INTERESTED -> OFFER).

**When to use:** For the core application tracking feature. Status is the most important field.

**Trade-offs:** More upfront code than a simple enum setter, but prevents invalid states and makes the kanban board logic predictable.

**Example:**

```kotlin
// application/ApplicationStatus.kt
enum class ApplicationStatus {
    INTERESTED,
    APPLIED,
    PHONE_SCREEN,
    INTERVIEW,
    OFFER,
    ACCEPTED,
    REJECTED,
    WITHDRAWN;

    fun canTransitionTo(target: ApplicationStatus): Boolean = when (this) {
        INTERESTED -> target in setOf(APPLIED, WITHDRAWN)
        APPLIED -> target in setOf(PHONE_SCREEN, INTERVIEW, REJECTED, WITHDRAWN)
        PHONE_SCREEN -> target in setOf(INTERVIEW, REJECTED, WITHDRAWN)
        INTERVIEW -> target in setOf(OFFER, REJECTED, WITHDRAWN)
        OFFER -> target in setOf(ACCEPTED, REJECTED)
        ACCEPTED, REJECTED, WITHDRAWN -> false
    }
}
```

### Pattern 4: TanStack Query for Server State

**What:** Use TanStack Query (not Redux/Zustand) as the primary state management solution. Server state (applications, jobs, companies) lives in TanStack Query cache. Only minimal client state (UI preferences, sidebar open/closed) uses React state or context.

**When to use:** For all data fetching in the frontend. This app is primarily server-state driven.

**Trade-offs:** Learning curve if unfamiliar, but eliminates massive amounts of boilerplate around loading states, error handling, caching, and refetching.

**Example:**

```typescript
// lib/hooks/useApplications.ts
export function useApplications() {
  return useQuery({
    queryKey: ['applications'],
    queryFn: () => api.applications.list(),
  })
}

export function useUpdateApplicationStatus() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ id, status }: { id: number; status: string }) =>
      api.applications.updateStatus(id, status),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['applications'] })
    },
  })
}
```

## Data Flow

### Primary Request Flow (CRUD Operations)

```
User Action (click, form submit)
    |
Next.js Page/Component
    |
TanStack Query Hook (useApplications, useMutation)
    |
API Client Function (lib/api/applications.ts)
    | HTTP request with JWT in Authorization header
    v
Spring Security Filter Chain
    | validates JWT, sets SecurityContext
    v
REST Controller (@RestController)
    | validates input (@Valid), extracts user from SecurityContext
    v
Service Layer (@Service, @Transactional)
    | business logic, status validation, authorization checks
    v
Repository Layer (Spring Data JPA)
    | generates SQL from method names / @Query
    v
PostgreSQL
    | result set
    v
Entity -> DTO mapping (in Service)
    | JSON serialization (Jackson + kotlin-module)
    v
HTTP Response -> TanStack Query Cache -> React Re-render
```

### Document Upload Flow

```
User selects file (PDF/DOCX)
    |
Frontend: FormData with multipart/form-data POST
    |
DocumentController: @PostMapping with @RequestParam file: MultipartFile
    |
DocumentService:
    1. Validate file type and size
    2. Store binary via StorageService (local FS or S3)
    3. Create Document entity (metadata: name, type, size, storagePath, userId)
    4. Save metadata to PostgreSQL
    5. Return DocumentDto with download URL
    |
Response: { id, name, type, size, downloadUrl, createdAt }
```

### AI Analysis Flow (Later Phase)

```
User clicks "Analyze CV for this job"
    |
Frontend: POST /api/v1/ai/analyze-cv { applicationId, documentId }
    |
AiController -> CvAnalysisService:
    1. Load job description (from Job entity)
    2. Load company info (from Company entity)
    3. Load CV content (from StorageService, parse PDF/DOCX)
    4. Construct prompt with job + company + CV context
    5. Call Spring AI ChatClient (Anthropic)
    6. Return structured suggestions
    |
Response: { suggestions: [...], relevanceScore, missingKeywords: [...] }
```

### Authentication Flow

```
Registration: POST /api/v1/auth/register { email, password }
    -> Hash password (BCrypt)
    -> Save User entity
    -> Return JWT pair (access + refresh)

Login: POST /api/v1/auth/login { email, password }
    -> Validate credentials
    -> Return JWT pair

Authenticated Request:
    -> Frontend stores JWT in httpOnly cookie or memory
    -> Every API request includes Authorization: Bearer <token>
    -> JwtFilter extracts and validates token
    -> Sets SecurityContextHolder with UserDetails
    -> Controller accesses user via @AuthenticationPrincipal
```

## Key Data Flows

1. **Application lifecycle:** User creates application (INTERESTED) -> updates status through stages -> each transition validated by state machine -> kanban board reflects current states via query invalidation.

2. **Document-to-application linking:** Documents exist independently (a CV can be reused) -> link table `application_documents` associates specific documents with specific applications -> "Which CV did I use for this job?" is always answerable.

3. **AI context assembly:** AI features pull data from multiple services (Job, Company, Document) -> assemble into a rich prompt -> send to LLM -> return structured response. This is a cross-cutting concern that depends on core domain being complete first.

## Scaling Considerations

| Scale | Architecture Adjustments |
|-------|--------------------------|
| 1 user (MVP) | Single Spring Boot instance, local file storage, Docker Compose, SQLite or PostgreSQL in container. This is where you start. |
| 10-100 users | Add proper JWT with refresh tokens, rate limiting on AI endpoints (cost control), S3 for document storage, basic monitoring. |
| 1000+ users | Add Redis for session/cache, move to K8s, separate frontend deployment (Vercel), add CDN for document downloads, async AI processing with job queue. |

### Scaling Priorities

1. **First bottleneck: AI API costs and latency.** LLM calls are slow (2-10 seconds) and expensive. Add request queuing and caching of AI responses for identical inputs before adding more compute.
2. **Second bottleneck: Document storage.** Local filesystem does not scale. Migrate to S3/MinIO before hitting disk limits. The StorageService abstraction makes this a config change, not a rewrite.

## Anti-Patterns

### Anti-Pattern 1: Storing Files in PostgreSQL

**What people do:** Save PDF/DOCX binary content as bytea/BLOB columns in PostgreSQL.
**Why it's wrong:** Bloats database size, makes backups slow, degrades query performance on the same database, and PostgreSQL is not optimized for large binary streaming.
**Do this instead:** Store metadata (filename, type, size, storage path) in PostgreSQL. Store binary content on local filesystem (dev) or S3 (prod) behind the `StorageService` abstraction.

### Anti-Pattern 2: Fat Controllers

**What people do:** Put business logic (status validation, authorization checks, complex queries) directly in Spring controllers.
**Why it's wrong:** Cannot unit test business logic without spinning up Spring context, violates single responsibility, makes controllers unmaintainable.
**Do this instead:** Controllers only handle HTTP concerns (request mapping, validation annotations, response codes). All logic goes in `@Service` classes.

### Anti-Pattern 3: N+1 Queries in JPA

**What people do:** Eagerly load all relationships or rely on lazy loading without thinking about access patterns, leading to N+1 SELECT problems.
**Why it's wrong:** Loading 50 applications that each lazy-load their company and documents produces 150+ queries instead of 3.
**Do this instead:** Use `@EntityGraph` or JPQL `JOIN FETCH` for known access patterns. Use DTOs projected directly from queries for list views. Reserve entity loading for single-item detail views and mutations.

### Anti-Pattern 4: Shared Mutable State in Frontend

**What people do:** Use Redux or global state stores to cache server data, manually keeping it in sync with the backend.
**Why it's wrong:** Leads to stale data bugs, complex synchronization logic, and unnecessary re-renders. Every mutation requires manual cache updates.
**Do this instead:** Use TanStack Query as the server state cache. It handles staleness, refetching, optimistic updates, and cache invalidation automatically.

### Anti-Pattern 5: Monorepo Without Clear Boundaries

**What people do:** Import backend types in frontend or share code between backend and frontend without a clear contract.
**Why it's wrong:** Creates tight coupling. The REST API (OpenAPI spec) IS the contract. Frontend and backend should be independently deployable.
**Do this instead:** Define the API contract explicitly. Frontend TypeScript types mirror backend DTOs but are independently maintained. Consider generating frontend types from an OpenAPI spec if they drift.

## Integration Points

### External Services

| Service | Integration Pattern | Notes |
|---------|---------------------|-------|
| Anthropic Claude API | Spring AI ChatClient with `spring-ai-starter-model-anthropic` | Already in build.gradle.kts. Use ChatClient.Builder, not raw HTTP. Configure via `spring.ai.anthropic.api-key` in env vars, never in config files. |
| PostgreSQL | Spring Data JPA + Flyway | Already configured. Use Flyway for all schema changes -- never modify schema manually. |
| File Storage (S3) | StorageService abstraction with Spring Cloud AWS (later) | Start with local filesystem. Swap implementation via Spring profiles. |

### Internal Boundaries

| Boundary | Communication | Notes |
|----------|---------------|-------|
| Frontend <-> Backend | REST API over HTTP, JSON payloads, JWT auth | API versioning via URL prefix `/api/v1/`. CORS configured for frontend origin. |
| Backend <-> PostgreSQL | Spring Data JPA (Hibernate) | Connection pooling via HikariCP (Spring Boot default). |
| Backend <-> File Storage | StorageService interface | Profile-driven implementation selection (`@Profile("dev")` vs `@Profile("prod")`). |
| Backend <-> Anthropic | Spring AI ChatClient | Async-capable for long-running analysis. Consider timeout configuration. |
| Auth module <-> All modules | SecurityContext (thread-local) | After JWT filter runs, any service can access current user via `SecurityContextHolder`. |

## Build Order (Dependency Chain)

This ordering reflects technical dependencies -- what must exist before the next piece can work.

```
Phase 1: Foundation
  backend/config/ (security, CORS, database)
  backend/common/ (base entity, exception handler)
  backend/auth/ (user entity, JWT, login/register)
  Flyway V1 (users table)
  Docker Compose (PostgreSQL)

Phase 2: Core Domain
  backend/company/ (CRUD)
  backend/job/ (CRUD, linked to company)
  backend/application/ (CRUD, status state machine, linked to job)
  Flyway V2-V4

Phase 3: Documents
  backend/document/ (upload, download, metadata)
  StorageService abstraction (local implementation)
  Link documents to applications
  Flyway V5

Phase 4: Frontend Shell
  Next.js setup with App Router
  Auth pages (login, register)
  AuthProvider + JWT handling
  API client layer
  TanStack Query setup

Phase 5: Frontend Features
  Company/Job CRUD pages
  Application tracking (table view)
  Kanban board view
  Document upload/management

Phase 6: AI Features
  Spring AI ChatClient configuration
  CV analysis service
  Cover letter generation service
  Frontend AI interaction pages
```

**Key dependency insight:** The backend can be fully built and tested (via REST Docs/Postman) before writing any frontend code. The frontend depends on the API existing but not on specific implementation details. AI features depend on ALL core domain entities existing (they read from jobs, companies, documents, and applications).

## Migration Note: Current Structure to Monorepo

The project currently has Spring Boot at the root. To adopt the monorepo structure:

1. Create `backend/` directory
2. Move `src/`, `build.gradle.kts`, `gradle/`, `gradlew`, `gradlew.bat` into `backend/`
3. Create a root `settings.gradle.kts` that includes `backend` as a subproject (or keep backend as standalone Gradle project)
4. Create `frontend/` with `npx create-next-app@latest`
5. Create `infra/docker/` and move `compose.yaml` there
6. Update `compose.yaml` to include both backend and frontend services

Alternatively, keep backend and frontend as independent projects within the monorepo (no shared build system). This is simpler and is recommended -- Gradle for backend, npm/pnpm for frontend, Docker Compose ties them together.

## Sources

- [Spring Boot 4.0 Migration Guide](https://github.com/spring-projects/spring-boot/wiki/Spring-Boot-4.0-Migration-Guide)
- [Next Level Kotlin Support in Spring Boot 4](https://spring.io/blog/2025/12/18/next-level-kotlin-support-in-spring-boot-4/)
- [Spring Boot 4 Overview - JetBrains](https://blog.jetbrains.com/idea/2025/11/spring-boot-4/)
- [Spring AI 2.0 + Spring Boot 4 Guide](https://usama.codes/blog/spring-ai-2-spring-boot-4-guide)
- [Spring AI Agentic Patterns](https://spring.io/blog/2026/01/13/spring-ai-generic-agent-skills/)
- [Hexagonal Architecture with Spring Boot + Kotlin](https://medium.com/@hieunv/understanding-hexagonal-architecture-through-a-practical-application-2f2d28f604d9)
- [Migrating to Modular Monolith with Spring Modulith](https://blog.jetbrains.com/idea/2026/02/migrating-to-modular-monolith-using-spring-modulith-and-intellij-idea/)
- [Clean Architecture with Spring Boot - Baeldung](https://www.baeldung.com/spring-boot-clean-architecture)
- [Scalable File Upload Patterns in Spring Boot](https://github.com/orgs/community/discussions/180838)
- [Spring Boot + Next.js Starter Kit](https://dev.to/nermin_karapandzic/ive-created-an-open-source-spring-boot-nextjs-starter-kit-6fk)

---
*Architecture research for: JobHunt - Job Application Tracker*
*Researched: 2026-03-19*
