package com.alex.job.hunt.jobhunt.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.mail.javamail.MimeMessageHelper
import org.springframework.stereotype.Service

@Service
class EmailService(
    private val mailSender: JavaMailSender,
    @Value("\${app.mail-from}") private val mailFrom: String,
    @Value("\${app.frontend-base-url}") private val frontendBaseUrl: String,
) {

    private val logger = LoggerFactory.getLogger(EmailService::class.java)

    fun sendPasswordResetEmail(to: String, token: String) {
        val resetUrl = "$frontendBaseUrl/auth/reset-password?token=$token"
        try {
            val message = mailSender.createMimeMessage()
            val helper = MimeMessageHelper(message, true, "UTF-8")
            helper.setTo(to)
            helper.setFrom(mailFrom)
            helper.setSubject("Reset your JobHunt password")
            helper.setText(buildResetEmailHtml(escapeHtml(resetUrl)), true)
            mailSender.send(message)
            logger.info("Password reset email sent successfully")
        } catch (e: Exception) {
            logger.error("Failed to send password reset email", e)
        }
    }

    private fun escapeHtml(input: String): String =
        input.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&#x27;")

    private fun buildResetEmailHtml(resetUrl: String): String = """
        <!DOCTYPE html>
        <html>
        <head><meta charset="UTF-8"></head>
        <body style="margin: 0; padding: 0; background-color: #f4f4f5; font-family: Arial, Helvetica, sans-serif;">
          <table role="presentation" width="100%" cellpadding="0" cellspacing="0" style="background-color: #f4f4f5; padding: 32px 0;">
            <tr>
              <td align="center">
                <table role="presentation" width="600" cellpadding="0" cellspacing="0" style="background-color: #ffffff; border-radius: 8px; overflow: hidden; max-width: 600px;">
                  <!-- Header -->
                  <tr>
                    <td style="padding: 32px 32px 16px 32px;">
                      <p style="margin: 0; font-size: 20px; font-weight: 600; color: #171717;">JobHunt</p>
                    </td>
                  </tr>
                  <!-- Heading -->
                  <tr>
                    <td style="padding: 0 32px 16px 32px;">
                      <h1 style="margin: 0; font-size: 24px; font-weight: 600; color: #171717;">Password Reset</h1>
                    </td>
                  </tr>
                  <!-- Body -->
                  <tr>
                    <td style="padding: 0 32px 24px 32px;">
                      <p style="margin: 0; font-size: 16px; font-weight: 400; color: #525252; line-height: 1.6;">We received a request to reset your password. Click the button below to set a new password.</p>
                    </td>
                  </tr>
                  <!-- CTA Button -->
                  <tr>
                    <td style="padding: 0 32px 24px 32px;">
                      <a href="$resetUrl" style="display: inline-block; background-color: #171717; color: #fafafa; padding: 12px 24px; border-radius: 6px; text-decoration: none; font-size: 16px; font-weight: 600;">Reset Password</a>
                    </td>
                  </tr>
                  <!-- Expiry notice -->
                  <tr>
                    <td style="padding: 0 32px 24px 32px;">
                      <p style="margin: 0; font-size: 14px; color: #737373; font-style: italic;">This link expires in 1 hour.</p>
                    </td>
                  </tr>
                  <!-- Footer -->
                  <tr>
                    <td style="padding: 16px 32px 32px 32px; border-top: 1px solid #e4e4e7;">
                      <p style="margin: 0; font-size: 12px; color: #a1a1aa;">If you didn't request this, you can safely ignore this email.</p>
                    </td>
                  </tr>
                </table>
              </td>
            </tr>
          </table>
        </body>
        </html>
    """.trimIndent()
}
