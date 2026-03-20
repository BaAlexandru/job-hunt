package com.alex.job.hunt.jobhunt.interview

import com.alex.job.hunt.jobhunt.TestHelper
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.databind.json.JsonMapper
import java.util.UUID

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
class InterviewControllerIntegrationTests {

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
    fun `should create interview with 201 and return roundNumber 1`() {
        val token = registerAndGetToken("interview-create@test.com")
        val jobId = createJob(token, "Interview Job")
        val appId = createApplication(token, jobId)

        mockMvc.perform(
            post("/api/interviews")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"applicationId":"$appId","scheduledAt":"2026-04-01T10:00:00Z","interviewType":"VIDEO","stage":"SCREENING"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.roundNumber").value(1))
            .andExpect(jsonPath("$.interviewType").value("VIDEO"))
            .andExpect(jsonPath("$.stage").value("SCREENING"))
            .andExpect(jsonPath("$.outcome").value("SCHEDULED"))
            .andExpect(jsonPath("$.result").value("PENDING"))
    }

    @Test
    fun `should auto-increment roundNumber for same application`() {
        val token = registerAndGetToken("round-inc@test.com")
        val jobId = createJob(token, "Round Job")
        val appId = createApplication(token, jobId)

        val id1 = createInterview(token, appId, "2026-04-01T10:00:00Z")
        val id2 = createInterview(token, appId, "2026-04-08T10:00:00Z")

        mockMvc.perform(
            get("/api/interviews/$id1").header("Authorization", "Bearer $token")
        ).andExpect(jsonPath("$.roundNumber").value(1))

        mockMvc.perform(
            get("/api/interviews/$id2").header("Authorization", "Bearer $token")
        ).andExpect(jsonPath("$.roundNumber").value(2))
    }

    @Test
    fun `should assign correct roundNumber after archiving an interview`() {
        val token = registerAndGetToken("round-archive@test.com")
        val jobId = createJob(token, "Archive Round Job")
        val appId = createApplication(token, jobId)

        val id1 = createInterview(token, appId, "2026-04-01T10:00:00Z")
        createInterview(token, appId, "2026-04-08T10:00:00Z")

        // Archive round 1
        mockMvc.perform(
            delete("/api/interviews/$id1").header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        // Create new interview -- should be round 3, not 2
        val id3 = createInterview(token, appId, "2026-04-15T10:00:00Z")

        mockMvc.perform(
            get("/api/interviews/$id3").header("Authorization", "Bearer $token")
        ).andExpect(jsonPath("$.roundNumber").value(3))
    }

    @Test
    fun `should return 404 when application not found`() {
        val token = registerAndGetToken("no-app@test.com")
        val randomId = UUID.randomUUID()

        mockMvc.perform(
            post("/api/interviews")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"applicationId":"$randomId","scheduledAt":"2026-04-01T10:00:00Z","interviewType":"VIDEO","stage":"SCREENING"}""")
        ).andExpect(status().isNotFound)
    }

    @Test
    fun `should list interviews by application`() {
        val token = registerAndGetToken("list-int@test.com")
        val jobId = createJob(token, "List Job")
        val appId = createApplication(token, jobId)

        createInterview(token, appId, "2026-04-01T10:00:00Z")
        createInterview(token, appId, "2026-04-08T10:00:00Z")

        mockMvc.perform(
            get("/api/interviews").param("applicationId", appId)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(2))
    }

    @Test
    fun `should exclude archived interviews from list`() {
        val token = registerAndGetToken("exclude-arch@test.com")
        val jobId = createJob(token, "Archive List Job")
        val appId = createApplication(token, jobId)

        val id1 = createInterview(token, appId, "2026-04-01T10:00:00Z")
        createInterview(token, appId, "2026-04-08T10:00:00Z")

        mockMvc.perform(
            delete("/api/interviews/$id1").header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/interviews").param("applicationId", appId)
                .header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalElements").value(1))
    }

    @Test
    fun `should get interview by id`() {
        val token = registerAndGetToken("get-int@test.com")
        val jobId = createJob(token, "Get Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId, "2026-04-01T10:00:00Z")

        mockMvc.perform(
            get("/api/interviews/$intId").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.id").value(intId))
            .andExpect(jsonPath("$.applicationId").value(appId))
            .andExpect(jsonPath("$.roundNumber").value(1))
    }

    @Test
    fun `should update interview outcome and result`() {
        val token = registerAndGetToken("update-out@test.com")
        val jobId = createJob(token, "Update Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId, "2026-04-01T10:00:00Z")

        mockMvc.perform(
            put("/api/interviews/$intId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"outcome":"COMPLETED","result":"PASSED"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.outcome").value("COMPLETED"))
            .andExpect(jsonPath("$.result").value("PASSED"))
    }

    @Test
    fun `should update interview feedback fields`() {
        val token = registerAndGetToken("update-fb@test.com")
        val jobId = createJob(token, "Feedback Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId, "2026-04-01T10:00:00Z")

        mockMvc.perform(
            put("/api/interviews/$intId")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"candidateFeedback":"Great culture","companyFeedback":"Strong candidate"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.candidateFeedback").value("Great culture"))
            .andExpect(jsonPath("$.companyFeedback").value("Strong candidate"))
    }

    @Test
    fun `should archive interview with 204`() {
        val token = registerAndGetToken("archive-int@test.com")
        val jobId = createJob(token, "Archive Job")
        val appId = createApplication(token, jobId)
        val intId = createInterview(token, appId, "2026-04-01T10:00:00Z")

        mockMvc.perform(
            delete("/api/interviews/$intId").header("Authorization", "Bearer $token")
        ).andExpect(status().isNoContent)

        mockMvc.perform(
            get("/api/interviews/$intId").header("Authorization", "Bearer $token")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.archived").value(true))
    }

    @Test
    fun `should not access other user interviews`() {
        val token1 = registerAndGetToken("user1-int@test.com")
        val jobId = createJob(token1, "Isolation Job")
        val appId = createApplication(token1, jobId)
        val intId = createInterview(token1, appId, "2026-04-01T10:00:00Z")

        val token2 = registerAndGetToken("user2-int@test.com")

        mockMvc.perform(
            get("/api/interviews/$intId").header("Authorization", "Bearer $token2")
        ).andExpect(status().isNotFound)
    }

    // --- Helpers ---

    private fun registerAndGetToken(email: String): String =
        TestHelper.registerAndGetToken(mockMvc, jsonMapper, emailVerificationTokenRepository, email)

    private fun createJob(token: String, title: String): String =
        TestHelper.createJob(mockMvc, jsonMapper, token, title)

    private fun createApplication(token: String, jobId: String): String =
        TestHelper.createApplication(mockMvc, jsonMapper, token, jobId)

    private fun createInterview(token: String, appId: String, scheduledAt: String): String =
        TestHelper.createInterview(mockMvc, jsonMapper, token, appId, scheduledAt)
}
