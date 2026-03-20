package com.alex.job.hunt.jobhunt.security

import com.alex.job.hunt.jobhunt.repository.TokenBlocklistRepository
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter

@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider,
    private val userDetailsService: UserDetailsService,
    private val tokenBlocklistRepository: TokenBlocklistRepository
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        val token = extractToken(request)

        if (token != null && jwtTokenProvider.validateToken(token)) {
            val tokenId = jwtTokenProvider.getTokenId(token)
            if (!tokenBlocklistRepository.existsByTokenId(tokenId)) {
                if (jwtTokenProvider.getTokenType(token) == "access") {
                    val username = jwtTokenProvider.getUsername(token)
                    val userDetails = userDetailsService.loadUserByUsername(username)
                    val auth = UsernamePasswordAuthenticationToken(
                        userDetails, null, userDetails.authorities
                    )
                    SecurityContextHolder.getContext().authentication = auth
                }
            }
        }

        filterChain.doFilter(request, response)
    }

    private fun extractToken(request: HttpServletRequest): String? =
        request.getHeader("Authorization")
            ?.takeIf { it.startsWith("Bearer ") }
            ?.substring(7)
}
