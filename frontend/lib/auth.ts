import { betterAuth } from "better-auth"
import { nextCookies } from "better-auth/next-js"
import { Pool } from "pg"

export const auth = betterAuth({
  database: new Pool({
    connectionString: process.env.DATABASE_URL,
  }),
  emailAndPassword: {
    enabled: true,
    requireEmailVerification: false,
    minPasswordLength: 8,
    autoSignIn: true,
    revokeSessionsOnPasswordReset: true,
    sendResetPassword: async ({ user, token }) => {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"
      fetch(`${apiUrl}/auth/send-reset-email`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "X-Internal-Secret": process.env.INTERNAL_API_SECRET || "",
        },
        body: JSON.stringify({ email: user.email, token }),
      }).catch((e) => {
        console.error("Failed to send reset email via backend:", e)
      })
    },
  },
  plugins: [nextCookies()],
})
