# Phase 10: Gap Closure - Context

**Gathered:** 2026-03-22
**Audited:** 2026-03-22
**Status:** Ready for planning

<domain>
## Phase Boundary

Complete three features that were backend-complete but missing UI or email delivery in v1.0: interview notes UI in InterviewsTab, document version history panel, and password reset email via SMTP. GAP-01 needs two new frontend hooks (`useUpdateInterviewNote`, `useDeleteInterviewNote`) and a type update (`InterviewNoteResponse` missing `noteType` + `interviewId` fields) — no backend changes. GAP-02 is purely frontend — all backend endpoints and frontend hooks/types already exist. GAP-03 needs backend SMTP integration (new dependency + email service + config) plus frontend verification of Better Auth UI reset flow.

</domain>

<decisions>
## Implementation Decisions

### Interview Notes UI (GAP-01)
- Inline expandable section per interview row — click interview to expand notes below it
- Note type selector dropdown when creating a note (5 types: PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL)
- Default note type: GENERAL
- Note types displayed as colored badges on each note (6 Badge variants available: default, secondary, destructive, outline, ghost, link)
- Inline editing: click note text to edit in-place, save on blur (follows existing QuickNotes pattern at `application-detail.tsx:234-244`)
- Delete via trash icon with ConfirmDialog (uses existing `confirm-dialog.tsx` component with AlertDialog wrapper)
- Need to implement missing `useUpdateInterviewNote()` and `useDeleteInterviewNote()` hooks (existing hooks at `use-interviews.ts:130-163`)
- Need to add `noteType` and `interviewId` fields to frontend `InterviewNoteResponse` type (currently at `api.ts:330-335`, only has id/content/createdAt/updatedAt — backend DTO has both fields at `InterviewNoteDtos.kt:23-30`)
- **Scope note:** Requirement says "view and add" but full CRUD (including edit/delete) is intentional — incomplete CRUD would be poor UX

### Document Version Panel (GAP-02)
- Expandable row in document list — click document to expand version history below it (consistent with interview notes pattern)
- Each version shows: version number, filename, file size, note, date, "Current" badge if applicable (all fields available in `DocumentVersionResponse` — 8 fields at `api.ts:289-298`)
- Non-current versions show "Set as Current" button; current version shows "Current" badge
- Upload new version: inline dropzone (react-dropzone v15.0.0, already used in `document-upload.tsx`) with optional note field inside expanded panel
- Download button per version (uses existing `useDownloadVersionUrl` hook at `use-documents.ts:220-225`)
- Delete via trash icon + ConfirmDialog; disable/hide delete button when only 1 version remains (backend already blocks, but prevent in UI too)
- **All hooks verified present:** `useDocumentVersions` (line 144), `useCreateDocumentVersion` (line 155), `useSetCurrentVersion` (line 195), `useDeleteDocumentVersion` (line 227), `useDownloadVersionUrl` (line 220) — all in `use-documents.ts`
- **Lowest implementation risk** of all 3 GAPs — purely UI work with all backend and hook infrastructure complete

