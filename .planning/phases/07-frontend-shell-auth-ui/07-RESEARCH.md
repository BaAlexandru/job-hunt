# Phase 7: Frontend Shell & Auth UI - Research

**Researched:** 2026-03-20
**Domain:** Next.js frontend initialization, Better Auth with PostgreSQL, Better Auth UI components, responsive layout shell
**Confidence:** HIGH

## Summary

Phase 7 initializes the entire frontend from scratch using Next.js 16.2 (latest stable as of March 2026) with App Router, TypeScript, Tailwind CSS v4, and shadcn/ui. Authentication is handled by Better Auth with its own PostgreSQL database tables (user, session, account, verification), and the UI is provided by `@daveyplate/better-auth-ui` which offers pre-built shadcn/ui-styled auth components (AuthView for sign-in/sign-up/forgot-password/reset-password, UserButton for the top bar, SignedIn/SignedOut for conditional rendering).

Better Auth connects directly to the same PostgreSQL database via the `pg` Pool adapter. It manages users, sessions, passwords, email verification, and password reset entirely within its own tables. The frontend's auth flow is independent from the backend's JWT-based auth (Phase 2) -- unification is a Phase 8 concern. Better Auth uses session cookies (not JWT) for auth state, and `getSessionCookie` from `better-auth/cookies` enables lightweight session detection in Next.js 16's `proxy.ts` for route protection.

The Better Auth UI library eliminates the need to hand-build auth forms. A single dynamic route (`app/auth/[path]/page.tsx`) with `<AuthView path={path} />` renders all auth pages. `viewPaths` configuration maps Better Auth's default paths to custom URLs (e.g., SIGN_IN -> "login", SIGN_UP -> "register"). The library requires `<AuthUIProvider>` wrapping the app with the auth client, navigation functions, and Link component.

