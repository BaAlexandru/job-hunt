---
phase: 07-frontend-shell-auth-ui
plan: 03
subsystem: ui
tags: [next.js, react, shadcn-ui, responsive, sidebar, dashboard, better-auth-ui]

# Dependency graph
requires:
  - phase: 07-01
    provides: Next.js project, shadcn/ui components (button, sheet), cn utility, providers, auth config
  - phase: 07-02
    provides: EmptyState shared component, auth pages, landing page
provides:
  - Dashboard layout with sidebar + topbar
  - Responsive navigation (sidebar on desktop, Sheet hamburger on mobile)
  - 5 placeholder pages (Dashboard, Applications, Companies, Jobs, Documents)
  - UserButton integration for account actions
affects: [08-feature-pages]

# Tech tracking
tech-stack:
  added: []
  patterns: [route-group-layout, shared-navItems-array, controlled-sheet-state]

key-files:
  created:
    - frontend/app/(dashboard)/layout.tsx
    - frontend/components/layout/sidebar.tsx
    - frontend/components/layout/topbar.tsx
    - frontend/components/layout/mobile-nav.tsx
    - frontend/app/(dashboard)/dashboard/page.tsx
    - frontend/app/(dashboard)/applications/page.tsx
    - frontend/app/(dashboard)/companies/page.tsx
    - frontend/app/(dashboard)/jobs/page.tsx
    - frontend/app/(dashboard)/documents/page.tsx
  modified: []

key-decisions:
  - "Sidebar exports navItems array reused by MobileNav for single source of truth"
  - "Controlled Sheet state so mobile nav closes on link click"
  - "Route group (dashboard) for shared layout without URL prefix"

patterns-established:
  - "Route group pattern: (dashboard) groups authenticated pages under shared layout"
  - "Shared navItems: single array exported from sidebar, consumed by mobile-nav"
  - "Responsive nav: hidden md:flex for sidebar, md:hidden for hamburger"

requirements-completed: [INFR-05]

# Metrics
duration: 13min
completed: 2026-03-20
---

# Phase 7 Plan 3: Dashboard Layout Summary

**Responsive dashboard shell with sidebar navigation (5 items), UserButton topbar, hamburger Sheet for mobile, and 5 placeholder pages with UI-SPEC copywriting**

## Performance

- **Duration:** 13 min
- **Started:** 2026-03-20T13:25:38Z
- **Completed:** 2026-03-20T13:38:45Z
- **Tasks:** 1 of 1 auto tasks complete (Task 2 is human-verify checkpoint)
- **Files modified:** 9

## Accomplishments
- Sidebar with Dashboard, Applications, Companies, Jobs, Documents nav items with lucide-react icons
- Active nav item highlighted with bg-accent text-accent-foreground
- Topbar with UserButton from @daveyplate/better-auth-ui and mobile hamburger menu
- Mobile Sheet navigation reuses navItems from sidebar, closes on link click
- All 5 placeholder pages with exact UI-SPEC copywriting via EmptyState component

## Task Commits

Each task was committed atomically:

1. **Task 1: Create dashboard layout, sidebar, topbar, mobile nav, and all placeholder pages** - `e4f7c1b` (feat)

**Prerequisite commits (Plans 07-01 and 07-02 were not yet executed):**
- **Plan 07-01: Initialize Next.js frontend** - `2c0db89` (feat)
- **Plan 07-02: Auth pages, landing page, shared components** - `b49de03` (feat)
- **Cleanup: Remove .gitkeep, add favicon** - `b7d6bad` (chore)

