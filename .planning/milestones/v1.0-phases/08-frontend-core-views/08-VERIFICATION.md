---
phase: 08-frontend-core-views
verified: 2026-03-21T20:30:00Z
status: human_needed
score: 40/40 must-haves verified
re_verification:
  previous_status: gaps_found
  previous_score: 33/33 (code-level) with 7 gaps (GAP-09 through GAP-15)
  gaps_closed:
    - "GAP-09: Dialog close button now stays visible when scrolling long forms — children in overflow-y-auto inner wrapper, close button outside"
    - "GAP-10: Dialogs no longer auto-focus first input on mobile — onOpenAutoFocus={(e) => e.preventDefault()} added to DialogPrimitive.Content"
    - "GAP-11: Dashboard timestamps now correct — Math.abs guard prevents negatives; 'just now', Xm, Xh, Xd, localeDateString tiers"
    - "GAP-12: Kanban horizontal scroll no longer displaces sidebar — sidebar is now position:fixed, layout uses md:ml-64 offset"
    - "GAP-13: Status dropdown no longer blocks table scroll — DropdownMenu modal={false} removes viewport overlay; min-w-[180px] prevents truncation"
    - "GAP-14: Sidebar remains fixed during all scroll — fixed inset-y-0 left-0 z-40; topbar sticky top-0 z-30"
    - "GAP-15: Dashboard recent activity cards navigate to /applications?applicationId=UUID and auto-open detail panel via useSearchParams useEffect"
  gaps_remaining: []
  regressions: []
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
    expected: "Clicking a kanban card or list row opens the Sheet from the right at 92% width on mobile / max-w-lg on desktop with 4 functional tabs: Overview, Notes, Interviews, Documents. All tabs scroll vertically within the panel (flex-1 min-h-0 overflow-y-auto wrapper confirmed in code)."
    why_human: "Sheet slide-over animation, width, and tab content rendering require visual verification"
  - test: "Theme toggle light/dark switching"
    expected: "Clicking the Sun/Moon button in topbar toggles theme. Dark mode has visible card borders, distinct card surfaces from background, visible input outlines. Light mode unaffected."
    why_human: "Visual contrast and theme switching require browser verification"
  - test: "Dialog mobile behavior — sticky close button and no auto-focus"
    expected: "Opening a job creation form (15+ fields) on mobile: dialog is capped at 85vh, children scroll internally, X button stays visible at top-right at all scroll positions. Opening any edit dialog does NOT pop the mobile keyboard automatically."
    why_human: "Overflow scroll behavior, button stickiness, and keyboard popup require physical or emulated mobile browser testing"
  - test: "Sidebar fixed during kanban horizontal scroll"
    expected: "On desktop, scrolling the kanban board (8 columns) horizontally does NOT move the sidebar. The sidebar stays anchored to the left edge. The topbar stays at the top."
    why_human: "Fixed/sticky CSS positioning behavior during scroll requires browser verification"
  - test: "Dashboard recent activity card deep link"
    expected: "Clicking a recent activity card navigates to /applications?applicationId=UUID. The applications page loads and immediately opens the detail panel for that specific application. Closing the panel clears the URL param."
    why_human: "Navigation, URL param handling, and auto-open behavior require browser interaction"
  - test: "Status dropdown in data table — no scroll blocking, full text"
    expected: "Opening a status dropdown in the applications list does NOT block horizontal table scrolling. Status text is fully readable (e.g., 'Phone Screen' not truncated). Dropdown repositions if near viewport edge."
    why_human: "Radix modal=false behavior and dropdown collision padding require browser testing"
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

**Phase Goal:** Build the core frontend views — applications kanban/list, companies, jobs, documents, dashboard
**Verified:** 2026-03-21T20:30:00Z
**Status:** human_needed
**Re-verification:** Yes — third pass, after gap closure plans 08-09, 08-10, 08-11

## Re-verification Summary