**Primary recommendation:** Use Next.js 16.2 with App Router, Better Auth with pg Pool adapter (direct PostgreSQL connection), `@daveyplate/better-auth-ui` for all auth UI, shadcn/ui for layout components, TanStack Query for server state, and `proxy.ts` with `getSessionCookie` for route protection.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- Better Auth as the auth framework (replaces NextAuth.js -- Auth.js v5 never left beta, absorbed by Better Auth Sep 2025)
- Full Better Auth with its own PostgreSQL database tables (not proxy/stateless mode)
- Better Auth manages users, sessions, passwords, email verification, and password reset in its own tables (user, session, account, verification) in the same PostgreSQL database
- `@daveyplate/better-auth-ui` provides pre-built shadcn/ui auth components: AuthView for sign-in/sign-up/forgot-password/reset-password/verify-email, UserButton for the top bar
- viewPaths configured to map Better Auth routes to project URLs: SIGN_IN="login", SIGN_UP="register", FORGOT_PASSWORD="forgot-password", RESET_PASSWORD="reset-password"
- Better Auth's emailAndPassword: { enabled: true } with its own user/account tables
- Session management via Better Auth cookies (not backend JWT)
- For backend API calls (Phase 8+), backend security will be updated to accept Better Auth session tokens -- this is a Phase 8 concern
- Backend auth endpoints (Phase 2) remain functional but are not used by the frontend; they serve as the backend's own auth layer until Phase 8 unifies auth
- Flyway migration added to backend for Better Auth tables (convention: V{N}__phase07_better_auth_tables.sql)
- Unauthenticated users see a simple landing page with login/register buttons
- After login, user lands on a Dashboard / Home page (placeholder content in Phase 7)
- Sidebar + top bar layout for authenticated users
- Sidebar contains nav links: Dashboard, Applications, Companies, Jobs, Documents
- Top bar with user info and logout action
- Sidebar collapses to hamburger slide-over menu on mobile
- All nav links present from Phase 7 -- unbuilt feature pages show empty state placeholders ("Coming soon")
- Dark mode: respect OS/system preference automatically via Tailwind dark: variant, no manual toggle
- Native fetch with a thin wrapper function (no axios/ky dependency) for non-auth API calls
- Auth operations go through Better Auth client (authClient), NOT through apiClient
- apiClient wrapper handles: base URL (http://localhost:8080/api), auth headers, JSON parsing, error standardization
- 401 handling in apiClient will be updated in Phase 8 to work with Better Auth sessions
- TanStack Query set up in Phase 7 -- provider configured, auth mutations use it from the start
- shadcn/ui component library (Radix UI + Tailwind CSS)
- React Hook Form + Zod for form handling and validation
- Sonner toast notifications via shadcn/ui integration for success/error feedback
- Clean & minimal aesthetic -- neutral defaults, subtle borders, light shadows, whitespace (Linear/Notion-inspired)
- pnpm as package manager (per frontend/CLAUDE.md)

### Claude's Discretion
- Next.js App Router vs Pages Router (App Router recommended for new projects)
- Better Auth database ID format (text vs UUID)
- File/folder structure within frontend/
- Tailwind theme customization details
- TanStack Query default options (staleTime, retry)
- Loading skeleton and spinner designs
- Exact responsive breakpoints
- ESLint/Prettier configuration
- Whether to use next/font for font optimization

### Deferred Ideas (OUT OF SCOPE)
- Manual dark/light mode toggle -- currently system-preference only. Could add toggle in a future iteration
- OAuth2 social login (Google, GitHub) -- backend doesn't support it yet, would need backend changes
- PWA support -- could add service worker and manifest later for mobile experience
- Real dashboard content (stats, charts, recent activity) -- needs data from Phases 4-6 first
- NextAuth.js / Auth.js -- originally considered but Auth.js v5 never left beta, absorbed by Better Auth Sep 2025; replaced by Better Auth with full DB setup (own tables in PostgreSQL)
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| INFR-05 | Responsive web design usable on mobile viewports | Tailwind CSS v4 responsive utilities (`hidden md:flex`, `md:hidden`), shadcn/ui Sheet component for mobile nav, sidebar collapse to hamburger at md (768px) breakpoint, tested at 375px/768px/1280px viewports |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Next.js | 16.2 | React framework with App Router | Latest stable (March 2026), Turbopack default, proxy.ts for route protection |
| React | 19 | UI library | Bundled with Next.js 16 |
| TypeScript | 5.x | Type safety | Default with create-next-app |
| Tailwind CSS | 4.x | Utility-first styling | Default with create-next-app, CSS-based config via @theme (no tailwind.config.ts) |
| pnpm | latest | Package manager | Per frontend/CLAUDE.md convention |

### Authentication
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| better-auth | latest | Full auth framework with own DB tables | Manages users, sessions, passwords, email verification, password reset in PostgreSQL |
| @daveyplate/better-auth-ui | latest | Pre-built shadcn/ui auth components | AuthView, UserButton, SignedIn/SignedOut -- eliminates hand-built auth forms |
| pg | latest | PostgreSQL driver for Better Auth | Better Auth connects directly to PostgreSQL via pg Pool adapter |

### UI Components
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| shadcn/ui | latest | Component library (Radix UI + Tailwind) | Copy-paste components, full ownership, Next.js 16 compatible |
| @radix-ui/* | (via shadcn) | Accessible primitives | Underlying engine for shadcn components |
| next-themes | latest | Dark mode (system preference) | Official shadcn recommendation for theme management |
| sonner | latest | Toast notifications | shadcn/ui's recommended toast library |
| lucide-react | latest | Icons | Tree-shakeable, consistent style, shadcn default |

### Data & Forms
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| @tanstack/react-query | 5.x | Server state management | Caching, mutations, loading states for API calls |
| @tanstack/react-query-devtools | 5.x | Dev tools | Inspect queries during development |
| react-hook-form | latest | Form state management | shadcn/ui's form component is built on it |
| @hookform/resolvers | latest | Zod integration for RHF | Connects Zod schemas to react-hook-form |
| zod | latest | Schema validation | Type-safe validation matching backend constraints |

### Dev Tools
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| eslint | 9.x | Linting | Default with create-next-app |
| @types/pg | latest | TypeScript types for pg | Type safety for PostgreSQL Pool configuration |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| Better Auth (full DB) | Custom auth (React Context + backend JWT) | Simpler, no extra tables; but must hand-build all auth forms, session management, email verification, password reset |
| Better Auth (full DB) | Better Auth proxy/stateless | Does NOT work for emailAndPassword -- stateless mode only supports OAuth providers |
| Better Auth UI | Hand-built auth forms | Full control over UI; but significantly more code (5+ form components vs 1 AuthView) |
| pg Pool adapter | Prisma/Drizzle adapter | ORM adds unnecessary complexity for 4 tables Better Auth manages itself |
| axios | Native fetch wrapper | fetch is built-in, no extra dependency, sufficient for non-auth API calls |

**Installation:**
```bash
cd frontend
pnpm create next-app@latest . --yes
pnpm add better-auth @daveyplate/better-auth-ui pg @tanstack/react-query @tanstack/react-query-devtools react-hook-form @hookform/resolvers zod next-themes lucide-react sonner
pnpm add -D @types/pg
npx shadcn@latest init
npx shadcn@latest add button card input label form sonner separator avatar dropdown-menu sheet
```

**Flyway migration for Better Auth tables:**
Next migration version is V7 (after V6__phase03_create_jobs.sql). The migration `V7__phase07_better_auth_tables.sql` creates 4 tables: `"user"`, `session`, `account`, `verification`. Better Auth uses TEXT primary keys by default. For PostgreSQL, set `generateId: false` to let the database generate UUIDs, or use `generateId: "uuid"` for Better Auth to generate them as text strings.

**Recommendation (Claude's Discretion - ID format):** Use TEXT primary keys (Better Auth's default). The backend's existing `users` table uses UUID, but Better Auth's `user` table is separate and managed entirely by Better Auth. Keeping Better Auth's default TEXT IDs avoids configuration complexity. Phase 8 will map between the two user systems.

## Architecture Patterns

### Recommended Project Structure
```
frontend/
├── app/
│   ├── layout.tsx              # Root layout (providers, fonts, Toaster)
│   ├── page.tsx                # Landing page (unauthenticated)
│   ├── auth/
│   │   └── [path]/page.tsx     # Dynamic auth route (AuthView renders all auth pages)
│   └── (dashboard)/            # Authenticated route group (sidebar layout)
│       ├── layout.tsx          # Sidebar + topbar layout
│       ├── dashboard/page.tsx  # Dashboard home (placeholder)
│       ├── applications/page.tsx  # Placeholder
│       ├── companies/page.tsx     # Placeholder
│       ├── jobs/page.tsx          # Placeholder
│       └── documents/page.tsx     # Placeholder
├── components/
│   ├── ui/                     # shadcn/ui generated components
│   ├── layout/
│   │   ├── sidebar.tsx         # Navigation sidebar
│   │   ├── topbar.tsx          # Top bar with UserButton
│   │   └── mobile-nav.tsx      # Mobile hamburger sheet
│   ├── providers.tsx           # All providers (QueryClient, ThemeProvider, AuthUIProvider)
│   └── shared/
│       ├── empty-state.tsx     # "Coming soon" placeholder
│       └── loading.tsx         # Loading spinner
├── lib/
│   ├── auth.ts                 # Better Auth server config (betterAuth())
│   ├── auth-client.ts          # Better Auth client (createAuthClient())
│   ├── api-client.ts           # Fetch wrapper for non-auth API calls (Phase 8+)
│   ├── query-client.ts         # TanStack Query client config
│   └── utils.ts                # cn() utility (shadcn generates this)
├── types/
│   └── api.ts                  # API response/request types
├── proxy.ts                    # Route protection (getSessionCookie check)
├── .env.local                  # Environment variables
├── components.json             # shadcn/ui config
├── package.json
└── tsconfig.json
```

**Key structural decisions (Claude's Discretion):**
- App Router (not Pages Router) -- standard for new Next.js 16 projects
- Single dynamic `auth/[path]/page.tsx` route for all auth views (Better Auth UI pattern)
- Route group `(dashboard)` for sidebar layout without affecting URL paths
- `lib/auth.ts` for server-side Better Auth config, `lib/auth-client.ts` for client-side
- Providers consolidated in single `components/providers.tsx` client component
- No `hooks/` or `validators.ts` needed initially -- Better Auth UI handles auth forms, Zod schemas needed only in Phase 8+

### Pattern 1: Better Auth Server Configuration
**What:** Server-side auth config connecting to PostgreSQL and enabling emailAndPassword.
**When to use:** Once, in `lib/auth.ts`.
**Source:** [Better Auth Installation Docs](https://better-auth.com/docs/installation), [Better Auth Email & Password](https://better-auth.com/docs/authentication/email-password)
```typescript
// lib/auth.ts
import { betterAuth } from "better-auth"
import { nextCookies } from "better-auth/next-js"
import { Pool } from "pg"

export const auth = betterAuth({
  database: new Pool({
    connectionString: process.env.DATABASE_URL,
  }),
  emailAndPassword: {
    enabled: true,
    requireEmailVerification: false, // Enable when email service is configured
    minPasswordLength: 8,
    autoSignIn: true, // Auto sign-in after registration
    sendResetPassword: async ({ user, url, token }, request) => {
      // Phase 7: Log to console. Phase 8+: integrate email service
      console.log(`Password reset for ${user.email}: ${url}`)
    },
  },
  plugins: [
    nextCookies(), // Must be last plugin -- auto-sets cookies in server actions
  ],
})
```

### Pattern 2: Better Auth Client
**What:** Client-side auth client for React hooks and API calls.
**When to use:** Once, in `lib/auth-client.ts`.
**Source:** [Better Auth Basic Usage](https://better-auth.com/docs/basic-usage)
```typescript
// lib/auth-client.ts
import { createAuthClient } from "better-auth/react"

export const authClient = createAuthClient({
  baseURL: process.env.NEXT_PUBLIC_APP_URL || "http://localhost:3000",
})
```

### Pattern 3: Better Auth API Route Handler
**What:** Next.js App Router catch-all route that handles all Better Auth API requests.
**When to use:** Once, in `app/api/auth/[...all]/route.ts`.
**Source:** [Better Auth Next.js Integration](https://better-auth.com/docs/integrations/next)
```typescript
// app/api/auth/[...all]/route.ts
import { auth } from "@/lib/auth"
import { toNextJsHandler } from "better-auth/next-js"

export const { GET, POST } = toNextJsHandler(auth)
```

### Pattern 4: AuthUIProvider + All Providers
**What:** Client component wrapping app with AuthUIProvider, QueryClientProvider, and ThemeProvider.
**When to use:** Once, wrapping children in root layout.
**Source:** [Better Auth UI Provider](https://better-auth-ui.com/components/auth-ui-provider)
```typescript
// components/providers.tsx
"use client"

import { AuthUIProvider } from "@daveyplate/better-auth-ui"
import { QueryClientProvider } from "@tanstack/react-query"
import { ReactQueryDevtools } from "@tanstack/react-query-devtools"
import { ThemeProvider } from "next-themes"
import { useRouter } from "next/navigation"
import Link from "next/link"
import { useState } from "react"
import { authClient } from "@/lib/auth-client"
import { makeQueryClient } from "@/lib/query-client"

export function Providers({ children }: { children: React.ReactNode }) {
  const router = useRouter()
  const [queryClient] = useState(() => makeQueryClient())

  return (
    <QueryClientProvider client={queryClient}>
      <AuthUIProvider
        authClient={authClient}
        navigate={router.push}
        replace={router.replace}
        onSessionChange={() => router.refresh()}
        Link={Link}
        viewPaths={{
          SIGN_IN: "login",
          SIGN_UP: "register",
          FORGOT_PASSWORD: "forgot-password",
          RESET_PASSWORD: "reset-password",
        }}
      >
        <ThemeProvider
          attribute="class"
          defaultTheme="system"
          enableSystem
          disableTransitionOnChange
        >
          {children}
        </ThemeProvider>
      </AuthUIProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  )
}
```

### Pattern 5: Dynamic Auth Route with AuthView
**What:** Single dynamic route renders all auth pages via Better Auth UI's AuthView component.
**When to use:** In `app/auth/[path]/page.tsx`.
**Source:** [Better Auth UI Next.js Integration](https://better-auth-ui.com/integrations/next-js)
```typescript
// app/auth/[path]/page.tsx
import { AuthView } from "@daveyplate/better-auth-ui"
import { authViewPaths } from "@daveyplate/better-auth-ui/server"

export const dynamicParams = false

export function generateStaticParams() {
  return Object.values(authViewPaths).map((path) => ({ path }))
}

export default async function AuthPage({
  params,
}: {
  params: Promise<{ path: string }>
}) {
  const { path } = await params
  return (
    <main className="flex min-h-screen items-center justify-center p-4">
      <AuthView path={path} />
    </main>
  )
}
```

### Pattern 6: Route Protection via proxy.ts
**What:** Next.js 16 proxy using getSessionCookie for lightweight session detection.
**When to use:** At frontend project root as `proxy.ts`.
**Source:** [Better Auth Next.js Integration](https://better-auth.com/docs/integrations/next)
```typescript
// proxy.ts
import { NextRequest, NextResponse } from "next/server"
import { getSessionCookie } from "better-auth/cookies"

const publicPaths = ["/", "/auth"]

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Allow public paths and auth routes
  if (publicPaths.some((p) => pathname === p || pathname.startsWith(p + "/"))) {
    return NextResponse.next()
  }

  // Check for Better Auth session cookie
  const sessionCookie = getSessionCookie(request)
  if (!sessionCookie) {
    return NextResponse.redirect(new URL("/auth/login", request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|api|.*\\.png$).*)"],
}
```

### Pattern 7: Dashboard Layout with Sidebar
**What:** Route group layout with sidebar for desktop and Sheet for mobile.
**When to use:** In `app/(dashboard)/layout.tsx`.
```typescript
// app/(dashboard)/layout.tsx
import { Sidebar } from "@/components/layout/sidebar"
import { Topbar } from "@/components/layout/topbar"

export default function DashboardLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <div className="flex h-screen">
      <Sidebar />
      <div className="flex flex-1 flex-col">
        <Topbar />
        <main className="flex-1 overflow-auto p-6">{children}</main>
      </div>
    </div>
  )
}
```

### Pattern 8: API Client for Non-Auth Calls (Phase 8+)
**What:** Thin fetch wrapper for backend API calls. Auth calls go through authClient, not this wrapper.
**When to use:** For Phase 8+ API calls to backend (companies, jobs, applications).
```typescript
// lib/api-client.ts
const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

export class ApiError extends Error {
  constructor(
    public status: number,
    public data: Record<string, unknown>,
  ) {
    super(data.message as string || "Request failed")
  }
}

export async function apiClient<T>(
  path: string,
  options: RequestInit = {},
): Promise<T> {
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...options.headers,
  }

  const response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: "include",
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "Request failed" }))
    throw new ApiError(response.status, error)
  }

  if (response.status === 204) return undefined as T
  return response.json()
}
```

### Pattern 9: TanStack Query Client Configuration
**What:** QueryClient factory with sensible defaults.
**Source:** [TanStack Query Docs](https://tanstack.com/query/latest/docs)
```typescript
// lib/query-client.ts
import { QueryClient } from "@tanstack/react-query"

export function makeQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 60 * 1000, // 1 minute
        retry: 1,
        refetchOnWindowFocus: false,
      },
    },
  })
}
```

### Anti-Patterns to Avoid
- **Storing tokens in localStorage:** XSS vulnerability. Better Auth uses HTTP-only session cookies automatically.
- **Hand-building auth forms when Better Auth UI exists:** AuthView handles sign-in, sign-up, forgot-password, reset-password with a single component. Do not re-implement.
- **Using Better Auth stateless/proxy mode for emailAndPassword:** Stateless mode only works for OAuth social providers. emailAndPassword requires database tables.
- **Heavy logic in proxy.ts:** Use `getSessionCookie` for lightweight cookie check only. Full session validation happens server-side in page components or API routes.
- **Importing server-only code in client components:** Next.js App Router enforces the server/client boundary. `lib/auth.ts` (server) must not be imported from client components. Use `lib/auth-client.ts` instead.
- **Creating middleware.ts instead of proxy.ts:** Next.js 16 renamed middleware to proxy. The function export must be named `proxy`, not `middleware`.
- **Creating tailwind.config.ts:** Tailwind CSS v4 uses CSS-based configuration via `@theme` directives in `app/globals.css`. The old JS config file approach is deprecated.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Auth forms (login, register, etc.) | Custom form components | `@daveyplate/better-auth-ui` AuthView | Pre-built, shadcn-styled, handles all auth views in one component |
| User menu button | Custom dropdown | `@daveyplate/better-auth-ui` UserButton | Pre-built avatar dropdown with sign-out action |
| Auth conditional rendering | Custom `if (session)` checks | SignedIn / SignedOut components | Clean declarative pattern from Better Auth UI |
| Session management | Custom cookies/tokens | Better Auth session cookies | Automatic, secure, HTTP-only, managed by framework |
| Email verification flow | Custom verification logic | Better Auth emailVerification config | Handles token generation, verification, expiry |
| Password reset flow | Custom reset logic | Better Auth sendResetPassword/resetPassword | Handles token generation, email callback, password update |
| Toast notifications | Custom toast system | Sonner (via shadcn) | Accessible, animated, promise-aware, theme-aware |
| Dark mode | Custom theme detection | next-themes | Handles SSR hydration mismatch, system preference, class strategy |
| Accessible dropdowns/dialogs | Custom modals/menus | Radix UI (via shadcn) | Keyboard navigation, focus trapping, ARIA attributes |
| Mobile navigation | Custom responsive menu | shadcn Sheet component | Slide-over panel with overlay, accessible, animated |
| Icon system | SVG imports/sprite sheets | lucide-react | Tree-shakeable, consistent style, shadcn default |

**Key insight:** Between Better Auth UI and shadcn/ui, approximately 80% of Phase 7's UI is provided by libraries. The implementation work is primarily configuration, layout composition, and wiring providers together.

## Common Pitfalls

### Pitfall 1: Better Auth Tables Must Exist Before Starting the App
**What goes wrong:** Better Auth throws database errors on startup because its required tables don't exist.
**Why it happens:** Better Auth expects `user`, `session`, `account`, and `verification` tables. The project uses Flyway for schema management, not Better Auth's CLI migrate command.
**How to avoid:** Create Flyway migration `V7__phase07_better_auth_tables.sql` with all 4 tables before running the frontend. Better Auth's table `user` conflicts with PostgreSQL's reserved word -- must be quoted as `"user"` in SQL.
**Warning signs:** "relation does not exist" errors on app startup.

### Pitfall 2: CORS Between Frontend and Better Auth API
**What goes wrong:** Better Auth API routes run at `localhost:3000/api/auth/*`, same origin as the frontend. No CORS issues for auth. But `apiClient` calls to `localhost:8080/api` (backend) are cross-origin.
**Why it happens:** Two separate servers: Next.js on 3000, Spring Boot on 8080.
**How to avoid:** Backend CORS already allows `localhost:3000` with credentials. Ensure `credentials: "include"` in apiClient fetch calls.
**Warning signs:** CORS errors only on non-auth API calls (Phase 8+).

### Pitfall 3: getSessionCookie Import Issues
**What goes wrong:** `import { getSessionCookie } from "better-auth/cookies"` fails to resolve.
**Why it happens:** Reported in [GitHub issue #5672](https://github.com/better-auth/better-auth/issues/5672) for some Next.js versions.
**How to avoid:** Ensure better-auth is latest version. If import fails, fall back to manually checking for the session cookie by name: `request.cookies.get("better-auth.session_token")`.
**Warning signs:** Module resolution error in proxy.ts at build time.

### Pitfall 4: Hydration Mismatch with Theme
**What goes wrong:** Server renders light theme, client detects dark preference, causing flash of unstyled content.
**Why it happens:** Server doesn't know the user's system preference.
**How to avoid:** Use `next-themes` with `suppressHydrationWarning` on `<html>` tag. next-themes injects a script to set the class before hydration.
**Warning signs:** Brief white flash on dark-mode systems, React hydration warnings in console.

### Pitfall 5: Tailwind CSS v4 Configuration Changes
**What goes wrong:** Creating `tailwind.config.ts` when Tailwind v4 uses CSS-based configuration.
**Why it happens:** Most tutorials reference the old JS config (Tailwind v3 era).
**How to avoid:** Let `create-next-app` set up Tailwind. Use `@theme` in `app/globals.css` for customization. shadcn/ui init handles this automatically.
**Warning signs:** Tailwind classes not applying, config file being ignored.

### Pitfall 6: Next.js 16 proxy.ts vs middleware.ts
**What goes wrong:** Creating `middleware.ts` instead of `proxy.ts`.
**Why it happens:** Most tutorials and Stack Overflow answers still reference middleware.ts (Next.js 15 era).
**How to avoid:** Use `proxy.ts` at the frontend project root. The function export must be named `proxy`, not `middleware`. Use codemod if migrating: `npx @next/codemod@canary middleware-to-proxy .`
**Warning signs:** File being ignored, deprecation warnings.

### Pitfall 7: Better Auth "user" Table Name Conflict
**What goes wrong:** SQL migration fails because `user` is a reserved word in PostgreSQL.
**Why it happens:** Better Auth uses `user` as the table name by default.
**How to avoid:** Always quote the table name in SQL: `CREATE TABLE "user" (...)`. Better Auth's Kysely adapter handles this automatically, but Flyway migrations must quote it manually.
**Warning signs:** SQL syntax error during migration.

### Pitfall 8: AuthUIProvider viewPaths Must Match Route Structure
**What goes wrong:** Auth links navigate to wrong URLs or show 404.
**Why it happens:** viewPaths in AuthUIProvider don't match the actual file-system route structure.
**How to avoid:** The dynamic route is at `app/auth/[path]/page.tsx`. viewPaths values become the `path` param. So `SIGN_IN: "login"` maps to `/auth/login`. Ensure `generateStaticParams` returns all viewPaths values.
**Warning signs:** 404 on auth pages, wrong page rendering.

### Pitfall 9: create-next-app Generates AGENTS.md and CLAUDE.md
**What goes wrong:** Generated CLAUDE.md conflicts with existing `frontend/CLAUDE.md` convention.
**Why it happens:** Next.js 16.2 create-next-app generates these files by default for AI agent support.
**How to avoid:** After `create-next-app`, review and update the generated CLAUDE.md to match project conventions. Keep AGENTS.md as reference for Next.js-specific patterns.
**Warning signs:** Conflicting instructions between generated and project CLAUDE.md.

## Code Examples

### Better Auth PostgreSQL Migration (Flyway)
```sql
-- V7__phase07_better_auth_tables.sql
-- Better Auth core tables (user, session, account, verification)
-- See: https://better-auth.com/docs/concepts/database

CREATE TABLE "user" (
    id TEXT PRIMARY KEY,
    name TEXT NOT NULL,
    email TEXT NOT NULL UNIQUE,
    email_verified BOOLEAN NOT NULL DEFAULT FALSE,
    image TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE session (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    token TEXT NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    ip_address TEXT,
    user_agent TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE account (
    id TEXT PRIMARY KEY,
    user_id TEXT NOT NULL REFERENCES "user"(id) ON DELETE CASCADE,
    account_id TEXT NOT NULL,
    provider_id TEXT NOT NULL,
    access_token TEXT,
    refresh_token TEXT,
    access_token_expires_at TIMESTAMPTZ,
    refresh_token_expires_at TIMESTAMPTZ,
    scope TEXT,
    id_token TEXT,
    password TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE verification (
    id TEXT PRIMARY KEY,
    identifier TEXT NOT NULL,
    value TEXT NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT now()
);
```

### Environment Variables (.env.local)
```bash
# Better Auth
BETTER_AUTH_SECRET=<generate-with-openssl-rand-base64-32>
BETTER_AUTH_URL=http://localhost:3000
DATABASE_URL=postgresql://myuser:secret@localhost:5432/jobhunt

# Backend API (for Phase 8+ non-auth calls)
NEXT_PUBLIC_API_URL=http://localhost:8080/api
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

### Root Layout
```typescript
// app/layout.tsx
import type { Metadata } from "next"
import { Inter } from "next/font/google"
import { Toaster } from "@/components/ui/sonner"
import { Providers } from "@/components/providers"
import "./globals.css"

const inter = Inter({
  subsets: ["latin"],
  weight: ["400", "600"],
  variable: "--font-inter",
})

export const metadata: Metadata = {
  title: "JobHunt",
  description: "Track your job search",
}

export default function RootLayout({
  children,
}: {
  children: React.ReactNode
}) {
  return (
    <html lang="en" suppressHydrationWarning>
      <body className={`${inter.variable} font-sans antialiased`}>
        <Providers>{children}</Providers>
        <Toaster />
      </body>
    </html>
  )
}
```

### Landing Page with SignedIn/SignedOut
```typescript
// app/page.tsx
import Link from "next/link"
import { SignedIn, SignedOut } from "@daveyplate/better-auth-ui"
import { Button } from "@/components/ui/button"
import { redirect } from "next/navigation"

export default function LandingPage() {
  return (
    <>
      <SignedIn>
        {/* Redirect authenticated users to dashboard */}
        {redirect("/dashboard")}
      </SignedIn>
      <SignedOut>
        <main className="flex min-h-screen flex-col items-center justify-center gap-8 p-4">
          <div className="max-w-xl text-center">
            <h1 className="text-2xl font-semibold leading-tight">
              Track your job search
            </h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Keep every application, interview, and document organized in one place.
            </p>
          </div>
          <div className="flex gap-4">
            <Button asChild>
              <Link href="/auth/register">Start tracking jobs</Link>
            </Button>
            <Button variant="outline" asChild>
              <Link href="/auth/login">Sign in</Link>
            </Button>
          </div>
        </main>
      </SignedOut>
    </>
  )
}
```

### Topbar with UserButton
```typescript
// components/layout/topbar.tsx
"use client"

