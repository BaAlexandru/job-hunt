---
phase: 07-frontend-shell-auth-ui
plan: 01
subsystem: ui
tags: [nextjs, better-auth, shadcn-ui, tailwind-v4, tanstack-query, vitest, postgresql, flyway]

requires:
  - phase: 04-application-tracking
    provides: Flyway migrations V1-V8, PostgreSQL schema
provides:
  - Next.js 16.2 project with App Router, TypeScript, Tailwind CSS v4
  - Better Auth server/client configuration with PostgreSQL
  - shadcn/ui component library (button, card, input, label, sonner, separator, avatar, dropdown-menu, sheet)
  - API client wrapper for backend calls (Phase 8+)
  - TanStack Query provider configuration
  - Vitest test infrastructure with passing api-client tests
  - Route protection via proxy.ts with session cookie check
  - Flyway V9 migration for Better Auth tables (user, session, account, verification)
affects: [07-02, 07-03, 08-frontend-features]

tech-stack:
  added: [next@16.2.0, react@19.2.4, better-auth@1.5.5, "@daveyplate/better-auth-ui@3.3.15", pg@8.20.0, "@tanstack/react-query@5.91.3", react-hook-form@7.71.2, zod@4.3.6, next-themes@0.4.6, sonner@2.0.7, lucide-react@0.577.0, shadcn@4.1.0, radix-ui@1.4.3, vitest@4.1.0, tailwindcss@4]
  patterns: [app-router, server-components, css-based-tailwind-config, oklch-colors, proxy-route-protection, session-cookie-auth]

key-files:
  created:
    - frontend/package.json
    - frontend/app/layout.tsx
    - frontend/app/globals.css
    - frontend/lib/auth.ts
    - frontend/lib/auth-client.ts
    - frontend/lib/api-client.ts
    - frontend/lib/query-client.ts
    - frontend/components/providers.tsx
    - frontend/app/api/auth/[...all]/route.ts
    - frontend/proxy.ts
    - frontend/vitest.config.ts
    - frontend/__tests__/lib/api-client.test.ts
    - backend/src/main/resources/db/migration/V9__phase07_better_auth_tables.sql
  modified:
    - frontend/CLAUDE.md

key-decisions:
  - "V9 migration (not V7) for Better Auth tables since V7-V8 already exist from Phase 4"
  - "TEXT primary keys for Better Auth tables (framework default, separate from backend UUID users table)"
  - "radix-ui package added to resolve shadcn v4 component imports"
  - "base-nova shadcn style with OKLCH neutral colors for Tailwind v4"

patterns-established:
  - "Better Auth server config in lib/auth.ts with pg Pool adapter"
  - "Better Auth client in lib/auth-client.ts with createAuthClient"
  - "Provider nesting: QueryClientProvider > AuthUIProvider > ThemeProvider"
  - "API route handler at app/api/auth/[...all]/route.ts for Better Auth"
  - "proxy.ts with getSessionCookie for route protection"
  - "apiClient fetch wrapper with credentials:include for backend calls"
  - "Vitest with jsdom environment and @vitejs/plugin-react"

requirements-completed: [INFR-05]

duration: 17min
completed: 2026-03-20
---

# Phase 7 Plan 01: Frontend Foundation Summary

**Next.js 16.2 with Better Auth (pg Pool + session cookies), shadcn/ui (base-nova/OKLCH), TanStack Query, Vitest, and Flyway V9 migration for auth tables**

## Performance

- **Duration:** 17 min
- **Started:** 2026-03-20T13:24:38Z
- **Completed:** 2026-03-20T13:41:16Z
- **Tasks:** 2
- **Files modified:** 43

## Accomplishments
- Next.js 16.2 project initialized with all Phase 7 dependencies (better-auth, shadcn/ui, TanStack Query, Vitest)
- Better Auth server/client configured with PostgreSQL connection, emailAndPassword enabled, nextCookies plugin
- shadcn/ui initialized with base-nova style, OKLCH colors, 13 UI components installed
- API client wrapper ready for Phase 8+ backend calls with credentials:include
- Vitest configured with 5 passing api-client tests
- Route protection via proxy.ts using getSessionCookie
- Flyway V9 migration creates Better Auth tables (user, session, account, verification)
- Provider tree: QueryClientProvider > AuthUIProvider (with viewPaths) > ThemeProvider

