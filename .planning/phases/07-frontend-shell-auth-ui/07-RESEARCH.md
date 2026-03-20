# Phase 7: Frontend Shell & Auth UI - Research

**Researched:** 2026-03-20
**Domain:** Next.js frontend initialization, authentication UI, API client, responsive layout
**Confidence:** MEDIUM (Next.js 16 is newer than training data; Auth.js ecosystem in transition)

## Summary

Phase 7 initializes the entire frontend from scratch. The current stable Next.js version is **16.2** (not 15 as originally planned), which introduces a breaking change: `middleware.ts` has been renamed to `proxy.ts`. The frontend will use the App Router with TypeScript, Tailwind CSS v4, and shadcn/ui for components. The backend auth API is fully built with 7 endpoints (register, login, refresh, logout, verify, password-reset request, password-reset confirm).

Auth.js (NextAuth) is in a transitional state: the main contributor left in January 2025, v5 never exited beta, and the project was absorbed by Better Auth in September 2025. The `next-auth@5.0.0-beta.30` package works with Next.js 16 but requires peer dependency overrides. Given that the backend already handles all JWT logic (token generation, refresh rotation, blocklisting), and the credentials provider is the only provider needed, **Auth.js adds significant complexity for minimal benefit**. A simpler custom auth approach using React Context + the fetch wrapper is recommended as a Claude's Discretion item.

TanStack Query v5.91 is the current stable version and integrates well with Next.js App Router via a client-side provider pattern. shadcn/ui v3.5+ fully supports Next.js 16, Tailwind CSS v4, and React 19.

**Primary recommendation:** Use Next.js 16.2 with App Router, custom auth via React Context (no NextAuth overhead), shadcn/ui for components, TanStack Query for server state, and `proxy.ts` for route protection.

<user_constraints>
## User Constraints (from CONTEXT.md)

### Locked Decisions
- NextAuth.js (Auth.js) as the auth framework — wrap existing backend JWT API as a "credentials" provider
- Access token managed by NextAuth session; refresh token via HTTP-only cookie from backend
- Full auth UI pages: login, register, email verification, password reset request, password reset confirm
- Unauthenticated users see a simple landing page with login/register buttons
- After login, user lands on a Dashboard / Home page (placeholder content in Phase 7)
- Sidebar + top bar layout for authenticated users
- Sidebar contains nav links: Dashboard, Applications, Companies, Jobs, Documents
- Top bar with user info and logout action
- Sidebar collapses to hamburger slide-over menu on mobile
- All nav links present from Phase 7 — unbuilt feature pages show empty state placeholders
- Dark mode: respect OS/system preference automatically via Tailwind `dark:` variant, no manual toggle
- Native fetch with a thin wrapper function (no axios/ky dependency)
- Wrapper handles: base URL, auth headers, JSON parsing, error standardization
- 401 handling: auto-refresh via /api/auth/refresh, then retry the original request seamlessly
- TanStack Query set up in Phase 7 — provider configured, auth mutations use it from the start
- shadcn/ui component library (Radix UI + Tailwind CSS)
- React Hook Form + Zod for form handling and validation
- Sonner toast notifications via shadcn/ui integration
- Clean & minimal aesthetic — neutral defaults, subtle borders, light shadows, whitespace (Linear/Notion-inspired)
- pnpm as package manager

### Claude's Discretion
- Next.js App Router vs Pages Router (App Router recommended for new projects)
- Exact NextAuth.js configuration and session strategy
- File/folder structure within frontend/
- Tailwind theme customization details
- TanStack Query default options (staleTime, retry)
- Loading skeleton and spinner designs
- Exact responsive breakpoints
- ESLint/Prettier configuration
- Whether to use next/font for font optimization

### Deferred Ideas (OUT OF SCOPE)
- Manual dark/light mode toggle — currently system-preference only
- OAuth2 social login (Google, GitHub) — backend doesn't support it yet
- PWA support — could add service worker and manifest later
- Real dashboard content (stats, charts, recent activity) — needs data from Phases 4-6 first
</user_constraints>

