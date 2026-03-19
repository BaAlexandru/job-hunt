# Roadmap: JobHunt

## Overview

JobHunt is built backend-first: the Kotlin/Spring Boot API is developed and tested (via Swagger/Postman) before any frontend code is written. The first six phases deliver a complete, tested REST API covering authentication, company/job management, application tracking, interview management, and document handling. The final two phases build the Next.js frontend on top of the stable API, delivering the kanban board, list views, and responsive design. AI features (CV analysis, cover letter generation) are deferred to v2 but the data model accommodates them.

## Phases

**Phase Numbering:**
- Integer phases (1, 2, 3): Planned milestone work
- Decimal phases (2.1, 2.2): Urgent insertions (marked with INSERTED)

Decimal phases appear between their surrounding integers in numeric order.

- [ ] **Phase 1: Foundation & Infrastructure** - Monorepo structure, Docker Compose, PostgreSQL, Flyway, CLAUDE.md files
- [ ] **Phase 2: Authentication** - User registration, JWT login/logout, password reset API
- [ ] **Phase 3: Company & Job Domain** - Company CRUD and job posting CRUD with linking
- [ ] **Phase 4: Application Tracking** - Application CRUD, status state machine, notes, search, filtering
- [ ] **Phase 5: Interview Management** - Interview scheduling, round tracking, notes, timeline
- [ ] **Phase 6: Document Management** - File upload/download, application linking, versioning, categorization
- [ ] **Phase 7: Frontend Shell & Auth UI** - Next.js setup, auth pages, API client, responsive layout
- [ ] **Phase 8: Frontend Core Views** - Kanban board, list/table view, all feature pages

## Phase Details

### Phase 1: Foundation & Infrastructure
**Goal**: A runnable monorepo with Docker Compose, PostgreSQL, Flyway migrations, and developer tooling -- the foundation everything else builds on
**Depends on**: Nothing (first phase)
**Requirements**: INFR-01, INFR-02, INFR-03, INFR-04, DEVX-01, DEVX-02
**Success Criteria** (what must be TRUE):
  1. Running `docker compose up` starts the Spring Boot backend and PostgreSQL database successfully
  2. The monorepo has /backend, /frontend, and /infra directories with the Spring Boot app in /backend
  3. Flyway runs at least one baseline migration on startup and the schema is visible in PostgreSQL
  4. CLAUDE.md files exist in each module directory with module-specific guidance
  5. Kotlin compiler plugins (plugin.spring, plugin.jpa) are configured and verified working
**Plans**: TBD

Plans:
- [ ] 01-01: TBD
- [ ] 01-02: TBD

### Phase 2: Authentication
**Goal**: Users can create accounts, log in with persistent sessions, log out, and reset forgotten passwords via the REST API
**Depends on**: Phase 1
**Requirements**: AUTH-01, AUTH-02, AUTH-03, AUTH-04
**Success Criteria** (what must be TRUE):
  1. User can register with email and password and receive a JWT token
  2. User can log in and the JWT persists across requests (validated via Swagger/Postman)
  3. User can log out and the token is invalidated
  4. User can request a password reset and complete the reset flow via email link
  5. CORS is configured correctly with JWT filter chain ordering (OPTIONS requests pass without auth)
**Plans**: TBD

Plans:
- [ ] 02-01: TBD
- [ ] 02-02: TBD

### Phase 3: Company & Job Domain
**Goal**: Users can manage companies and job postings through the API, with jobs linked to companies
**Depends on**: Phase 2
**Requirements**: COMP-01, COMP-02, COMP-03, JOBS-01, JOBS-02, JOBS-03, JOBS-04
**Success Criteria** (what must be TRUE):
  1. User can create a company with name, website, location, and notes via API
  2. User can edit and delete their own companies, and list all their companies
  3. User can create a job posting with title, description, URL, salary range, location, and job type
  4. User can link a job posting to an existing company
  5. User can store and retrieve the full job description text for any job posting
**Plans**: TBD

Plans:
- [ ] 03-01: TBD
- [ ] 03-02: TBD

