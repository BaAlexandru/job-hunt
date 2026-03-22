package com.alex.job.hunt.jobhunt.service

import com.alex.job.hunt.jobhunt.entity.PasswordResetToken
import com.alex.job.hunt.jobhunt.entity.UserEntity
import com.alex.job.hunt.jobhunt.repository.PasswordResetTokenRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertContains
import kotlin.test.assertTrue

class PasswordResetServiceTests {

    private lateinit var userRepository: UserRepository
    private lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    private lateinit var passwordEncoder: PasswordEncoder
    private lateinit var rateLimiter: RateLimiter
    private lateinit var emailService: EmailService
    private lateinit var passwordResetService: PasswordResetService

    private val frontendBaseUrl = "http://localhost:3000"

    private fun testUser(email: String = "user@example.com") = UserEntity(
        id = UUID.randomUUID(),
        email = email,
        password = "hashed-password",
        enabled = true,
    )

    @BeforeEach
    fun setUp() {
        userRepository = mockk(relaxed = true)
        passwordResetTokenRepository = mockk(relaxed = true)
        passwordEncoder = mockk(relaxed = true)
        rateLimiter = mockk(relaxed = true)
        emailService = mockk(relaxed = true)

        every { rateLimiter.isAllowed(any(), any(), any()) } returns true
        every { passwordResetTokenRepository.save(any<PasswordResetToken>()) } answers { firstArg() }
        every { userRepository.save(any<UserEntity>()) } answers { firstArg() }

        passwordResetService = PasswordResetService(
            userRepository = userRepository,
            passwordResetTokenRepository = passwordResetTokenRepository,
            passwordEncoder = passwordEncoder,
            rateLimiter = rateLimiter,
            emailService = emailService,
            frontendBaseUrl = frontendBaseUrl,
        )
    }

    @Test
    fun `requestReset sends email when user exists`() {
        val user = testUser()
        every { userRepository.findByEmail("user@example.com") } returns user

        passwordResetService.requestReset("user@example.com")

        verify(exactly = 1) { emailService.sendPasswordResetEmail("user@example.com", any()) }
    }

    @Test
    fun `requestReset sends email with correct reset URL format`() {
        val user = testUser()
        every { userRepository.findByEmail("user@example.com") } returns user

        val urlSlot = slot<String>()
        every { emailService.sendPasswordResetEmail(any(), capture(urlSlot)) } returns Unit

        passwordResetService.requestReset("user@example.com")

        val capturedUrl = urlSlot.captured
        assertContains(capturedUrl, "http://localhost:3000/auth/reset-password?token=")
    }

    @Test
    fun `requestReset does not send email when user does not exist`() {
        every { userRepository.findByEmail("unknown@example.com") } returns null

        passwordResetService.requestReset("unknown@example.com")

        verify(exactly = 0) { emailService.sendPasswordResetEmail(any(), any()) }
    }

    @Test
    fun `requestReset returns safe message regardless of user existence`() {
        every { userRepository.findByEmail(any()) } returns null

        val result = passwordResetService.requestReset("nonexistent@example.com")

        assertEquals("If an account with that email exists, a reset link has been sent.", result.message)
    }

    @Test
    fun `requestReset saves token before sending email`() {
        val user = testUser()
        every { userRepository.findByEmail("user@example.com") } returns user

        passwordResetService.requestReset("user@example.com")

        verify(exactly = 1) { passwordResetTokenRepository.save(any<PasswordResetToken>()) }
    }

    @Test
    fun `requestReset throws RateLimitException when rate limited`() {
        every { rateLimiter.isAllowed(any(), any(), any()) } returns false

        assertThrows<RateLimitException> {
            passwordResetService.requestReset("user@example.com")
        }
    }

    @Test
    fun `requestReset checks rate limit with email-specific key`() {
        every { userRepository.findByEmail(any()) } returns null

        passwordResetService.requestReset("test@example.com")

        verify { rateLimiter.isAllowed("password-reset:test@example.com", 3, 3600) }
    }

    @Test
    fun `confirmReset throws InvalidTokenException for unknown token`() {
        every { passwordResetTokenRepository.findByToken("bad-token") } returns null

        assertThrows<InvalidTokenException> {
            passwordResetService.confirmReset("bad-token", "newpassword")
        }
    }

    @Test
    fun `confirmReset throws InvalidTokenException for used token`() {
        val user = testUser()
        val token = PasswordResetToken(
            id = UUID.randomUUID(),
            user = user,
            token = "used-token",
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS),
            used = true,
        )
        every { passwordResetTokenRepository.findByToken("used-token") } returns token

        assertThrows<InvalidTokenException> {
            passwordResetService.confirmReset("used-token", "newpassword")
        }
    }

    @Test
    fun `confirmReset throws InvalidTokenException for expired token`() {
        val user = testUser()
        val token = PasswordResetToken(
            id = UUID.randomUUID(),
            user = user,
            token = "expired-token",
            expiresAt = Instant.now().minus(1, ChronoUnit.HOURS),
            used = false,
        )
        every { passwordResetTokenRepository.findByToken("expired-token") } returns token

        assertThrows<InvalidTokenException> {
            passwordResetService.confirmReset("expired-token", "newpassword")
        }
    }

    @Test
    fun `confirmReset updates password for valid token`() {
        val user = testUser()
        val token = PasswordResetToken(
            id = UUID.randomUUID(),
            user = user,
            token = "valid-token",
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS),
            used = false,
        )
        every { passwordEncoder.encode("newpassword123") } returns "encoded-password"
        every { passwordResetTokenRepository.findByToken("valid-token") } returns token

        passwordResetService.confirmReset("valid-token", "newpassword123")

        assertEquals("encoded-password", user.password)
        verify { userRepository.save(user) }
    }

    @Test
    fun `confirmReset marks token as used`() {
        val user = testUser()
        val token = PasswordResetToken(
            id = UUID.randomUUID(),
            user = user,
            token = "valid-token",
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS),
            used = false,
        )
        every { passwordEncoder.encode(any()) } returns "encoded"
        every { passwordResetTokenRepository.findByToken("valid-token") } returns token

        passwordResetService.confirmReset("valid-token", "newpass")

        assertTrue(token.used, "Token should be marked as used")
        verify { passwordResetTokenRepository.save(token) }
    }
}