| Item | Previous (post-08) | Current (post-11) |
|------|--------------------|-------------------|
| Overall status | gaps_found | human_needed |
| Score | 33/33 code-level, 7 gaps | 40/40 code-level, 0 gaps |
| Gaps closed | — | 7 (GAP-09 through GAP-15) |
| Gaps remaining | 7 | 0 |
| Regressions | 0 | 0 |

All 7 UAT-identified gaps from the previous verification are confirmed closed in the codebase. All 33 previously-verified truths remain passing. 7 new truths from plans 08-09, 08-10, 08-11 pass. No regressions.

## Goal Achievement

### Observable Truths

| # | Truth | Status | Evidence |
|---|-------|--------|----------|
| 1 | Backend SecurityConfig updated to validate Better Auth session cookies | VERIFIED | `BetterAuthSessionFilter.kt` registered via `addFilterBefore` in `SecurityConfig.kt` |
| 2 | TypeScript types exist mirroring all backend DTOs | VERIFIED | `types/api.ts` exports 12 interfaces + `STATUS_TRANSITIONS`, `STATUS_COLORS`, `isValidTransition` |
| 3 | TanStack Query hooks exist for all 5 entity domains | VERIFIED | `use-applications.ts`, `use-companies.ts`, `use-jobs.ts`, `use-interviews.ts`, `use-documents.ts` all export CRUD + query hooks |
| 4 | All required shadcn/ui components installed | VERIFIED | `dialog.tsx`, `badge.tsx`, `table.tsx`, `sheet.tsx`, `kanban.tsx`, `skeleton.tsx` et al present in `components/ui/` |
| 5 | Shared reusable components exist | VERIFIED | `confirm-dialog.tsx`, `data-table.tsx`, `status-badge.tsx` all present and substantive |
| 6 | apiClient handles FormData uploads without Content-Type collision | VERIFIED | `api-client.ts`: `!(options.body instanceof FormData) && { "Content-Type": "application/json" }` |
| 7 | User can view applications as kanban board with 8 status columns and drag-and-drop | VERIFIED | `application-board.tsx` uses `KanbanBoard`, `KanbanColumn`, groups by all 8 `APPLICATION_STATUSES`, calls `useUpdateApplicationStatus`, checks `isValidTransition` |
| 8 | Invalid status transitions are prevented during drag (invalid columns dim) | VERIFIED | `application-board.tsx`: `opacity-40 pointer-events-none` applied via `isDimmed` flag during drag |
| 9 | User can view applications as sortable, filterable table/list | VERIFIED | `application-list.tsx` uses `DataTable`, `ColumnDef`, `STATUS_TRANSITIONS`, `FilterBar` integrated |
| 10 | Clicking a card/row opens slide-over detail panel with 4 tabs | VERIFIED | `application-detail.tsx`: Sheet with Overview, Notes, Interviews, Documents tabs |
| 11 | User can create new applications via modal dialog | VERIFIED | `application-form.tsx`: Dialog, `standardSchemaResolver`, job combobox, date pickers |
| 12 | User can toggle between Board and List views with preference remembered | VERIFIED | `applications/page.tsx`: `useViewPreference("jobhunt:applications-view", "board")`; `use-view-preference.ts` uses `localStorage` with SSR guard |
| 13 | User can view companies in a card grid | VERIFIED | `companies/page.tsx`: `useCompanies`, `useJobs`, `jobCountMap`; `company-card.tsx` renders name, Briefcase, DropdownMenu |
| 14 | User can create, edit, and delete companies via modal dialogs | VERIFIED | `company-form.tsx`: Dialog + `useCreateCompany`; delete uses `ConfirmDialog` |
| 15 | User can view a company's details and linked jobs | VERIFIED | `companies/[id]/page.tsx`: calls `useCompany(id)` and `useJobs({ companyId })` |
| 16 | User can view all jobs in a filterable table | VERIFIED | `jobs/page.tsx`: `useJobs`, `useCompanies`; `job-list.tsx` uses `DataTable` with `companyName` column |
| 17 | User can upload PDF and DOCX documents via drag-and-drop | VERIFIED | `document-upload.tsx`: `useDropzone` wired to `useUploadDocument` |
| 18 | User can see dashboard summary metrics | VERIFIED | `dashboard/page.tsx`: `useApplications({ size: 1000 })`, 4 metric cards, `StatusBadge`, recent activity |
| 19 | New Better Auth user auto-provisioned in backend users table on first request | VERIFIED | `BetterAuthSessionFilter.kt` lines 85-101: catches `EmptyResultDataAccessException`, executes `INSERT INTO users` |
| 20 | Error states use muted styling (not destructive red) | VERIFIED | All 5 dashboard pages use `text-muted-foreground` for error paths |
| 21 | User can toggle between light and dark theme via topbar button | VERIFIED | `theme-toggle.tsx`: `useTheme`, `setTheme`, Sun/Moon icons; `topbar.tsx` line 30: `<ThemeToggle />` |
| 22 | Modal dialogs have internal scroll for long content on mobile | VERIFIED | `dialog.tsx` line 75: `<div className="overflow-y-auto flex-1 flex flex-col gap-4">{children}</div>` — children scroll within the dialog frame |
| 23 | Tapping outside a modal dialog does not close it | VERIFIED | `dialog.tsx` lines 63-67: `onInteractOutside={(e) => { e.preventDefault() }}` |
| 24 | Auth forms constrained to ~448px max-width centered on desktop | VERIFIED | `auth/[path]/page.tsx`: `<div className="w-full max-w-md">` wraps `<AuthView>` |
| 25 | Dark theme has improved contrast (cards, borders, inputs, sidebar) | VERIFIED | `globals.css`: `--card: oklch(0.235 0 0)`, `--border: oklch(1 0 0 / 15%)`, `--input: oklch(1 0 0 / 20%)`, `--muted: oklch(0.30 0 0)`, `--sidebar: oklch(0.22 0 0)` |
| 26 | App layout padding reduces on mobile (12px mobile, 24px desktop) | VERIFIED | `(dashboard)/layout.tsx` line 14: `<main className="flex-1 overflow-auto p-3 sm:p-6">` |
| 27 | Data tables scroll horizontally on mobile instead of overflowing | VERIFIED | `data-table.tsx`: `<div className="overflow-x-auto">` wraps `<Table>` |
| 28 | All page headers stack vertically on mobile with full-width buttons | VERIFIED | `applications/page.tsx`: `flex-col gap-3 sm:flex-row sm:items-center sm:justify-between`; same pattern in companies, jobs, documents pages |
| 29 | Sheet detail panel is full-width on mobile, capped at sm:max-w-lg on desktop | VERIFIED | `sheet.tsx`: `data-[side=right]:w-[92%]` and `data-[side=right]:sm:max-w-lg` |
| 30 | Filter bar has no double padding | VERIFIED | `filter-bar.tsx` container: `flex flex-wrap items-center gap-4 py-3` — no `px-6` present |
| 31 | Clicking (not dragging) a kanban card opens the application detail panel | VERIFIED | `application-board.tsx`: `useSensors` with `activationConstraint: { distance: 5 }` (mouse) and `{ delay: 200, tolerance: 5 }` (touch); `ApplicationCard` has `onClick={() => onSelectApplication(app.id)}` |
| 32 | Dragging a kanban card still works for status transitions | VERIFIED | `KanbanItem` retains `asHandle`; `handleValueChange` still calls `updateStatus.mutate`; sensors fire after 5px movement |
| 33 | Dashboard metric cards and kanban board optimized for mobile | VERIFIED | `dashboard/page.tsx` line 136: `grid-cols-2 gap-3 sm:gap-4 lg:grid-cols-4`; `application-board.tsx` line 193: `w-[240px] sm:w-[280px]` |
| 34 | Dialog close button remains visible when scrolling long form content (GAP-09) | VERIFIED | `dialog.tsx` line 75: children wrapped in `overflow-y-auto flex-1 flex flex-col gap-4`; close button at lines 76-88 is outside the scroll wrapper, anchored to dialog frame corner |
| 35 | Dialogs do not auto-focus the first input on mobile, avoiding keyboard popup (GAP-10) | VERIFIED | `dialog.tsx` line 68: `onOpenAutoFocus={(e) => e.preventDefault()}` on `DialogPrimitive.Content` |
| 36 | Recent activity timestamps display as minutes, hours, or days ago without negative values (GAP-11) | VERIFIED | `dashboard/page.tsx` lines 31-44: `Math.abs(diff)` guards against negative values; `"just now"` for sub-minute; Xm, Xh, Xd, localeDateString tiers |
| 37 | Sidebar remains fixed during horizontal scroll on desktop (GAP-14) | VERIFIED | `sidebar.tsx` line 26: `hidden fixed inset-y-0 left-0 z-40 w-64 flex-col border-r bg-background md:flex`; `layout.tsx` line 12: `md:ml-64` offsets content column |
| 38 | Topbar remains fixed at top of content column (GAP-14) | VERIFIED | `topbar.tsx` line 15: `sticky top-0 z-30 flex h-14 items-center justify-between border-b bg-background px-6` |
| 39 | Status dropdown in data table does not block scroll and shows full text (GAP-13) | VERIFIED | `application-list.tsx` line 39: `<DropdownMenu modal={false}>`; line 46: `<DropdownMenuContent align="start" className="min-w-[180px]" sideOffset={4} collisionPadding={8}>` |
| 40 | Clicking a recent activity card navigates to applications page and opens that application's detail panel (GAP-15) | VERIFIED | `dashboard/page.tsx` line 194: `href={\`/applications?applicationId=${app.id}\`}`; `applications/page.tsx` lines 27-35: `useSearchParams`, `useEffect` reads `applicationId` param, calls `setSelectedApplicationId(appId)`; cleanup via `router.replace("/applications", { scroll: false })` |

