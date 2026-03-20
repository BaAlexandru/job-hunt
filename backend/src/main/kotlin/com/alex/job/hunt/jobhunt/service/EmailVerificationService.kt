package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.dto.MessageResponse
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.springframework.stereotype.Service
import java.time.Instant

class InvalidTokenException(message: String) : RuntimeException(message)

@Service
class EmailVerificationService(
    private val emailVerificationTokenRepository: EmailVerificationTokenRepository,
    private val userRepository: UserRepository
) {

    fun verify(token: String): MessageResponse {
        val verificationToken = emailVerificationTokenRepository.findByToken(token)
            ?: throw InvalidTokenException("Invalid verification token")

        if (verificationToken.used) {
            throw InvalidTokenException("Token already used")
        }

        if (verificationToken.expiresAt.isBefore(Instant.now())) {
            throw InvalidTokenException("Token expired")
        }

        verificationToken.used = true
        verificationToken.updatedAt = Instant.now()
        emailVerificationTokenRepository.save(verificationToken)

        val user = verificationToken.user
        user.enabled = true
        user.updatedAt = Instant.now()
        userRepository.save(user)

        return MessageResponse("Email verified successfully. You can now log in.")
    }
}
