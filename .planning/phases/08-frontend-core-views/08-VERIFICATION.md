---
phase: 08-frontend-core-views
verified: 2026-03-21T16:00:00Z
status: gaps_found
score: 33/33 must-haves verified (code-level), 8 gaps found during manual UAT
re_verification:
  previous_status: human_needed
  previous_score: 21/21
  gaps_closed:
    - "Theme toggle (light/dark) is now present in topbar via ThemeToggle component"
    - "Dialog content has internal scroll (max-h-[85vh] overflow-y-auto) for long forms on mobile"
    - "Tapping outside a modal dialog no longer closes it (onInteractOutside e.preventDefault)"
    - "Auth forms constrained to max-w-md (~448px) on desktop"
    - "Dark theme contrast improved — cards (0.235), borders (15%), inputs (20%), sidebar (0.22)"
    - "App layout responsive from mobile to desktop with p-3 sm:p-6 main padding"
    - "Data tables wrapped in overflow-x-auto for horizontal scroll on mobile"
    - "All page headers stack vertically on mobile (flex-col sm:flex-row pattern)"
    - "Sheet detail panel is 92% width on mobile and max-w-lg on desktop"
    - "Filter bar double padding removed (px-6 gone from filter-bar.tsx)"
    - "Kanban cards clickable via distance-based sensor activation constraints (distance:5 mouse, delay:200 touch)"
    - "Kanban board columns narrowed to 240px on mobile (sm:w-[280px] on desktop)"
    - "Dashboard metric cards use 2-column grid on mobile with tighter spacing"
  gaps_remaining:
    - "Dialog X close button scrolls away on long forms — needs sticky header or fixed position"
    - "Dialog auto-opens keyboard on mobile edit — should not auto-focus first input field"
    - "Recent activity shows negative minutes (-2761 m ago) — time formatting broken, needs hours/days conversion"
    - "Kanban board in sheet panel only shows first 2 columns, cannot scroll horizontally; card opens partially"
    - "Status dropdown in data table blocks horizontal scroll; status text truncated"
    - "Sidebar not fixed on desktop horizontal scroll; disappears when navigating from dashboard card"
    - "Dashboard card click redirects to /applications page instead of opening detail panel on current board"
    - "Notes tab crashed with notes?.slice is not a function (FIXED in e7bffeb — paginated response extraction)"
  regressions: []