<phase_requirements>
## Phase Requirements

| ID | Description | Research Support |
|----|-------------|-----------------|
| INFR-05 | Responsive web design usable on mobile viewports | Tailwind CSS v4 responsive utilities, shadcn/ui mobile-first components, sidebar collapse to hamburger on mobile, proxy.ts or layout-level breakpoint handling |
</phase_requirements>

## Standard Stack

### Core
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| Next.js | 16.2 | React framework with App Router | Latest stable, Turbopack default, proxy.ts for route protection |
| React | 19 (canary via Next.js) | UI library | Bundled with Next.js 16 |
| TypeScript | 5.x | Type safety | Default with create-next-app |
| Tailwind CSS | 4.x | Utility-first styling | Default with create-next-app, no tailwind.config.ts needed |
| pnpm | latest | Package manager | Per frontend/CLAUDE.md convention |

### Authentication
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| next-auth | 5.0.0-beta.30 | Auth framework (if using NextAuth approach) | Works with Next.js 16, credentials provider for custom backend |

**IMPORTANT NOTE on NextAuth:** Auth.js/NextAuth is in maintenance mode (absorbed by Better Auth, Sep 2025). v5 never left beta. For a credentials-only setup with a custom backend that already handles JWT, NextAuth adds complexity without proportional benefit. The Credentials provider explicitly warns it doesn't persist sessions to a database, and the JWT/session callback dance to pass through custom backend tokens is error-prone (see Pitfalls section). **Recommended alternative (Claude's Discretion):** Custom auth context with the fetch wrapper, storing the access token in memory and relying on the backend's HTTP-only refresh cookie. This is simpler, more transparent, and avoids the NextAuth abstraction leaks.

