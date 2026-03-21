---
phase: 08-frontend-core-views
plan: 03
subsystem: ui
tags: [react, next.js, tanstack-query, zod, shadcn-ui, crud, data-table]

# Dependency graph
requires:
  - phase: 08-01
    provides: types, API hooks, shared components (DataTable, EmptyState, ConfirmDialog)
  - phase: 03-company-crud
    provides: backend company and job REST APIs
provides:
  - Companies card grid page with job count per company
  - Company detail page with linked jobs table
  - Jobs filterable data table page
  - Company and job CRUD forms with Zod validation
  - Create/edit/delete flows via modal dialogs
affects: [08-04]

# Tech tracking
tech-stack:
  added: []
  patterns: [standardSchemaResolver for Zod v4 + react-hook-form, Controller for Select/Popover form fields, useMemo job count aggregation]

key-files:
  created:
    - frontend/app/(dashboard)/companies/[id]/page.tsx
    - frontend/components/companies/company-card.tsx
    - frontend/components/companies/company-form.tsx
    - frontend/components/jobs/job-list.tsx
    - frontend/components/jobs/job-form.tsx
  modified:
    - frontend/app/(dashboard)/companies/page.tsx
    - frontend/app/(dashboard)/jobs/page.tsx

key-decisions:
  - "Used standardSchemaResolver instead of zodResolver for Zod v4 compatibility"
  - "Salary fields stored as strings in form, converted to numbers on submit"
  - "Company combobox via Popover+Command for searchable company selection in job form"

patterns-established:
  - "Form pattern: standardSchemaResolver + react-hook-form + Controller for controlled components"
  - "CRUD page pattern: list page with add/edit/delete state, form dialog, confirm dialog"
  - "Card grid pattern: responsive grid with skeleton loading and empty state"

requirements-completed: [APPL-03, APPL-04]

# Metrics
duration: 8min
completed: 2026-03-21
---

# Phase 8 Plan 3: Companies & Jobs Pages Summary

**Companies card grid with job counts and Jobs filterable data table, both with full CRUD via modal dialogs using Zod v4 validation**

## Performance

- **Duration:** 8 min
- **Started:** 2026-03-21T06:39:02Z
- **Completed:** 2026-03-21T06:47:37Z
- **Tasks:** 2
- **Files modified:** 7

## Accomplishments
- Companies page renders responsive card grid showing name, website, location, and job count per company
- Company detail page shows company info with linked jobs in a data table
- Jobs page renders filterable data table with company dropdown filter and pagination
- Job form supports all fields including salary types, company combobox, and calendar date picker
- All CRUD operations (create/edit/delete) via modal dialogs with toast notifications

## Task Commits

Each task was committed atomically:

1. **Task 1: Companies page (card grid with job count, form, detail with jobs)** - `44d76d4` (feat)
2. **Task 2: Jobs page (table, form, company filter)** - `d63cfd1` (feat)

## Files Created/Modified
- `frontend/app/(dashboard)/companies/page.tsx` - Companies card grid page with job count aggregation
- `frontend/app/(dashboard)/companies/[id]/page.tsx` - Company detail with linked jobs table
- `frontend/components/companies/company-card.tsx` - Company card with name, website, location, job count, dropdown actions
- `frontend/components/companies/company-form.tsx` - Create/edit company dialog with Zod validation
- `frontend/app/(dashboard)/jobs/page.tsx` - Jobs data table page with company filter
- `frontend/components/jobs/job-list.tsx` - Jobs data table with salary formatting and action columns
- `frontend/components/jobs/job-form.tsx` - Create/edit job dialog with all fields including salary and closing date

## Decisions Made
- Used `standardSchemaResolver` from `@hookform/resolvers/standard-schema` instead of `zodResolver` from `@hookform/resolvers/zod` because Zod v4.3.6 has a version mismatch with the zodResolver type signatures (minor version `3` vs expected `0`)
- Salary min/max stored as string form fields and converted to numbers on submit to avoid `z.coerce.number()` type inference issues with Standard Schema resolver
- Company selection in job form uses Popover+Command combobox for searchable selection

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] zodResolver incompatible with Zod v4.3.6**
- **Found during:** Task 1 (CompanyForm)
- **Issue:** `zodResolver(companySchema)` fails TypeScript check -- Zod v4 `_zod.version.minor` is `3` but resolver expects `0`
- **Fix:** Switched to `standardSchemaResolver` from `@hookform/resolvers/standard-schema` which uses Standard Schema protocol (Zod v4 implements this)
- **Files modified:** frontend/components/companies/company-form.tsx, frontend/components/jobs/job-form.tsx
- **Verification:** `pnpm build` passes with no type errors
- **Committed in:** 44d76d4 (Task 1), d63cfd1 (Task 2)

**2. [Rule 3 - Blocking] z.coerce.number() type inference with Standard Schema resolver**
- **Found during:** Task 2 (JobForm)
- **Issue:** `z.coerce.number().optional()` produces `unknown` type through Standard Schema inference, causing resolver type mismatch
- **Fix:** Changed salary fields to `z.string().optional()` in schema, convert to numbers manually in `onSubmit`
- **Files modified:** frontend/components/jobs/job-form.tsx
- **Verification:** `pnpm build` passes
- **Committed in:** d63cfd1 (Task 2)

---

**Total deviations:** 2 auto-fixed (2 blocking)
**Impact on plan:** Both fixes required for Zod v4 compatibility. No scope creep.

## Issues Encountered
- Linter automatically reformatted Zod schema syntax (`.optional().or(z.literal(""))` to `.union([...]).optional()`) and changed import from `"zod"` to `"zod/v4"` -- resolved by using `"zod"` import with standardSchemaResolver

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- Companies and Jobs CRUD pages complete and accessible from sidebar navigation
- Ready for Plan 04 (Applications page) which can link to these companies and jobs
- Job form supports `defaultCompanyId` prop for pre-filling company from detail page

---
*Phase: 08-frontend-core-views*
*Completed: 2026-03-21*