## Files Created/Modified
- `frontend/components/layout/sidebar.tsx` - Desktop sidebar with 5 nav items, exports navItems array
- `frontend/components/layout/topbar.tsx` - Top bar with UserButton and mobile hamburger Sheet
- `frontend/components/layout/mobile-nav.tsx` - Mobile navigation reusing navItems with onNavigate callback
- `frontend/app/(dashboard)/layout.tsx` - Dashboard route group layout with Sidebar + Topbar
- `frontend/app/(dashboard)/dashboard/page.tsx` - "Welcome to JobHunt" empty state
- `frontend/app/(dashboard)/applications/page.tsx` - "No applications yet" empty state
- `frontend/app/(dashboard)/companies/page.tsx` - "No companies yet" empty state
- `frontend/app/(dashboard)/jobs/page.tsx` - "No job postings yet" empty state
- `frontend/app/(dashboard)/documents/page.tsx` - "No documents yet" empty state

## Decisions Made
- Sidebar exports `navItems` array so MobileNav can reuse it (single source of truth)
- Controlled Sheet state via useState so mobile nav closes when user clicks a link
- Route group `(dashboard)` used to share layout across all authenticated pages without affecting URL paths

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] Executed Plans 07-01 and 07-02 as prerequisites**
- **Found during:** Pre-execution check
- **Issue:** Plans 07-01 and 07-02 had not been executed -- frontend directory was empty (only CLAUDE.md)
- **Fix:** Executed both plans inline: initialized Next.js, installed all deps, configured shadcn/ui, Better Auth, providers, auth pages, landing page, shared components
- **Files modified:** 46 files across frontend/ and backend migration
- **Verification:** pnpm test passes (5/5), pnpm build succeeds
- **Committed in:** `2c0db89` (07-01), `b49de03` (07-02)

**2. [Rule 1 - Bug] Used V9 instead of V7 for Flyway migration**
- **Found during:** Task 1 (Plan 07-01)
- **Issue:** Plan specified V7 but V7 and V8 already exist (from Phase 04)
- **Fix:** Used V9__phase07_better_auth_tables.sql
- **Files modified:** backend/src/main/resources/db/migration/V9__phase07_better_auth_tables.sql
- **Verification:** No naming conflict
- **Committed in:** `2c0db89`

**3. [Rule 3 - Blocking] Installed radix-ui package**
- **Found during:** Build verification
- **Issue:** shadcn/ui v4 components import from `radix-ui` (v2 unified package) but it wasn't auto-installed
- **Fix:** Ran `pnpm add radix-ui`
- **Files modified:** frontend/package.json, frontend/pnpm-lock.yaml
- **Verification:** pnpm build succeeds
- **Committed in:** `2c0db89`

**4. [Rule 1 - Bug] Made landing page a client component for SignedIn/SignedOut**
- **Found during:** Plan 07-02 implementation
- **Issue:** SignedIn/SignedOut are client components that need React context; used client-side redirect instead of server-side redirect()
- **Fix:** Added "use client" directive and useEffect-based redirect for authenticated users
- **Files modified:** frontend/app/page.tsx
- **Verification:** pnpm build succeeds
- **Committed in:** `b49de03`

---

**Total deviations:** 4 auto-fixed (1 bug, 2 blocking, 1 bug)
**Impact on plan:** All fixes necessary for correctness and build success. No scope creep.

## Issues Encountered
- Windows permission errors when trying to `rm -rf node_modules` -- used `cmd.exe /c rmdir /s /q` as workaround
- shadcn/ui init required `--defaults` flag since interactive prompts don't work in headless CLI

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Complete frontend shell ready for Phase 8 feature page development
- All 5 dashboard routes are placeholder pages, ready to be replaced with real UI
- Auth flow (register, login, logout) functional via Better Auth UI
- Route protection via proxy.ts redirects unauthenticated users to /auth/login
- Human verification of full auth flow and responsive layout pending (Task 2 checkpoint)

## Self-Check: PASSED

All 9 created files verified on disk. All 4 commit hashes (e4f7c1b, b49de03, 2c0db89, b7d6bad) found in git log.

---
*Phase: 07-frontend-shell-auth-ui*
*Completed: 2026-03-20*
