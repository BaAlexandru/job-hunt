---
phase: 08-frontend-core-views
plan: 06
subsystem: ui
tags: [next-themes, dark-mode, radix-dialog, tailwind, accessibility]

# Dependency graph
requires:
  - phase: 07-frontend-shell-auth-ui
    provides: ThemeProvider, topbar, dialog component, auth pages
provides:
  - Theme toggle button in topbar (light/dark switching)
  - Dialog with internal scroll and no accidental overlay close
  - Auth form width constraint on desktop
  - Improved dark theme contrast
affects: []

# Tech tracking
tech-stack:
  added: []
  patterns:
    - "onInteractOutside preventDefault on dialogs for mobile safety"
    - "max-h-[85vh] overflow-y-auto for fixed dialogs with long content"

key-files:
  created:
    - frontend/components/layout/theme-toggle.tsx
  modified:
    - frontend/components/layout/topbar.tsx
    - frontend/components/ui/dialog.tsx
    - frontend/app/auth/[path]/page.tsx
    - frontend/app/globals.css

key-decisions:
  - "Sun/Moon toggle with next-themes useTheme, simple dark/light binary toggle"
  - "onInteractOutside preventDefault on all dialogs to prevent accidental close on mobile"
  - "max-h-[85vh] overflow-y-auto instead of scrollIntoView for fixed-positioned dialog scroll"

patterns-established:
  - "Dialog content uses internal scroll (max-h-[85vh]) instead of page-level scroll workarounds"

requirements-completed: [APPL-03, APPL-04]

# Metrics
duration: 2min
completed: 2026-03-21
---

# Phase 08 Plan 06: UI/UX Gap Closure Summary

**Theme toggle with Sun/Moon icons, dialog internal scroll + overlay close prevention, auth form width constraint, and dark theme contrast improvements**

## Performance

- **Duration:** 2 min
- **Started:** 2026-03-21T15:45:21Z
- **Completed:** 2026-03-21T15:47:41Z
- **Tasks:** 3
- **Files modified:** 5

## Accomplishments
- Theme toggle (Sun/Moon) in topbar using next-themes, toggles between light and dark
- Dialog content capped at 85vh with internal scroll for long forms on mobile
- Overlay click/tap prevented from closing dialogs (must use X or Cancel)
- Auth forms constrained to max-w-md (448px) on desktop
- Dark theme contrast improved: cards, borders, inputs, sidebar all more visible

## Task Commits

Each task was committed atomically:

1. **Task 1: Create ThemeToggle component and add to Topbar** - `abf125a` (feat)
2. **Task 2: Fix dialog mobile overflow and prevent overlay close, constrain auth forms** - `c9cdcfe` (fix)
3. **Task 3: Improve dark theme contrast in globals.css** - `660acc2` (fix)

## Files Created/Modified
- `frontend/components/layout/theme-toggle.tsx` - Sun/Moon toggle using next-themes useTheme
- `frontend/components/layout/topbar.tsx` - Added ThemeToggle before UserButton
- `frontend/components/ui/dialog.tsx` - max-h-[85vh] overflow-y-auto + onInteractOutside
- `frontend/app/auth/[path]/page.tsx` - w-full max-w-md wrapper around AuthView
- `frontend/app/globals.css` - Dark theme contrast improvements (card, border, input, sidebar)

## Decisions Made
- Simple dark/light binary toggle (no system option in toggle, but system theme still works as default)
- onInteractOutside preventDefault on all dialogs globally, not per-instance
- max-h-[85vh] overflow-y-auto chosen over scrollIntoView since dialog is fixed-positioned

## Deviations from Plan

None - plan executed exactly as written.

## Issues Encountered
None

## User Setup Required
None - no external service configuration required.

## Next Phase Readiness
- All four UAT UI/UX issues resolved
- Build passes successfully
- Ready for remaining gap closure plans (08-07, 08-08)

---
*Phase: 08-frontend-core-views*
*Completed: 2026-03-21*
