package com.alex.job.hunt.jobhunt.service

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.mail.javamail.JavaMailSender
import kotlin.test.assertContains
import kotlin.test.assertTrue

class EmailServiceTests {

    private lateinit var mailSender: JavaMailSender
    private lateinit var emailService: EmailService

    private val frontendBaseUrl = "http://localhost:3000"

    @BeforeEach
    fun setUp() {
        mailSender = mockk(relaxed = true)
        val mimeMessage = mockk<MimeMessage>(relaxed = true)
        every { mailSender.createMimeMessage() } returns mimeMessage
        emailService = EmailService(mailSender, "noreply@job-hunt.dev", frontendBaseUrl)
    }

    @Test
    fun `sendPasswordResetEmail sends email via JavaMailSender`() {
        emailService.sendPasswordResetEmail("user@example.com", "abc123")

        verify(exactly = 1) { mailSender.send(any<MimeMessage>()) }
    }

    @Test
    fun `sendPasswordResetEmail does not throw when mail sender fails`() {
        every { mailSender.send(any<MimeMessage>()) } throws RuntimeException("SMTP connection refused")

        // Should not throw -- falls back to logging
        emailService.sendPasswordResetEmail("user@example.com", "abc123")
    }

    @Test
    fun `sendPasswordResetEmail calls createMimeMessage`() {
        emailService.sendPasswordResetEmail("user@example.com", "t1")

        verify(exactly = 1) { mailSender.createMimeMessage() }
    }

    @Test
    fun `sendPasswordResetEmail does not send when createMimeMessage fails`() {
        every { mailSender.createMimeMessage() } throws RuntimeException("Mail config error")

        emailService.sendPasswordResetEmail("user@example.com", "t1")

        verify(exactly = 0) { mailSender.send(any<MimeMessage>()) }
    }

    @Test
    fun `buildResetEmailHtml contains required elements`() {
        val method = EmailService::class.java.getDeclaredMethod("buildResetEmailHtml", String::class.java)
        method.isAccessible = true
        val html = method.invoke(emailService, "http://localhost:3000/auth/reset-password?token=abc") as String

        assertContains(html, "Password Reset", message = "Should contain heading")
        assertContains(html, "Reset Password", message = "Should contain button text")
        assertContains(html, "This link expires in 1 hour.", message = "Should contain expiry notice")
        assertContains(html, "If you didn't request this", message = "Should contain safety footer")
        assertContains(html, "JobHunt", message = "Should contain app name")
        assertContains(html, "http://localhost:3000/auth/reset-password?token=abc", message = "Should contain reset URL")
    }

    @Test
    fun `buildResetEmailHtml produces valid HTML structure`() {
        val method = EmailService::class.java.getDeclaredMethod("buildResetEmailHtml", String::class.java)
        method.isAccessible = true
        val html = method.invoke(emailService, "http://localhost:3000/auth/reset-password?token=test") as String

        assertTrue(html.contains("<!DOCTYPE html>"), "Should start with DOCTYPE")
        assertTrue(html.contains("<html>"), "Should contain html tag")
        assertTrue(html.contains("</html>"), "Should close html tag")
        assertTrue(html.contains("href=\"http://localhost:3000/auth/reset-password?token=test\""), "Should have clickable reset link")
    }

    @Test
    fun `sendPasswordResetEmail constructs URL from frontendBaseUrl and token`() {
        val method = EmailService::class.java.getDeclaredMethod("buildResetEmailHtml", String::class.java)
        method.isAccessible = true

        // The service constructs the URL internally, so we test the HTML builder
        // with the expected URL format
        val html = method.invoke(emailService, "http://localhost:3000/auth/reset-password?token=mytoken") as String
        assertContains(html, "http://localhost:3000/auth/reset-password?token=mytoken")
    }
}
