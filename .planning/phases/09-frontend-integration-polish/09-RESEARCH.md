# Phase 9: Frontend Integration Polish - Research

**Researched:** 2026-03-21
**Domain:** Frontend contract fixes, UI wiring, route protection, dead code removal
**Confidence:** HIGH

## Summary

Phase 9 closes 5 specific integration gaps identified in the v1.0 milestone audit. All issues are precisely scoped: a Page response unwrapping bug in the interview notes hook, a missing Timeline tab in the application detail panel, a hardcoded document category, a route guard file naming verification, and one dead hook to remove. No new features, no new libraries, no architectural changes.

The backend API contracts are fully functional and verified. Every fix is contained to 1-2 frontend files. The existing codebase already has all required UI components (Tabs, Select, Sheet) and hooks (useTimeline, useInterviewNotes) in place -- they just need wiring or minor corrections.

**Primary recommendation:** Fix each gap in isolation -- they are independent changes that can be planned as separate small tasks.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Interview notes fix: unwrap Page response in the hook layer (useInterviewNotes), keep component code unchanged
- Timeline tab: 5th tab in application detail panel after Documents, use existing useTimeline() hook, fix field name mapping (backend: date/summary/details -> frontend: occurredAt/title/metadata), simple chronological feed most-recent-first
- Document category: shadcn/ui Select component, default OTHER, remove hardcoded category from upload mutation
- Route guard: verify proxy.ts discovery by Next.js 16, rename to middleware.ts if needed (fallback)
- Dead code: remove useApplicationTransitions hook and its imports

### Claude's Discretion
- Timeline entry visual design (icons per type, spacing, typography)
- Timeline expandable details interaction (accordion, inline expand, or always visible)
- Exact positioning of category dropdown in the upload form
- Loading/error states for the timeline tab
- Whether to fix field names in the frontend type or add a mapping layer in the hook

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope.
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| INTV-03 | User can add notes and conversation details per interview stage | Interview notes Page unwrapping fix enables correct rendering of notes in the Interviews tab |
| INTV-04 | User can view a timeline of all interactions and interview stages per application | Timeline tab implementation with field mapping connects existing backend/hook to UI |
| DOCS-05 | User can categorize documents by type (CV, cover letter, portfolio, other) | Category selector on upload form replaces hardcoded OTHER value |
| AUTH-02 | User can log in and stay logged in across sessions via JWT | Route guard verification ensures unauthenticated users are redirected to sign-in |
</phase_requirements>

## Standard Stack

### Core (already installed, no new dependencies)

| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Next.js | 16.2.0 | App framework with proxy.ts middleware | Already in use |
| TanStack Query | 5.91.3 | Server state + hooks | All API hooks use this |
| shadcn/ui (Radix) | latest | Select, Tabs, Sheet components | Already in use throughout |
| date-fns | 4.1.0 | Date formatting for timeline | Already imported in application-detail.tsx |
| lucide-react | 0.577.0 | Icons for timeline entries | Already imported |
| better-auth | 1.5.5 | Cookie-based session auth | Route guard uses getSessionCookie |

### No New Dependencies Required

This phase requires zero new packages. All UI components and utilities are already installed and in use.

## Architecture Patterns

### Pattern 1: Page Response Unwrapping in Hook Layer

**What:** Backend returns `Page<T>` (Spring Data paginated wrapper), frontend hook extracts `content` array.
**When to use:** Any hook calling a paginated backend endpoint where the component expects a flat array.
**Existing precedent:** `useApplicationNotes()` in `use-applications.ts` already does this correctly.

```typescript
// Source: frontend/hooks/use-applications.ts lines 204-215 (existing pattern)
export function useApplicationNotes(applicationId: string) {
  return useQuery({
    queryKey: applicationKeys.notes(applicationId),
    queryFn: async () => {
      const page = await apiClient<PaginatedResponse<NoteResponse>>(
        `/applications/${applicationId}/notes`,
      )
      return page.content  // <-- unwrap Page to array
    },
    enabled: !!applicationId,
  })
}
```

**The fix for useInterviewNotes:** Change `apiClient<InterviewNoteResponse[]>` to `apiClient<PaginatedResponse<InterviewNoteResponse>>` and return `page.content`. Identical pattern to useApplicationNotes.

### Pattern 2: Field Name Mapping in Hook Layer