### Phase 4: Application Tracking
**Goal**: Users can track job applications through a full status lifecycle with notes, dates, and search/filter capabilities
**Depends on**: Phase 3
**Requirements**: APPL-01, APPL-02, APPL-05, APPL-06, APPL-07
**Success Criteria** (what must be TRUE):
  1. User can create an application linked to a job posting and move it through all 8 statuses (Interested, Applied, Phone Screen, Interview, Offer, Rejected, Accepted, Withdrawn) with validated transitions
  2. User can track applied date, last activity date, and next action date per application
  3. User can add and edit free-text notes on any application
  4. User can search applications by text and filter by status, company, and date range via API
**Plans**: TBD

Plans:
- [ ] 04-01: TBD
- [ ] 04-02: TBD

### Phase 5: Interview Management
**Goal**: Users can schedule interviews, track multiple rounds per application, add notes per stage, and view the full interaction timeline
**Depends on**: Phase 4
**Requirements**: INTV-01, INTV-02, INTV-03, INTV-04
**Success Criteria** (what must be TRUE):
  1. User can schedule an interview with date, time, type (phone/video/onsite), and location or meeting link
  2. User can track multiple interview rounds per application (screening, technical, behavioral, final)
  3. User can add notes and conversation details to each interview stage
  4. User can retrieve a chronological timeline of all interactions and interview stages for an application
**Plans**: TBD

Plans:
- [ ] 05-01: TBD
- [ ] 05-02: TBD

### Phase 6: Document Management
**Goal**: Users can upload, download, version, categorize, and link documents to job applications
**Depends on**: Phase 4
**Requirements**: DOCS-01, DOCS-02, DOCS-03, DOCS-04, DOCS-05
**Success Criteria** (what must be TRUE):
  1. User can upload PDF and DOCX files and the system stores them securely (UUID filenames, path traversal prevention)
  2. User can link documents to specific job applications (which CV was sent where)
  3. User can upload multiple versions of the same document and retrieve any version
  4. User can download previously uploaded documents
  5. User can categorize documents by type (CV, cover letter, portfolio, other)
**Plans**: TBD

Plans:
- [ ] 06-01: TBD
- [ ] 06-02: TBD

### Phase 7: Frontend Shell & Auth UI
**Goal**: A working Next.js frontend with authentication pages, API client layer, and responsive layout shell
**Depends on**: Phase 2
**Requirements**: INFR-05
**Success Criteria** (what must be TRUE):
  1. User can register and log in through browser UI and see their authenticated state
  2. User can log out from any page in the frontend
  3. The application layout is responsive and usable on mobile viewports
  4. The API client handles JWT tokens automatically (attach to requests, handle 401 redirects)
**Plans**: TBD

Plans:
- [ ] 07-01: TBD
- [ ] 07-02: TBD

### Phase 8: Frontend Core Views
**Goal**: Users interact with all features through polished frontend pages including the kanban board and list views
**Depends on**: Phase 3, Phase 4, Phase 5, Phase 6, Phase 7
**Requirements**: APPL-03, APPL-04
**Success Criteria** (what must be TRUE):
  1. User can view applications as a kanban board and drag-and-drop between status columns to change status
  2. User can view applications as a sortable, filterable table/list
  3. User can manage companies, jobs, applications, interviews, and documents through the frontend UI
  4. All CRUD operations from phases 3-6 are accessible through frontend pages
**Plans**: TBD

Plans:
- [ ] 08-01: TBD
- [ ] 08-02: TBD
- [ ] 08-03: TBD

## Progress

**Execution Order:**
Phases execute in numeric order: 1 -> 2 -> 3 -> 4 -> 5 -> 6 -> 7 -> 8
Note: Phase 6 and Phase 7 can run in parallel (both depend on earlier phases, not each other).

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Foundation & Infrastructure | 0/0 | Not started | - |
| 2. Authentication | 0/0 | Not started | - |
| 3. Company & Job Domain | 0/0 | Not started | - |
| 4. Application Tracking | 0/0 | Not started | - |
| 5. Interview Management | 0/0 | Not started | - |
| 6. Document Management | 0/0 | Not started | - |
| 7. Frontend Shell & Auth UI | 0/0 | Not started | - |
| 8. Frontend Core Views | 0/0 | Not started | - |