**Score:** 40/40 truths verified (code-level)

### Required Artifacts

| Artifact | Expected | Status | Details |
|----------|----------|--------|---------|
| `frontend/components/ui/dialog.tsx` | Flex-col with inner scroll wrapper, sticky close button, auto-focus prevention | VERIFIED | Line 68: `onOpenAutoFocus` + `e.preventDefault()`; line 70: outer `flex flex-col max-h-[85vh]` no `overflow-y-auto`; line 75: inner `overflow-y-auto flex-1 flex flex-col gap-4`; close button outside scroll wrapper |
| `frontend/app/(dashboard)/dashboard/page.tsx` | Math.abs guard in relativeTime; applicationId in recent activity links | VERIFIED | Lines 31-44: `Math.abs(diff)`, "just now", Xm/Xh/Xd tiers; line 194: `href={\`/applications?applicationId=${app.id}\`}` |
| `frontend/components/layout/sidebar.tsx` | Fixed sidebar with z-40 and bg-background | VERIFIED | Line 26: `hidden fixed inset-y-0 left-0 z-40 w-64 flex-col border-r bg-background md:flex` |
| `frontend/components/layout/topbar.tsx` | Sticky topbar with z-30 and bg-background | VERIFIED | Line 15: `sticky top-0 z-30 flex h-14 items-center justify-between border-b bg-background px-6` |
| `frontend/app/(dashboard)/layout.tsx` | Content column offset with md:ml-64 | VERIFIED | Line 12: `flex flex-1 flex-col md:ml-64` |
| `frontend/components/applications/application-detail.tsx` | Sheet with flex-1 min-h-0 overflow-y-auto wrapper around Tabs | VERIFIED | Line 108: `<div className="flex-1 min-h-0 overflow-y-auto">` wraps Tabs section; SheetHeader is outside the scroll wrapper |
| `frontend/components/applications/application-list.tsx` | DropdownMenu modal=false, min-w-[180px], collisionPadding | VERIFIED | Line 39: `modal={false}`; line 46: `min-w-[180px]`, `sideOffset={4}`, `collisionPadding={8}` |
| `frontend/app/(dashboard)/applications/page.tsx` | useSearchParams + useEffect for applicationId, router.replace cleanup, Suspense boundary | VERIFIED | Line 3: `Suspense` import; line 4: `useSearchParams`, `useRouter`; lines 27-35: `useEffect` reads param; lines 141-143: `router.replace` cleanup; lines 156-167: Suspense wraps ApplicationsContent |

