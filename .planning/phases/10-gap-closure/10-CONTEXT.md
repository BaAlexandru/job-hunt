# Phase 10: Gap Closure - Context

**Gathered:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Complete three features that were backend-complete but missing UI or email delivery in v1.0: interview notes UI in InterviewsTab, document version history panel, and password reset email via SMTP. No new backend entities or API changes needed for GAP-01/GAP-02 — purely frontend work. GAP-03 needs backend SMTP integration plus frontend verification.

</domain>

<decisions>
## Implementation Decisions

### Interview Notes UI (GAP-01)
- Inline expandable section per interview row — click interview to expand notes below it
- Note type selector dropdown when creating a note (5 types: PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL)
- Default note type: GENERAL
- Note types displayed as colored badges on each note
- Inline editing: click note text to edit in-place, save on blur (follows existing QuickNotes pattern)
- Delete via trash icon with ConfirmDialog (uses existing ConfirmDialog component)
- Need to implement missing `useUpdateInterviewNote()` and `useDeleteInterviewNote()` hooks
- Need to add `noteType` and `interviewId` fields to frontend `InterviewNoteResponse` type

### Document Version Panel (GAP-02)
- Expandable row in document list — click document to expand version history below it (consistent with interview notes pattern)
- Each version shows: version number, filename, file size, note, date, "Current" badge if applicable
- Non-current versions show "Set as Current" button; current version shows "Current" badge
- Upload new version: inline dropzone (react-dropzone, already in project) with optional note field inside expanded panel
- Download button per version (uses existing `useDownloadVersionUrl` hook)
- Delete via trash icon + ConfirmDialog; disable/hide delete button when only 1 version remains (backend already blocks, but prevent in UI too)

### Password Reset Email (GAP-03)
- SMTP provider: Gmail SMTP with App Password
- Add `spring-boot-starter-mail` dependency to backend
- SMTP credentials configured in application.yml (secrets management via AWS service deferred to later phase)
- Simple HTML email template: single-column layout with app name header, reset link button, 1-hour expiry notice, "didn't request this" footer
- Update `PasswordResetService.requestReset()` to send email instead of logging to console
- Frontend: use existing Better Auth UI for forgot-password and reset-confirm pages, wire to custom backend endpoints
- Verify Better Auth UI includes password reset flow; fill gaps only if needed

### Claude's Discretion
- Exact color scheme for note type badges
- Typography and spacing within expandable sections
- HTML email template styling details
- Loading states and skeleton UI for expandable sections
- Error handling and toast messages

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Interview Notes (GAP-01)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/InterviewNoteController.kt` — CRUD endpoints for interview notes
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/InterviewNoteDtos.kt` — Request/response DTOs with noteType enum
- `frontend/components/applications/application-detail.tsx` — InterviewsTab component (lines 437-558) where notes UI must be added
- `frontend/hooks/use-interviews.ts` — Existing `useInterviewNotes` and `useCreateInterviewNote` hooks
- `frontend/types/api.ts` — InterviewNoteResponse type (needs noteType + interviewId fields)

### Document Versions (GAP-02)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/DocumentController.kt` — Version endpoints (create, list, set-current, download, delete)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/DocumentDtos.kt` — DocumentVersionResponse DTO
- `frontend/components/documents/document-list.tsx` — Document list where version panel must be added
- `frontend/hooks/use-documents.ts` — All version hooks already exist (query, create, set-current, delete, download URL)
- `frontend/types/api.ts` — DocumentVersionResponse type (already complete)

### Password Reset Email (GAP-03)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/PasswordResetService.kt` — Token flow (line 44 logs instead of emailing)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/AuthController.kt` — Reset endpoints
- `backend/src/main/resources/application.yml` — Where spring.mail.* config goes
- `frontend/app/auth/[path]/page.tsx` — Better Auth UI pages (verify reset flow exists)
- `frontend/lib/auth-client.ts` — Auth client configuration

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `ConfirmDialog` component: Used for destructive actions throughout the app — reuse for note/version delete
- `react-dropzone`: Already used for document upload — reuse for version upload inline dropzone
- `sonner` toasts: Success/error notifications across the app
- `Dialog` / `Sheet` components: Available but not needed (using expandable rows instead)
- TanStack Query hooks pattern: query key factory + auto-invalidation on mutations
- `QuickNotes` component in application-detail: Inline edit-on-click pattern to follow for interview notes

### Established Patterns
- Expandable content: New pattern for this phase, but consistent with shadcn/ui Accordion/Collapsible
- Form data uploads: `FormData` with fetch API directly (not axios) for multipart — follow existing document upload pattern
- Badge variants: shadcn/ui Badge component with color variants for status — extend for note types
- Controlled forms with React state + zod validation

### Integration Points
- InterviewsTab in `application-detail.tsx` — notes section expands within each interview row
- Document list in `document-list.tsx` — version panel expands within each document row
- `PasswordResetService` — replace `logger.info()` call with email send
- `build.gradle.kts` — add `spring-boot-starter-mail` dependency

</code_context>

<specifics>
## Specific Ideas

- Both expandable patterns (notes and versions) should feel consistent — same expand/collapse animation, similar layout structure
- Interview notes inline edit should follow the existing QuickNotes save-on-blur pattern already in the app
- Password reset email should be minimal and professional — no heavy branding, just functional

</specifics>

<deferred>
## Deferred Ideas

None — discussion stayed within phase scope

</deferred>

---

*Phase: 10-gap-closure*
*Context gathered: 2026-03-22*
