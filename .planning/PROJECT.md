# JobHunt - Job Application Tracker

## What This Is

A full-stack web application for tracking job applications end-to-end — from discovering companies and positions to managing interviews, documents, and application status. Backend API in Kotlin/Spring Boot, frontend in Next.js with kanban board and responsive design. Designed for a single user initially but architected for multi-user.

## Core Value

The ONE thing that must work: tracking jobs you've applied to with their status, documents, and timeline — so nothing falls through the cracks during a job search.

## Requirements

### Validated

- ✓ User authentication (register, login, logout, sessions) — v1.0
- ✓ Company management (CRUD, list, detail) — v1.0
- ✓ Job posting management (CRUD, company link, description storage) — v1.0
- ✓ Application tracking with 8-status state machine — v1.0
- ✓ Kanban board with drag-and-drop status transitions — v1.0
- ✓ Sortable/filterable table/list view — v1.0
- ✓ Application search across companies, jobs, notes — v1.0
- ✓ Document upload/download (PDF/DOCX via MinIO S3) — v1.0
- ✓ Document linking to applications — v1.0
- ✓ Interview scheduling with rounds, types, locations — v1.0
- ✓ Interview round tracking (screening, technical, behavioral, final) — v1.0
- ✓ Chronological timeline per application — v1.0
- ✓ Responsive layout (sidebar, mobile hamburger, theme toggle) — v1.0
- ✓ Docker Compose local dev (PostgreSQL, Redis, MinIO) — v1.0
- ✓ Flyway-managed database schema (15 migrations) — v1.0
- ✓ Module-specific CLAUDE.md files and project skills — v1.0

### Active

<!-- v1.1: Infrastructure & Deployment -->
- [ ] Interview notes UI (backend complete, needs frontend component in InterviewsTab)
- [ ] Document version management UI (backend complete, needs version history panel)
- [ ] Password reset email delivery (Better Auth callback needs SMTP transport)
- [ ] Visibility & Sharing (private/public/shared companies and jobs)
- [ ] Production Docker images (multi-stage builds for backend + frontend)
- [ ] Self-managed K3s on AWS EC2 (t3.small, single cluster)
- [ ] Namespace-based staging/production separation
- [ ] ArgoCD + GitOps deployment pipeline (core-mode)
- [ ] Cloudflare DNS (job-hunt.dev) + proxy TLS + HTTPS-only
- [ ] Database and storage on K8s (PostgreSQL, Redis, MinIO)
- [ ] GitHub Actions CI + GHCR image registry

<!-- Deferred to v3 -->
<!-- - AI: Analyze job description + CV and suggest adjustments -->
<!-- - AI: Generate or improve cover letters tailored to specific jobs -->

### Out of Scope

- Real-time notifications/alerts — not needed for single-user MVP
- Mobile native app — responsive web sufficient, PWA if needed later
- Job board integrations (LinkedIn, Indeed APIs) — manual entry first
- In-app document editor — upload files only, no rich text editing
- Browser extension — separate codebase, manual entry acceptable
- Email integration/parsing — unreliable, privacy concerns
- Gamification — job search is stressful, clean analytics better
- AI features (CV analysis, cover letter generation) — deferred to v3

## Current Milestone: v1.1 Infrastructure & Deployment

**Goal:** Close v1.0 UI gaps, then build a full deployment pipeline with K3s on AWS EC2, ArgoCD GitOps, namespace-based staging/prod, and go live at job-hunt.dev.

**Domain:** job-hunt.dev (Cloudflare DNS, proxied, HTTPS-only)

**Target features:**
- v1.0 gap closure (interview notes UI, doc version UI, password reset email)
- Visibility & Sharing (private/public/shared on companies and jobs, share by email)
- Production-ready Docker images (multi-stage builds, <200MB each)
- K3s on AWS EC2 t3.small (single cluster, namespace-based staging/prod, staging scale-to-zero)
- ArgoCD core-mode + GitOps CI/CD pipeline
- Cloudflare proxy TLS + Origin CA cert (Full Strict mode)
- Data stores on K8s (PostgreSQL, Redis, MinIO with StatefulSets + backups)
- GitHub Actions CI + GHCR image registry

**Phases:** 9 phases (10-18), 4 parallelizable in wave 1

## Context

Shipped v1.0 MVP in 4 days (2026-03-19 to 2026-03-22).
Codebase: ~18,300 LOC (8k Kotlin, 10k TypeScript, 300 SQL).
Tech stack: Kotlin + Spring Boot 4.0.4, Next.js 16.2, PostgreSQL, Redis, MinIO.
212 git commits across 9 phases and 30 plans.
3 known gaps accepted as tech debt (interview notes UI, doc version UI, password reset email).

## Constraints

- **Tech Stack (Backend)**: Kotlin + Spring Boot 4.0.4, Spring Security + JWT + Better Auth session filter, Spring Data JPA + PostgreSQL, Flyway migrations
- **Tech Stack (Frontend)**: React 19 + TypeScript, Next.js 16.2 (Turbopack), TanStack Query, Tailwind CSS 4.0 / shadcn/ui, Better Auth, Zod v4
- **Tech Stack (AI)**: Spring AI with flexible provider abstraction (deferred to v2)
- **Infrastructure**: Docker Compose for local dev (PostgreSQL, Redis, MinIO in containers)
- **Architecture**: Monorepo (`/backend`, `/frontend`, `/infra`), multi-user ready

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Kotlin over Java | Modern language, learning opportunity, Spring Boot support | ✓ Good |
| Spring Boot 4.0.4 | Access to latest Spring AI, accept milestone risks | ✓ Good |
| Better Auth over custom JWT for frontend | Own DB tables, full auth UI components, session cookies | ✓ Good |
| Backend-first development | Build and test API before frontend, stable contracts | ✓ Good |
| No API versioning | Monorepo lockstep evolution, flat `/api/` prefix | ✓ Good |
| MinIO for document storage | S3-compatible, local dev via Docker, production-ready | ✓ Good |
| Dice UI Kanban composables | Drag validation, column dimming, click vs drag discrimination | ✓ Good |
| Fixed sidebar + sticky topbar | Prevents horizontal scroll displacement on kanban | ✓ Good |
| BetterAuthSessionFilter before JWT filter | Cookie auth first, JWT fallback for API testing | ✓ Good |
| standardSchemaResolver for forms | Zod v4 incompatible with zodResolver, Standard Schema works | ✓ Good |
| Phase 6.1 moved to v1.1 Phase 11 | Visibility & Sharing split into own phase with VISI-01..05 requirements | ✓ Good |
| Staging scale-to-zero | t3.small (2GB) can't run two full environments; staging defaults to replicas=0 | -- Pending |
| Self-managed K8s over EKS | EKS control plane ~$73/mo, self-managed on free-tier EC2 much cheaper | -- Pending |
| ArgoCD + GitOps over GitHub Actions | K8s-native GitOps, better learning opportunity, production-grade pattern | -- Pending |
| Namespace separation over separate clusters | Research: free-tier can't run two clusters, namespace isolation sufficient for single-dev | -- Pending |
| Cloudflare proxy over cert-manager | Free CDN + DDoS + TLS termination, Origin CA for backend encryption | -- Pending |
| K3s over kubeadm | Lightweight, single binary, bundles Traefik + CoreDNS, 512MB minimum | -- Pending |
| AI deferred to v3 | Infrastructure is higher priority, AI features need stable deployment first | -- Pending |

---
*Last updated: 2026-03-22 after v1.1 audit — phases renumbered 10-18, GAP-04 split to Phase 11, memory mitigation documented*
