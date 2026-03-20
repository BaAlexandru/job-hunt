package com.alex.job.hunt.jobhunt.company

import com.alex.job.hunt.jobhunt.dto.AuthRequest
import com.alex.job.hunt.jobhunt.dto.CreateCompanyRequest
import com.alex.job.hunt.jobhunt.dto.RegisterRequest
import com.alex.job.hunt.jobhunt.dto.UpdateCompanyRequest
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.PasswordResetTokenRepository
import com.alex.job.hunt.jobhunt.repository.TokenBlocklistRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class CompanyControllerIntegrationTests {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var jsonMapper: JsonMapper

    @Autowired
    lateinit var companyRepository: CompanyRepository

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository

    @Autowired
    lateinit var passwordResetTokenRepository: PasswordResetTokenRepository

    @Autowired
    lateinit var tokenBlocklistRepository: TokenBlocklistRepository

    @BeforeEach
    fun setUp() {
        companyRepository.deleteAll()
        tokenBlocklistRepository.deleteAll()
        passwordResetTokenRepository.deleteAll()
        emailVerificationTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    // --- COMP-01: Create ---

    @Test
    fun `should return 201 when creating company with all fields`() {
        val token = registerAndGetToken()
        val request = CreateCompanyRequest(
            name = "Google",
            website = "https://google.com",
            location = "Mountain View, CA",
            notes = "Tech giant"
        )

        mockMvc.perform(
            post("/api/companies")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.name").value("Google"))
            .andExpect(jsonPath("$.website").value("https://google.com"))
            .andExpect(jsonPath("$.location").value("Mountain View, CA"))
            .andExpect(jsonPath("$.notes").value("Tech giant"))
            .andExpect(jsonPath("$.archived").value(false))
            .andExpect(jsonPath("$.archivedAt").isEmpty())
            .andExpect(jsonPath("$.createdAt").exists())
            .andExpect(jsonPath("$.updatedAt").exists())
    }

    @Test
    fun `should return 201 when creating company with only name`() {
        val token = registerAndGetToken()
        val request = CreateCompanyRequest(name = "Startup Inc")

        mockMvc.perform(
            post("/api/companies")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("Startup Inc"))
            .andExpect(jsonPath("$.website").isEmpty())
            .andExpect(jsonPath("$.location").isEmpty())
            .andExpect(jsonPath("$.notes").isEmpty())
    }

    @Test
    fun `should return 400 when name is blank`() {
        val token = registerAndGetToken()
        val request = CreateCompanyRequest(name = "")

        mockMvc.perform(
            post("/api/companies")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.fieldErrors.name").exists())
    }

    @Test
    fun `should return 401 when no auth token`() {
        val request = CreateCompanyRequest(name = "Google")

        mockMvc.perform(
            post("/api/companies")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isUnauthorized)
    }

    // --- COMP-02: Update + Delete ---

    @Test
    fun `should return 200 when updating company`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "Original Name")

        val updateRequest = UpdateCompanyRequest(
            name = "Updated Name",
            website = "https://updated.com",
            location = "New York",
            notes = "Updated notes"
        )

        mockMvc.perform(
            put("/api/companies/$companyId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.name").value("Updated Name"))
            .andExpect(jsonPath("$.website").value("https://updated.com"))
            .andExpect(jsonPath("$.location").value("New York"))
            .andExpect(jsonPath("$.notes").value("Updated notes"))
    }

    @Test
    fun `should return 404 when updating non-existent company`() {
        val token = registerAndGetToken()
        val updateRequest = UpdateCompanyRequest(name = "Updated")

        mockMvc.perform(
            put("/api/companies/${UUID.randomUUID()}")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 404 when updating another user's company`() {
        val tokenA = registerAndGetToken("userA@example.com")
        val companyId = createCompany(tokenA, "User A Company")

        val tokenB = registerAndGetToken("userB@example.com")
        val updateRequest = UpdateCompanyRequest(name = "Hijacked")

        mockMvc.perform(
            put("/api/companies/$companyId")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(updateRequest))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `should return 204 when archiving company`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "To Archive")

        mockMvc.perform(
            delete("/api/companies/$companyId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `should return 404 when archiving non-existent company`() {
        val token = registerAndGetToken()

        mockMvc.perform(
            delete("/api/companies/${UUID.randomUUID()}")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isNotFound)
    }

    // --- COMP-03: List + Get ---

    @Test
    fun `should return paginated list of companies`() {
        val token = registerAndGetToken()
        createCompany(token, "Company A")
        createCompany(token, "Company B")
        createCompany(token, "Company C")

        mockMvc.perform(
            get("/api/companies")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.content.length()").value(3))
    }

    @Test
    fun `should exclude archived companies by default`() {
        val token = registerAndGetToken()
        createCompany(token, "Active Company")
        val archivedId = createCompany(token, "Archived Company")

        // Archive one
        mockMvc.perform(
            delete("/api/companies/$archivedId")
                .header("Authorization", "Bearer $token")
        )

        mockMvc.perform(
            get("/api/companies")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Active Company"))
    }

    @Test
    fun `should include archived when requested`() {
        val token = registerAndGetToken()
        createCompany(token, "Active Company")
        val archivedId = createCompany(token, "Archived Company")

        // Archive one
        mockMvc.perform(
            delete("/api/companies/$archivedId")
                .header("Authorization", "Bearer $token")
        )

        mockMvc.perform(
            get("/api/companies")
                .param("includeArchived", "true")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `should filter by name`() {
        val token = registerAndGetToken()
        createCompany(token, "Google")
        createCompany(token, "Apple")

        mockMvc.perform(
            get("/api/companies")
                .param("q", "goo")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Google"))
    }

    @Test
    fun `should return empty page when no companies`() {
        val token = registerAndGetToken()

        mockMvc.perform(
            get("/api/companies")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
            .andExpect(jsonPath("$.content").isEmpty())
    }

    @Test
    fun `should return 200 for single company`() {
        val token = registerAndGetToken()
        val companyId = createCompany(token, "Google")

        mockMvc.perform(
            get("/api/companies/$companyId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(companyId))
            .andExpect(jsonPath("$.name").value("Google"))
    }

    @Test
    fun `should not return other users' companies in list`() {
        val tokenA = registerAndGetToken("userA@example.com")
        createCompany(tokenA, "User A Company")

        val tokenB = registerAndGetToken("userB@example.com")

        mockMvc.perform(
            get("/api/companies")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String = "test@example.com", password: String = "Password123!"): String {
        // Register
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(RegisterRequest(email, password)))
        )
            .andExpect(status().isCreated)

        // Get verification token from DB (latest one created for this registration)
        val verificationToken = emailVerificationTokenRepository.findAll()
            .last().token

        // Verify email
        mockMvc.perform(get("/api/auth/verify").param("token", verificationToken))
            .andExpect(status().isOk)

        // Login and extract access token
        val loginResult = mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(AuthRequest(email, password)))
        )
            .andExpect(status().isOk)
            .andReturn()

        return jsonMapper.readTree(loginResult.response.contentAsString)
            .get("accessToken").textValue()
    }

    private fun createCompany(token: String, name: String): String {
        val request = CreateCompanyRequest(name = name)
        val result = mockMvc.perform(
            post("/api/companies")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        return jsonMapper.readTree(result.response.contentAsString)
            .get("id").textValue()
    }
}
