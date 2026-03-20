# Phase 7 — Review & Audit Report

**Reviewed:** 2026-03-20
**Reviewer:** Claude Opus 4.6 (with Context7 + Sequential Thinking)
**Sources:** Better Auth docs (Context7), Next.js 16 docs (Context7), shadcn/ui docs (Context7), Better Auth UI docs (Context7)

---

## Executive Summary

Phase 7 planning is **well-structured and thorough** for layout, styling, API client, and frontend architecture. However, there is a **critical architectural mismatch** in the Better Auth integration strategy that will cause implementation failures if not addressed before execution.

**Original Verdict: BLOCK on auth approach, PASS on everything else.**
**Updated Verdict (2026-03-20): ALL ISSUES RESOLVED. Plans updated for full Better Auth with own DB + Better Auth UI.**

---

## Critical Issues

### 1. Better Auth Cannot Proxy to an External Backend for emailAndPassword (BLOCKING)

**What the plan assumes:** Better Auth can run in "stateless/cookie-cache" mode, intercepting `signIn.email()` and `signUp.email()` calls via hooks, and proxying them to the Spring Boot backend API.

**What the docs actually say:**
- Stateless mode (omitting database config) is documented **only for OAuth/social providers** where the external provider handles authentication
- `emailAndPassword: { enabled: true }` requires Better Auth to **own user tables** (`user`, `session`, `account`, `verification`) for password hashing, lookup, and verification
- Better Auth's `signIn.email()` internally does: lookup user by email -> verify password hash -> create session. Without its own user database, step 1 and 2 fail
- The plan's Approach A (hooks to intercept) would need to completely replace Better Auth's built-in flow, defeating the purpose
- The plan's Approach B/C are workarounds that add complexity without proportional benefit

**Evidence from Context7 docs:**
```
"Set up stateless session management by omitting the database configuration
in Better Auth. This enables authentication without any database backend,
supporting OAuth social providers like Google."
```
No mention of emailAndPassword working without a database.

**Impact:** Plan 07-01 Task 2 will fail during implementation. The executor will hit one of the fallback approaches, each adding unplanned complexity.

**Recommended fix (choose one):**

| Option | Complexity | Recommendation |
|--------|-----------|----------------|
| **A: Custom Auth (React Context + fetch wrapper)** | Low | RECOMMENDED — matches RESEARCH.md's own recommendation. No extra dependency. Full control. |
| **B: Full Better Auth with own DB tables** | High | Only if user wants Better Auth UI components. Requires Flyway migrations for Better Auth tables, user sync between backends. Overkill for personal tool. |
| **C: Better Auth for session cookies only** | Medium | Worst of both worlds — adds dependency without using its features. |

### 2. Better Auth UI Opportunity (INFORMATIONAL)

`@daveyplate/better-auth-ui` provides pre-built shadcn/ui auth components:
- `AuthView` — renders sign-in, sign-up, forgot-password, reset-password pages
- `UserButton` — avatar dropdown with sign-out
- `SignedIn` / `SignedOut` — conditional rendering
- `AuthUIProvider` — wraps app with auth context

This would eliminate most of Plan 02 (hand-built auth forms). **However**, it requires a fully configured Better Auth instance with its own database — only relevant if Option B is chosen.

---

## Confirmed Correct (No Changes Needed)