gaps:
  - id: GAP-09
    truth: "Dialog close button remains accessible when scrolling long forms"
    status: failed
    severity: medium
    test: "Dialog mobile behavior"
    description: "The X close button is absolute-positioned inside the overflow-y-auto container. On long forms (job creation, 15+ fields), scrolling down hides the X button. Fix: make close button sticky or add a non-scrolling header wrapper."
    artifacts:
      - path: "frontend/components/ui/dialog.tsx"
        issue: "Close button is absolute inside scrollable container — scrolls away on long content"
  - id: GAP-10
    truth: "Dialogs do not auto-focus input fields on mobile to avoid keyboard popup"
    status: failed
    severity: medium
    test: "Dialog mobile behavior"
    description: "When opening an edit dialog on mobile, the keyboard automatically opens because the first input field is auto-focused. User wants to see the form content first before deciding what to edit."
    artifacts:
      - path: "frontend/components/ui/dialog.tsx"
        issue: "No autoFocus prevention — Radix dialog focuses first focusable element by default"
  - id: GAP-11
    truth: "Recent activity timestamps display correctly as minutes, hours, or days ago"
    status: failed
    severity: medium
    test: "Dashboard metrics accuracy"
    description: "Recent activity shows '-2761 m ago' instead of proper time formatting. Need to: handle negative values, convert 60+ minutes to hours, convert 24+ hours to days."
    artifacts:
      - path: "frontend/app/(dashboard)/dashboard/page.tsx"
        issue: "Time formatting function produces negative values and doesn't convert to hours/days"
  - id: GAP-12
    truth: "Kanban board is fully scrollable horizontally inside the sheet detail panel"
    status: failed
    severity: high
    test: "Sheet panel width and kanban scroll"
    description: "When the application board is shown inside the sheet panel, only the first 2 columns are visible. Users cannot scroll left/right within the sheet to see other columns. The burger menu and theme toggle remain fixed at edges during horizontal scroll. Opening a card in the sheet only shows it partially with no scroll."
    artifacts:
      - path: "frontend/components/applications/application-board.tsx"
        issue: "Kanban board container not scrollable when rendered inside sheet overlay"
      - path: "frontend/components/ui/sheet.tsx"
        issue: "Sheet content doesn't handle internal horizontal overflow for wide content like kanban boards"
  - id: GAP-13
    truth: "Status dropdown in data table does not block horizontal scroll; full status text visible"
    status: failed
    severity: low
    test: "Data table scroll behavior"
    description: "When clicking a status cell in the data table, a dropdown opens that blocks horizontal scrolling until dismissed. The dropdown also truncates status text (e.g., 'Interested Inte...'). Need wider dropdown or text wrapping."
    artifacts:
      - path: "frontend/components/applications/application-list.tsx"
        issue: "Inline status dropdown blocks table scroll and truncates status labels"
  - id: GAP-14
    truth: "Sidebar remains visible when scrolling horizontally on desktop"
    status: failed
    severity: medium
    test: "Navigation stability"
    description: "On desktop, horizontal scroll (e.g., on wide kanban board) causes the sidebar to scroll off-screen. Sidebar should be fixed/sticky so users can always navigate. Same issue on mobile — clicking a dashboard card redirects to /applications but hides navigation."
    artifacts:
      - path: "frontend/components/layout/sidebar.tsx"
        issue: "Sidebar not position-fixed; scrolls with page content on horizontal scroll"
      - path: "frontend/app/(dashboard)/layout.tsx"
        issue: "Layout doesn't prevent sidebar from scrolling with main content"
  - id: GAP-15
    truth: "Clicking a recent activity card on dashboard opens that application's detail panel"
    status: failed
    severity: medium
    test: "Dashboard navigation"
    description: "Clicking a card in the dashboard recent activity section redirects to /applications page without opening the specific card. Expected: redirect to /applications and auto-open the detail panel for that application, or open the detail panel inline."
    artifacts:
      - path: "frontend/app/(dashboard)/dashboard/page.tsx"
        issue: "Recent activity links navigate to /applications without opening the specific application detail"
human_verification:
  - test: "Kanban drag-and-drop status transition (APPL-03)"
    expected: "Dragging a card to a valid column moves the application to that status with a success toast. Dragging to an invalid column dims that column, card snaps back, and toast shows 'Cannot move to that status from the current one.'"
    why_human: "Drag-and-drop interaction and CSS opacity dimming cannot be verified programmatically"
  - test: "Kanban card click-to-open detail panel (APPL-03)"
    expected: "Clicking (not dragging) a kanban card opens the application detail slide-over panel. Dragging 5px+ still activates drag-and-drop. Touch: quick tap opens panel, long press (200ms) activates drag."
    why_human: "Distance activation constraint behavior and distinguishing click from drag requires live browser interaction"
  - test: "Board/List view toggle persistence (APPL-03, APPL-04)"
    expected: "Selecting 'List' tab then refreshing the page keeps the List view active (localStorage persists across page loads)"
    why_human: "localStorage persistence across page reload requires browser interaction"
  - test: "Application detail slide-over panel tabs (APPL-03, APPL-04)"
    expected: "Clicking a kanban card or list row opens the Sheet from the right at 92% width on mobile / max-w-lg on desktop with 4 functional tabs: Overview, Notes, Interviews, Documents"
    why_human: "Sheet slide-over animation, width, and tab content rendering require visual verification"
  - test: "Theme toggle light/dark switching"
    expected: "Clicking the Sun/Moon button in topbar toggles theme. Dark mode has visible card borders, distinct card surfaces from background, visible input outlines. Light mode unaffected."
    why_human: "Visual contrast and theme switching require browser verification"
  - test: "Dialog mobile behavior — internal scroll and no overlay close"
    expected: "Opening a job creation form (15+ fields) on mobile: dialog is capped at 85vh and scrolls internally. Tapping outside the dialog does NOT close it — user must press X or Cancel button."
    why_human: "Overflow and tap-outside interaction require physical or emulated mobile browser testing"
  - test: "Mobile layout responsiveness at 320px"
    expected: "All 5 pages render without horizontal overflow at 320px width. Page headers stack vertically. Buttons go full-width. Tables scroll horizontally. Kanban scrolls horizontally with 240px columns."
    why_human: "Visual layout and overflow behavior require browser resize testing"
  - test: "New Better Auth user auto-provisioning end-to-end"
    expected: "Register a brand new user via the frontend auth UI. Navigate to /dashboard — should show 'Welcome to JobHunt' empty state, not an error. Backend logs contain 'Auto-provisioned backend user for Better Auth email: ...' entry."
    why_human: "Auto-provisioning requires a live registration flow with backend running. Cannot verify DB INSERT side-effect without end-to-end test."
  - test: "Document drag-and-drop upload"
    expected: "Dropping a PDF or DOCX onto the upload zone uploads the file, shows spinner during upload, displays success toast, and file appears in the list immediately"
    why_human: "File drag-and-drop interaction and upload feedback require browser testing"
  - test: "Dashboard metrics accuracy"
    expected: "Dashboard correctly shows total applications count, active count, offer count, status breakdown, and recent activity list reflecting real data from the backend"
    why_human: "Metric aggregation correctness requires live data from backend to verify"
