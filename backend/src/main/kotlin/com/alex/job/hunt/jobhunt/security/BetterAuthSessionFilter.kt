package com.alex.job.hunt.jobhunt.security

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.slf4j.LoggerFactory
import org.springframework.dao.EmptyResultDataAccessException
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

    private val log = LoggerFactory.getLogger(BetterAuthSessionFilter::class.java)

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
            // Step 1: Validate Better Auth session and get the user's email
            val sessionResult = jdbcTemplate.queryForMap(
                """
                SELECT u.email
                FROM session s
                JOIN "user" u ON u.id = s."userId"
                WHERE s.token = ?
                  AND s."expiresAt" > NOW()
                """.trimIndent(),
                token
            )

            val email = sessionResult["email"] as String

            // Step 2: Look up or auto-create the backend user
            val backendUser = try {
                jdbcTemplate.queryForMap(
                    "SELECT id, email, enabled FROM users WHERE email = ?",
                    email
                )
            } catch (_: EmptyResultDataAccessException) {
                // No backend user exists — auto-provision one
                log.info("Auto-provisioned backend user for Better Auth email: {}", email)
                jdbcTemplate.queryForMap(
                    """
                    INSERT INTO users (id, email, password, role, enabled, created_at, updated_at)
                    VALUES (gen_random_uuid(), ?, '', 'USER', true, NOW(), NOW())
                    RETURNING id, email, enabled
                    """.trimIndent(),
                    email
                )
            }

            val backendUserId = backendUser["id"] as UUID
            val backendEmail = backendUser["email"] as String
            val enabled = backendUser["enabled"] as Boolean

            if (!enabled) return

            val userDetails = AppUserDetails(
                userId = backendUserId,
                email = backendEmail,
                password = "",
                authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
                enabled = true
            )

            val auth = UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.authorities
            )
            SecurityContextHolder.getContext().authentication = auth
        } catch (_: Exception) {
            // Session not found or DB error
            // — let the filter chain continue unauthenticated
        }
    }
}
