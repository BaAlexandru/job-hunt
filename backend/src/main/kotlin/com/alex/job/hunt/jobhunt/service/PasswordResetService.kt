package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.MessageResponse
import com.alex.job.hunt.jobhunt.entity.PasswordResetToken
import com.alex.job.hunt.jobhunt.repository.PasswordResetTokenRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

class RateLimitException(message: String) : RuntimeException(message)

@Service
class PasswordResetService(
    private val userRepository: UserRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val rateLimiter: RateLimiter
) {

    private val logger = LoggerFactory.getLogger(PasswordResetService::class.java)

    fun requestReset(email: String): MessageResponse {
        if (!rateLimiter.isAllowed("password-reset:$email", 3, 3600)) {
            throw RateLimitException("Too many reset requests. Try again later.")
        }

        val user = userRepository.findByEmail(email)

        if (user != null) {
            val token = UUID.randomUUID().toString()
            val resetToken = PasswordResetToken(
                user = user,
                token = token,
                expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
            )
            passwordResetTokenRepository.save(resetToken)

            logger.info("Password reset link: http://localhost:8080/api/auth/password-reset/confirm?token=$token")
        }

        return MessageResponse("If an account with that email exists, a reset link has been sent.")
    }

    fun confirmReset(token: String, newPassword: String): MessageResponse {
        val resetToken = passwordResetTokenRepository.findByToken(token)
            ?: throw InvalidTokenException("Invalid reset token")

        if (resetToken.used) {
            throw InvalidTokenException("Token already used")
        }

        if (resetToken.expiresAt.isBefore(Instant.now())) {
            throw InvalidTokenException("Token expired")
        }

        resetToken.used = true
        resetToken.updatedAt = Instant.now()
        passwordResetTokenRepository.save(resetToken)

        val user = resetToken.user
        user.password = passwordEncoder.encode(newPassword)!!
        user.updatedAt = Instant.now()
        userRepository.save(user)

        return MessageResponse("Password reset successfully. You can now log in with your new password.")
    }
}
