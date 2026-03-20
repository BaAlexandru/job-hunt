package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.AuthRequest
import com.alex.job.hunt.jobhunt.dto.AuthResponse
import com.alex.job.hunt.jobhunt.dto.MessageResponse
import com.alex.job.hunt.jobhunt.dto.RegisterRequest
import com.alex.job.hunt.jobhunt.entity.EmailVerificationToken
import com.alex.job.hunt.jobhunt.entity.TokenBlocklistEntry
import com.alex.job.hunt.jobhunt.entity.UserEntity
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.TokenBlocklistRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import com.alex.job.hunt.jobhunt.security.JwtTokenProvider
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class RegistrationException(message: String) : RuntimeException(message)
class AuthenticationException(message: String) : RuntimeException(message)

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val tokenBlocklistRepository: TokenBlocklistRepository,
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtTokenProvider: JwtTokenProvider
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)

    @Transactional
    fun register(request: RegisterRequest): MessageResponse {
        if (userRepository.existsByEmail(request.email)) {
            throw RegistrationException("Registration failed")
        }

        val user = UserEntity(
            email = request.email,
            password = passwordEncoder.encode(request.password)!!,
            enabled = false
        )
        val savedUser = userRepository.save(user)

        val token = UUID.randomUUID().toString()
        val verificationToken = EmailVerificationToken(
            user = savedUser,
            token = token,
            expiresAt = Instant.now().plus(24, ChronoUnit.HOURS)
        )
        emailVerificationTokenRepository.save(verificationToken)

        logger.info("Email verification link: http://localhost:8080/api/auth/verify?token=$token")

        return MessageResponse("Registration successful. Please check your email to verify your account.")
    }

    fun login(request: AuthRequest): Pair<AuthResponse, String> {
        val user = userRepository.findByEmail(request.email)
            ?: throw AuthenticationException("Invalid credentials")

        if (!passwordEncoder.matches(request.password, user.password)) {
            throw AuthenticationException("Invalid credentials")
        }

        if (!user.enabled) {
            throw AuthenticationException("Account not verified. Please check your email.")
        }

        val accessToken = jwtTokenProvider.createAccessToken(user.id!!, user.email, user.role.name)
        val refreshToken = jwtTokenProvider.createRefreshToken(user.id!!, user.email)

        return Pair(
            AuthResponse(accessToken, jwtTokenProvider.getAccessExpirationMs() / 1000),
            refreshToken
        )
    }

    fun refresh(refreshToken: String): Pair<AuthResponse, String> {
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw AuthenticationException("Invalid refresh token")
        }

        if (jwtTokenProvider.getTokenType(refreshToken) != "refresh") {
            throw AuthenticationException("Invalid token type")
        }

        val tokenId = jwtTokenProvider.getTokenId(refreshToken)
        if (tokenBlocklistRepository.existsByTokenId(tokenId)) {
            throw AuthenticationException("Token revoked")
        }

        // Blocklist the old refresh token
        tokenBlocklistRepository.save(
            TokenBlocklistEntry(
                tokenId = tokenId,
                expiresAt = jwtTokenProvider.getExpiration(refreshToken).toInstant()
            )
        )

        val username = jwtTokenProvider.getUsername(refreshToken)
        val user = userRepository.findByEmail(username)
            ?: throw AuthenticationException("User not found")

        val newAccessToken = jwtTokenProvider.createAccessToken(user.id!!, user.email, user.role.name)
        val newRefreshToken = jwtTokenProvider.createRefreshToken(user.id!!, user.email)

        return Pair(
            AuthResponse(newAccessToken, jwtTokenProvider.getAccessExpirationMs() / 1000),
            newRefreshToken
        )
    }

    @Transactional
    fun logout(accessToken: String?, refreshToken: String?) {
        if (accessToken != null && jwtTokenProvider.validateToken(accessToken)) {
            tokenBlocklistRepository.save(
                TokenBlocklistEntry(
                    tokenId = jwtTokenProvider.getTokenId(accessToken),
                    expiresAt = jwtTokenProvider.getExpiration(accessToken).toInstant()
                )
            )
        }

        if (refreshToken != null && jwtTokenProvider.validateToken(refreshToken)) {
            tokenBlocklistRepository.save(
                TokenBlocklistEntry(
                    tokenId = jwtTokenProvider.getTokenId(refreshToken),
                    expiresAt = jwtTokenProvider.getExpiration(refreshToken).toInstant()
                )
            )
        }
    }
}