---

# Phase 8: Frontend Core Views Verification Report

**Phase Goal:** Build dashboard, application tracking (kanban + list), company management, job board, and document management views in Next.js
**Verified:** 2026-03-21T16:00:00Z
**Status:** human_needed
**Re-verification:** Yes — after gap closure plans 08-06, 08-07, 08-08

## Re-verification Summary

| Item | Previous (post-05) | Current (post-08) |
|------|--------------------|-------------------|
| Overall status | human_needed | human_needed |
| Score | 21/21 | 33/33 (12 new truths from plans 06-08) |
| Gaps closed | — | 13 UAT issues resolved at code level |
| Gaps remaining | 0 | 0 |
| Regressions | — | 0 |

All 13 UAT-identified fixes from plans 08-06, 08-07, and 08-08 are confirmed in the codebase. The 21 truths verified after plan 08-05 continue to pass. No regressions detected.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Backend SecurityConfig updated to validate Better Auth session cookies | VERIFIED | `BetterAuthSessionFilter.kt` (127 lines) registered via `addFilterBefore` in `SecurityConfig.kt` |
| 2 | TypeScript types exist mirroring all backend DTOs | VERIFIED | `types/api.ts` (340 lines) exports 12 interfaces + `STATUS_TRANSITIONS`, `STATUS_COLORS`, `isValidTransition` |
| 3 | TanStack Query hooks exist for all 5 entity domains | VERIFIED | `use-applications.ts`, `use-companies.ts`, `use-jobs.ts`, `use-interviews.ts`, `use-documents.ts` all export CRUD + query hooks |
| 4 | All required shadcn/ui components installed | VERIFIED | `dialog.tsx`, `badge.tsx`, `table.tsx`, `sheet.tsx`, `kanban.tsx`, `skeleton.tsx` et al present in `components/ui/` |
| 5 | Shared reusable components exist | VERIFIED | `confirm-dialog.tsx`, `data-table.tsx`, `status-badge.tsx` all present and substantive |
| 6 | apiClient handles FormData uploads without Content-Type collision | VERIFIED | `api-client.ts`: `!(options.body instanceof FormData) && { "Content-Type": "application/json" }` |
| 7 | User can view applications as kanban board with 8 status columns and drag-and-drop | VERIFIED | `application-board.tsx` (235 lines) uses `KanbanBoard`, `KanbanColumn`, groups by all 8 `APPLICATION_STATUSES`, calls `useUpdateApplicationStatus`, checks `isValidTransition` |
| 8 | Invalid status transitions are prevented during drag (invalid columns dim) | VERIFIED | `application-board.tsx`: `opacity-40 pointer-events-none` applied via `isDimmed` flag during drag |
| 9 | User can view applications as sortable, filterable table/list | VERIFIED | `application-list.tsx` uses `DataTable`, `ColumnDef`, `STATUS_TRANSITIONS`, `FilterBar` integrated |
| 10 | Clicking a card/row opens slide-over detail panel with 4 tabs | VERIFIED | `application-detail.tsx` (905 lines): Sheet with Overview, Notes, Interviews, Documents tabs |
| 11 | User can create new applications via modal dialog | VERIFIED | `application-form.tsx` (323 lines): Dialog, `standardSchemaResolver`, job combobox, date pickers |
| 12 | User can toggle between Board and List views with preference remembered | VERIFIED | `applications/page.tsx`: `useViewPreference("jobhunt:applications-view", "board")`; `use-view-preference.ts` uses `localStorage` with SSR guard |
| 13 | User can view companies in a card grid | VERIFIED | `companies/page.tsx`: `useCompanies`, `useJobs`, `jobCountMap`; `company-card.tsx` renders name, Briefcase, DropdownMenu |
| 14 | User can create, edit, and delete companies via modal dialogs | VERIFIED | `company-form.tsx` (176 lines): Dialog + `useCreateCompany`; delete uses `ConfirmDialog` |
| 15 | User can view a company's details and linked jobs | VERIFIED | `companies/[id]/page.tsx`: calls `useCompany(id)` and `useJobs({ companyId })` |
| 16 | User can view all jobs in a filterable table | VERIFIED | `jobs/page.tsx`: `useJobs`, `useCompanies`; `job-list.tsx` uses `DataTable` with `companyName` column |
| 17 | User can upload PDF and DOCX documents via drag-and-drop | VERIFIED | `document-upload.tsx` (80 lines): `useDropzone` wired to `useUploadDocument` |
| 18 | User can see dashboard summary metrics | VERIFIED | `dashboard/page.tsx` (218 lines): `useApplications({ size: 1000 })`, 4 metric cards, `StatusBadge`, recent activity |
| 19 | New Better Auth user auto-provisioned in backend users table on first request | VERIFIED | `BetterAuthSessionFilter.kt` lines 85-101: catches `EmptyResultDataAccessException`, executes `INSERT INTO users` |
| 20 | Error states use muted styling (not destructive red) | VERIFIED | All 5 dashboard pages use `text-muted-foreground` for error paths |
| 21 | User can toggle between light and dark theme via topbar button | VERIFIED | `theme-toggle.tsx` (22 lines): `useTheme`, `setTheme(theme === "dark" ? "light" : "dark")`, Sun/Moon icons; `topbar.tsx` line 30: `<ThemeToggle />` |
| 22 | Modal dialogs have internal scroll for long content on mobile | VERIFIED | `dialog.tsx` line 69: `max-h-[85vh] overflow-y-auto` in `DialogContent` className |
| 23 | Tapping outside a modal dialog does not close it | VERIFIED | `dialog.tsx` lines 63-67: `onInteractOutside={(e) => { e.preventDefault() }}` |
| 24 | Auth forms constrained to ~448px max-width centered on desktop | VERIFIED | `auth/[path]/page.tsx` lines 17-19: `<div className="w-full max-w-md">` wraps `<AuthView>` |
| 25 | Dark theme has improved contrast (cards, borders, inputs, sidebar) | VERIFIED | `globals.css`: `--card: oklch(0.235 0 0)`, `--border: oklch(1 0 0 / 15%)`, `--input: oklch(1 0 0 / 20%)`, `--muted: oklch(0.30 0 0)`, `--sidebar: oklch(0.22 0 0)` |
| 26 | App layout padding reduces on mobile (12px mobile, 24px desktop) | VERIFIED | `(dashboard)/layout.tsx` line 14: `<main className="flex-1 overflow-auto p-3 sm:p-6">` |
| 27 | Data tables scroll horizontally on mobile instead of overflowing | VERIFIED | `data-table.tsx` lines 58-118: `<div className="overflow-x-auto">` wraps `<Table>` |
| 28 | All page headers stack vertically on mobile with full-width buttons | VERIFIED | `applications/page.tsx` line 43: `flex-col gap-3 sm:flex-row sm:items-center sm:justify-between`; same pattern confirmed in `companies`, `jobs`, `documents` pages |
| 29 | Sheet detail panel is full-width on mobile, capped at sm:max-w-lg on desktop | VERIFIED | `sheet.tsx` line 65: `data-[side=right]:w-[92%]` and `data-[side=right]:sm:max-w-lg` (same for left-side) |
| 30 | Filter bar has no double padding | VERIFIED | `filter-bar.tsx` container: `flex flex-wrap items-center gap-4 py-3` — no `px-6` present |
| 31 | Clicking (not dragging) a kanban card opens the application detail panel | VERIFIED | `application-board.tsx` lines 57-65: `useSensors` with `activationConstraint: { distance: 5 }` (mouse) and `{ delay: 200, tolerance: 5 }` (touch); `sensors={sensors}` passed to `<Kanban>`; `ApplicationCard` has `onClick={() => onSelectApplication(app.id)}` |
| 32 | Dragging a kanban card still works for status transitions | VERIFIED | `KanbanItem` retains `asHandle`; `handleValueChange` still calls `updateStatus.mutate`; sensors fire after 5px movement |
| 33 | Dashboard metric cards and kanban board optimized for mobile | VERIFIED | `dashboard/page.tsx` line 136: `grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4`; `application-board.tsx` line 193: `w-[240px] sm:w-[280px]`; board container line 173: `p-2 sm:p-4` |

