package com.alex.job.hunt.jobhunt.visibility

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.dto.CreateShareRequest
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class SharedWithMeControllerTest {

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
    fun `GET shared companies returns items shared with authenticated user`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val companyId = TestHelper.createCompany(mockMvc, jsonMapper, tokenA, "Shared Co")

        // Share company
        mockMvc.perform(
            post("/api/companies/$companyId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateShareRequest("recipient@test.com")))
        )

        // Recipient sees shared companies
        mockMvc.perform(
            get("/api/shared/companies")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].name").value("Shared Co"))
            .andExpect(jsonPath("$.content[0].isOwner").value(false))
    }

    @Test
    fun `GET shared jobs returns items shared with authenticated user`() {
        val tokenA = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "owner@test.com")
        val tokenB = TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, "recipient@test.com")
        val jobId = TestHelper.createJob(mockMvc, jsonMapper, tokenA, "Shared Job")

        // Share job
        mockMvc.perform(
            post("/api/jobs/$jobId/shares")
                .header("Authorization", "Bearer $tokenA")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(CreateShareRequest("recipient@test.com")))
        )

        // Recipient sees shared jobs
        mockMvc.perform(
            get("/api/shared/jobs")
                .header("Authorization", "Bearer $tokenB")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.content[0].title").value("Shared Job"))
            .andExpect(jsonPath("$.content[0].isOwner").value(false))
    }
}
