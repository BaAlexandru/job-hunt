# Frontend - Claude Code Instructions

## Stack

- Next.js 16.2 with App Router, TypeScript, Tailwind CSS v4
- pnpm package manager
- Not a Gradle subproject -- standalone Node.js project in this directory
- Better Auth for authentication (session cookies, not JWT)
- @daveyplate/better-auth-ui for pre-built auth components
- shadcn/ui (Radix UI + Tailwind) for UI components
- TanStack Query v5 for server state
- Vitest + React Testing Library for tests

## Key Commands

- Dev: `pnpm dev` (port 3000)
- Build: `pnpm build`
- Test: `pnpm test`
- Test watch: `pnpm test:watch`
- Lint: `pnpm lint`

## Conventions

- API base URL: http://localhost:8080/api (via NEXT_PUBLIC_API_URL)
- Auth operations go through `authClient` (lib/auth-client.ts), NOT apiClient
- Non-auth API calls go through `apiClient` (lib/api-client.ts) with credentials: "include"
- Providers in components/providers.tsx: QueryClientProvider > AuthUIProvider > ThemeProvider
- Route protection via proxy.ts using getSessionCookie from better-auth/cookies
- Tailwind v4 CSS-based config (no tailwind.config.ts) -- use @theme in globals.css
- shadcn/ui components in components/ui/ -- OKLCH colors, base-nova style
- next/font/google for Inter font (weights 400, 600)
