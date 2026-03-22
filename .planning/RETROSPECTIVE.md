# Project Retrospective

*A living document updated after each milestone. Lessons feed forward into future planning.*

## Milestone: v1.0 — MVP

**Shipped:** 2026-03-22
**Phases:** 9 | **Plans:** 30 | **Timeline:** 4 days

### What Was Built
- Full-stack job application tracker: Kotlin/Spring Boot API + Next.js frontend
- 6-domain REST API with 31 endpoints (auth, companies, jobs, applications, interviews, documents)
- Kanban board with drag-and-drop and filterable list view for applications
- Document management with MinIO S3, versioning, and categorization
- Interview scheduling with round tracking and chronological timeline
- Responsive layout with fixed sidebar, mobile nav, dark/light theme

### What Worked
- Backend-first development strategy: building and testing the full API before any frontend work produced stable contracts that frontend phases consumed cleanly
- Wave-based parallel execution: Phase 8 and 9 plans executed in parallel within waves, halving execution time
- Gap closure pattern: Phase 8 used 7 iterative gap-closure plans (08-05 through 08-11) to polish UI issues found during verification -- small, focused fixes rather than one large rework plan
- Better Auth integration: using Better Auth with own PostgreSQL tables instead of custom JWT for frontend gave complete auth UI components out of the box

### What Was Inefficient
- Phase 2 backend auth endpoints (register, login, logout, password-reset) were built and tested but then bypassed entirely by Better Auth in Phase 7 -- the frontend never calls the Spring Boot auth controller
- SUMMARY.md files lacked `one_liner` and `requirements_completed` frontmatter fields, making automated accomplishment extraction impossible at milestone audit time
- Multiple STATE.md frontmatter blocks accumulated instead of being replaced cleanly, creating noise
- Interview notes hooks were fixed in Phase 9 but no component was created to use them -- the hook fix was correct but incomplete without a UI surface

### Patterns Established
- Branch-per-phase with PR review before merging to master
- Flyway migration naming: V{N}__{phaseNN}_{description}.sql
- BetterAuthSessionFilter + JWT filter chain ordering for dual auth support
- standardSchemaResolver for Zod v4 + react-hook-form compatibility
- Fixed sidebar + sticky topbar CSS pattern for kanban-safe scrolling
- CAST(:param AS type) IS NULL pattern for null-safe optional JPQL parameters

### Key Lessons
1. When frontend replaces backend auth (Better Auth vs JWT), design the switch early to avoid building throwaway code
2. Gap closure works best as many small plans (1-2 tasks each) rather than one large polish plan
3. Phase verifications should check end-to-end UI wiring, not just backend API + hook existence -- a hook with no component consumer is still a gap
4. Document version management and interview notes UI should have been in Phase 8 scope, not discovered as gaps in audit

### Cost Observations
- Model mix: primarily opus for execution, sonnet for verification/integration checks
- Sessions: ~10 sessions across 4 days
- Notable: parallel agent execution in waves cut Phase 8 and 9 execution time significantly

---

## Cross-Milestone Trends

### Process Evolution

| Milestone | Timeline | Phases | Key Change |
|-----------|----------|--------|------------|
| v1.0 | 4 days | 9 | Backend-first, gap closure iteration pattern |

### Cumulative Quality

| Milestone | Backend Tests | Frontend Tests | LOC |
|-----------|--------------|----------------|-----|
| v1.0 | Integration tests for all domains | None (visual verification) | ~18,300 |

### Top Lessons (Verified Across Milestones)

1. Backend-first development with stable API contracts reduces frontend integration issues
2. Iterative gap closure (small fix plans) is more efficient than large rework plans
