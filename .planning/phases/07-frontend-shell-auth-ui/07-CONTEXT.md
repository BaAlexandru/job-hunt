# Phase 7: Frontend Shell & Auth UI - Context

**Gathered:** 2026-03-20
**Status:** Ready for planning

<domain>
## Phase Boundary

A working Next.js frontend with authentication pages (register, login, email verification, password reset, logout), an API client layer that handles JWT tokens automatically, and a responsive layout shell with sidebar navigation. All nav links present with placeholder pages for features built in later phases. No CRUD feature pages — those are Phase 8.

</domain>

<decisions>
## Implementation Decisions

### Authentication approach
- Better Auth as the auth framework (replaces NextAuth.js — Auth.js v5 never left beta, absorbed by Better Auth Sep 2025)
- Wrap existing backend JWT API via Better Auth's custom provider/plugin system
- Access token managed client-side; refresh token via HTTP-only cookie from backend
- Full auth UI pages: login, register, email verification, password reset request, password reset confirm
- Unauthenticated users see a simple landing page with login/register buttons
- After login, user lands on a Dashboard / Home page (placeholder content in Phase 7)

### Layout & navigation
- Sidebar + top bar layout for authenticated users
- Sidebar contains nav links: Dashboard, Applications, Companies, Jobs, Documents
- Top bar with user info and logout action
- Sidebar collapses to hamburger slide-over menu on mobile
- All nav links present from Phase 7 — unbuilt feature pages show empty state placeholders ("Coming soon")
- Dark mode: respect OS/system preference automatically via Tailwind `dark:` variant, no manual toggle

### API client design
- Native fetch with a thin wrapper function (no axios/ky dependency)
- Wrapper handles: base URL (`http://localhost:8080/api`), auth headers, JSON parsing, error standardization
- 401 handling: auto-refresh via `/api/auth/refresh`, then retry the original request seamlessly
- TanStack Query set up in Phase 7 — provider configured, auth mutations use it from the start
- Establishes query/mutation patterns that Phase 8 builds on

### Styling & components
- shadcn/ui component library (Radix UI + Tailwind CSS)
- React Hook Form + Zod for form handling and validation
- Sonner toast notifications via shadcn/ui integration for success/error feedback
- Clean & minimal aesthetic — neutral defaults, subtle borders, light shadows, whitespace (Linear/Notion-inspired)
- pnpm as package manager (per frontend/CLAUDE.md)

### Claude's Discretion
- Next.js App Router vs Pages Router (App Router recommended for new projects)
- Exact Better Auth configuration and session strategy
- File/folder structure within frontend/
- Tailwind theme customization details
- TanStack Query default options (staleTime, retry)
- Loading skeleton and spinner designs
- Exact responsive breakpoints
- ESLint/Prettier configuration
- Whether to use next/font for font optimization

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` — Tech stack constraints (Next.js, TypeScript, TanStack Query, Tailwind CSS, shadcn/ui)
- `.planning/REQUIREMENTS.md` — INFR-05 (responsive web design)
- `.planning/ROADMAP.md` — Phase 7 success criteria (4 criteria)

### Backend auth API (integration target)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/AuthController.kt` — All auth endpoints (register, login, refresh, logout, verify, password-reset)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/SecurityConfig.kt` — CORS config (localhost:3000), security filter chain
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/JwtTokenProvider.kt` — JWT claims structure, token types
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/` — Auth request/response DTOs (AuthRequest, AuthResponse, RegisterRequest, etc.)

### Prior phase context
- `.planning/phases/02-authentication/02-CONTEXT.md` — Token strategy (access + refresh pair), refresh cookie details, endpoint security rules, CORS decisions

### Frontend conventions
- `frontend/CLAUDE.md` — API base URL, JWT integration notes, pnpm, not a Gradle subproject

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- No frontend code exists yet — Phase 7 initializes the entire frontend project
- Backend auth API is fully built and tested: register, login, refresh, logout, verify, password-reset endpoints
- CORS configured for `http://localhost:3000` with credentials enabled

### Established Patterns (from backend, to maintain consistency)
- Auth flow: POST /api/auth/login returns `{ accessToken, expiresIn, tokenType }` + sets `refresh_token` HTTP-only cookie
- Refresh: POST /api/auth/refresh reads cookie, returns new access token + rotated cookie
- Error responses: standardized `{ status, error, message, timestamp, path }` format from GlobalExceptionHandler
- Validation errors: `{ fieldErrors: { field: message } }` format

### Integration Points
- `compose.yaml`: Backend + PostgreSQL + Redis already running. Frontend runs standalone via `pnpm dev` on port 3000
- Backend CORS: Already allows `http://localhost:3000` with all methods and credentials
- JWT: Access token in Authorization header, refresh token in HTTP-only cookie (path: /api/auth/refresh)

</code_context>

<specifics>
## Specific Ideas

- User is learning Kotlin but this phase is TypeScript/React — different learning context
- Landing page should be simple — this is a personal tool, not a SaaS product
- Dashboard is placeholder in Phase 7 — real dashboard content comes when data APIs exist
- Linear/Notion aesthetic reference — clean, professional, not playful
- All navigation scaffolded even for unbuilt features — Phase 8 fills in the real pages

</specifics>

<deferred>
## Deferred Ideas

- Manual dark/light mode toggle — currently system-preference only. Could add toggle in a future iteration
- OAuth2 social login (Google, GitHub) — backend doesn't support it yet, would need backend changes
- PWA support — could add service worker and manifest later for mobile experience
- Real dashboard content (stats, charts, recent activity) — needs data from Phases 4-6 first
- NextAuth.js / Auth.js — originally considered but Auth.js v5 never left beta, absorbed by Better Auth Sep 2025

</deferred>

---

*Phase: 07-frontend-shell-auth-ui*
*Context gathered: 2026-03-20*
*Updated: 2026-03-20 — switched auth framework from NextAuth.js to Better Auth per user decision*
