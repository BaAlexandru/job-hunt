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
        return request.cookies
            ?.firstOrNull { it.name == COOKIE_NAME }
            ?.value
    }

    private fun authenticateFromSession(token: String) {
        try {
            // Query session + user in one join. Better Auth session.token is the cookie value.
            val result = jdbcTemplate.queryForMap(
                """
                SELECT s.user_id, s.expires_at, u.email, u.name
                FROM session s
                JOIN "user" u ON u.id = s.user_id
                WHERE s.token = ?
                """.trimIndent(),
                token
            )

            val expiresAt = (result["expires_at"] as java.sql.Timestamp).toInstant()
            if (expiresAt.isBefore(Instant.now())) {
                return // Session expired
            }

            val userId = UUID.fromString(result["user_id"] as String)
            val email = result["email"] as String

            // Create AppUserDetails so SecurityContextUtil.getCurrentUserId() works
            val userDetails = AppUserDetails(
                userId = userId,
                email = email,
                password = "", // Not needed for session-based auth
                authorities = listOf(SimpleGrantedAuthority("ROLE_USER")),
                enabled = true
            )

            val auth = UsernamePasswordAuthenticationToken(
                userDetails, null, userDetails.authorities
            )
            SecurityContextHolder.getContext().authentication = auth
        } catch (_: Exception) {
            // Session not found or DB error -- let the filter chain continue unauthenticated
        }
    }
}