**Score:** 33/33 truths verified (code-level)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `frontend/components/layout/theme-toggle.tsx` | Sun/Moon toggle using useTheme | VERIFIED | 22 lines; `useTheme`, `setTheme`, Button `className="relative"`, absolute Moon icon overlay |
| `frontend/components/layout/topbar.tsx` | Topbar with ThemeToggle before UserButton | VERIFIED | Imports `ThemeToggle`; line 30: `<ThemeToggle />` then `<UserButton />` inside `flex items-center gap-2` |
| `frontend/components/ui/dialog.tsx` | DialogContent with onInteractOutside and max-h-[85vh] | VERIFIED | Lines 63-67: `onInteractOutside` with `e.preventDefault()`; line 69: `max-h-[85vh] overflow-y-auto`; no `scrollIntoView` present |
| `frontend/app/auth/[path]/page.tsx` | Auth page with max-w-md constraint | VERIFIED | `<div className="w-full max-w-md">` wraps `<AuthView>` |
| `frontend/app/globals.css` | Improved dark theme contrast variables | VERIFIED | `--card: oklch(0.235)`, `--border: 15%`, `--input: 20%`, `--muted: oklch(0.30)`, `--sidebar: oklch(0.22)` |
| `frontend/app/(dashboard)/layout.tsx` | Responsive layout with p-3 sm:p-6 | VERIFIED | `main` element: `p-3 sm:p-6`; `Sidebar` hidden via `hidden md:flex` in sidebar.tsx |
| `frontend/components/shared/data-table.tsx` | DataTable with overflow-x-auto wrapper | VERIFIED | `<div className="overflow-x-auto">` wraps `<Table>` at line 58 |
| `frontend/app/(dashboard)/applications/page.tsx` | Responsive header with flex-col sm:flex-row | VERIFIED | Line 43: `flex-col gap-3 sm:flex-row sm:items-center sm:justify-between`; loading skeletons use `w-[240px] sm:w-[280px]` |
| `frontend/components/ui/sheet.tsx` | Sheet with w-[92%] mobile, sm:max-w-lg desktop | VERIFIED | `data-[side=right]:w-[92%]` and `data-[side=right]:sm:max-w-lg` on line 65 |
| `frontend/app/(dashboard)/companies/page.tsx` | Responsive header + w-full sm:w-auto button | VERIFIED | Line 80: responsive header; line 82: `w-full sm:w-auto` button |
| `frontend/app/(dashboard)/jobs/page.tsx` | Responsive header + w-full sm:w-[200px] select | VERIFIED | Line 90: responsive header; line 100: `w-full sm:w-[200px]` select trigger |
| `frontend/app/(dashboard)/documents/page.tsx` | Responsive header + w-full sm:w-[160px] select | VERIFIED | Line 66: responsive header; line 69: `w-full sm:w-[160px]` select trigger |
| `frontend/app/(dashboard)/dashboard/page.tsx` | 2-col mobile metrics, mobile-optimized layout | VERIFIED | Line 136: `grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4`; line 131: `space-y-4 sm:space-y-8`; line 193: recent activity `flex-col gap-1 sm:flex-row sm:items-center sm:justify-between` |
| `frontend/components/applications/application-board.tsx` | Custom sensors with activationConstraint, narrow mobile columns | VERIFIED | Lines 57-65: `useSensors` with `distance: 5` + `delay: 200`; line 193: `w-[240px] sm:w-[280px]`; line 173: `p-2 sm:p-4` |
| `frontend/components/applications/application-card.tsx` | Card with onClick prop | VERIFIED | Line 10: `onClick: () => void` prop; line 18: `onClick={onClick}` on Card element |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `topbar.tsx` | `theme-toggle.tsx` | `import ThemeToggle` | WIRED | Line 9: `import { ThemeToggle } from "./theme-toggle"`; line 30: `<ThemeToggle />` renders before `<UserButton />` |
| `theme-toggle.tsx` | `next-themes` | `useTheme` hook | WIRED | Line 3: `import { useTheme } from "next-themes"`; line 8: `const { theme, setTheme } = useTheme()` |
| `dialog.tsx` DialogContent | Radix UI event | `onInteractOutside` e.preventDefault() | WIRED | Lines 63-67: handler present before `{...props}` spread |
| `(dashboard)/layout.tsx` | `sidebar.tsx` | Sidebar hidden at md breakpoint | WIRED | `sidebar.tsx` line 26: `className="hidden w-64 flex-col border-r md:flex"` |
| `data-table.tsx` | `table.tsx` | Table wrapped in overflow container | WIRED | Lines 58-118: `<div className="overflow-x-auto"><Table>...</Table></div>` |
| `application-board.tsx` | `@dnd-kit/core` | Custom sensors with distance activation | WIRED | Lines 5-12: sensors imported; lines 57-65: `sensors` built with `activationConstraint`; line 180: `sensors={sensors}` on `<Kanban>` |
| `application-board.tsx` | `application-card.tsx` | onClick fires onSelectApplication | WIRED | Line 211: `<ApplicationCard ... onClick={() => onSelectApplication(app.id)} />`; `KanbanItem` retains `asHandle` |
| `BetterAuthSessionFilter.kt` | `users` table | `INSERT INTO users` when EmptyResultDataAccessException | WIRED | Lines 85-101: catch block executes `INSERT INTO users ... RETURNING id, email, enabled` |

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| APPL-03 | 08-01, 08-02, 08-05, 08-06, 08-07, 08-08 | User can view applications as a kanban board with drag-and-drop between status columns | SATISFIED | `application-board.tsx`: 8 status columns, `handleValueChange` calls `useUpdateApplicationStatus`, `isValidTransition` guards, invalid columns dim via `opacity-40`, cards now clickable (distance:5 sensor), mobile-optimized (240px columns). Dialog UX improved (08-06). Users auto-provisioned (08-05). Responsive layout (08-07). |
| APPL-04 | 08-01, 08-02, 08-05, 08-06, 08-07, 08-08 | User can view applications as a sortable, filterable table/list | SATISFIED | `application-list.tsx` renders `DataTable` with `overflow-x-auto` wrapper (08-07); `FilterBar` has no double padding (08-07); inline status dropdown uses `STATUS_TRANSITIONS`. All theme, dialog, and responsiveness fixes apply. |

