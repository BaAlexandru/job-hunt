# Phase 10: Gap Closure - Research

**Researched:** 2026-03-22
**Domain:** Frontend UI completion (React/Next.js), Backend SMTP email (Spring Boot/Kotlin)
**Confidence:** HIGH

## Summary

Phase 10 closes three feature gaps from v1.0: interview notes UI (GAP-01), document version history panel (GAP-02), and password reset email delivery (GAP-03). The research confirms that GAP-02 is the lowest risk item (purely UI, all hooks and types present). GAP-01 requires two missing frontend hooks and a type update but follows well-established patterns already in the codebase. GAP-03 is the only item requiring backend changes (spring-boot-starter-mail dependency, SMTP config, email service).

All three features have complete backend implementations. The frontend uses TanStack Query for data fetching with a consistent hook pattern, react-dropzone for file uploads, shadcn/ui for components, and sonner for toast notifications. The expandable row pattern (needed for GAP-01 and GAP-02) is new to the app but straightforward with React state -- no additional libraries needed. Better Auth UI already includes forgot-password and reset-password pages via `authViewPaths`, so frontend work for GAP-03 is limited to verifying the flow works end-to-end and updating the `sendResetPassword` callback.

**Primary recommendation:** Execute GAP-02 first (lowest risk, purely UI), then GAP-01 (missing hooks + type fix), then GAP-03 (backend dependency + email service + verification).

<user_constraints>

## User Constraints (from CONTEXT.md)

### Locked Decisions
- **GAP-01:** Inline expandable section per interview row; note type selector dropdown (5 types: PREPARATION, QUESTION_ASKED, FEEDBACK, FOLLOW_UP, GENERAL); default note type GENERAL; colored badges per note type; inline editing with save-on-blur (QuickNotes pattern); delete via trash icon + ConfirmDialog; implement missing useUpdateInterviewNote and useDeleteInterviewNote hooks; add noteType and interviewId to frontend InterviewNoteResponse type; full CRUD intentional (not just view+add)
- **GAP-02:** Expandable row in document list; each version shows version number, filename, file size, note, date, "Current" badge; non-current versions show "Set as Current" button; upload new version via inline dropzone with optional note; download button per version; delete via trash icon + ConfirmDialog; disable delete when only 1 version remains; all hooks verified present
- **GAP-03:** Gmail SMTP with App Password; add spring-boot-starter-mail dependency; SMTP config in application.yml (plain text acceptable until Phase 17); simple HTML email template (single-column, app name header, reset link button, 1-hour expiry notice, "didn't request this" footer); update PasswordResetService.requestReset() to send email instead of logging; SMTP error handling with fallback to logging; verify Better Auth UI includes password reset flow
- **Both expandable patterns should feel consistent** -- same expand/collapse animation, similar layout structure
- **Interview notes inline edit follows existing QuickNotes save-on-blur pattern**
- **Password reset email should be minimal and professional**

### Claude's Discretion
- Exact color scheme for note type badges
- Typography and spacing within expandable sections
- HTML email template styling details
- Loading states and skeleton UI for expandable sections
- Error handling and toast messages

### Deferred Ideas (OUT OF SCOPE)
None -- discussion stayed within phase scope

</user_constraints>

<phase_requirements>

## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| GAP-01 | User can view and add interview notes in the InterviewsTab UI | Existing hooks (useInterviewNotes, useCreateInterviewNote), backend CRUD endpoints verified, expandable row pattern documented, QuickNotes save-on-blur pattern for inline edit, Badge component for note types |
| GAP-02 | User can view document version history and upload new versions in the UI | All 5 version hooks verified present, DocumentVersionResponse type complete (8 fields), react-dropzone already used in document-upload.tsx, expandable row pattern documented |
| GAP-03 | User receives password reset email via SMTP when requesting a reset | spring-boot-starter-mail dependency documented, JavaMailSender auto-configuration verified, PasswordResetService line 44 identified for replacement, Better Auth UI forgot-password/reset-password paths confirmed available |

</phase_requirements>

## Standard Stack