import { UserButton } from "@daveyplate/better-auth-ui"
import { Menu } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Sheet, SheetContent, SheetTrigger } from "@/components/ui/sheet"
import { MobileNav } from "./mobile-nav"

export function Topbar() {
  return (
    <header className="flex h-14 items-center justify-between border-b px-6">
      <div className="md:hidden">
        <Sheet>
          <SheetTrigger asChild>
            <Button variant="ghost" size="icon">
              <Menu className="h-5 w-5" />
            </Button>
          </SheetTrigger>
          <SheetContent side="left" className="w-64 p-0">
            <MobileNav />
          </SheetContent>
        </Sheet>
      </div>
      <div className="flex-1" />
      <UserButton />
    </header>
  )
}
```

### Responsive Sidebar
```typescript
// components/layout/sidebar.tsx
"use client"

import { usePathname } from "next/navigation"
import Link from "next/link"
import { cn } from "@/lib/utils"
import {
  LayoutDashboard,
  Briefcase,
  Building2,
  FileText,
  FolderOpen,
} from "lucide-react"

const navItems = [
  { href: "/dashboard", label: "Dashboard", icon: LayoutDashboard },
  { href: "/applications", label: "Applications", icon: Briefcase },
  { href: "/companies", label: "Companies", icon: Building2 },
  { href: "/jobs", label: "Jobs", icon: FileText },
  { href: "/documents", label: "Documents", icon: FolderOpen },
]

