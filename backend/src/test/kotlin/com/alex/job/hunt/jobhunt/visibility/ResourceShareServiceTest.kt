package com.alex.job.hunt.jobhunt.visibility

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.dto.CreateShareRequest
import com.alex.job.hunt.jobhunt.dto.SetVisibilityRequest
import com.alex.job.hunt.jobhunt.entity.Visibility
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.InterviewNoteRepository
import com.alex.job.hunt.jobhunt.repository.InterviewRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
import com.alex.job.hunt.jobhunt.repository.PasswordResetTokenRepository
import com.alex.job.hunt.jobhunt.repository.ResourceShareRepository
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class ResourceShareServiceTest {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jsonMapper: JsonMapper
    @Autowired lateinit var interviewNoteRepository: InterviewNoteRepository
    @Autowired lateinit var interviewRepository: InterviewRepository
    @Autowired lateinit var applicationNoteRepository: ApplicationNoteRepository
    @Autowired lateinit var applicationRepository: ApplicationRepository
    @Autowired lateinit var resourceShareRepository: ResourceShareRepository
    @Autowired lateinit var jobRepository: JobRepository
    @Autowired lateinit var companyRepository: CompanyRepository
    @Autowired lateinit var tokenBlocklistRepository: TokenBlocklistRepository
    @Autowired lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    @Autowired lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository
    @Autowired lateinit var userRepository: UserRepository

    @BeforeEach
    fun setUp() {
        interviewNoteRepository.deleteAll()
        interviewRepository.deleteAll()
        applicationNoteRepository.deleteAll()
        applicationRepository.deleteAll()
        resourceShareRepository.deleteAll()
        jobRepository.deleteAll()
        companyRepository.deleteAll()
        tokenBlocklistRepository.deleteAll()
        passwordResetTokenRepository.deleteAll()
        emailVerificationTokenRepository.deleteAll()
        userRepository.deleteAll()
    }

    @Test
    fun `createShare with valid email creates share and returns ShareResponse`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, tokenA, "Shared Co")

        val request = CreateShareRequest(email = "recipient@test.com")
        mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.id").exists())
            .andExpect(jsonPath("$.email").value("recipient@test.com"))
            .andExpect(jsonPath("$.sharedAt").exists())
    }

    @Test
    fun `createShare with unknown email throws NotFoundException`() {
        val token = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, token, "My Co")

        val request = CreateShareRequest(email = "nobody@test.com")
        mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `createShare with own email throws ConflictException`() {
        val token = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, token, "My Co")

        val request = CreateShareRequest(email = "owner@test.com")
        mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `createShare duplicate throws ConflictException`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, tokenA, "Shared Co")

        val request = CreateShareRequest(email = "recipient@test.com")
        // First share succeeds
        mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)

        // Duplicate share fails
        mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isConflict)
    }

    @Test
    fun `revokeShare by owner succeeds`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, tokenA, "Shared Co")

        val request = CreateShareRequest(email = "recipient@test.com")
        val shareResult = mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val shareId = jsonMapper.readTree(shareResult.response.contentAsString).get("id").textValue()

        mockMvc.perform(
            delete("/api/companies/$companyId/shares/$shareId")
                .header("Authorization", "Bearer $tokenA")
        )
            .andExpect(status().isNoContent)
    }

    @Test
    fun `revokeShare by non-owner throws NotFoundException`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, tokenA, "Shared Co")

        val request = CreateShareRequest(email = "recipient@test.com")
        val shareResult = mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val shareId = jsonMapper.readTree(shareResult.response.contentAsString).get("id").textValue()

        // Non-owner tries to revoke
        mockMvc.perform(
            delete("/api/companies/$companyId/shares/$shareId")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `listShares returns all shares for resource`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, tokenA, "Shared Co")

        // Create share
        val request = CreateShareRequest(email = "recipient@test.com")
        mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)

        // List shares
        mockMvc.perform(
            get("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].email").value("recipient@test.com"))
    }
}
