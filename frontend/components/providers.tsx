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
