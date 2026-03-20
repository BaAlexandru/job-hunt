"use client"

import Link from "next/link"
import { SignedIn, SignedOut } from "@daveyplate/better-auth-ui"
import { Button } from "@/components/ui/button"
import { useRouter } from "next/navigation"
import { useEffect } from "react"

function RedirectToDashboard() {
  const router = useRouter()
  useEffect(() => {
    router.replace("/dashboard")
  }, [router])
  return null
}

export default function LandingPage() {
  return (
    <>
      <SignedIn>
        <RedirectToDashboard />
      </SignedIn>
      <SignedOut>
        <main className="flex min-h-screen flex-col items-center justify-center gap-8 p-4">
          <div className="max-w-xl text-center">
            <h1 className="text-2xl font-semibold leading-tight">
              Track your job search
            </h1>
            <p className="mt-2 text-sm text-muted-foreground">
              Keep every application, interview, and document organized in one
              place.
            </p>
          </div>
          <div className="flex gap-4">
            <Button asChild>
              <Link href="/auth/sign-up">Start tracking jobs</Link>
            </Button>
            <Button variant="outline" asChild>
              <Link href="/auth/sign-in">Sign in</Link>
            </Button>
          </div>
        </main>
      </SignedOut>
    </>
  )
}