### Key Link Verification

| From | To | Via | Status | Details |
|------|----|-----|--------|---------|
| `dialog.tsx` DialogContent | Radix dialog open event | `onOpenAutoFocus` e.preventDefault() | WIRED | Line 68: `onOpenAutoFocus={(e) => e.preventDefault()}` before `{...props}` spread |
| `dialog.tsx` children wrapper | close button | `flex flex-col` outer / `overflow-y-auto` inner separation | WIRED | Children in scroll div (line 75); close button outside (lines 76-88); close button is `absolute top-2 right-2` relative to outer `flex flex-col` container |
| `dashboard/page.tsx` recent activity | `applications/page.tsx` | `?applicationId=UUID` query param in Link href | WIRED | Line 194: `href={\`/applications?applicationId=${app.id}\`}` |
| `applications/page.tsx` | detail panel open state | `useSearchParams` + `useEffect` | WIRED | Lines 27-35: `searchParams.get("applicationId")` → `setSelectedApplicationId(appId)` |
| `applications/page.tsx` | URL cleanup | `router.replace` on panel close | WIRED | Lines 141-143: `if (searchParams.has("applicationId")) { router.replace("/applications", { scroll: false }) }` |
| `sidebar.tsx` | viewport | `fixed inset-y-0 left-0 z-40` | WIRED | Line 26: sidebar is out of document flow, anchored to viewport left |
| `layout.tsx` content column | sidebar width | `md:ml-64` offset | WIRED | Line 12: `md:ml-64` compensates for fixed 256px sidebar on desktop |
| `topbar.tsx` | content column top | `sticky top-0 z-30` | WIRED | Line 15: topbar sticks to top of its scroll container (the content column flex div) |
| `application-list.tsx` DropdownMenu | background scroll | `modal={false}` | WIRED | Line 39: `modal={false}` removes full-viewport overlay that blocked pointer events |
| `application-detail.tsx` Tabs | sheet panel scroll | `flex-1 min-h-0 overflow-y-auto` wrapper | WIRED | Line 108: wrapper enables vertical scroll for tabs content; SheetHeader pinned above |