### Core (Already in Project)
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| @tanstack/react-query | ^5.91.3 | Data fetching, mutations, cache invalidation | Already used for all API hooks |
| @tanstack/react-table | ^8.21.3 | Table rendering with sorting | Used in document-list, data-table |
| react-dropzone | ^15.0.0 | File upload drag-and-drop | Already used in document-upload.tsx |
| shadcn/ui | ^4.1.0 | UI component library (Badge, Button, Select, etc.) | Project standard |
| sonner | ^2.0.7 | Toast notifications | Already configured with custom icons |
| @daveyplate/better-auth-ui | ^3.3.15 | Auth UI (login, signup, forgot-password, reset-password) | Already integrated with AuthView + dynamic routing |
| spring-boot-starter-mail | (managed by BOM) | JavaMailSender auto-configuration | Spring Boot standard for email |

### New Dependencies Required
| Library | Purpose | Added Where |
|---------|---------|-------------|
| spring-boot-starter-mail | SMTP email sending via JavaMailSender | backend/build.gradle.kts |

No new frontend dependencies needed. All required libraries are already installed.

**Installation (backend only):**
```kotlin
// In backend/build.gradle.kts dependencies block:
implementation("org.springframework.boot:spring-boot-starter-mail")
```

## Architecture Patterns

### Expandable Row Pattern (NEW -- used by GAP-01 and GAP-02)

Both interview notes and document versions use an expandable section within a list item. This is implemented with React state (`useState<string | null>` for expanded ID), not a library component. No Collapsible or Accordion component exists in the project currently.

**Pattern:**
```typescript
// Source: Derived from existing codebase patterns
const [expandedId, setExpandedId] = useState<string | null>(null)

// Toggle on row click
const toggleExpand = (id: string) => {
  setExpandedId(prev => prev === id ? null : id)
}

// In JSX: render expanded content conditionally after the row
{items.map((item) => (
  <div key={item.id}>
    <div onClick={() => toggleExpand(item.id)}>
      {/* Row content + ChevronDown/ChevronRight icon */}
    </div>
    {expandedId === item.id && (
      <div className="border-l-2 ml-4 pl-4 pb-4">
        {/* Expanded content: notes list or version history */}
      </div>
    )}
  </div>
))}
```

### Inline Edit Pattern (QuickNotes -- GAP-01 reuses)

**Source:** `application-detail.tsx:234-244`
```typescript
// Local state overrides server value, onChange updates state, onBlur saves
const [value, setValue] = useState(serverValue)
<Textarea
  value={value}
  onChange={(e) => setValue(e.target.value)}
  onBlur={saveToServer}  // mutation.mutate() + toast
/>
```

### TanStack Query Hook Pattern (for missing hooks)

**Source:** `use-interviews.ts:143-163` (useCreateInterviewNote as template)
```typescript
export function useUpdateInterviewNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ interviewId, noteId, content }: {
      interviewId: string; noteId: string; content: string
    }) => apiClient<InterviewNoteResponse>(
      `/interviews/${interviewId}/notes/${noteId}`,
      { method: "PUT", body: JSON.stringify({ content }) },
    ),
    onSuccess: (_data, { interviewId }) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.notes(interviewId),
      })
    },
  })
}

export function useDeleteInterviewNote() {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: ({ interviewId, noteId }: {
      interviewId: string; noteId: string
    }) => apiClient<void>(
      `/interviews/${interviewId}/notes/${noteId}`,
      { method: "DELETE" },
    ),
    onSuccess: (_data, { interviewId }) => {
      queryClient.invalidateQueries({
        queryKey: interviewKeys.notes(interviewId),
      })
    },
  })
}
```

### Note Type Badge Pattern

**Source:** `status-badge.tsx:1-30` shows dynamic color mapping pattern
```typescript
const NOTE_TYPE_COLORS: Record<string, { bg: string; text: string }> = {
  PREPARATION: { bg: "bg-blue-100", text: "text-blue-800" },
  QUESTION_ASKED: { bg: "bg-purple-100", text: "text-purple-800" },
  FEEDBACK: { bg: "bg-green-100", text: "text-green-800" },
  FOLLOW_UP: { bg: "bg-amber-100", text: "text-amber-800" },
  GENERAL: { bg: "bg-gray-100", text: "text-gray-800" },
}
```