### UI Components
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| shadcn/ui | 3.5+ | Component library (Radix UI + Tailwind) | Copy-paste components, full ownership, Next.js 16 compatible |
| @radix-ui/* | (via shadcn) | Accessible primitives | Underlying engine for shadcn components |
| next-themes | latest | Dark mode (system preference) | Official shadcn recommendation for theme management |
| sonner | latest | Toast notifications | shadcn/ui's recommended toast library |

### Data & Forms
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| @tanstack/react-query | 5.91 | Server state management | Caching, mutations, loading states for API calls |
| @tanstack/react-query-devtools | 5.91 | Dev tools | Inspect queries during development |
| react-hook-form | latest | Form state management | shadcn/ui's form component is built on it |
| @hookform/resolvers | latest | Zod integration for RHF | Connects Zod schemas to react-hook-form |
| zod | latest | Schema validation | Type-safe validation matching backend constraints |

### Dev Tools
| Library | Version | Purpose | Why Standard |
|---------|---------|---------|--------------|
| eslint | 9.x | Linting | Default with create-next-app |
| lucide-react | latest | Icons | shadcn/ui's default icon library |

### Alternatives Considered
| Instead of | Could Use | Tradeoff |
|------------|-----------|----------|
| NextAuth.js | Custom auth context + fetch wrapper | Simpler for credentials-only, no beta dependency, full control over token lifecycle; loses NextAuth's session abstraction |
| NextAuth.js | Better Auth | The successor to Auth.js, but adds its own abstractions; overkill for credentials-only with custom backend |
| axios | Native fetch wrapper | fetch is built-in, no extra dependency, sufficient for this use case |
| Tailwind CSS v4 | CSS Modules | Tailwind is default with Next.js, better for rapid prototyping |

**Installation (if using NextAuth approach):**
```bash
cd frontend
pnpm create next-app@latest . --yes
pnpm add next-auth@beta @tanstack/react-query @tanstack/react-query-devtools react-hook-form @hookform/resolvers zod next-themes lucide-react sonner
npx shadcn@latest init
npx shadcn@latest add button card input label form toast sonner
```

**Installation (recommended custom auth approach):**
```bash
cd frontend
pnpm create next-app@latest . --yes
pnpm add @tanstack/react-query @tanstack/react-query-devtools react-hook-form @hookform/resolvers zod next-themes lucide-react sonner
npx shadcn@latest init
npx shadcn@latest add button card input label form sonner separator avatar dropdown-menu sheet
```

## Architecture Patterns

### Recommended Project Structure
```
frontend/
├── app/
│   ├── layout.tsx              # Root layout (providers, fonts, Toaster)
│   ├── page.tsx                # Landing page (unauthenticated)
│   ├── (auth)/                 # Auth route group (no layout shell)
│   │   ├── login/page.tsx
│   │   ├── register/page.tsx
│   │   ├── verify/page.tsx
│   │   ├── forgot-password/page.tsx
│   │   └── reset-password/page.tsx
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
│   │   ├── topbar.tsx          # Top bar with user info
│   │   └── mobile-nav.tsx      # Mobile hamburger sheet
│   ├── auth/
│   │   ├── login-form.tsx
│   │   ├── register-form.tsx
│   │   └── ...
│   └── shared/
│       ├── empty-state.tsx     # "Coming soon" placeholder
│       └── loading.tsx         # Loading spinner/skeleton
├── lib/
│   ├── api-client.ts           # Fetch wrapper with auth
│   ├── auth-context.tsx        # Auth state management (if custom auth)
│   ├── query-client.ts         # TanStack Query client config
│   └── validators.ts           # Shared Zod schemas
├── hooks/
│   ├── use-auth.ts             # Auth hook (login, logout, register)
│   └── use-mobile.ts           # Responsive breakpoint hook
├── types/
│   └── api.ts                  # API response/request types
├── proxy.ts                    # Route protection (was middleware.ts)
├── components.json             # shadcn/ui config
├── package.json
└── tsconfig.json
```

### Pattern 1: Route Groups for Layout Separation
**What:** Next.js route groups `(auth)` and `(dashboard)` share different layouts without affecting URL paths.
**When to use:** When auth pages need a centered card layout and dashboard pages need sidebar layout.
**Example:**
```typescript
// app/(dashboard)/layout.tsx
import { Sidebar } from "@/components/layout/sidebar"
import { Topbar } from "@/components/layout/topbar"

export default function DashboardLayout({ children }: { children: React.ReactNode }) {
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

### Pattern 2: API Client with Token Management
**What:** Thin fetch wrapper that attaches access token, handles 401 with refresh retry.
**When to use:** Every API call to the backend.
**Example:**
```typescript
// lib/api-client.ts
const API_BASE = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"

let accessToken: string | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
}

export async function apiClient<T>(
  path: string,
  options: RequestInit = {}
): Promise<T> {
  const headers: HeadersInit = {
    "Content-Type": "application/json",
    ...options.headers,
  }

  if (accessToken) {
    (headers as Record<string, string>)["Authorization"] = `Bearer ${accessToken}`
  }

  let response = await fetch(`${API_BASE}${path}`, {
    ...options,
    headers,
    credentials: "include", // Send cookies for refresh token
  })

  // Auto-refresh on 401
  if (response.status === 401 && accessToken) {
    const refreshed = await refreshAccessToken()
    if (refreshed) {
      (headers as Record<string, string>)["Authorization"] = `Bearer ${accessToken}`
      response = await fetch(`${API_BASE}${path}`, {
        ...options,
        headers,
        credentials: "include",
      })
    }
  }

  if (!response.ok) {
    const error = await response.json().catch(() => ({ message: "Request failed" }))
    throw new ApiError(response.status, error)
  }

  // Handle 204 No Content
  if (response.status === 204) return undefined as T

  return response.json()
}

async function refreshAccessToken(): Promise<boolean> {
  try {
    const res = await fetch(`${API_BASE}/auth/refresh`, {
      method: "POST",
      credentials: "include", // Sends refresh_token cookie
    })
    if (!res.ok) return false
    const data = await res.json()
    accessToken = data.accessToken
    return true
  } catch {
    return false
  }
}
```

### Pattern 3: Auth Context (Custom Auth Approach)
**What:** React context managing auth state, exposing login/logout/register functions.
**When to use:** Custom auth without NextAuth.
**Example:**
```typescript
// lib/auth-context.tsx
"use client"

import { createContext, useContext, useCallback, useEffect, useState } from "react"
import { apiClient, setAccessToken } from "./api-client"

interface User {
  email: string
}

interface AuthContextType {
  user: User | null
  isLoading: boolean
  login: (email: string, password: string) => Promise<void>
  register: (email: string, password: string) => Promise<void>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextType | null>(null)

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(null)
  const [isLoading, setIsLoading] = useState(true)

  // Try to restore session on mount via refresh
  useEffect(() => {
    refreshSession().finally(() => setIsLoading(false))
  }, [])

  async function refreshSession() {
    try {
      const res = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/auth/refresh`,
        { method: "POST", credentials: "include" }
      )
      if (res.ok) {
        const data = await res.json()
        setAccessToken(data.accessToken)
        // Decode user from JWT or store from login response
        setUser({ email: "user@example.com" }) // simplified
      }
    } catch {}
  }

  const login = useCallback(async (email: string, password: string) => {
    const data = await apiClient<{ accessToken: string; expiresIn: number }>(
      "/auth/login",
      { method: "POST", body: JSON.stringify({ email, password }) }
    )
    setAccessToken(data.accessToken)
    setUser({ email })
  }, [])

  const logout = useCallback(async () => {
    await apiClient("/auth/logout", { method: "POST" }).catch(() => {})
    setAccessToken(null)
    setUser(null)
  }, [])

  // ... register similar to login

  return (
    <AuthContext.Provider value={{ user, isLoading, login, register: login, logout }}>
      {children}
    </AuthContext.Provider>
  )
}

export function useAuth() {
  const ctx = useContext(AuthContext)
  if (!ctx) throw new Error("useAuth must be used within AuthProvider")
  return ctx
}
```

### Pattern 4: TanStack Query Provider Setup
**What:** QueryClient created outside component tree, wrapped in provider.
**When to use:** Root layout, once per app.
**Example:**
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

// components/providers.tsx
"use client"
import { QueryClientProvider } from "@tanstack/react-query"
import { ReactQueryDevtools } from "@tanstack/react-query-devtools"
import { ThemeProvider } from "next-themes"
import { useState } from "react"
import { makeQueryClient } from "@/lib/query-client"

export function Providers({ children }: { children: React.ReactNode }) {
  const [queryClient] = useState(() => makeQueryClient())

  return (
    <QueryClientProvider client={queryClient}>
      <ThemeProvider attribute="class" defaultTheme="system" enableSystem disableTransitionOnChange>
        {children}
      </ThemeProvider>
      <ReactQueryDevtools initialIsOpen={false} />
    </QueryClientProvider>
  )
}
```

### Pattern 5: Route Protection via proxy.ts
**What:** Next.js 16 proxy (formerly middleware) redirects unauthenticated users.
**When to use:** Protecting dashboard routes.
**Example:**
```typescript
// proxy.ts (root of frontend/)
import { NextRequest, NextResponse } from "next/server"

const publicPaths = ["/", "/login", "/register", "/verify", "/forgot-password", "/reset-password"]

export function proxy(request: NextRequest) {
  const { pathname } = request.nextUrl

  // Allow public paths
  if (publicPaths.some(p => pathname === p || pathname.startsWith(p + "/"))) {
    return NextResponse.next()
  }

  // Check for auth cookie or token indicator
  // Note: Cannot read access token from memory here (proxy runs in separate context)
  // Use a lightweight "session" cookie set by the auth flow
  const hasSession = request.cookies.has("has_session")

  if (!hasSession) {
    return NextResponse.redirect(new URL("/login", request.url))
  }

  return NextResponse.next()
}

export const config = {
  matcher: ["/((?!_next/static|_next/image|favicon.ico|.*\\.png$).*)"],
}
```

### Anti-Patterns to Avoid
- **Storing access tokens in localStorage:** XSS vulnerability. Keep access token in memory (module-level variable or React state). The refresh token is already in an HTTP-only cookie.
- **Using NextAuth's database adapter with credentials provider:** NextAuth explicitly warns against this. Credentials provider does not persist sessions to database.
- **Heavy logic in proxy.ts:** proxy.ts runs in Node.js runtime but should stay lightweight. Do not make database calls or complex auth checks there.
- **Importing server-only code in client components:** Next.js App Router enforces the server/client boundary. Mark client components with `"use client"` directive.

## Don't Hand-Roll

| Problem | Don't Build | Use Instead | Why |
|---------|-------------|-------------|-----|
| Form validation | Custom validation logic | Zod + react-hook-form + shadcn Form | Type-safe, declarative, mirrors backend validation rules |
| Toast notifications | Custom toast system | Sonner (via shadcn) | Accessible, animated, promise-aware, theme-aware |
| Dark mode | Custom theme toggle/detection | next-themes | Handles SSR hydration mismatch, system preference detection, localStorage persistence |
| Accessible dropdowns/dialogs | Custom modals/menus | Radix UI (via shadcn) | Keyboard navigation, focus trapping, ARIA attributes |
| Mobile navigation | Custom responsive menu | shadcn Sheet component | Slide-over panel with overlay, accessible, animated |
| Icon system | SVG imports/sprite sheets | lucide-react | Tree-shakeable, consistent style, shadcn default |

**Key insight:** shadcn/ui provides the building blocks (Button, Card, Input, Form, Sheet, DropdownMenu, Avatar, Separator) that would take days to build with proper accessibility. Install components as needed via `npx shadcn@latest add [component]`.

## Common Pitfalls

### Pitfall 1: NextAuth JWT Callback Stale Token
**What goes wrong:** After refreshing the access token via the JWT callback, `getServerSession` still returns the old token. The client-side SessionProvider caches the session.
**Why it happens:** NextAuth's session is a JWT cookie it manages separately from your backend tokens. The JWT callback runs server-side, but the client SessionProvider doesn't automatically re-fetch.
**How to avoid:** If using NextAuth, remove the `session` prop from `SessionProvider` so it always fetches fresh. Or avoid NextAuth entirely for credentials-only (recommended).
**Warning signs:** 401 errors after token should have been refreshed.

### Pitfall 2: CORS Credentials with fetch
**What goes wrong:** Refresh token cookie not sent with fetch requests, causing 401 on refresh.
**Why it happens:** `fetch` defaults to `credentials: "same-origin"`. The backend is on port 8080, frontend on 3000 (cross-origin).
**How to avoid:** Always use `credentials: "include"` in the fetch wrapper. Backend CORS already has `allowCredentials = true`.
**Warning signs:** Refresh endpoint returns 400 (missing cookie) instead of 200.

### Pitfall 3: Hydration Mismatch with Theme
**What goes wrong:** Server renders light theme, client detects dark preference, causing flash of unstyled content (FOUC).
**Why it happens:** Server doesn't know the user's system preference.
**How to avoid:** Use `next-themes` with `suppressHydrationWarning` on `<html>` tag. next-themes injects a script to set the class before hydration.
**Warning signs:** Brief white flash on dark-mode systems, React hydration warnings in console.

### Pitfall 4: proxy.ts Cannot Access In-Memory State
**What goes wrong:** Trying to check the access token (stored in module variable) from proxy.ts fails.
**Why it happens:** proxy.ts runs in a separate Node.js context, not the browser. It has no access to client-side state.
**How to avoid:** Set a lightweight cookie (e.g., `has_session=true`) during login that proxy.ts can check. This is a hint only -- real auth validation happens in the API client.
**Warning signs:** Proxy always redirects to login even when user is authenticated.

### Pitfall 5: Tailwind CSS v4 Configuration Changes
**What goes wrong:** Trying to create `tailwind.config.ts` manually when Tailwind v4 uses CSS-based configuration.
**Why it happens:** Tailwind v4 moved to `@theme` directives in CSS instead of JavaScript config files.
**How to avoid:** Let `create-next-app` set up Tailwind. Use `@theme` in `app/globals.css` for customization. shadcn/ui init handles this automatically.
**Warning signs:** Tailwind classes not applying, config file being ignored.

### Pitfall 6: Next.js 16 middleware.ts vs proxy.ts
**What goes wrong:** Creating `middleware.ts` instead of `proxy.ts`, or using edge runtime which is not supported in proxy.
**Why it happens:** Most tutorials and documentation still reference middleware.ts (Next.js 15 era).
**How to avoid:** Use `proxy.ts` at the project root. The function export should be named `proxy`, not `middleware`. Runtime is always Node.js.
**Warning signs:** Deprecation warnings, file being ignored.

### Pitfall 7: peer dependency conflict with next-auth
**What goes wrong:** `pnpm install next-auth@beta` fails with peer dependency error for Next.js 16.
**Why it happens:** next-auth v5 beta specifies `next@^12 || ^13 || ^14 || ^15` as peer dep, doesn't include 16.
**How to avoid:** Add to package.json: `"pnpm": { "overrides": { "next-auth": { "next": "$next" } } }` or use `--force`.
**Warning signs:** Install errors mentioning peer dependency mismatch.

## Code Examples

### Backend API Integration Types
```typescript
// types/api.ts
// Mirrors backend DTOs exactly

export interface AuthResponse {
  accessToken: string
  expiresIn: number
  tokenType: string
}

export interface MessageResponse {
  message: string
}

export interface ErrorResponse {
  status: number
  error: string
  message: string
  path: string
  timestamp: string
  fieldErrors?: Record<string, string>
}

export interface LoginRequest {
  email: string
  password: string
}

export interface RegisterRequest {
  email: string
  password: string
}

export interface PasswordResetRequest {
  email: string
}

export interface PasswordResetConfirmRequest {
  token: string
  newPassword: string
}
```

### Zod Schemas Matching Backend Validation
```typescript
// lib/validators.ts
import { z } from "zod"

export const loginSchema = z.object({
  email: z.string().email("Invalid email format"),
  password: z.string().min(1, "Password is required"),
})

export const registerSchema = z.object({
  email: z.string().email("Invalid email format"),
  password: z
    .string()
    .min(8, "Password must be at least 8 characters")
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
      "Password must contain at least one uppercase letter, one lowercase letter, and one number"
    ),
})

export const passwordResetSchema = z.object({
  email: z.string().email("Invalid email format"),
})

export const passwordResetConfirmSchema = z.object({
  token: z.string().min(1, "Token is required"),
  newPassword: z
    .string()
    .min(8, "Password must be at least 8 characters")
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).+$/,
      "Must contain uppercase, lowercase, and number"
    ),
})

export type LoginInput = z.infer<typeof loginSchema>
export type RegisterInput = z.infer<typeof registerSchema>
```

### Auth Mutation with TanStack Query
```typescript
// hooks/use-auth.ts (TanStack Query mutation pattern)
import { useMutation, useQueryClient } from "@tanstack/react-query"
import { apiClient } from "@/lib/api-client"
import { toast } from "sonner"
import type { AuthResponse, LoginRequest } from "@/types/api"

export function useLogin() {
  return useMutation({
    mutationFn: (data: LoginRequest) =>
      apiClient<AuthResponse>("/auth/login", {
        method: "POST",
        body: JSON.stringify(data),
      }),
    onSuccess: (data) => {
      // Store access token, set session cookie, redirect
      toast.success("Logged in successfully")
    },
    onError: (error) => {
      toast.error("Login failed. Please check your credentials.")
    },
  })
}
```

### shadcn/ui Login Form with React Hook Form
```typescript
// components/auth/login-form.tsx
"use client"

import { useForm } from "react-hook-form"
import { zodResolver } from "@hookform/resolvers/zod"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Form, FormControl, FormField, FormItem, FormLabel, FormMessage } from "@/components/ui/form"
import { loginSchema, type LoginInput } from "@/lib/validators"
import { useLogin } from "@/hooks/use-auth"

export function LoginForm() {
  const form = useForm<LoginInput>({
    resolver: zodResolver(loginSchema),
    defaultValues: { email: "", password: "" },
  })
  const loginMutation = useLogin()

  function onSubmit(data: LoginInput) {
    loginMutation.mutate(data)
  }

  return (
    <Form {...form}>
      <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-4">
        <FormField
          control={form.control}
          name="email"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Email</FormLabel>
              <FormControl>
                <Input placeholder="you@example.com" type="email" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <FormField
          control={form.control}
          name="password"
          render={({ field }) => (
            <FormItem>
              <FormLabel>Password</FormLabel>
              <FormControl>
                <Input type="password" {...field} />
              </FormControl>
              <FormMessage />
            </FormItem>
          )}
        />
        <Button type="submit" className="w-full" disabled={loginMutation.isPending}>
          {loginMutation.isPending ? "Signing in..." : "Sign in"}
        </Button>
      </form>
    </Form>
  )
}
```

### Responsive Sidebar with Mobile Sheet
```typescript
// components/layout/sidebar.tsx
"use client"

import { usePathname } from "next/navigation"
import Link from "next/link"
import { cn } from "@/lib/utils"
import { LayoutDashboard, Briefcase, Building2, FileText, FolderOpen } from "lucide-react"

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
    <aside className="hidden md:flex w-64 flex-col border-r bg-background">
      <div className="p-6">
        <h1 className="text-xl font-bold">JobHunt</h1>
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
| middleware.ts | proxy.ts | Next.js 16 (2025) | File renamed, function renamed, no edge runtime in proxy |
| tailwind.config.ts | @theme in CSS (Tailwind v4) | Tailwind CSS v4 (2025) | No JS config file, CSS-native configuration |
| NextAuth.js v4 | Auth.js v5 / Better Auth | 2023-2025 | v5 never left beta, Auth.js absorbed by Better Auth Sep 2025 |
| next-auth/middleware export | Custom proxy.ts logic | Next.js 16 | next-auth/middleware deprecated, use proxy.ts directly |
| HSL theme colors | OKLCH (shadcn v3.5+) | 2025 | shadcn converted to OKLCH with Tailwind v4 |
| QueryClientProvider only | QueryClientProvider + state init | TanStack Query v5 | QueryClient created in useState to prevent re-creation |

**Deprecated/outdated:**
- `middleware.ts`: Renamed to `proxy.ts` in Next.js 16. Will show deprecation warnings.
- `next-auth/middleware`: No longer applicable with proxy.ts
- `tailwind.config.ts`: Tailwind v4 uses CSS-based config via `@theme`
- HSL color format in shadcn: Now uses OKLCH

## Open Questions

1. **NextAuth vs Custom Auth**
   - What we know: User locked NextAuth as auth framework. However, Auth.js is in maintenance mode, v5 is beta-only, and the credentials provider + custom backend JWT pattern has well-documented pain points.
   - What's unclear: Whether the user is attached to NextAuth specifically or just wants "a proper auth solution."
   - Recommendation: Planner should implement custom auth (React Context + fetch wrapper) as it is simpler, more maintainable, and avoids the NextAuth abstraction leaks. The user decision was made before the Auth.js/Better Auth transition. If NextAuth is mandatory, use `next-auth@5.0.0-beta.30` with pnpm overrides for Next.js 16 peer dependency.

2. **proxy.ts Session Detection**
   - What we know: proxy.ts cannot access in-memory state. It can only read cookies.
   - What's unclear: Best cookie strategy for lightweight session detection (separate cookie vs reading refresh token cookie).
   - Recommendation: Set a non-HTTP-only `has_session=true` cookie on login, clear on logout. The refresh token cookie is scoped to `/api/auth/refresh` path and won't be visible to proxy.ts on other paths.

3. **create-next-app Defaults and AGENTS.md**
   - What we know: Next.js 16.2 create-next-app generates AGENTS.md and CLAUDE.md by default.
   - What's unclear: Whether these conflict with the project's existing CLAUDE.md convention.
   - Recommendation: Accept the generated files during init but review/adjust CLAUDE.md to match project conventions in frontend/CLAUDE.md.

## Validation Architecture

### Test Framework
| Property | Value |
|----------|-------|
| Framework | Vitest or Jest (via Next.js default) + React Testing Library |
| Config file | None yet - Wave 0 setup |
| Quick run command | `cd frontend && pnpm test` |
| Full suite command | `cd frontend && pnpm test:ci` |

### Phase Requirements -> Test Map
| Req ID | Behavior | Test Type | Automated Command | File Exists? |
|--------|----------|-----------|-------------------|-------------|
| INFR-05 | Responsive layout renders correctly | smoke/manual | Visual check at mobile/tablet/desktop breakpoints | No - Wave 0 |
| SC-1 | User can register and login through UI | integration | `pnpm test -- --grep "auth flow"` | No - Wave 0 |
| SC-2 | User can log out from any page | integration | `pnpm test -- --grep "logout"` | No - Wave 0 |
| SC-3 | Layout is responsive on mobile | manual-only | Resize browser / Chrome DevTools device mode | N/A |
| SC-4 | API client handles JWT automatically | unit | `pnpm test -- --grep "api-client"` | No - Wave 0 |

### Sampling Rate
- **Per task commit:** `pnpm test` (when test infrastructure exists)
- **Per wave merge:** `pnpm test && pnpm build` (build catches type errors)
- **Phase gate:** `pnpm build` must succeed, manual auth flow verification

### Wave 0 Gaps
- [ ] Test framework setup (Vitest recommended for speed, or use Next.js built-in Jest support)
- [ ] `__tests__/lib/api-client.test.ts` -- covers SC-4 (JWT auto-attach, 401 refresh retry)
- [ ] `__tests__/components/auth/login-form.test.tsx` -- covers SC-1 (form validation, submission)
- [ ] Manual verification checklist for SC-3 (responsive layout at 375px, 768px, 1280px)

## Sources

### Primary (HIGH confidence)
- [Next.js 16.2 Installation Docs](https://nextjs.org/docs/app/getting-started/installation) - create-next-app defaults, version 16.2.0
- [Next.js 16 proxy.ts migration](https://nextjs.org/docs/messages/middleware-to-proxy) - middleware renamed to proxy
- [shadcn/ui Next.js installation](https://ui.shadcn.com/docs/installation/next) - setup with Tailwind v4
- [shadcn/ui form docs](https://ui.shadcn.com/docs/forms/react-hook-form) - React Hook Form + Zod integration
- [TanStack Query v5 docs](https://tanstack.com/query/latest/docs) - current v5.91
- [shadcn/ui dark mode](https://ui.shadcn.com/docs/dark-mode/next) - next-themes setup

### Secondary (MEDIUM confidence)
- [Auth.js Credentials Provider](https://authjs.dev/getting-started/providers/credentials) - credentials provider limitations
- [Auth.js Refresh Token Rotation](https://authjs.dev/guides/refresh-token-rotation) - JWT callback refresh pattern
- [Auth.js / Better Auth merger](https://github.com/nextauthjs/next-auth/discussions/13252) - maintenance mode announcement
- [NextAuth + Next.js 16 compatibility](https://github.com/nextauthjs/next-auth/issues/13302) - peer dependency workaround

### Tertiary (LOW confidence)
- [next-auth@5.0.0-beta.30 compatibility claim](https://github.com/nextauthjs/next-auth/issues/13302) - community report, not officially verified
- Next.js 16 proxy.ts auth patterns - limited community examples available (new feature)

## Metadata

**Confidence breakdown:**
- Standard stack: HIGH - versions verified via official docs and npm
- Architecture: MEDIUM - App Router patterns well-established, proxy.ts patterns are new
- Pitfalls: HIGH - documented issues with NextAuth credentials, CORS, and hydration well-known
- Auth approach: MEDIUM - NextAuth is locked by user but research reveals significant concerns

**Research date:** 2026-03-20
**Valid until:** 2026-04-06 (Next.js and shadcn/ui ecosystem moving fast)