### Requirements Coverage

| Requirement | Source Plans | Description | Status | Evidence |
|-------------|-------------|-------------|--------|----------|
| APPL-03 | 08-01, 08-02, 08-05, 08-06, 08-07, 08-08, 08-09, 08-10, 08-11 | User can view applications as a kanban board with drag-and-drop between status columns | SATISFIED | `application-board.tsx`: 8 status columns, `handleValueChange` calls `useUpdateApplicationStatus`, `isValidTransition` guards, invalid columns dim via `opacity-40`, cards clickable (distance:5 sensor), mobile-optimized. Dialog UX improved (08-06, 08-09). Fixed layout prevents scroll displacement (08-10). |
| APPL-04 | 08-01, 08-02, 08-05, 08-06, 08-07, 08-08, 08-09, 08-10, 08-11 | User can view applications as a sortable, filterable table/list | SATISFIED | `application-list.tsx`: `DataTable` with `overflow-x-auto`, `FilterBar` (no double padding), status dropdown `modal={false}` with `min-w-[180px]`, full text displayed. Deep link from dashboard via `?applicationId` param opens detail panel. |

No orphaned requirements. Both APPL-03 and APPL-04 are marked Complete in `REQUIREMENTS.md` and are fully satisfied across all 11 plans.

### Anti-Patterns Found

| File | Line | Pattern | Severity | Impact |
|------|------|---------|----------|--------|
| `application-board.tsx` | 225 | `onClick={() => {}}` | INFO | Intentional no-op on `KanbanOverlay` drag ghost. Real cards properly call `onSelectApplication`. Not a stub. |
| `jobs/page.tsx` | ~101 | `placeholder="All Companies"` | INFO | Legitimate `SelectValue` placeholder prop, not a code stub. |
| `documents/page.tsx` | ~70 | `placeholder="Category"` | INFO | Legitimate `SelectValue` placeholder prop, not a code stub. |

