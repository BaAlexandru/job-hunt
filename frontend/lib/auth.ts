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
    sendResetPassword: async ({ user, url }) => {
      const apiUrl = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080/api"
      try {
        await fetch(`${apiUrl}/auth/send-reset-email`, {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({ email: user.email, resetUrl: url }),
        })
      } catch (e) {
        console.error("Failed to send reset email via backend:", e)
        console.log(`Password reset fallback for ${user.email}: ${url}`)
      }
    },
  },
  plugins: [nextCookies()],
})