| Area | Status | Notes |
|------|--------|-------|
| Next.js 16 App Router | PASS | Correct version, route groups, project structure |
| `proxy.ts` (not `middleware.ts`) | PASS | Confirmed by both Next.js 16 and Better Auth docs |
| `export function proxy()` named export | PASS | Matches official examples |
| `getSessionCookie` in proxy.ts | PASS | Better Auth docs show exact pattern for Next.js 16 |
| Tailwind CSS v4 (`@theme` in CSS) | PASS | No `tailwind.config.ts` needed |
| shadcn/ui installation + components | PASS | Component list complete, init command correct |
| OKLCH colors (shadcn v3.5+) | PASS | Correctly identified over HSL |
| TanStack Query v5 provider pattern | PASS | `useState(() => makeQueryClient())` correct |
| next-themes for dark mode | PASS | `attribute="class"` + `suppressHydrationWarning` correct |
| React Hook Form + Zod + shadcn Form | PASS | Still supported pattern |
| API client with 401 refresh retry | PASS | `credentials: "include"` for cross-origin cookies correct |
| Zod schemas matching backend validation | PASS | Password regex, email validation correct |
| Sonner toast notifications | PASS | shadcn/ui recommended toast library |
| Responsive sidebar with Sheet | PASS | `hidden md:flex` + Sheet on mobile correct |
| Route groups `(auth)` / `(dashboard)` | PASS | Correct layout separation pattern |
| Lucide React icons | PASS | shadcn default icon library |
| Inter font via next/font/google | PASS | Correct for Linear/Notion aesthetic |
| pnpm as package manager | PASS | Per frontend/CLAUDE.md |

---

## Minor Issues

### 3. shadcn/ui Form Pattern Evolution (LOW)

shadcn/ui now has native `Field`, `FieldLabel`, `FieldError`, `FieldDescription` components and promotes Next.js Server Actions + `useActionState` pattern. The plan uses React Hook Form + `Form` component pattern.

**Impact:** None — the RHF pattern still works and is well-suited for client-side forms with TanStack Query mutations. No change needed.

### 4. proxy.ts Session Detection Depends on Auth Approach (MEDIUM)

If switching to custom auth (Option A), the proxy.ts implementation needs adjustment:
- Cannot use `getSessionCookie` from `better-auth/cookies`
- Must use a custom session indicator cookie (e.g., `has_session=true` set on login, cleared on logout)
- Or check for the existence of a custom `session_token` cookie managed by the auth context

**Impact:** Plan 07-03 Task 2 needs minor update if auth approach changes.

---

## Per-Plan Assessment

### 07-01-PLAN.md (Project Init + Dependencies + Auth Setup)

| Aspect | Rating | Notes |
|--------|--------|-------|
| Next.js init | PASS | `pnpm create next-app@latest` correct |
| Dependencies | NEEDS UPDATE | Remove `better-auth` if switching to Option A |
| shadcn/ui setup | PASS | Init + component adds correct |
| Vitest setup | PASS | Config and stub tests correct |
| API types | PASS | Mirror backend DTOs correctly |
| Zod validators | PASS | Match backend validation rules |
| API client | PASS | Fetch wrapper with 401 retry well-designed |
| Auth setup | BLOCKED | Better Auth proxy approach won't work as planned |
| Provider tree | NEEDS UPDATE | Remove Better Auth provider if switching |
| Test infrastructure | PASS | Vitest + RTL + jsdom correct |

### 07-02-PLAN.md (Auth UI Pages)

| Aspect | Rating | Notes |
|--------|--------|-------|
| Login form | PASS | RHF + Zod + shadcn Form correct |
| Register form | PASS | Password validation matches backend |
| Email verify page | PASS | Token from URL + apiClient call |
| Forgot password | PASS | Single email field + toast |
| Reset password | PASS | Token + newPassword form |
| Auth layout | PASS | Centered card layout correct |
| Mutation hooks | NEEDS UPDATE | Must call apiClient directly instead of Better Auth client |
| Toast messages | PASS | Match UI-SPEC copywriting |
| Navigation links | PASS | Cross-linking between auth pages |

### 07-03-PLAN.md (Layout Shell + Route Protection)

| Aspect | Rating | Notes |
|--------|--------|-------|
| Landing page | PASS | CTAs, copy, layout all correct |
| Sidebar | PASS | navItems, active state, responsive hiding |
| Topbar | NEEDS UPDATE | `useSession()` source depends on auth approach |
| Mobile nav | PASS | Sheet component, hamburger, close on click |
| Empty states | PASS | Copy matches UI-SPEC |
| Dashboard layout | PASS | h-screen flex layout correct |
| proxy.ts | NEEDS UPDATE | Session detection depends on auth approach |
| Human verification | PASS | Comprehensive manual test checklist |

### 07-VALIDATION.md