No blockers. No regressions from plans 08-09, 08-10, or 08-11.

### Human Verification Required

All automated (code-level) checks passed with 40/40 truths verified. The following require browser-based testing:

#### 1. Kanban Drag-and-Drop Status Transition (APPL-03)

**Test:** Drag a kanban card to a valid column. Then drag to an invalid column.
**Expected:** Valid column: status updates with success toast. Invalid column: column dims (`opacity-40`), card snaps back, error toast shows.
**Why human:** Drag-and-drop interaction and CSS opacity dimming require live browser testing.

#### 2. Kanban Card Click vs. Drag Distinction (APPL-03)

**Test:** Click a kanban card. Then drag one 10px+. On touch: quick tap vs. long press (200ms).
**Expected:** Click opens detail panel. Mouse drag (5px+) activates drag-and-drop. Touch tap opens panel. Touch long press (200ms) activates drag.
**Why human:** Distance activation constraint requires live interaction to verify.

#### 3. Board/List View Persistence (APPL-03, APPL-04)

**Test:** Go to /applications, click "List" tab, then refresh.
**Expected:** Page reloads into List view (localStorage persists across reload).
**Why human:** localStorage across page reload requires browser interaction.

#### 4. Application Detail Panel — Sheet Width and Tabs

**Test:** Click a kanban card or list row on mobile (~375px) and desktop.
**Expected:** Sheet animates in at ~92% width on mobile and ~512px on desktop. All 4 tabs are functional. Long tab content (Notes, Interviews) scrolls vertically within the panel; SheetHeader stays pinned.
**Why human:** Sheet animation, width, and tab scroll require visual verification.

#### 5. Theme Toggle Light/Dark Switching

**Test:** Click the Sun/Moon button in the topbar. Verify both themes.
**Expected:** Dark mode shows visually distinct card surfaces, visible borders and input outlines. Theme persists across page refresh.
**Why human:** Visual contrast and CSS variable rendering require browser verification.

#### 6. Dialog Mobile Behavior — Sticky Close Button and No Auto-Focus

**Test:** Open any creation form (e.g., New Job with 15+ fields) on a 375px viewport. Scroll the dialog. Also open an edit dialog.
**Expected:** Dialog scrolls internally within ~85vh cap. X button remains visible at top-right at all scroll positions. Opening any dialog does NOT pop the mobile keyboard.
**Why human:** Overflow behavior, button visibility at scroll position, and keyboard popup require mobile browser testing.

#### 7. Sidebar Fixed During Kanban Horizontal Scroll

**Test:** On desktop, open the Applications page in Board view. Scroll the kanban board (8 columns) fully to the right.
**Expected:** The sidebar stays anchored to the left viewport edge. The topbar stays at the top. Navigation remains accessible from any scroll position.
**Why human:** Fixed/sticky CSS positioning behavior during live scroll requires browser verification.

#### 8. Dashboard Recent Activity Card Deep Link

**Test:** Click a card in the dashboard recent activity section.
**Expected:** Navigates to /applications?applicationId=UUID. The applications page loads and immediately opens the detail panel for that specific application. Closing the panel clears the URL param.
**Why human:** Navigation, URL param reading, Suspense rendering, and auto-open behavior require browser interaction.

#### 9. Status Dropdown in Data Table

**Test:** Open the applications list view. Click a status badge to open the dropdown. With dropdown open, attempt to scroll the table horizontally.
**Expected:** Horizontal scroll works while dropdown is open. Full status names are readable (e.g., "Phone Screen" not "Phone Scree..."). Dropdown repositions if near viewport edge.
**Why human:** Radix `modal={false}` pointer-event behavior and collision padding repositioning require browser testing.

#### 10. Mobile Layout Responsiveness at 320px

