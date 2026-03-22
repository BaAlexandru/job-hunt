package com.alex.job.hunt.jobhunt.visibility

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.dto.CreateShareRequest
import com.alex.job.hunt.jobhunt.dto.SetVisibilityRequest
import com.alex.job.hunt.jobhunt.dto.UpdateJobRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class JobVisibilityServiceTest {

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
    fun `owner can set job visibility to PUBLIC`() {
        val token = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, token, "Public Job")

        val request = SetVisibilityRequest(visibility = Visibility.PUBLIC)
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.visibility").value("PUBLIC"))
            .andExpect(jsonPath("$.isOwner").value(true))
    }

    @Test
    fun `getById returns job when user is owner`() {
        val token = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, token, "My Job")

        mockMvc.perform(
            get("/api/jobs/$jobId")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("My Job"))
            .andExpect(jsonPath("$.isOwner").value(true))
    }

    @Test
    fun `getById returns public job to non-owner`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "other@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Public Job")

        // Make public
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
        )

        // Non-owner can read
        mockMvc.perform(
            get("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Public Job"))
            .andExpect(jsonPath("$.isOwner").value(false))
    }

    @Test
    fun `getById returns shared job to share recipient`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Shared Job")

        // Set to SHARED
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.SHARED)))
        )

        // Create share
        mockMvc.perform(
            post("/api/jobs/$jobId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateShareRequest("recipient@test.com")))
        )

        // Recipient can read
        mockMvc.perform(
            get("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.title").value("Shared Job"))
            .andExpect(jsonPath("$.isOwner").value(false))
    }

    @Test
    fun `getById throws NotFoundException for private job accessed by non-owner`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "other@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Private Job")

        mockMvc.perform(
            get("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `getById throws NotFoundException for private job with existing share accessed by share recipient`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Private Shared Job")

        // Create share while PRIVATE
        mockMvc.perform(
            post("/api/jobs/$jobId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateShareRequest("recipient@test.com")))
        )

        // PRIVATE blocks non-owner even with share
        mockMvc.perform(
            get("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `update throws NotFoundException for non-owner even on public job`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "other@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Public Job")

        // Make public
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
        )

        // Non-owner cannot update
        mockMvc.perform(
            put("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenB")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(UpdateJobRequest(title = "Hijacked")))
        )
            .andExpect(status().isNotFound)
    }

    @Test
    fun `archiving job preserves associated shares`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Archivable Job")

        // Create share
        mockMvc.perform(
            post("/api/jobs/$jobId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateShareRequest("recipient@test.com")))
        )

        // Archive job
        mockMvc.perform(
            delete("/api/jobs/$jobId")
                .header("Authorization", "Bearer $tokenA")
        )
            .andExpect(status().isNoContent)

        // Shares should persist after archiving (CONTEXT.md: "Shares persist when a resource is archived")
        org.junit.jupiter.api.Assertions.assertEquals(1, resourceShareRepository.count())
    }

    @Test
    fun `changing visibility does not delete existing shares`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Shared Job")

        // Create share
        mockMvc.perform(
            post("/api/jobs/$jobId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateShareRequest("recipient@test.com")))
        )

        // Change visibility
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
        )

        // Shares still exist
        org.junit.jupiter.api.Assertions.assertEquals(1, resourceShareRepository.count())
    }

    @Test
    fun `browsePublic returns only PUBLIC jobs`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "other@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Public Job")
        TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Private Job")

        // Make one public
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
        )

        // Browse returns only public
        mockMvc.perform(
            get("/api/browse/jobs")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Public Job"))
            .andExpect(jsonPath("$.content[0].ownerEmail").value("owner@test.com"))
    }

    @Test
    fun `sharedWithMe returns only jobs shared with current user`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Shared Job")
        TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Not Shared Job")

        // Share one job
        mockMvc.perform(
            post("/api/jobs/$jobId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateShareRequest("recipient@test.com")))
        )

        // Recipient sees only shared
        mockMvc.perform(
            get("/api/shared/jobs")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Shared Job"))
            .andExpect(jsonPath("$.content[0].isOwner").value(false))
    }

    @Test
    fun `list returns only owner resources regardless of visibility`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "other@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Public Job")

        // Make public
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
        )

        // Other user's list does not include public jobs from other users
        mockMvc.perform(
            get("/api/jobs")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(0))
    }
}
