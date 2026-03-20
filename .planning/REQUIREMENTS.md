# Requirements: JobHunt

**Defined:** 2026-03-19
**Core Value:** Track jobs you've applied to with their status, documents, and timeline so nothing falls through the cracks during a job search.

## v1 Requirements

Requirements for initial release. Each maps to roadmap phases.

### Authentication

- [x] **AUTH-01**: User can create account with email and password
- [x] **AUTH-02**: User can log in and stay logged in across sessions via JWT
- [x] **AUTH-03**: User can log out from any page
- [x] **AUTH-04**: User can reset password via email link

### Company Management

- [x] **COMP-01**: User can add a company with name, website, location, and notes
- [x] **COMP-02**: User can edit and delete companies
- [x] **COMP-03**: User can view all companies in a list

### Job Posting Management

- [x] **JOBS-01**: User can add a job posting with title, description, URL, salary range, location, and job type
- [x] **JOBS-02**: User can link a job posting to a company
- [x] **JOBS-03**: User can edit and delete job postings
- [x] **JOBS-04**: User can store the full job description text

### Application Tracking

- [x] **APPL-01**: User can create an application linked to a job posting
- [x] **APPL-02**: User can set and change application status (Interested, Applied, Phone Screen, Interview, Offer, Rejected, Accepted, Withdrawn)
- [ ] **APPL-03**: User can view applications as a kanban board with drag-and-drop between status columns
- [ ] **APPL-04**: User can view applications as a sortable, filterable table/list
- [x] **APPL-05**: User can track application dates (applied date, last activity, next action date)
- [x] **APPL-06**: User can add free-text notes to each application
- [x] **APPL-07**: User can search applications by text and filter by status, company, and date range

### Interview Management

- [ ] **INTV-01**: User can schedule an interview with date, time, type (phone/video/onsite), and location or meeting link
- [ ] **INTV-02**: User can track multiple interview rounds per application (screening, technical, behavioral, final)
- [ ] **INTV-03**: User can add notes and conversation details per interview stage
- [ ] **INTV-04**: User can view a timeline of all interactions and interview stages per application

### Document Management

- [ ] **DOCS-01**: User can upload PDF and DOCX files (CVs, cover letters, other documents)
- [ ] **DOCS-02**: User can link uploaded documents to specific job applications
- [ ] **DOCS-03**: User can keep multiple versions of the same document
- [ ] **DOCS-04**: User can download previously uploaded documents
- [ ] **DOCS-05**: User can categorize documents by type (CV, cover letter, portfolio, other)

### Visibility & Sharing

- [ ] **VISI-01**: User can set visibility (PRIVATE/PUBLIC/SHARED) on companies and jobs
- [ ] **VISI-02**: User can share specific companies or jobs with other users (by email)
- [ ] **VISI-03**: User can browse public companies and jobs from other users
- [ ] **VISI-04**: User can view items shared with them
- [ ] **VISI-05**: Shared users can only VIEW (not edit/delete) unless granted EDIT permission

### Infrastructure

- [x] **INFR-01**: Application runs in Docker containers via Docker Compose
- [x] **INFR-02**: PostgreSQL runs as a container in the compose stack
- [x] **INFR-03**: Monorepo structure with /backend, /frontend, /infra directories
- [x] **INFR-04**: Database migrations managed by Flyway
- [ ] **INFR-05**: Responsive web design usable on mobile viewports

### Developer Experience

- [x] **DEVX-01**: Nested CLAUDE.md files per module (backend, frontend, infra)
- [x] **DEVX-02**: Dedicated project-level Claude Code skills

## v2 Requirements

Deferred to future release. Tracked but not in current roadmap. Data model designed to accommodate these without rewrites.

### Google Calendar Integration

- **GCAL-01**: Sync interview events to Google Calendar
- **GCAL-02**: Update calendar events when interview is rescheduled

### Analytics & Dashboards

- **DASH-01**: View application funnel (applied to interview to offer conversion rates)
- **DASH-02**: View application activity over time (weekly/monthly trends)
- **DASH-03**: View statistics by company, job type, and status

### AI Features

- **AI-01**: Analyze job description + CV and suggest CV adjustments
- **AI-02**: Generate or improve cover letters tailored to specific jobs

### Organization & Reminders

- **TAGS-01**: Tags and custom labels for applications
- **SALA-01**: Salary tracking and comparison across applications
- **RMND-01**: Follow-up reminders when no response after X days

### Data Import

- **SCRP-01**: URL scraping to auto-extract job posting details

## Out of Scope

Explicitly excluded. Documented to prevent scope creep.

| Feature | Reason |
|---------|--------|
| Job board integrations (LinkedIn, Indeed APIs) | APIs restricted/nonexistent for job seekers. High maintenance |
| In-app document editor | Massive effort. Google Docs/Word already exist. Upload-only approach |
| Browser extension | Separate codebase, high effort. Manual entry is acceptable |
| Email integration/parsing | Unreliable, privacy concerns, OAuth complexity |
| Real-time notifications/push | Single-user, self-entered data. Near-zero value |
| Mobile native app | Responsive web sufficient. PWA if needed later |
| Collaborative/sharing features | Single-user focus |
| Gamification (streaks, achievements) | Job search is stressful. Clean analytics is better |

## Traceability

Which phases cover which requirements. Updated during roadmap creation.

| Requirement | Phase | Status |
|-------------|-------|--------|
| AUTH-01 | Phase 2 | Complete |
| AUTH-02 | Phase 2 | Complete |
| AUTH-03 | Phase 2 | Complete |
| AUTH-04 | Phase 2 | Complete |
| COMP-01 | Phase 3 | Complete |
| COMP-02 | Phase 3 | Complete |
| COMP-03 | Phase 3 | Complete |
| JOBS-01 | Phase 3 | Complete |
| JOBS-02 | Phase 3 | Complete |
| JOBS-03 | Phase 3 | Complete |
| JOBS-04 | Phase 3 | Complete |
| APPL-01 | Phase 4 | Complete |
| APPL-02 | Phase 4 | Complete |
| APPL-03 | Phase 8 | Pending |
| APPL-04 | Phase 8 | Pending |
| APPL-05 | Phase 4 | Complete |
| APPL-06 | Phase 4 | Complete |
| APPL-07 | Phase 4 | Complete |
| INTV-01 | Phase 5 | Pending |
| INTV-02 | Phase 5 | Pending |
| INTV-03 | Phase 5 | Pending |
| INTV-04 | Phase 5 | Pending |
| DOCS-01 | Phase 6 | Pending |
| DOCS-02 | Phase 6 | Pending |
| DOCS-03 | Phase 6 | Pending |
| DOCS-04 | Phase 6 | Pending |
| DOCS-05 | Phase 6 | Pending |
| INFR-01 | Phase 1 | Complete |
| INFR-02 | Phase 1 | Complete |
| INFR-03 | Phase 1 | Complete |
| INFR-04 | Phase 1 | Complete |
| INFR-05 | Phase 7 | Pending |
| DEVX-01 | Phase 1 | Complete |
| DEVX-02 | Phase 1 | Complete |

**Coverage:**
- v1 requirements: 34 total
- Mapped to phases: 34
- Unmapped: 0

---
*Requirements defined: 2026-03-19*
*Last updated: 2026-03-19 after roadmap creation*