| Aspect | Rating | Notes |
|--------|--------|-------|
| Test infrastructure | PASS | Vitest + RTL correct |
| Sampling rate | PASS | After every task + wave |
| Wave 0 requirements | PASS | Correct test stubs |
| Manual verifications | PASS | Responsive checks at 375/768/1280 |

### 07-UI-SPEC.md

| Aspect | Rating | Notes |
|--------|--------|-------|
| Design system | PASS | shadcn defaults, OKLCH, lucide-react |
| Spacing scale | PASS | Tailwind v4 defaults |
| Typography | PASS | Inter 400/600, 14/20/28px scale |
| Color | PASS | Semantic tokens, 60/30/10 rule |
| Copywriting | PASS | Complete, consistent, professional |
| Component inventory | PASS | All needed components listed |
| Layout contract | PASS | Dimensions, breakpoints specified |
| Interaction states | PASS | Hover, active, disabled, focus |
| Registry safety | PASS | Official shadcn only |

---

## Actions Taken (2026-03-20)

**User chose Option B: Full Better Auth with own database tables + Better Auth UI components.**

All planning documents have been updated:

1. **07-CONTEXT.md**: Auth decision updated to full Better Auth with own DB + Better Auth UI
2. **07-01-PLAN.md**: Rewritten with Flyway migration for Better Auth tables, pg driver, AuthUIProvider in providers, DATABASE_URL in .env.local
3. **07-02-PLAN.md**: Rewritten to use AuthView dynamic route instead of 5 hand-built auth pages — dramatically simplified
4. **07-03-PLAN.md**: Topbar updated to use UserButton from Better Auth UI, proxy.ts unchanged (getSessionCookie confirmed correct)
5. **07-RESEARCH.md**: Auth stack updated to better-auth + @daveyplate/better-auth-ui + pg, open questions resolved

### Original Recommendations (for reference):

### If choosing Option A (Custom Auth — RECOMMENDED):

1. **07-CONTEXT.md**: Update "Better Auth" decision to "Custom auth (React Context + fetch wrapper)" with rationale
2. **07-01-PLAN.md**:
   - Remove `better-auth` from dependencies
   - Replace `lib/auth.ts` (Better Auth server) with `lib/auth-context.tsx` (React Context)
   - Remove `lib/auth-client.ts` (Better Auth client)
   - Remove `app/api/auth/[...all]/route.ts` (Better Auth handler)
   - Add `AuthProvider` to providers.tsx
   - Remove `BETTER_AUTH_SECRET` and `BETTER_AUTH_URL` from .env.local
3. **07-02-PLAN.md**:
   - Update mutation hooks to call `apiClient` directly (no Better Auth client)
   - Login: `apiClient<AuthResponse>("/auth/login", ...)` then `setAccessToken(data.accessToken)` + set session cookie
4. **07-03-PLAN.md**:
   - Topbar: Use custom `useAuth()` hook instead of `authClient.useSession()`
   - proxy.ts: Check custom `has_session` cookie instead of `getSessionCookie`
5. **07-RESEARCH.md**: Update to reflect custom auth as the chosen approach

### If choosing Option B (Full Better Auth with DB):

1. Add Flyway migration for Better Auth tables (user, session, account, verification)
2. Configure Better Auth with PostgreSQL database adapter pointing to the existing DB
3. Consider using `@daveyplate/better-auth-ui` for pre-built auth components
4. Handle user sync between Better Auth and backend user tables
5. This is significantly more complex and not recommended for a personal tool

---

## Conclusion

Phase 7 planning quality is high — the project structure, component choices, styling approach, API client design, responsive layout, and UI spec are all well-researched and correct. The single blocking issue is the Better Auth proxy architecture, which the RESEARCH.md itself flagged as risky. Switching to custom auth (Option A) would make all three plans executable without major restructuring, as the auth forms, layout, and API client patterns remain the same — only the auth state management layer changes.

---

*Review conducted: 2026-03-20*
*Tools used: Context7 MCP (Better Auth, Next.js 16, shadcn/ui, Better Auth UI docs), Sequential Thinking MCP*