## Task Commits

Each task was committed atomically:

1. **Task 1+2: Initialize Next.js, install deps, configure all modules** - `2c0db89` (feat)

**Plan metadata:** pending (this summary commit)

## Files Created/Modified
- `frontend/package.json` - All Phase 7 dependencies, test scripts
- `frontend/app/layout.tsx` - Root layout with Inter font, Providers, Toaster, suppressHydrationWarning
- `frontend/app/globals.css` - Tailwind v4 CSS config with OKLCH theme variables
- `frontend/lib/auth.ts` - Better Auth server config with pg Pool, emailAndPassword
- `frontend/lib/auth-client.ts` - Better Auth client for React (createAuthClient)
- `frontend/lib/api-client.ts` - Fetch wrapper with ApiError class, credentials:include
- `frontend/lib/query-client.ts` - TanStack Query client factory with sensible defaults
- `frontend/lib/utils.ts` - cn() utility from shadcn/ui
- `frontend/components/providers.tsx` - Provider tree (QueryClient > AuthUI > Theme)
- `frontend/app/api/auth/[...all]/route.ts` - Better Auth catch-all API route
- `frontend/proxy.ts` - Route protection with getSessionCookie
- `frontend/vitest.config.ts` - Vitest with jsdom, React plugin, @ alias
- `frontend/__tests__/lib/api-client.test.ts` - 5 tests for apiClient wrapper
- `frontend/components/ui/` - 13 shadcn/ui components (button, card, input, etc.)
- `frontend/components.json` - shadcn/ui configuration (base-nova, neutral, OKLCH)
- `frontend/CLAUDE.md` - Updated with actual stack and conventions
- `backend/src/main/resources/db/migration/V9__phase07_better_auth_tables.sql` - 4 Better Auth tables + indexes

## Decisions Made
- **V9 migration number:** Plan specified V7 but V7-V8 already existed from Phase 4. Used V9 to avoid Flyway version conflict.
- **TEXT primary keys for Better Auth:** Kept framework default rather than converting to UUID. Phase 8 will map between Better Auth user table and backend users table.
- **radix-ui package:** shadcn v4 components import from `radix-ui` (not individual `@radix-ui/*` packages). Added as dependency to resolve build errors.
- **base-nova shadcn style:** create-next-app with shadcn selected base-nova style (neutral colors, OKLCH format) which aligns with Linear/Notion aesthetic from UI-SPEC.

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 1 - Bug] Migration version V9 instead of V7**
- **Found during:** Task 1 (Flyway migration creation)
- **Issue:** Plan specified V7__phase07_better_auth_tables.sql but V7 and V8 already exist from Phase 4
- **Fix:** Created V9__phase07_better_auth_tables.sql instead
- **Files modified:** backend/src/main/resources/db/migration/V9__phase07_better_auth_tables.sql
- **Verification:** No version conflict with existing migrations
- **Committed in:** 2c0db89

**2. [Rule 3 - Blocking] Added radix-ui package for shadcn v4 components**
- **Found during:** Task 1 (build verification)
- **Issue:** shadcn/ui v4 components import from `radix-ui` package which was not auto-installed
- **Fix:** Added `radix-ui` as dependency via `pnpm add radix-ui`
- **Files modified:** frontend/package.json, frontend/pnpm-lock.yaml
- **Verification:** `pnpm build` succeeds without module resolution errors
- **Committed in:** 2c0db89

---

**Total deviations:** 2 auto-fixed (1 bug, 1 blocking)
**Impact on plan:** Both fixes necessary for correctness. No scope creep.

## Issues Encountered
None beyond the auto-fixed deviations above.

## User Setup Required
None - no external service configuration required. PostgreSQL runs via Docker Compose (compose.yaml). .env.local uses dev defaults.

## Next Phase Readiness
- Auth pages (Plan 02) can use AuthView with configured AuthUIProvider and viewPaths
- Dashboard layout (Plan 03) can use shadcn/ui components and provider tree
- All imports and providers resolve correctly
- Build and tests pass

## Self-Check: PASSED

All key files verified present. Commit 2c0db89 verified in git history. `pnpm build` and `pnpm test` both succeed.

---
*Phase: 07-frontend-shell-auth-ui*
*Completed: 2026-03-20*
