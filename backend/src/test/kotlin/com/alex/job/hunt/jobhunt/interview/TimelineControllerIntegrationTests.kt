package com.alex.job.hunt.jobhunt.interview

import com.alex.job.hunt.jobhunt.TestHelper
import com.alex.job.hunt.jobhunt.dto.CreateNoteRequest
import com.alex.job.hunt.jobhunt.repository.ApplicationNoteRepository
import com.alex.job.hunt.jobhunt.repository.ApplicationRepository
import com.alex.job.hunt.jobhunt.repository.CompanyRepository
import com.alex.job.hunt.jobhunt.repository.EmailVerificationTokenRepository
import com.alex.job.hunt.jobhunt.repository.InterviewNoteRepository
import com.alex.job.hunt.jobhunt.repository.InterviewRepository
import com.alex.job.hunt.jobhunt.repository.JobRepository
import com.alex.job.hunt.jobhunt.repository.PasswordResetTokenRepository
import com.alex.job.hunt.jobhunt.repository.TokenBlocklistRepository
import com.alex.job.hunt.jobhunt.repository.UserRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class TimelineControllerIntegrationTests {

    @Autowired lateinit var mockMvc: MockMvc
    @Autowired lateinit var jsonMapper: JsonMapper
    @Autowired lateinit var interviewNoteRepository: InterviewNoteRepository
    @Autowired lateinit var interviewRepository: InterviewRepository
    @Autowired lateinit var applicationNoteRepository: ApplicationNoteRepository
    @Autowired lateinit var applicationRepository: ApplicationRepository
    @Autowired lateinit var jobRepository: JobRepository
    @Autowired lateinit var companyRepository: CompanyRepository
    @Autowired lateinit var tokenBlocklistRepository: TokenBlocklistRepository
    @Autowired lateinit var passwordResetTokenRepository: PasswordResetTokenRepository
    @Autowired lateinit var emailVerificationTokenRepository: EmailVerificationTokenRepository
    @Autowired lateinit var userRepository: UserRepository
    @Autowired lateinit var redisTemplate: StringRedisTemplate

    @BeforeEach
    fun setUp() {
        interviewNoteRepository.deleteAll()
        interviewRepository.deleteAll()
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
    fun `should return empty timeline for application with no activity`() {
        val token = registerAndGetToken("empty-tl@test.com")
        val jobId = createJob(token, "Empty TL Job")
        val appId = createApplication(token, jobId)

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(0))
    }

    @Test
    fun `should return interviews in timeline`() {
        val token = registerAndGetToken("tl-interview@test.com")
        val jobId = createJob(token, "TL Interview Job")
        val appId = createApplication(token, jobId)
        createInterview(token, appId)

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("INTERVIEW"))
    }

    @Test
    fun `should return application notes in timeline`() {
        val token = registerAndGetToken("tl-appnote@test.com")
        val jobId = createJob(token, "TL AppNote Job")
        val appId = createApplication(token, jobId)
        addApplicationNote(token, appId, "Some application note")

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("APPLICATION_NOTE"))
    }

    @Test
    fun `should return interview notes in timeline`() {
        val token = registerAndGetToken("tl-intnote@test.com")
        val jobId = createJob(token, "TL IntNote Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId)
        createInterviewNote(token, intId, "Interview prep notes")

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.type == 'INTERVIEW_NOTE')]").exists())
    }

    @Test
    fun `should return all entry types sorted by date descending`() {
        val token = registerAndGetToken("tl-sorted@test.com")
        val jobId = createJob(token, "TL Sorted Job")
        val appId = createApplication(token, jobId)

        // Create entries: interview scheduled in the future, app note now, interview note now
        createInterview(token, appId, "2026-05-01T10:00:00Z")
        addApplicationNote(token, appId, "Application note")
        val intId2 = createInterview(token, appId, "2026-03-01T10:00:00Z")
        createInterviewNote(token, intId2, "Interview note")

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(4))
            // The future interview (2026-05-01) should be first (most recent)
            .andExpect(jsonPath("$[0].type").value("INTERVIEW"))
    }

    @Test
    fun `should filter timeline by type`() {
        val token = registerAndGetToken("tl-filter@test.com")
        val jobId = createJob(token, "TL Filter Job")
        val appId = createApplication(token, jobId)

        createInterview(token, appId)
        addApplicationNote(token, appId, "A note")

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .param("types", "INTERVIEW")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].type").value("INTERVIEW"))
    }

    @Test
    fun `should exclude archived interviews and their notes from timeline`() {
        val token = registerAndGetToken("tl-exclude@test.com")
        val jobId = createJob(token, "TL Exclude Job")
        val appId = createApplication(token, jobId)

        val intId = createInterview(token, appId)
        createInterviewNote(token, intId, "Note on interview to archive")

        // Archive the interview
        mockMvc.perform(
            delete("/api/interviews/$intId").header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[?(@.type == 'INTERVIEW')]").doesNotExist())
            .andExpect(jsonPath("$[?(@.type == 'INTERVIEW_NOTE')]").doesNotExist())
    }

    @Test
    fun `should return 404 for non-owned application`() {
        val token1 = registerAndGetToken("user1-tl@test.com")
        val jobId = createJob(token1, "TL Isolation Job")
        val appId = createApplication(token1, jobId)

        val token2 = registerAndGetToken("user2-tl@test.com")

        mockMvc.perform(
            get("/api/applications/$appId/timeline")
                .header("Authorization", "Bearer $token2")
        ).andExpect(status().isNotFound)
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String): String =
        TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, email)

    private fun createJob(token: String, title: String): String =
        TestHelper.createJob(mockMvc, jsonMapper, token, title)

    private fun createApplication(token: String, jobId: String): String =
        TestHelper.createApplication(mockMvc, jsonMapper, token, jobId)

    private fun createInterview(token: String, appId: String, scheduledAt: String = "2026-04-01T10:00:00Z"): String =
        TestHelper.createInterview(mockMvc, jsonMapper, token, appId, scheduledAt)

    private fun createInterviewNote(token: String, intId: String, content: String): String =
        TestHelper.createInterviewNote(mockMvc, jsonMapper, token, intId, content)

    private fun addApplicationNote(token: String, appId: String, content: String) {
        val request = CreateNoteRequest(content = content)
        mockMvc.perform(
            post("/api/applications/$appId/notes")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
    }
}
