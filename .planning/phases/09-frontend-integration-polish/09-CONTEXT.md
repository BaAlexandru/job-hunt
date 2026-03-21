# Phase 9: Frontend Integration Polish - Context

**Gathered:** 2026-03-21
**Status:** Ready for planning

<domain>
## Phase Boundary

Close integration gaps found during v1.0 milestone audit. Fix frontend-backend contract mismatches, wire missing UI features, verify route protection, and remove dead code. All 5 items are defined by the audit report (INT-01 through INT-04 + degraded proxy.ts flow). No new features — purely closing what's already built but not properly connected.

</domain>

<decisions>
## Implementation Decisions

### Interview notes contract (INT-01)
- Backend GET /api/interviews/{id}/notes returns Page<InterviewNoteResponse> (paginated wrapper)
- Frontend useInterviewNotes() hook must unwrap the Page response to extract the content array
- Fix in the hook layer — keep the component code unchanged

### Timeline tab (INT-02)
- Add a "Timeline" tab to the application detail panel (5th tab after Documents)
- Use the existing useTimeline() hook in use-applications.ts to fetch data
- Fix field name mapping: backend returns date/summary/details, frontend type expects occurredAt/title/metadata
- Display as a simple chronological feed (most recent first) — each entry shows type icon, date, summary, and expandable details

### Document category selector (INT-03)
- Add category dropdown to the document upload form (CV, Cover Letter, Portfolio, Other)
- Use shadcn/ui Select component — consistent with other form patterns
- Default selection: OTHER (preserves current behavior if user doesn't change it)
- Remove the hardcoded category from the upload mutation

### Route guard verification (degraded flow)
- Verify proxy.ts is correctly discovered by Next.js 16 at runtime
- Unauthenticated users hitting /dashboard/* routes should redirect to /auth/sign-in
- If proxy.ts naming doesn't work, rename to middleware.ts (Next.js convention)

### Dead code cleanup (INT-04)
- Remove useApplicationTransitions hook from use-applications.ts
- Remove any imports referencing it
- No other cleanup needed — hook is the only dead code identified

### Claude's Discretion
- Timeline entry visual design (icons per type, spacing, typography)
- Timeline expandable details interaction (accordion, inline expand, or always visible)
- Exact positioning of category dropdown in the upload form
- Any loading/error states for the timeline tab
- Whether to fix field names in the frontend type or add a mapping layer in the hook

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Audit report (defines all 5 gaps)
- `.planning/v1.0-MILESTONE-AUDIT.md` — INT-01 through INT-04 gap definitions, degraded proxy.ts flow, tech debt items

### Backend API contracts
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/InterviewNoteController.kt` — Interview notes endpoint (Page response)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/TimelineController.kt` — Timeline endpoint (date/summary/details fields)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/DocumentController.kt` — Document upload endpoint (category parameter)

### Frontend files to modify
- `frontend/hooks/use-interviews.ts` — Interview notes hook (Page unwrapping fix)
- `frontend/hooks/use-applications.ts` — Timeline hook (field mapping) + useApplicationTransitions (remove)
- `frontend/components/applications/application-detail.tsx` — Add Timeline tab
- `frontend/components/documents/document-upload.tsx` — Add category selector
- `frontend/proxy.ts` — Route guard verification

### Prior phase context
- `.planning/phases/08-frontend-core-views/08-CONTEXT.md` — Application detail panel design, tab structure, component patterns

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `Tabs` component (shadcn/ui): Already used for 4 tabs in application detail — add 5th
- `Select` component (shadcn/ui): Available for category dropdown
- `useTimeline()` hook: Already implemented in use-applications.ts, just needs wiring
- `useInterviewNotes()` hook: Exists, needs Page response unwrapping

### Established Patterns
- Application detail panel uses TabsList/TabsTrigger/TabsContent pattern
- Form fields use shadcn/ui components with react-hook-form + Zod validation
- API hooks follow TanStack Query pattern with query key factory
- Document upload uses react-dropzone with mutation hooks

### Integration Points
- Timeline tab connects to existing application detail panel (add tab trigger + content)
- Category selector connects to existing document upload form
- Interview notes fix is contained to the hook's response transformation
- Route guard is standalone middleware file

</code_context>

<specifics>
## Specific Ideas

No specific requirements — all 5 items are precisely defined by the milestone audit report. Standard approaches apply.

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope.

</deferred>

---

*Phase: 09-frontend-integration-polish*
*Context gathered: 2026-03-21*