No orphaned requirements. Both APPL-03 and APPL-04 are satisfied with full implementation evidence across all 8 plans.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `application-board.tsx` | 225 | `onClick={() => {}}` | INFO | Intentional no-op on `KanbanOverlay` drag ghost. Real cards properly call `onSelectApplication`. Not a stub. |
| `jobs/page.tsx` | 101 | `placeholder="All Companies"` | INFO | Legitimate `SelectValue` placeholder prop, not a code stub. |
| `documents/page.tsx` | 70 | `placeholder="Category"` | INFO | Legitimate `SelectValue` placeholder prop, not a code stub. |

No blockers. No regressions introduced by plans 08-06, 08-07, or 08-08.

### Human Verification Required

All automated (code-level) checks passed with 33/33 truths verified. The following require browser-based testing:

#### 1. Theme Toggle Light/Dark Switching

**Test:** Click the Sun/Moon button in the topbar. Verify both themes.
**Expected:** Dark mode shows visually distinct card surfaces, visible borders and input outlines, readable navigation. Light mode is unaffected. Theme persists across page refresh.
**Why human:** Visual contrast ratios and CSS variable rendering require browser verification.

#### 2. Dialog Mobile Behavior

**Test:** Open any creation form (e.g., New Job with 15+ fields) on a 375px viewport. Scroll the dialog. Tap outside.
**Expected:** Dialog scrolls internally within ~85vh cap. Tapping outside does NOT close it — only X button or Cancel closes.
**Why human:** Overflow behavior and tap-outside interaction require mobile browser testing.