### Password Reset Email (GAP-03)
- SMTP provider: Gmail SMTP with App Password
- Add `spring-boot-starter-mail` dependency to backend (confirmed absent from `build.gradle.kts`)
- SMTP credentials configured in application.yml (confirmed no `spring.mail.*` config exists yet — file is 42 lines)
- **Security note:** SMTP credentials will be in plain text in application.yml until Phase 17 (Sealed Secrets) — acceptable for local dev
- Simple HTML email template: single-column layout with app name header, reset link button, 1-hour expiry notice, "didn't request this" footer
- Update `PasswordResetService.requestReset()` to send email instead of logging to console (line 44: `logger.info("Password reset link: ...")`)
- Backend token flow already fully implemented and tested: UUID token, 1-hour expiry (line 40), rate-limited 3 requests/hour/email (line 29)
- Frontend: Better Auth UI `sendResetPassword` callback exists in `lib/auth.ts:9-16` but currently logs to console — the `AuthView` component with dynamic `[path]` routing should render forgot-password pages automatically via `authViewPaths`
- **Add SMTP error handling:** If email send fails, fall back to logging (don't break the reset flow)
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
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/InterviewNoteController.kt` — CRUD endpoints (create:27-34, list:36-43, update:45-53, delete:55-63)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/InterviewNoteDtos.kt` — CreateRequest (lines 9-15), UpdateRequest (lines 17-21), Response (lines 23-30) with noteType enum
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/entity/enums.kt` — InterviewNoteType enum (line 27): PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL
- `frontend/components/applications/application-detail.tsx` — InterviewsTab component (lines 453-558) where notes UI must be added
- `frontend/hooks/use-interviews.ts` — Existing: `useInterviewNotes` (lines 130-141), `useCreateInterviewNote` (lines 143-163). Missing: `useUpdateInterviewNote`, `useDeleteInterviewNote`
- `frontend/types/api.ts` — InterviewNoteResponse (lines 330-335) has only id/content/createdAt/updatedAt — needs `noteType: string` and `interviewId: string` fields to match backend DTO

### Document Versions (GAP-02)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/DocumentController.kt` — Version endpoints (create:88-97, list:99-103, set-current:105-112, download:114-125, delete:127-135)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/dto/DocumentDtos.kt` — DocumentVersionResponse DTO (lines 20-29, 8 fields: id, versionNumber, originalFilename, contentType, fileSize, note, isCurrent, createdAt)
- `frontend/components/documents/document-list.tsx` — Document list table with ActionCell (lines 39-76) — version panel must be added as expandable row
- `frontend/hooks/use-documents.ts` — All 5 version hooks verified: useDocumentVersions (144), useCreateDocumentVersion (155), useSetCurrentVersion (195), useDownloadVersionUrl (220), useDeleteDocumentVersion (227)
- `frontend/types/api.ts` — DocumentVersionResponse type (lines 289-298, 8 fields) — already complete, matches backend DTO

### Password Reset Email (GAP-03)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/PasswordResetService.kt` — Token flow: requestReset (lines 27-48, line 44 logs instead of emailing), confirmReset (lines 50-73, validates + updates password)
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/controller/AuthController.kt` — Reset endpoints: POST /api/auth/password-reset (lines 111-115), POST /api/auth/password-reset/confirm (lines 117-121)
- `backend/src/main/resources/application.yml` — 42 lines, no spring.mail.* config — needs addition
- `backend/build.gradle.kts` — 75 lines, no spring-boot-starter-mail — needs addition
- `backend/src/main/kotlin/com/alex/job/hunt/jobhunt/service/EmailVerificationService.kt` — InvalidTokenException defined (line 10), already used by PasswordResetService
- `frontend/app/auth/[path]/page.tsx` — Better Auth UI: AuthView component with dynamic path routing, generateStaticParams from authViewPaths
- `frontend/lib/auth.ts` — Server-side: sendResetPassword callback (lines 9-16) currently logs reset URL to console
- `frontend/lib/auth-client.ts` — Client-side: createAuthClient from better-auth/react

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets (all verified present)
- `ConfirmDialog` component at `components/shared/confirm-dialog.tsx:1-56`: AlertDialog wrapper with controlled state, variant prop (destructive/default), auto-close after confirm
- `react-dropzone` v15.0.0: Already used in `components/documents/document-upload.tsx:45-52` with onDrop callback, accept filter, isDragActive state
- `sonner` v2.0.7 toasts: Custom wrapper at `components/ui/sonner.tsx` with custom icons (CircleCheck, Info, TriangleAlert, OctagonX, Loader2), theme-aware
- `Dialog` at `components/ui/dialog.tsx:1-174` / `Sheet` at `components/ui/sheet.tsx:1-147`: Available but not needed (using expandable rows instead)
- TanStack Query hooks pattern: Query key factory at `hooks/use-applications.ts:39-52`, auto-invalidation on mutations, optimistic updates pattern at lines 129-178
- `QuickNotes` pattern in `application-detail.tsx:234-244`: Controlled component with local state override, onChange updates state, onBlur triggers save mutation, toast on success, reset state after save

### Established Patterns
- Expandable content: New pattern for this phase, but consistent with shadcn/ui Accordion/Collapsible
- Form data uploads: `FormData` with fetch API directly (not axios) for multipart at `hooks/use-documents.ts:80-104` — credentials: "include", manual JSON error parsing
- Badge variants: shadcn/ui Badge at `components/ui/badge.tsx` with 6 variants (default, secondary, destructive, outline, ghost, link) — extend for note types. StatusBadge pattern at `components/applications/status-badge.tsx:1-30` shows dynamic color mapping
- Controlled forms with React state + zod validation

### Integration Points
- InterviewsTab in `application-detail.tsx` (line 453) — notes section expands within each interview row
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

<audit>
## Audit Findings (2026-03-22)

**Verdict:** PASS — all canonical references verified at 100% accuracy against live codebase.

### Alignment Summary
- **REQUIREMENTS.md:** All 3 GAP requirements (GAP-01, GAP-02, GAP-03) addressed
- **ROADMAP.md:** All 3 success criteria match context scope, dependencies correct (none)
- **MILESTONES.md:** All 3 known gaps (INTV-03, DOCS-03, AUTH-04) targeted
- **RETROSPECTIVE.md:** Lessons applied — small plans per feature, E2E UI wiring focus, verify Better Auth before assuming
- **PROJECT.md:** Constraints followed — dependencies added only when phase starts
- **STATE.md:** Position correct (Phase 10 of 18, ready to plan)

### Verified Codebase State
- **GAP-01:** Backend CRUD complete (4 endpoints), 2 frontend hooks exist (notes query + create), 2 missing (update + delete), frontend type incomplete (missing noteType + interviewId)
- **GAP-02:** Backend complete (5 version endpoints), all 5 frontend hooks present, frontend type complete (8 fields match backend DTO) — lowest risk, purely UI
- **GAP-03:** Backend token flow fully implemented and tested (UUID token, 1-hour expiry, rate-limited), currently logs at line 44 — needs spring-boot-starter-mail dependency, email service, SMTP config. Frontend Better Auth UI has sendResetPassword callback in lib/auth.ts but logs to console

### Risks & Observations
1. **GAP-01 scope expansion:** Requirement says "view and add" but context includes edit/delete — intentional for complete CRUD UX
2. **GAP-03 SMTP credentials:** Plain text in application.yml until Phase 17 (Sealed Secrets) — accepted for local dev
3. **GAP-03 Better Auth UI:** sendResetPassword callback exists in lib/auth.ts:9-16 but verify forgot-password page renders before planning frontend work
4. **GAP-03 error handling:** Add SMTP failure fallback — if email send fails, log instead of breaking the reset flow
5. **STATE.md cleanup:** Duplicate frontmatter blocks (lines 1-14 and 16-30) should be consolidated
6. **No testing strategy specified:** v1.0 pattern was visual verification — consistent but worth noting

### Planning Recommendations
1. Create 3 separate plans (one per GAP requirement) — follows retrospective lesson about small plans
2. GAP-02 has lowest risk — consider executing first
3. Verify Better Auth UI forgot-password page during GAP-03 planning
4. Spring Boot 4.0.4 compatibility with spring-boot-starter-mail should be verified during research

</audit>

---

*Phase: 10-gap-closure*
*Context gathered: 2026-03-22*
*Audited: 2026-03-22*
