# Milestones

## v1.0 MVP (Shipped: 2026-03-22)

**Phases completed:** 9 phases, 30 plans
**Timeline:** 4 days (2026-03-19 to 2026-03-22)
**Commits:** 212
**Codebase:** 8,026 LOC Kotlin + 10,027 LOC TypeScript + 289 LOC SQL = ~18,342 LOC total
**Git range:** 7cec499..979258a

**Key accomplishments:**
1. Full-stack job application tracker with Kotlin/Spring Boot backend and Next.js frontend
2. 6-domain REST API: authentication, companies, jobs, applications, interviews, documents
3. Kanban board with drag-and-drop status transitions and sortable/filterable list view
4. Document management with MinIO S3 storage, versioning, and categorization
5. Interview scheduling with round tracking, notes, and chronological timeline
6. Responsive design with fixed sidebar, mobile hamburger nav, and theme toggle

**Known Gaps (accepted as tech debt):**
- INTV-03: Interview notes UI missing (backend + hooks complete, no component renders them)
- DOCS-03: Document version management UI missing (backend + hooks complete, no version UI)
- AUTH-04: Password reset email not delivered (console.log only, no email transport configured)

**Audit:** See milestones/v1.0-MILESTONE-AUDIT.md

---