#### 3. Kanban Card Click vs. Drag Distinction

**Test:** Click a kanban card. Then drag one 10px+. On touch: quick tap vs. long press (200ms).
**Expected:** Click opens detail panel. Mouse drag (5px+) activates drag-and-drop. Touch tap opens panel. Touch long press (200ms) activates drag.
**Why human:** Distance activation constraint behavior requires live interaction to verify.

#### 4. Mobile Layout at 320px Viewport

**Test:** Open each of the 5 pages at 320px browser width.
**Expected:** No horizontal overflow. Page headers stack. Tables scroll horizontally. Kanban scrolls with 240px columns. Dashboard shows 2-column metric cards.
**Why human:** Visual layout and overflow require browser resize testing.

#### 5. Board/List View Persistence

**Test:** Go to /applications, click "List" tab, then refresh.
**Expected:** Page reloads into List view (localStorage persists across reload).
**Why human:** localStorage across page reload requires browser interaction.

#### 6. Application Detail Panel — Sheet Width and Tabs

**Test:** Click a kanban card or list row on mobile (~375px) and desktop.
**Expected:** Sheet animates in at ~92% width on mobile and ~512px on desktop. All 4 tabs are functional.
**Why human:** Sheet animation and width require visual verification.

#### 7. New Better Auth User Auto-Provisioning End-to-End

