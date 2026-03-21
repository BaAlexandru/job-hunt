package com.alex.job.hunt.jobhunt.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import java.time.Instant
import java.util.UUID

/**
 * Authenticates requests using Better Auth session cookies.
 *
 * Better Auth stores sessions in its own `session` table with a token cookie.
 * This filter reads the cookie, validates the session against the DB, and sets
 * the Spring Security context so downstream code (SecurityContextUtil) works
 * identically whether the request came via JWT or session cookie.
 *
 * Note: Better Auth uses camelCase column names (userId, expiresAt) and TEXT
 * user IDs. The backend uses UUID user IDs in the `users` table. This filter
 * bridges the two by looking up the backend user via email.
 */
@Component
class BetterAuthSessionFilter(
    private val jdbcTemplate: JdbcTemplate
) : OncePerRequestFilter() {

    companion object {
        private const val COOKIE_NAME = "better-auth.session_token"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // Skip if already authenticated (e.g. by another filter)
        if (SecurityContextHolder.getContext().authentication != null) {
            filterChain.doFilter(request, response)
            return
        }

        val sessionToken = extractSessionToken(request)
        if (sessionToken != null) {
            authenticateFromSession(sessionToken)
        }

        filterChain.doFilter(request, response)
    }

    private fun extractSessionToken(request: HttpServletRequest): String? {
        val cookieValue = request.cookies
            ?.firstOrNull { it.name == COOKIE_NAME }
            ?.value ?: return null

        // Better Auth cookie format is "token.hash" — DB stores just the token part
        return cookieValue.substringBefore(".")
    }

    private fun authenticateFromSession(token: String) {
        try {
            // Join Better Auth session → Better Auth user → backend users (via email)
            // Better Auth uses camelCase columns; backend users table uses snake_case
            val result = jdbcTemplate.queryForMap(
                """
                SELECT bu.id AS backend_user_id, bu.email, bu.enabled
                FROM session s
                JOIN "user" u ON u.id = s."userId"
                JOIN users bu ON bu.email = u.email
                WHERE s.token = ?
                  AND s."expiresAt" > NOW()
                """.trimIndent(),
                token
            )

            val backendUserId = result["backend_user_id"] as UUID
            val email = result["email"] as String
            val enabled = result["enabled"] as Boolean

            if (!enabled) return

            val userDetails = AppUserDetails(
                userId = backendUserId,
                email = email,
                password = "",
                authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
                enabled = true
            )

            val auth = UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.authorities
            )
            SecurityContextHolder.getContext().authentication = auth
        } catch (_: Exception) {
            // Session not found, no matching backend user, or DB error
            // — let the filter chain continue unauthenticated
        }
    }
}
