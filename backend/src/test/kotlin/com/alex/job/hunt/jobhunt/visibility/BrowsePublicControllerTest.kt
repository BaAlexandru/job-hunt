package com.alex.job.hunt.jobhunt.visibility

import com.alex.job.hunt.jobhunt.TestHelper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class BrowsePublicControllerTest {

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
    fun `GET browse companies returns only PUBLIC companies with ownerEmail`() {
        val token = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, token, "Public Co")
        TestHelper.createCompany(mockMvc, jsonMapper, token, "Private Co")

        // Make one public
        mockMvc.perform(
            patch("/api/companies/$companyId/visibility")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
        )

        mockMvc.perform(
            get("/api/browse/companies")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Public Co"))
            .andExpect(jsonPath("$.content[0].ownerEmail").value("owner@test.com"))
    }

    @Test
    fun `GET browse jobs returns only PUBLIC jobs`() {
        val token = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, token, "Public Job")
        TestHelper.createJob(mockMvc, jsonMapper, token, "Private Job")

        // Make one public
        mockMvc.perform(
            patch("/api/jobs/$jobId/visibility")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
        )

        mockMvc.perform(
            get("/api/browse/jobs")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Public Job"))
            .andExpect(jsonPath("$.content[0].ownerEmail").value("owner@test.com"))
    }

    @Test
    fun `GET browse companies with q param filters by name`() {
        val token = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val companyId1 = TestHelper.createCompany(mockMvc, jsonMapper, token, "Google")
        val companyId2 = TestHelper.createCompany(mockMvc, jsonMapper, token, "Apple")

        // Make both public
        for (id in listOf(companyId1, companyId2)) {
            mockMvc.perform(
                patch("/api/companies/$id/visibility")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(jsonMapper.writeValueAsString(SetVisibilityRequest(Visibility.PUBLIC)))
            )
        }

        mockMvc.perform(
            get("/api/browse/companies")
                .param("q", "goo")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Google"))
    }
}