export function Sidebar() {
  const pathname = usePathname()

  return (
    <aside className="hidden w-64 flex-col border-r md:flex">
      <div className="p-6">
        <h1 className="text-xl font-semibold">JobHunt</h1>
      </div>
      <nav className="flex-1 space-y-1 px-3">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={cn(
              "flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors",
              pathname === item.href
                ? "bg-accent text-accent-foreground"
                : "text-muted-foreground hover:bg-accent hover:text-accent-foreground"
            )}
          >
            <item.icon className="h-4 w-4" />
            {item.label}
          </Link>
        ))}
      </nav>
    </aside>
  )
}
```

## State of the Art

| Old Approach | Current Approach | When Changed | Impact |
|--------------|------------------|--------------|--------|
| middleware.ts | proxy.ts | Next.js 16 (2025) | File renamed, function renamed, no edge runtime |
| tailwind.config.ts | @theme in CSS (Tailwind v4) | Tailwind v4 (2025) | No JS config file, CSS-native configuration |
| NextAuth.js / Auth.js v5 | Better Auth | Auth.js absorbed by Better Auth Sep 2025 | New framework, own DB tables, better DX |
| Hand-built auth forms | Better Auth UI (@daveyplate) | 2025 | Single AuthView component for all auth pages |
| HSL theme colors | OKLCH (shadcn v3.5+) | 2025 | shadcn converted to OKLCH with Tailwind v4 |
| next-auth/middleware | proxy.ts + getSessionCookie | Next.js 16 | Use better-auth/cookies for session detection |
| QueryClientProvider only | QueryClientProvider + useState init | TanStack Query v5 | QueryClient created in useState to prevent re-creation |

**Deprecated/outdated:**
- `middleware.ts`: Renamed to `proxy.ts` in Next.js 16
- `next-auth` / `@auth/*`: Auth.js v5 absorbed by Better Auth, never left beta
- `tailwind.config.ts`: Tailwind v4 uses CSS-based config via `@theme`
- HSL color format in shadcn: Now uses OKLCH

## Open Questions

1. **Better Auth "user" table vs backend "users" table**
   - What we know: Backend Phase 2 created a `users` table with UUID keys for JWT auth. Better Auth creates its own `user` table (TEXT keys) for session-based auth.
   - What's unclear: How Phase 8 will unify these two user systems for backend API calls.
   - Recommendation: Keep them separate in Phase 7. Phase 8 planning should address auth unification (options: dual auth, shared user table, or Better Auth session validation in Spring Boot).

2. **Email delivery for verification and password reset**
   - What we know: Better Auth supports `sendVerificationEmail` and `sendResetPassword` callbacks. The backend has its own email verification/reset via `mailtrap.io` (Phase 2).
   - What's unclear: Whether to configure an email service in Phase 7 or defer.
   - Recommendation: Phase 7 logs email content to console. Phase 8+ can integrate an email service (Resend, SendGrid, etc.) into Better Auth's callbacks.

3. **DATABASE_URL for Better Auth vs Docker Compose auto-config**
   - What we know: Backend uses Docker Compose auto-configuration (no explicit DB connection properties). Better Auth requires an explicit `DATABASE_URL` connection string.
   - What's unclear: The exact PostgreSQL connection string when running via Docker Compose.
   - Recommendation: Use `DATABASE_URL=postgresql://myuser:secret@localhost:5432/jobhunt` in `.env.local`, matching the compose.yaml PostgreSQL service configuration.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest + React Testing Library + @testing-library/jest-dom |
| Config file | `frontend/vitest.config.ts` (Wave 0 setup) |
| Quick run command | `cd frontend && pnpm test` |
| Full suite command | `cd frontend && pnpm test && pnpm build` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INFR-05 | Responsive layout renders correctly at mobile/desktop | manual | Chrome DevTools device mode at 375px, 768px, 1280px | N/A |
| SC-1 | User can register and login through UI | smoke/manual | Manual: navigate to /auth/register, create account, verify redirect to /dashboard | N/A |
| SC-2 | User can log out from any page | smoke/manual | Manual: click UserButton > Sign out, verify redirect to landing | N/A |
| SC-3 | Layout responsive on mobile | manual-only | Resize browser to 375px, verify sidebar hidden, hamburger visible | N/A |
| SC-4 | API client handles auth automatically | unit | `pnpm test -- api-client` | No - Wave 0 |

### Sampling Rate
- **Per task commit:** `pnpm build` (catches type errors and import issues)
- **Per wave merge:** `pnpm build && pnpm test` (when tests exist)
- **Phase gate:** `pnpm build` must succeed + manual auth flow verification + responsive check

### Wave 0 Gaps
- [ ] Vitest setup: `pnpm add -D vitest @testing-library/react @testing-library/jest-dom @vitejs/plugin-react jsdom`
- [ ] `frontend/vitest.config.ts` -- Vitest configuration with jsdom environment
- [ ] `frontend/__tests__/lib/api-client.test.ts` -- covers SC-4 (fetch wrapper, error handling)
- [ ] Manual verification checklist for INFR-05 (responsive at 375px, 768px, 1280px)

**Note:** Phase 7's auth UI is largely provided by Better Auth UI's pre-built components. Unit testing AuthView/UserButton (third-party components) has low value. Focus testing on custom code: apiClient wrapper, layout rendering, provider configuration.

## Sources

### Primary (HIGH confidence)
- [Better Auth Installation](https://better-auth.com/docs/installation) - Server setup, pg Pool adapter, emailAndPassword configuration
- [Better Auth Next.js Integration](https://better-auth.com/docs/integrations/next) - Route handler, proxy.ts, getSessionCookie, server components
- [Better Auth Database Schema](https://better-auth.com/docs/concepts/database) - Core tables (user, session, account, verification), ID generation
- [Better Auth Email & Password](https://better-auth.com/docs/authentication/email-password) - emailAndPassword config, verification, password reset
- [Better Auth PostgreSQL Adapter](https://better-auth.com/docs/adapters/postgresql) - pg Pool configuration, connection string
- [Better Auth Basic Usage](https://better-auth.com/docs/basic-usage) - signUp, signIn, signOut, useSession, session management
- [Better Auth UI Introduction](https://better-auth-ui.com/) - Component overview, features
- [Better Auth UI AuthUIProvider](https://better-auth-ui.com/components/auth-ui-provider) - Provider setup, viewPaths, required props
- [Better Auth UI Next.js Integration](https://better-auth-ui.com/integrations/next-js) - Dynamic route pattern, AuthView, generateStaticParams
- [Better Auth UI Custom Auth Paths](https://better-auth-ui.com/advanced/custom-auth-paths) - viewPaths keys (SIGN_IN, SIGN_UP, etc.)
- [Better Auth UI AuthView API](https://better-auth-ui.com/components/auth-view) - Props, view types, customization
- [Next.js 16.2 Blog](https://nextjs.org/blog/next-16-2) - Latest version confirmation, Turbopack, proxy.ts
- [shadcn/ui Documentation](https://ui.shadcn.com/) - Component installation, dark mode, form integration

### Secondary (MEDIUM confidence)
- [Better Auth UI GitHub](https://github.com/daveyplate/better-auth-ui) - Repository, component list
- [Better Auth UI npm](https://www.npmjs.com/package/@daveyplate/better-auth-ui) - Package version info
- [GitHub Issue #5672](https://github.com/better-auth/better-auth/issues/5672) - getSessionCookie import issues

### Tertiary (LOW confidence)
- [GitHub Issue #2170](https://github.com/better-auth/better-auth/issues/2170) - getSessionCookie edge runtime issues (may be resolved in latest)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - All libraries verified via official docs and npm
- Architecture: HIGH - Better Auth + Next.js patterns well-documented with official examples
- Better Auth UI: HIGH - Official docs provide clear Next.js integration with AuthView and viewPaths
- Pitfalls: HIGH - Known issues documented in GitHub issues and official docs
- Flyway migration: MEDIUM - Better Auth schema verified, but column types (snake_case vs camelCase) need validation against actual Better Auth expectations

**Research date:** 2026-03-20
**Valid until:** 2026-04-06 (Better Auth and Next.js ecosystem moving fast)
