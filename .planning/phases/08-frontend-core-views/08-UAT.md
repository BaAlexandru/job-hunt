---
status: complete
phase: 08-frontend-core-views
source: [08-01-SUMMARY.md, 08-02-SUMMARY.md, 08-03-SUMMARY.md, 08-04-SUMMARY.md]
started: 2026-03-21T07:00:00Z
updated: 2026-03-21T07:10:00Z
---

## Current Test

[testing complete]

## Tests

### 1. Applications Kanban Board
expected: Navigate to /applications. The page loads a Kanban board view with status columns. Application cards appear in their respective status columns showing company name, job title, and next action date.
result: issue
reported: "When navigating to applications page with a new user I'm getting this message in red: Could not load applications. Check your connection and try again. The message and its color gives an idea that there is an error or something is wrong. In case this is supposed to show because the user doesn't have any applications this is a wrong message."
severity: blocker

### 2. Kanban Drag-and-Drop
expected: Drag an application card from one status column to another valid column. The card moves and status updates. Invalid target columns dim/grey out during drag to indicate they are not valid transitions.
result: skipped
reason: Blocked by Test 1 — all API calls fail for new users

### 3. Applications List View
expected: Toggle from Board to List view on the applications page. A sortable data table appears with columns for company, job title, status, dates. Pagination controls appear at the bottom. An inline status dropdown on each row allows quick status changes.
result: skipped
reason: Blocked by Test 1 — all API calls fail for new users

### 4. View Toggle Persistence
expected: Switch between Board and List view, then navigate away and come back to /applications. The page remembers your last selected view (persisted to localStorage).
result: skipped
reason: Blocked by Test 1 — all API calls fail for new users

### 5. Application Filter Bar
expected: Filter bar appears with status multi-select, company combobox, date pickers, and a search input. Selecting filters narrows the displayed applications. Search is debounced (slight delay before filtering).
result: skipped
reason: Blocked by Test 1 — all API calls fail for new users

### 6. Create Application
expected: Click "New Application" (or similar button). A dialog opens with fields for company, job, status, dates, and notes. Fill in details and submit. The new application appears in the board/list.
result: issue
reported: "The button for creating a new application is there but I can't create a new application as there are no jobs available. No button to create company/job/document on their respective pages."
severity: blocker

### 7. Application Detail Panel
expected: Click on an application card or row. A slide-over panel opens from the right (~480px wide) with tabs: Overview, Notes, Interviews, Documents. Each tab shows relevant content for that application.
result: skipped
reason: Blocked by Test 1 — all API calls fail for new users

### 8. Companies Card Grid
expected: Navigate to /companies. A responsive card grid displays company cards showing company name, website, location, and job count. Loading shows skeleton cards. Empty state shown if no companies.
result: issue
reported: "Same error message as applications but in grey instead of red. No button to create a new company."
severity: blocker

### 9. Company CRUD
expected: Create a new company via dialog. Edit an existing company. Delete a company with confirmation dialog. Toast notifications confirm each action.
result: skipped
reason: Blocked by Test 8 — API calls fail, no create button available

### 10. Company Detail with Jobs
expected: Click a company card. A detail page loads showing company info and a data table of jobs linked to that company.
result: skipped
reason: Blocked by Test 8 — no companies to click

### 11. Jobs Data Table
expected: Navigate to /jobs. A filterable data table shows jobs with company name, title, salary info, and status. A company dropdown filter narrows results. Pagination works.
result: issue
reported: "Same error message as applications but in grey. No button to create a new job."
severity: blocker

### 12. Job CRUD
expected: Create a new job via dialog with fields including salary, company combobox, and closing date picker. Edit and delete jobs with confirmation. Toast notifications confirm actions.
result: skipped
reason: Blocked by Test 11 — API calls fail, no create button available

### 13. Documents Upload
expected: Navigate to /documents. A drag-and-drop upload zone is visible. Drag a PDF or DOCX file onto it (or click to browse). The file uploads with a toast notification on success.
result: issue
reported: "Same error message. No button to add a document."
severity: blocker

### 14. Documents List
expected: After uploading documents, a file browser table shows documents with name, category, size, and date. Category filter narrows the list. Download and delete actions are available per document.
result: skipped
reason: Blocked by Test 13 — API calls fail, can't upload documents

### 15. Dashboard Metrics
expected: Navigate to /dashboard. Summary metric cards show total applications, active count, offers, and interviews. A status breakdown section shows counts per status. A recent activity list shows latest changes.
result: issue
reported: "Dashboard is empty, only the message 'Could not load applications. Check your connection and try again.' is there."
severity: blocker

## Summary

total: 15
passed: 0
issues: 6
pending: 0
skipped: 9

## Gaps

- truth: "New user can load and interact with all pages after registering via Better Auth"
  status: failed
  reason: "User reported: All API calls return 500 for new users. Better Auth creates user in its own tables but no corresponding row exists in backend users table. BetterAuthSessionFilter silently fails the JOIN, no auth is set, controllers throw IllegalStateException."
  severity: blocker
  test: 1
  root_cause: "BetterAuthSessionFilter.kt JOINs session -> Better Auth user -> backend users via email. When backend users row doesn't exist (new registration), query returns empty, exception is silently caught, authentication is never set. All controllers then fail with 'No authenticated user in SecurityContext'."
  artifacts:
    - path: "backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/BetterAuthSessionFilter.kt"
      issue: "Silent exception catch when backend user not found — needs auto-creation or graceful handling"
    - path: "backend/src/main/kotlin/com/alex/job/hunt/jobhunt/security/SecurityContextUtil.kt"
      issue: "Throws IllegalStateException when no auth — correct behavior but surfaces as 500 to user"
    - path: "backend/src/main/kotlin/com/alex/job/hunt/jobhunt/config/GlobalExceptionHandler.kt"
      issue: "Generic Exception handler returns 500 — user sees 'Internal server error'"
  missing:
    - "Auto-create backend users row when BetterAuthSessionFilter finds a valid Better Auth session but no matching backend user"
    - "Or: add a registration webhook/hook that syncs Better Auth user creation to backend users table"
  debug_session: ""

- truth: "Empty state shows friendly message with action button, not error styling"
  status: failed
  reason: "User reported: Error message in red on applications page when user has no data. Jobs and companies show same message in grey. No create/add buttons visible on company, job, or document pages."
  severity: major
  test: 1
  root_cause: "Frontend pages show error state (isError from React Query) instead of empty state because API returns 500, not 200 with empty array. Additionally, even after auth is fixed, empty states may not show add/create CTAs on all pages."
  artifacts:
    - path: "frontend/app/(dashboard)/applications/page.tsx"
      issue: "Error state shows 'Could not load' in destructive color — once auth is fixed, need to verify empty state path"
    - path: "frontend/app/(dashboard)/companies/page.tsx"
      issue: "May not show create button when in error/empty state"
    - path: "frontend/app/(dashboard)/jobs/page.tsx"
      issue: "May not show create button when in error/empty state"
    - path: "frontend/app/(dashboard)/documents/page.tsx"
      issue: "May not show create button when in error/empty state"
  missing:
    - "Verify all pages show proper empty state with add/create CTA after auth fix"
    - "Ensure create buttons are always visible regardless of data loading state"
  debug_session: ""