**Test:** Start backend and frontend. Register a brand new user. Navigate to all five dashboard pages.
**Expected:** All pages load with empty states. Backend logs contain "Auto-provisioned backend user for Better Auth email: ..." entry.
**Why human:** Requires live PostgreSQL, session cookie, and HTTP request to trigger the INSERT path.

#### 8. Document Drag-and-Drop Upload

**Test:** Drag a PDF file onto the upload zone on /documents.
**Expected:** Upload spinner, success toast, file appears in list.
**Why human:** File drag events and upload progress require browser testing.

#### 9. Dashboard Metrics Accuracy

**Test:** After creating companies, jobs, and applications, navigate to /dashboard.
**Expected:** Metric cards show accurate counts. Status breakdown and recent activity reflect real data.
**Why human:** Metric aggregation with real data requires live backend connection.

### Gaps Summary

No gaps. All 33 observable truths have code-level evidence.

**Plan 08-06 (Theme + Dialog + Contrast):** ThemeToggle created and wired into topbar. `DialogContent` has `max-h-[85vh] overflow-y-auto` and `onInteractOutside` preventing accidental close. Auth page wrapped in `max-w-md`. Dark theme CSS variables updated for visible cards (0.235), borders (15%), inputs (20%), muted surfaces (0.30), sidebar (0.22).

**Plan 08-07 (Mobile Responsiveness):** Layout `main` padding changed to `p-3 sm:p-6`. `DataTable` wrapped in `overflow-x-auto`. All 5 page headers use `flex-col gap-3 sm:flex-row` pattern. Filter bar `px-6` removed. Sheet changed to `w-[92%]` on mobile + `sm:max-w-lg` on desktop. Action buttons and select triggers have mobile-first width classes.

**Plan 08-08 (Kanban Click + Mobile Optimization):** Custom `useSensors` with `activationConstraint: { distance: 5 }` (mouse) and `{ delay: 200, tolerance: 5 }` (touch) allows clicks to reach `ApplicationCard.onClick`. Dashboard metric grid uses `grid-cols-2` on mobile. Kanban columns use `w-[240px] sm:w-[280px]` and board container `p-2 sm:p-4`.

The phase status remains `human_needed` because the goal — users interacting with polished frontend pages — inherently requires human confirmation of visual polish and interactive behaviors. All automated evidence confirms the implementation is complete, correct, and wired.

---

_Verified: 2026-03-21T16:00:00Z_
_Verifier: Claude (gsd-verifier)_
_Re-verification: after gap closure plans 08-06, 08-07, 08-08_