**What:** Backend DTO field names differ from frontend TypeScript type. Map fields in the queryFn.
**When to use:** When the backend contract cannot be changed and the frontend type is used across multiple components.

**Backend TimelineEntry DTO fields:**
- `id: UUID`
- `date: Instant` (ISO string in JSON)
- `type: TimelineEntryType` (INTERVIEW, APPLICATION_NOTE, INTERVIEW_NOTE)
- `summary: String`
- `details: Map<String, Any?>?`

**Frontend TimelineEntry type fields:**
- `id: string`
- `occurredAt: string`
- `type: string`
- `title: string`
- `description: string | null`
- `metadata: Record<string, unknown> | null`

**Recommendation (Claude's discretion):** Add a mapping function in the hook rather than changing the frontend type. This keeps the frontend type semantically clear and isolates the contract translation.

```typescript
// Mapping in useTimeline hook
function mapTimelineEntry(raw: BackendTimelineEntry): TimelineEntry {
  return {
    id: raw.id,
    type: raw.type,
    occurredAt: raw.date,
    title: raw.summary,
    description: null,  // backend has no separate description
    metadata: raw.details ?? null,
  }
}
```

### Pattern 3: Adding a Tab to Application Detail Panel

**What:** The application detail panel uses shadcn/ui Tabs with TabsList/TabsTrigger/TabsContent.
**Current tabs:** Overview, Notes, Interviews, Documents (4 tabs).
**Addition:** Timeline tab as 5th tab after Documents.

```typescript
// In the TabsList, add:
<TabsTrigger value="timeline">Timeline</TabsTrigger>

// After the Documents TabsContent, add:
<TabsContent value="timeline">
  <TimelineTab applicationId={applicationId} />
</TabsContent>
```

### Pattern 4: Category Selector on Upload Form

**What:** Replace hardcoded `formData.append("category", "OTHER")` with user-selected value.
**Backend accepts:** CV, COVER_LETTER, PORTFOLIO, OTHER (DocumentCategory enum).
**UI:** shadcn/ui Select component with state, defaulting to OTHER.

### Pattern 5: Next.js 16 proxy.ts Convention

**What:** Next.js 16 renamed `middleware.ts` to `proxy.ts` with exported `proxy()` function.
**Current state:** File exists as `frontend/proxy.ts` with exported `proxy()` function and `config.matcher`.
**Verification:** This is the correct convention for Next.js 16.2.0. The file SHOULD be auto-discovered. The export name `proxy` matches the Next.js 16 convention.

### Anti-Patterns to Avoid
- **Fixing field mapping in the component:** Keep mapping in the hook layer, not scattered across components.
- **Changing backend DTOs to match frontend types:** Backend API is stable and tested; frontend adapts.
- **Adding pagination UI for interview notes:** The current usage does not need pagination controls; just unwrap all content.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Category selector UI | Custom dropdown | shadcn/ui Select | Already used throughout the app, consistent styling |
| Timeline feed layout | Custom list layout | Simple div + map with existing date-fns formatting | No need for a timeline library for a simple chronological list |
| Route protection | Custom auth check logic | better-auth getSessionCookie + proxy.ts | Already implemented, just needs verification |

**Key insight:** Everything needed is already in the codebase. This phase is about wiring and fixing, not building new infrastructure.

## Common Pitfalls

### Pitfall 1: Forgetting to Define Backend Response Type for Mapping
**What goes wrong:** If you type the apiClient generic as `TimelineEntry[]` (the frontend type), the mapping step gets skipped because TypeScript thinks the data is already in the right shape.
**Why it happens:** The frontend and backend types have different field names but the same structure, so TypeScript won't catch the mismatch at compile time if you use the wrong type.
**How to avoid:** Define a `BackendTimelineEntry` interface matching the actual backend field names (date, summary, details) and use it as the apiClient generic type. Then map to the frontend `TimelineEntry` type.
**Warning signs:** Timeline entries render with undefined title/occurredAt fields.

### Pitfall 2: Not Handling Empty Timeline
**What goes wrong:** Timeline tab shows a blank panel with no feedback.
**How to avoid:** Add an empty state message like "No timeline entries yet" (same pattern used in Notes and Interviews tabs).

### Pitfall 3: Proxy.ts Export Name Mismatch
**What goes wrong:** If the export function is named `middleware` instead of `proxy`, Next.js 16 won't discover it.
**Current state:** The file already exports `proxy()` which is correct. No change needed unless runtime verification fails.
**How to avoid:** Verify the function export name matches `proxy` (already correct in current code).

### Pitfall 4: Category Select State Not Resetting Between Uploads
**What goes wrong:** If multiple files are dropped, the category from the first upload persists.
**How to avoid:** The category state should be captured before the loop and applied to each file, or provide a way to set category before dropping files.

### Pitfall 5: useApplicationTransitions Key Factory Left Behind
**What goes wrong:** The `transitions` key factory method in `applicationKeys` becomes dead code after removing the hook.
**How to avoid:** Also remove the `transitions` key factory entry from `applicationKeys`. However, check if `useUpdateApplicationStatus` references `applicationKeys.transitions` in its `onSettled` (it does on line 182-184) -- that invalidation can be removed since the hook is dead, but keeping it is harmless since it just invalidates a query that no one is listening to.
**Recommendation:** Remove `useApplicationTransitions` function and its import references. Leave the `transitions` key factory and the invalidation in `useUpdateApplicationStatus.onSettled` -- removing them risks breaking the optimistic update rollback flow, and they are inert overhead.

## Code Examples

### Fix 1: Interview Notes Page Unwrapping

```typescript
// Source: Verified from backend InterviewNoteController.kt (returns Page<InterviewNoteResponse>)
// and existing pattern in useApplicationNotes
export function useInterviewNotes(interviewId: string) {
  return useQuery({
    queryKey: interviewKeys.notes(interviewId),
    queryFn: async () => {
      const page = await apiClient<PaginatedResponse<InterviewNoteResponse>>(
        `/interviews/${interviewId}/notes`,
      )
      return page.content
    },
    enabled: !!interviewId,
  })
}
```

### Fix 2: Timeline Hook with Field Mapping

```typescript
// Source: Verified from backend TimelineDtos.kt (date, summary, details fields)
interface BackendTimelineEntry {
  id: string
  date: string
  type: string
  summary: string
  details: Record<string, unknown> | null
}

export function useTimeline(applicationId: string) {
  return useQuery({
    queryKey: applicationKeys.timeline(applicationId),
    queryFn: async () => {
      const entries = await apiClient<BackendTimelineEntry[]>(
        `/applications/${applicationId}/timeline`,
      )
      return entries.map((e) => ({
        id: e.id,
        type: e.type,
        occurredAt: e.date,
        title: e.summary,
        description: null,
        metadata: e.details,
      })) as TimelineEntry[]
    },
    enabled: !!applicationId,
  })
}
```

### Fix 3: Document Category Selector

```typescript
// Source: Verified from backend DocumentCategory enum: CV, COVER_LETTER, PORTFOLIO, OTHER
const DOCUMENT_CATEGORIES = [
  { value: "OTHER", label: "Other" },
  { value: "CV", label: "CV" },
  { value: "COVER_LETTER", label: "Cover Letter" },
  { value: "PORTFOLIO", label: "Portfolio" },
] as const
```

### Fix 4: Timeline Tab Component Structure

```typescript
// Source: Existing tab pattern from application-detail.tsx
function TimelineTab({ applicationId }: { applicationId: string }) {
  const { data: entries, isLoading } = useTimeline(applicationId)

  if (isLoading) {
    return (
      <div className="flex flex-col gap-3 pt-4">
        <Skeleton className="h-12 w-full" />
        <Skeleton className="h-12 w-full" />
      </div>
    )
  }

  if (!entries || entries.length === 0) {
    return (
      <p className="pt-4 text-sm text-muted-foreground">
        No timeline entries yet.
      </p>
    )
  }

  // Entries already sorted descending by backend (Phase 05 decision)
  return (
    <div className="flex flex-col gap-3 pt-4">
      {entries.map((entry) => (
        <div key={entry.id} className="flex gap-3 rounded-md border p-3">
          {/* Type icon + date + title + expandable details */}
        </div>
      ))}
    </div>
  )
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| middleware.ts | proxy.ts | Next.js 16.0 (2025) | File and export rename; old name deprecated with warning |

**Deprecated/outdated:**
- `middleware.ts` export in Next.js 16: Still works but shows deprecation warning. Project already uses `proxy.ts` correctly.

## Open Questions

1. **proxy.ts runtime verification**
   - What we know: The file exists with correct export name (`proxy`) and correct matcher config. Next.js 16.2.0 officially uses `proxy.ts` convention.
   - What's unclear: Whether it actually works at runtime (the audit flagged this as needing verification).
   - Recommendation: Plan a manual verification step -- start dev server, navigate to `/dashboard` without auth, confirm redirect to `/auth/sign-in`. If it fails, the fallback (rename to middleware.ts) is documented in CONTEXT.md, but based on research this should NOT be needed since `proxy.ts` is the correct Next.js 16 convention.

2. **Timeline entry type icons**
   - What we know: Backend returns type as INTERVIEW, APPLICATION_NOTE, or INTERVIEW_NOTE.
   - Recommendation (Claude's discretion): Use lucide-react icons already imported -- `CalendarIcon` for INTERVIEW, `StickyNoteIcon` or `MessageSquareIcon` for notes. Keep it simple.

3. **Timeline details expandability**
   - Recommendation (Claude's discretion): Start with always-visible details (simplest). The details map is small (interview type, stage, location). No need for accordion complexity.

## Validation Architecture

### Test Framework

| Property | Value |
|----------|-------|
| Framework | Vitest + React Testing Library |
| Config file | `frontend/vitest.config.ts` |
| Quick run command | `cd frontend && pnpm test` |
| Full suite command | `cd frontend && pnpm test` |

### Phase Requirements to Test Map

| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INTV-03 | Interview notes render correctly (Page unwrap) | manual | Start dev, open application detail, view Interviews tab, check notes display | N/A |
| INTV-04 | Timeline tab displays chronological entries | manual | Start dev, open application detail, click Timeline tab, verify entries | N/A |
| DOCS-05 | Document upload includes category selector | manual | Start dev, go to Documents page, verify dropdown appears on upload form | N/A |
| AUTH-02 | Unauthenticated redirect to sign-in | manual | Start dev, clear cookies, navigate to /dashboard, verify redirect | N/A |

### Sampling Rate
- **Per task commit:** `cd frontend && pnpm build` (type checking + build verification)
- **Per wave merge:** `cd frontend && pnpm build && pnpm lint`
- **Phase gate:** Build passes + manual verification of all 5 success criteria

### Wave 0 Gaps
None -- all changes are small targeted fixes that are best verified manually via the running application. Build/lint provides automated sanity checking.

## Sources

### Primary (HIGH confidence)
- `backend/src/main/kotlin/.../controller/InterviewNoteController.kt` -- Confirmed Page<InterviewNoteResponse> return type
- `backend/src/main/kotlin/.../dto/TimelineDtos.kt` -- Confirmed field names: id, date, type, summary, details
- `backend/src/main/kotlin/.../controller/TimelineController.kt` -- Confirmed List<TimelineEntry> return (not paginated)
- `backend/src/main/kotlin/.../entity/enums.kt` -- Confirmed DocumentCategory: CV, COVER_LETTER, PORTFOLIO, OTHER
- `frontend/hooks/use-applications.ts` -- Confirmed useApplicationNotes Page unwrap pattern (lines 207-213)
- `frontend/hooks/use-interviews.ts` -- Confirmed bug: line 134 uses `InterviewNoteResponse[]` instead of `PaginatedResponse<InterviewNoteResponse>`
- `frontend/components/applications/application-detail.tsx` -- Confirmed 4-tab structure, useTimeline imported but not called
- `frontend/components/documents/document-upload.tsx` -- Confirmed hardcoded `category: "OTHER"` on line 19
- `frontend/proxy.ts` -- Confirmed correct Next.js 16 convention (export function proxy, config.matcher)

### Secondary (MEDIUM confidence)
- [Next.js 16 proxy.ts convention](https://nextjs.org/docs/app/api-reference/file-conventions/proxy) -- Confirmed proxy.ts is the correct file convention for Next.js 16
- [Next.js middleware-to-proxy migration](https://nextjs.org/docs/messages/middleware-to-proxy) -- Confirmed rename from middleware.ts to proxy.ts with export name change

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - all libraries already installed, zero new dependencies
- Architecture: HIGH - all patterns already established in codebase, just replicating existing patterns
- Pitfalls: HIGH - every issue is precisely scoped by the audit report with clear code locations

**Research date:** 2026-03-21
**Valid until:** 2026-04-21 (stable -- no moving parts)
