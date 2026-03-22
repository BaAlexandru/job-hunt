# Phase 8: Frontend Core Views - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Users interact with all backend features through polished frontend pages. Includes a kanban board with drag-and-drop status changes, a sortable/filterable table view, and full CRUD pages for companies, jobs, applications, interviews, and documents. All backend API endpoints from Phases 3-6 are surfaced in the frontend UI. Requirements APPL-03 (kanban board) and APPL-04 (sortable/filterable table) are delivered here.

</domain>

<decisions>
## Implementation Decisions

### Kanban board
- All 8 status columns visible with horizontal scroll if needed
- Compact cards: company name, job title, next action date (if set)
- Column headers show count badges (e.g., "Applied (3)")
- Invalid transitions prevented during drag — invalid columns dim/gray out, card snaps back
- Clicking a card opens a right-side slide-over detail panel (Linear-style)
- Top-level "+ New Application" button above the board
- Mobile: horizontal scroll through columns (swipe)
- Archived applications hidden by default with toggle to show

### List/table view
- Essential columns: Company, Job Title, Status (badge), Applied Date, Next Action Date, Last Activity
- Inline filter bar above table: Status dropdown, Company dropdown, Date range, Search input (GitHub issues style)
- Clicking a row opens the same slide-over detail panel as kanban
- Inline status dropdown in each row for quick status changes (shows valid transitions only)

### Application detail panel
- Right-side slide-over panel used from both kanban and list views (one shared component)
- Tabbed content inside panel: Overview | Notes | Interviews | Documents
- Overview tab: application metadata, status, dates, quick notes field
- Notes tab: full notes log with add/edit/delete
- Interviews tab: interview rounds with notes
- Documents tab: linked documents with upload capability

### CRUD page patterns
- Create/edit forms appear as centered modal dialogs (shadcn/ui Dialog)
- Delete actions require confirmation dialog before archive/soft-delete
- Companies page: card grid layout (company name, website, location, job count)
- Jobs page: standalone page showing all jobs with company filter
- Jobs also visible within company detail (both access paths)
- Documents page: drag-and-drop upload zone + file browser
- Interviews: managed from within application detail panel (Interviews tab)

### Page structure & navigation
- Applications page: tab bar to toggle between "Board" (kanban) and "List" (table) views
- Dashboard: summary cards with key metrics (total applications, by-status breakdown, upcoming interviews, recent activity)
- Existing sidebar nav links (Dashboard, Applications, Companies, Jobs, Documents) already scaffolded
- View preference (Board vs List) remembered across sessions

### Claude's Discretion
- Drag-and-drop library choice (dnd-kit, @hello-pangea/dnd, or similar)
- Table library choice (TanStack Table or manual implementation)
- Exact card styling and column widths on kanban
- Dashboard chart/metric component details
- Loading states and skeleton designs
- Pagination approach for list view (infinite scroll vs numbered pages)
- Form field ordering and layout within modals
- Date picker component choice
- File upload library for drag-and-drop zone
- How "remember view preference" is persisted (localStorage, cookie, etc.)

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project setup
- `.planning/PROJECT.md` — Tech stack constraints (Next.js, TypeScript, TanStack Query, Tailwind CSS, shadcn/ui)
- `.planning/REQUIREMENTS.md` — APPL-03 (kanban board), APPL-04 (sortable/filterable table)
- `.planning/ROADMAP.md` — Phase 8 success criteria (4 criteria) and dependencies (Phases 3-7)

### Frontend conventions
- `frontend/CLAUDE.md` — Stack details (Next.js 16.2, Tailwind v4, Better Auth, pnpm, shadcn/ui base-nova style)

### Prior phase context
- `.planning/phases/07-frontend-shell-auth-ui/07-CONTEXT.md` — Auth approach (Better Auth), layout shell, API client design, styling decisions (Linear/Notion aesthetic)
- `.planning/phases/04-application-tracking/04-CONTEXT.md` — 8-status state machine, transition rules, notes log, search/filter spec, date tracking

### Backend API endpoints (integration targets)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/CompanyController.kt` — Company CRUD endpoints
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/JobController.kt` — Job CRUD endpoints
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/ApplicationController.kt` — Application CRUD, status transitions, search/filter
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/ApplicationNoteController.kt` — Application notes CRUD
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/InterviewController.kt` — Interview CRUD
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/InterviewNoteController.kt` — Interview notes CRUD
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/TimelineController.kt` — Application timeline
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/DocumentController.kt` — Document upload/download/CRUD

### Existing frontend code
- `frontend/lib/api-client.ts` — API wrapper with credentials: "include"
- `frontend/lib/auth-client.ts` — Better Auth client
- `frontend/components/shared/empty-state.tsx` — EmptyState component (reusable)
- `frontend/components/ui/` — shadcn/ui components: card, button, input, select, tabs, sheet, dropdown-menu, checkbox, tooltip, avatar, separator, sonner
- `frontend/components/layout/sidebar.tsx` — Sidebar with navItems array
- `frontend/components/providers.tsx` — QueryClientProvider, AuthUIProvider, ThemeProvider

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Card` component (shadcn/ui): Available for kanban cards and company grid
- `Tabs` component (shadcn/ui): Available for Board/List toggle and detail panel sections
- `Sheet` component (shadcn/ui): Available for slide-over detail panel (already used for mobile nav)
- `DropdownMenu` component (shadcn/ui): Available for inline status change dropdowns
- `Select`, `Input`, `Button`, `Checkbox`: Form building blocks for filters and modals
- `EmptyState` component: Reusable for empty pages/sections
- `apiClient`: Thin fetch wrapper for all backend API calls
- `Sonner` toast: Already set up for success/error notifications
- TanStack Query: Provider configured, ready for data fetching patterns

### Established Patterns
- App Router with (dashboard) route group for authenticated layout
- shadcn/ui base-nova style with OKLCH colors
- Tailwind v4 CSS-based config (no tailwind.config.ts)
- Linear/Notion aesthetic: clean, neutral, subtle borders, whitespace
- Sidebar navigation with navItems array (single source of truth for nav)
- Better Auth session cookies for authentication (credentials: "include" on API calls)

### Integration Points
- Placeholder pages exist at: applications, companies, jobs, documents (currently show EmptyState)
- Dashboard page exists with placeholder content
- All sidebar nav links already wired
- Backend CORS allows localhost:3000 with credentials
- Backend auth needs updating to accept Better Auth sessions (noted in Phase 7 context as Phase 8 concern)

</code_context>

<specifics>
## Specific Ideas

- Linear-style slide-over panel for application details — kanban stays visible underneath
- GitHub issues-style inline filter bar for the list view
- Compact kanban cards — focus on scannability, click for details
- Both access paths to jobs: standalone Jobs page AND within company detail
- Dashboard as a real landing page with metrics, not just a redirect

</specifics>

<deferred>
## Deferred Ideas

- Analytics dashboard with charts (funnel, trends) — v2 requirement (DASH-01, DASH-02, DASH-03)
- Tags and custom labels on applications — v2 (TAGS-01)
- Salary comparison view — v2 (SALA-01)
- Follow-up reminders — v2 (RMND-01)
- Dark/light mode manual toggle — currently system-preference only (noted in Phase 7)
- Keyboard shortcuts for kanban (arrow keys to navigate, shortcuts to change status)

</deferred>

---

*Phase: 08-frontend-core-views*
*Context gathered: 2026-03-21*