### Spring Boot Email Service Pattern

**Source:** Spring Boot official docs
```kotlin
@Service
class EmailService(
    private val mailSender: JavaMailSender
) {
    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    fun sendPasswordResetEmail(to: String, resetUrl: String) {
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setTo(to)
            helper.setSubject("Reset your JobHunt password")
            helper.setText(buildResetEmailHtml(resetUrl), true)
            helper.setFrom("noreply@job-hunt.dev")
            mailSender.send(message)
        } catch (e: Exception) {
            logger.error("Failed to send password reset email to $to", e)
            logger.info("Password reset link (email failed): $resetUrl")
        }
    }
}
```

### Anti-Patterns to Avoid
- **Modifying DataTable component for expandable rows:** The existing DataTable is a generic reusable component. Do NOT add expandable row logic to it. Instead, build custom list rendering in the specific components (InterviewsTab, DocumentList).
- **Creating a separate page for version history:** Versions should expand inline within the document list, not navigate to a new route.
- **Sending noteType on update requests:** The backend UpdateInterviewNoteRequest only accepts `content` (not noteType). NoteType is set only at creation time.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| SMTP email sending | Custom HTTP email API calls | spring-boot-starter-mail + JavaMailSender | Connection pooling, retry, MIME handling, auto-configuration |
| File upload UI | Custom drag-and-drop | react-dropzone (already in project) | Browser compatibility, accessibility, drag events |
| Data fetching/caching | Manual fetch + useState | TanStack Query hooks (already in project) | Cache invalidation, loading/error states, deduplication |
| Auth reset UI pages | Custom forgot-password forms | Better Auth UI AuthView (already in project) | Consistent auth flow, token handling, redirect logic |

## Common Pitfalls

### Pitfall 1: Frontend type mismatch with backend DTO
**What goes wrong:** InterviewNoteResponse on frontend has only 4 fields (id, content, createdAt, updatedAt) but backend returns 6 fields (adds interviewId, noteType). JSON deserialization silently drops extra fields, but code that references noteType or interviewId gets undefined.
**Why it happens:** Type was defined during initial scaffolding before backend DTO was finalized.
**How to avoid:** Update `frontend/types/api.ts` InterviewNoteResponse FIRST, before building any UI that references noteType or interviewId.
**Warning signs:** Badge shows "undefined" or no color mapping found.

### Pitfall 2: Better Auth UI reset flow relies on sendResetPassword callback
**What goes wrong:** The `ForgotPasswordForm` in Better Auth UI calls `authClient.requestPasswordReset()` which triggers the server-side `sendResetPassword` callback. If this callback does not actually send an email, the user sees "success" but never receives an email.
**Why it happens:** The callback currently just logs to console in `lib/auth.ts:14-15`.
**How to avoid:** There are TWO password reset paths in this app: (1) Better Auth's own path via `lib/auth.ts` sendResetPassword callback, and (2) the custom backend path via `PasswordResetService`. The CONTEXT.md specifies updating `PasswordResetService.requestReset()` to send email. Verify which path the Better Auth UI actually uses and ensure email sending works for that path.
**Warning signs:** User clicks "forgot password" and sees success but no email arrives.

### Pitfall 3: createInterviewNote hook does not send noteType
**What goes wrong:** The existing `useCreateInterviewNote` hook only sends `{ content }` in the body. The backend accepts `noteType` as optional (defaults to null which then defaults to GENERAL on the entity). But the UI decision requires a note type selector.
**Why it happens:** Hook was created before noteType was planned for the UI.
**How to avoid:** Update the `useCreateInterviewNote` mutation to accept and send `noteType` alongside `content`.
**Warning signs:** All notes created via UI show as GENERAL regardless of dropdown selection.

### Pitfall 4: Document version delete when only one version exists
**What goes wrong:** Backend returns 400 when trying to delete the last version, but if UI doesn't prevent it, user sees an error toast.
**Why it happens:** Backend validation exists but UI doesn't match.
**How to avoid:** Disable or hide the delete button when version list has only 1 item. Check `versions.length <= 1`.
**Warning signs:** Error toast on delete attempt.