**Test:** Open each of the 5 pages at 320px browser width.
**Expected:** No horizontal overflow. Page headers stack. Buttons go full-width. Tables scroll horizontally. Kanban scrolls with 240px columns. Dashboard shows 2-column metric cards.
**Why human:** Visual layout and overflow require browser resize testing.

#### 11. New Better Auth User Auto-Provisioning End-to-End

**Test:** Start backend and frontend. Register a brand new user. Navigate to all five dashboard pages.
**Expected:** All pages load with empty states. Backend logs contain "Auto-provisioned backend user for Better Auth email: ..." entry.
**Why human:** Requires live PostgreSQL, session cookie, and HTTP request to trigger the INSERT path.

#### 12. Document Drag-and-Drop Upload

**Test:** Drag a PDF file onto the upload zone on /documents.
**Expected:** Upload spinner, success toast, file appears in list.
**Why human:** File drag events and upload progress require browser testing.

#### 13. Dashboard Metrics Accuracy

**Test:** After creating companies, jobs, and applications, navigate to /dashboard.
**Expected:** Metric cards show accurate counts. Status breakdown and recent activity reflect real data.
**Why human:** Metric aggregation with real data requires live backend connection.

### Gaps Summary

No gaps remain. All 7 previously-identified gaps (GAP-09 through GAP-15) are confirmed closed in the codebase.

**Plan 08-09 (Dialog UX + Timestamps):** DialogContent restructured to `flex flex-col` with inner `overflow-y-auto flex-1` wrapper; close button outside the scroll area stays anchored. `onOpenAutoFocus={(e) => e.preventDefault()}` prevents mobile keyboard popup. `relativeTime` uses `Math.abs(diff)` and adds "just now" / hours / days tiers.

**Plan 08-10 (Fixed Layout + Sheet Scroll):** Sidebar changed from flex item to `fixed inset-y-0 left-0 z-40 bg-background` — fully out of document flow, never displaces during horizontal scroll. `layout.tsx` adds `md:ml-64` to offset content column. Topbar becomes `sticky top-0 z-30 bg-background`. `application-detail.tsx` wraps Tabs in `flex-1 min-h-0 overflow-y-auto` div for vertical scroll within the Sheet panel.

**Plan 08-11 (Status Dropdown + Deep Link):** `DropdownMenu modal={false}` removes the Radix viewport overlay that blocked all pointer events; `min-w-[180px]` and `collisionPadding={8}` ensure full status text is readable. Dashboard recent activity links updated to `/applications?applicationId=${app.id}`; `applications/page.tsx` wrapped in Suspense, reads param via `useSearchParams` in `useEffect`, auto-opens detail panel, cleans up URL on close via `router.replace`.

**Post-UAT fixes (after human testing of plans 08-09 through 08-11):**

**Layout overflow containment (GAP-17):** The outer flex container and content column in `layout.tsx` lacked overflow containment. Wide content (kanban board's `min-w-max` = ~2000px) propagated its intrinsic minimum width up through flex items with default `min-width: auto`, blowing out the entire page width on mobile. Fixed by adding `overflow-hidden` to the outer container and `min-w-0` to the content column flex item. This resolved: Applications/Jobs pages appearing in "desktop mode" on mobile, topbar elements being pushed off-screen, hamburger menu unreachable, and dialogs opening outside the viewport.

**Kanban touch scroll (GAP-18):** The `KanbanItem` and `KanbanColumn` components applied `touch-action: none` (via `touch-none` class) when `asHandle` was true, blocking ALL native touch gestures including scrolling. Since cards fill most of the board, users could only scroll by finding empty spaces between cards. Fixed by removing `touch-none` from both components in `kanban.tsx`, relying on the existing `TouchSensor` with `delay: 200, tolerance: 5` activation constraint to distinguish scroll (immediate swipe) from drag (200ms hold).

**Human testing status:** User confirmed all pages behave correctly on mobile after these fixes. Phase 08 is complete.

---

_Verified: 2026-03-21T22:00:00Z_
_Verifier: Claude (gsd-verifier)_
_Final pass: after human UAT confirmation of all fixes_
