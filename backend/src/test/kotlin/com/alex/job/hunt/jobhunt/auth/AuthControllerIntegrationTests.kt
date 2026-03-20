package com.alex.job.hunt.jobhunt.auth

import com.alex.job.hunt.jobhunt.dto.AuthRequest
import com.alex.job.hunt.jobhunt.dto.PasswordResetConfirmRequest
import com.alex.job.hunt.jobhunt.dto.PasswordResetRequest
import com.alex.job.hunt.jobhunt.dto.RegisterRequest
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
import com.alex.job.hunt.jobhunt.repository.PasswordResetTokenRepository
import com.alex.job.hunt.jobhunt.repository.TokenBlocklistRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.springframework.data.redis.core.StringRedisTemplate
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class AuthControllerIntegrationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository

    @Autowired
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    lateinit var tokenBlocklistRepository: TokenBlocklistRepository

    @Autowired
    lateinit var applicationNoteRepository: ApplicationNoteRepository

    @Autowired
    lateinit var applicationRepository: ApplicationRepository

    @Autowired
    lateinit var jobRepository: JobRepository

    @Autowired
    lateinit var companyRepository: CompanyRepository

    @Autowired
    lateinit var redisTemplate: StringRedisTemplate

    @Autowired
    lateinit var jsonMapper: JsonMapper

    @BeforeEach
    fun setUp() {
        applicationNoteRepository.deleteAll()
        applicationRepository.deleteAll()
        jobRepository.deleteAll()
        companyRepository.deleteAll()
        tokenBlocklistRepository.deleteAll()
        passwordResetTokenRepository.deleteAll()
        emailVerificationTokenRepository.deleteAll()
        userRepository.deleteAll()
        redisTemplate.connectionFactory?.connection?.serverCommands()?.flushAll()
    }

    @Test
    fun registerSuccess() {
        val request = RegisterRequest(email = "test@example.com", password = "Test1234")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.message").value("Registration successful. Please check your email to verify your account."))
    }

    @Test
    fun registerDuplicateEmail() {
        val request = RegisterRequest(email = "test@example.com", password = "Test1234")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.message").value("Registration failed"))
    }

    @Test
    fun registerWeakPassword() {
        val request = RegisterRequest(email = "test@example.com", password = "weak")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
    }

    @Test
    fun loginUnverifiedAccount() {
        val registerRequest = RegisterRequest(email = "test@example.com", password = "Test1234")
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(registerRequest))
        )

        val loginRequest = AuthRequest(email = "test@example.com", password = "Test1234")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.message").value("Account not verified. Please check your email."))
    }

    @Test
    fun verifyEmailAndLogin() {
        // Register
        val registerRequest = RegisterRequest(email = "test@example.com", password = "Test1234")
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(registerRequest))
        )
            .andExpect(status().isCreated)

        // Get verification token from DB
        val verificationToken = emailVerificationTokenRepository.findAll().first().token

        // Verify email
        mockMvc.perform(get("/api/auth/verify").param("token", verificationToken))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Email verified successfully. You can now log in."))

        // Login
        val loginRequest = AuthRequest(email = "test@example.com", password = "Test1234")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(loginRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
            .andExpect(header().exists("Set-Cookie"))
    }

    @Test
    fun refreshToken() {
        // Register and verify
        registerAndVerify("test@example.com", "Test1234")

        // Login
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(AuthRequest("test@example.com", "Test1234")))
        )
            .andExpect(status().isOk)
            .andReturn()

        val refreshCookie = loginResult.response.getCookie("refresh_token")!!

        // Refresh
        mockMvc.perform(
            post("/api/auth/refresh")
                .cookie(Cookie("refresh_token", refreshCookie.value))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
    }

    @Test
    fun logoutInvalidatesTokens() {
        // Register and verify
        registerAndVerify("test@example.com", "Test1234")

        // Login
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(AuthRequest("test@example.com", "Test1234")))
        )
            .andExpect(status().isOk)
            .andReturn()

        val responseBody = loginResult.response.contentAsString
        val accessToken = jsonMapper.readTree(responseBody).get("accessToken").textValue()
        val refreshCookie = loginResult.response.getCookie("refresh_token")!!

        // Logout
        mockMvc.perform(
            post("/api/auth/logout")
                .header("Authorization", "Bearer $accessToken")
                .cookie(Cookie("refresh_token", refreshCookie.value))
        )
            .andExpect(status().isOk)

        // Verify tokens are blocklisted
        val blocklistEntries = tokenBlocklistRepository.findAll()
        assert(blocklistEntries.size >= 1) { "Expected blocklisted tokens after logout" }

        // Verify the old access token is rejected on a protected endpoint
        mockMvc.perform(
            get("/api/protected")
                .header("Authorization", "Bearer $accessToken")
        )
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun passwordResetFlow() {
        // Register and verify
        registerAndVerify("test@example.com", "Test1234")

        // Request password reset
        mockMvc.perform(
            post("/api/auth/password-reset")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(PasswordResetRequest("test@example.com")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("If an account with that email exists, a reset link has been sent."))

        // Get reset token from DB
        val resetToken = passwordResetTokenRepository.findAll().first().token

        // Confirm password reset
        mockMvc.perform(
            post("/api/auth/password-reset/confirm")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    jsonMapper.writeValueAsString(
                        PasswordResetConfirmRequest(token = resetToken, newPassword = "NewPass123")
                    )
                )
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.message").value("Password reset successfully. You can now log in with your new password."))

        // Login with new password
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(AuthRequest("test@example.com", "NewPass123")))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").exists())
    }

    @Test
    fun protectedEndpointRequiresAuth() {
        // Public actuator info endpoint
        mockMvc.perform(get("/actuator/info"))
            .andExpect(status().isOk)

        // Non-existent API endpoint without token should return 401
        mockMvc.perform(get("/api/protected"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun corsHeaders() {
        mockMvc.perform(
            options("/api/auth/login")
                .header("Origin", "http://localhost:3000")
                .header("Access-Control-Request-Method", "POST")
                .header("Access-Control-Request-Headers", "Content-Type")
        )
            .andExpect(status().isOk)
            .andExpect(header().string("Access-Control-Allow-Origin", "http://localhost:3000"))
    }

    private fun registerAndVerify(email: String, password: String) {
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(RegisterRequest(email, password)))
        )
            .andExpect(status().isCreated)

        val verificationToken = emailVerificationTokenRepository.findAll().first().token

        mockMvc.perform(get("/api/auth/verify").param("token", verificationToken))
            .andExpect(status().isOk)
    }
}