### Pitfall 5: Gmail SMTP App Password configuration
**What goes wrong:** Gmail blocks standard password login for SMTP. Must use an App Password (16-character code from Google Account security settings) with 2FA enabled.
**Why it happens:** Google deprecated "less secure app access" years ago.
**How to avoid:** Document that the user needs to: (1) enable 2-step verification on their Google account, (2) generate an App Password at myaccount.google.com/apppasswords, (3) use that 16-char code in application.yml.
**Warning signs:** Authentication failures with javax.mail.AuthenticationFailedException.

## Code Examples

### GAP-01: InterviewNoteResponse type fix
```typescript
// Source: frontend/types/api.ts lines 330-335 -- needs update
export interface InterviewNoteResponse {
  id: string
  interviewId: string  // ADD
  content: string
  noteType: string     // ADD
  createdAt: string
  updatedAt: string
}
```

### GAP-01: Updated useCreateInterviewNote (add noteType)
```typescript
// Source: use-interviews.ts:143-163 -- needs noteType parameter
mutationFn: ({
  interviewId,
  content,
  noteType,
}: {
  interviewId: string
  content: string
  noteType?: string
}) =>
  apiClient<InterviewNoteResponse>(
    `/interviews/${interviewId}/notes`,
    { method: "POST", body: JSON.stringify({ content, noteType }) },
  ),
```

### GAP-03: application.yml SMTP config
```yaml
# Source: Spring Boot mail auto-configuration docs
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: ${SMTP_USERNAME:your-email@gmail.com}
    password: ${SMTP_PASSWORD:your-app-password}
    properties:
      mail.smtp.auth: true
      mail.smtp.starttls.enable: true
      mail.smtp.starttls.required: true
```

### GAP-03: PasswordResetService update (line 44 replacement)
```kotlin
// Replace logger.info("Password reset link: ...") with:
emailService.sendPasswordResetEmail(user.email, resetUrl)
// Where resetUrl = "${frontendBaseUrl}/auth/reset-password?token=$token"
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| javax.mail | jakarta.mail (via spring-boot-starter-mail) | Spring Boot 3.0+ | Package names changed from javax.* to jakarta.* |
| JavaMail direct | JavaMailSender + MimeMessageHelper | Long-standing | Spring abstraction handles connection pooling |
| TanStack Table getExpandedRowModel | Custom expand state in parent | Current best practice for simple cases | Simpler than configuring table expansion API when expandable content is rich/custom |

## Open Questions

1. **Which password reset path does Better Auth UI actually invoke?**
   - What we know: Better Auth UI's ForgotPasswordForm calls `authClient.requestPasswordReset()` which hits the Better Auth server-side handler. The custom backend has a separate `POST /api/auth/password-reset` endpoint.
   - What's unclear: Does the Better Auth forgot-password flow use the `sendResetPassword` callback in `lib/auth.ts`, or does it call the custom backend endpoint? These are two separate systems.
   - Recommendation: During implementation, test the forgot-password page to see which endpoint is called. If Better Auth's own flow is used, update the `sendResetPassword` callback in `lib/auth.ts` to call an email-sending API. If the custom backend flow is used, update `PasswordResetService` as planned. Likely BOTH need updating -- Better Auth UI uses its own server-side callback, and the custom endpoint is a separate API.

2. **Frontend URL for password reset link in email**
   - What we know: Better Auth UI's ResetPasswordForm expects token in URL search params at `/auth/reset-password?token=XXX`
   - What's unclear: The exact URL format the backend should construct. Is it the Better Auth path or the custom backend confirm endpoint?
   - Recommendation: Use `http://localhost:3000/auth/reset-password?token=$token` for local dev. Make the base URL configurable.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework (Backend) | JUnit 5 + MockK + SpringBootTest (via spring-boot-starter-test) |
| Framework (Frontend) | Vitest 4.1.0 + Testing Library + jsdom |
| Backend test command | `./gradlew :backend:test` |
| Frontend test command | `cd frontend && pnpm test` |
| Backend config | JUnit5 via `useJUnitPlatform()` in build.gradle.kts |
| Frontend config | `frontend/vitest.config.ts` |

