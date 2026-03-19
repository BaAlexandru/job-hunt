# JobHunt - Job Application Tracker

## What This Is

A personal web application for tracking job applications end-to-end — from discovering companies and positions to managing CVs, cover letters, and application statuses. Designed for a single user initially but architected to support multiple users later. Includes AI-powered features for CV optimization and cover letter generation based on job descriptions.

## Core Value

The ONE thing that must work: tracking jobs you've applied to with their status, documents, and timeline — so nothing falls through the cracks during a job search.

## Requirements

### Validated

(None yet — ship to validate)

### Active

- [ ] User authentication (register/login with JWT)
- [ ] Company management (add/edit companies of interest)
- [ ] Job posting management (add jobs with descriptions, URLs, company link)
- [ ] Application tracking with status flow (Interested → Applied → Interview → Offer → Rejected/Accepted)
- [ ] Dual view: table/list view AND kanban board for application status
- [ ] Document management: upload CVs, cover letters, other docs (PDF/DOCX)
- [ ] Link documents to specific job applications (which CV, which cover letter was used)
- [ ] Track application dates and timeline
- [ ] AI: Analyze company + job description + CV and suggest CV adjustments
- [ ] AI: Generate or improve cover letters tailored to specific jobs
- [ ] URL scraping for job posting details (v2 — start with manual entry)

### Out of Scope

- Real-time notifications/alerts — not needed for single-user MVP
- Mobile native app — web-first, responsive design sufficient
- Job board integrations (LinkedIn, Indeed APIs) — manual entry first
- Collaborative features (sharing job lists) — single user focus
- In-app document editor — upload files only, no rich text editing

## Context

- Personal project to support an active job search
- Developer wants to learn Kotlin alongside building something useful
- AI features are deliberately deferred to later phases — core tracking comes first
- Monorepo structure: `/backend`, `/frontend`, `/infra` in a single repository
- Nested CLAUDE.md files per module for specialized AI assistance
- Dedicated project-level skills for Claude Code

## Constraints

- **Tech Stack (Backend)**: Kotlin + Spring Boot 4.x, Spring Security + JWT, Spring Data JPA + PostgreSQL, Flyway migrations
- **Tech Stack (Frontend)**: React 18+ with TypeScript, Next.js (with Turbopack), TanStack Query, Tailwind CSS / shadcn/ui
- **Tech Stack (AI)**: Spring AI with flexible provider abstraction (Claude, OpenAI, swappable)
- **Infrastructure**: Docker + Docker Compose for local dev first, Kubernetes (Helm) for production later
- **Database**: PostgreSQL in container
- **Deployment**: Docker Compose locally first, K8s + optional Vercel later
- **Architecture**: Monorepo (`/backend`, `/frontend`, `/infra`), designed for eventual multi-user

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin over Java | More modern, learning opportunity, great Spring Boot support | — Pending |
| Next.js over Vite SPA | SSR capabilities, built-in routing, Turbopack bundler | — Pending |
| Monorepo structure | Simpler management, one PR for full features, split later if needed | — Pending |
| Flexible AI provider | Avoid vendor lock-in, swap between Claude/OpenAI as needed | — Pending |
| Upload-only documents | Simpler than building a rich text editor, covers the core need | — Pending |
| Docker Compose first | Get running locally fast, add K8s when ready to deploy | — Pending |
| Multi-user ready architecture | Design for one user but don't hardcode single-tenancy | — Pending |

---
*Last updated: 2026-03-19 after initialization*