### Phase Requirements to Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| GAP-01 | Interview notes CRUD displayed in UI | manual (UI) | N/A -- visual verification | N/A |
| GAP-01 | useUpdateInterviewNote hook calls correct endpoint | unit | `cd frontend && pnpm vitest run __tests__/hooks/use-interviews.test.ts` | No -- Wave 0 |
| GAP-01 | useDeleteInterviewNote hook calls correct endpoint | unit | `cd frontend && pnpm vitest run __tests__/hooks/use-interviews.test.ts` | No -- Wave 0 |
| GAP-02 | Document version history displayed in expanded row | manual (UI) | N/A -- visual verification | N/A |
| GAP-02 | Upload new version creates entry | manual (UI) | N/A -- visual verification | N/A |
| GAP-03 | EmailService sends email via JavaMailSender | unit | `./gradlew :backend:test --tests "*EmailService*"` | No -- Wave 0 |
| GAP-03 | PasswordResetService calls EmailService instead of logging | integration | `./gradlew :backend:test --tests "*PasswordResetService*"` | No -- Wave 0 |
| GAP-03 | SMTP failure falls back to logging | unit | `./gradlew :backend:test --tests "*EmailService*"` | No -- Wave 0 |

### Sampling Rate
- **Per task commit:** `./gradlew :backend:test` (backend changes) or `cd frontend && pnpm test` (frontend changes)
- **Per wave merge:** Both backend and frontend test suites
- **Phase gate:** All tests green before `/gsd:verify-work`

### Wave 0 Gaps
- [ ] `frontend/__tests__/hooks/use-interviews.test.ts` -- covers GAP-01 hook tests
- [ ] `backend/src/test/kotlin/.../service/EmailServiceTests.kt` -- covers GAP-03 email sending + fallback
- [ ] `backend/src/test/kotlin/.../service/PasswordResetServiceTests.kt` -- update existing if present, or create for GAP-03 integration

## Sources

### Primary (HIGH confidence)
- Codebase inspection: All referenced files verified at stated line numbers
- `application-detail.tsx:437-558` -- InterviewsTab current implementation
- `use-interviews.ts:130-163` -- Existing interview note hooks
- `use-documents.ts:144-240` -- All 5 document version hooks
- `document-list.tsx:1-172` -- Document list table component
- `PasswordResetService.kt:1-74` -- Backend reset flow
- `auth.ts:1-19` -- Better Auth server config with sendResetPassword callback
- `auth/[path]/page.tsx:1-23` -- Better Auth UI dynamic routing

### Secondary (MEDIUM confidence)
- [Better Auth UI - Custom Auth Paths](https://better-auth-ui.com/advanced/custom-auth-paths) -- authViewPaths includes forgot-password and reset-password
- [Better Auth UI - Password Management (DeepWiki)](https://deepwiki.com/better-auth-ui/better-auth-ui/4.6-password-management) -- ForgotPasswordForm calls requestPasswordReset, ResetPasswordForm extracts token from URL params
- [Spring Boot Email Docs](https://docs.spring.io/spring-boot/reference/io/email.html) -- JavaMailSender auto-configuration with spring-boot-starter-mail
- [Spring Framework Email Integration](https://docs.spring.io/spring-framework/reference/integration/email.html) -- MimeMessageHelper for HTML emails

### Tertiary (LOW confidence)
- Gmail SMTP App Password requirement -- verified by common knowledge but specific config should be tested during implementation

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH -- all libraries already in project, only spring-boot-starter-mail is new (well-documented Spring Boot standard)
- Architecture: HIGH -- all patterns derived from existing codebase code with verified line numbers
- Pitfalls: HIGH -- identified from codebase analysis (type mismatch, dual reset paths, missing noteType in hook)
- Better Auth UI reset flow: MEDIUM -- confirmed forgot-password/reset-password pages exist, but interaction between Better Auth and custom backend reset needs runtime verification

**Research date:** 2026-03-22
**Valid until:** 2026-04-22 (30 days -- stable stack, no fast-moving dependencies)
